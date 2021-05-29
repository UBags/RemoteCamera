package com.costheta.machine;

import com.costheta.camera.processor.ClipImageProcessor;
import com.costheta.camera.processor.ImageProcessor;
import com.costheta.camera.processor.Inspector;
import com.costheta.camera.processor.ResultEvaluator;
import com.costheta.camera.remote.client.GenericClientCamera;
import com.costheta.camera.remote.client.NetworkClientCamera;
import com.costheta.camera.remote.client.RemoteShutdown;
import com.costheta.camera.remote.image.DefaultImages;
import com.costheta.image.CosThetaImageView;
import com.costheta.image.utils.ImageUtils;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.shape.Rectangle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class InspectionPoint implements MachiningNode, Inspector, ResultEvaluator, RemoteShutdown {

    private static final Logger logger = LogManager.getLogger(InspectionPoint.class);
    private static final String SAVE_IMAGES_KEY = "save.original.client";

    private MachiningNode parent;

    private String name;
    private NetworkClientCamera camera;
    private boolean saveImage = false;

    public CosThetaImageView getImageView() {
        return imageView;
    }

    private CosThetaImageView imageView;
    private BufferedImage currentImage;

    private int imageWidth;
    private int imageHeight;

    private ArrayList<ImageProcessor> imageProcessors = new ArrayList<>();
    private ArrayList<ProcessingResult> currentResults = new ArrayList<>();

    private String baseStringPathOfIPoint = null;
    private Path basePathOfIPoint = null;
    private int pictureCount = 1;

    public Path getBasePathOfIPoint() {
        return basePathOfIPoint;
    }

    public void setBasePathOfIPoint(Path basePathOfIPoint) {
        if (this.basePathOfIPoint != null) {
            return;
        }
        this.basePathOfIPoint = basePathOfIPoint;
        this.baseStringPathOfIPoint = this.basePathOfIPoint.toAbsolutePath().toString();
    }

    public String getBaseStringPathOfIPoint() {
        return baseStringPathOfIPoint;
    }

    public InspectionPoint(String name, NetworkClientCamera camera) {
        logger.trace("Entering constructor()");
        if ((name == null) || ("".equals(name))) {
            throw new IllegalArgumentException("Every Inspection Point needs to have a name");
        }
        if (camera == null) {
            throw new IllegalArgumentException("An Inspection Point cannot be created without a valid camera");
        }
        this.name = name;
        this.camera = camera;
        String saveIms = System.getProperty(SAVE_IMAGES_KEY, "false");
        saveImage = Boolean.parseBoolean(saveIms);
        logger.trace("Leaving constructor()");
    }

    public void setImageDimensions() {
        this.imageWidth = ((Part) parent).getIndividualImageWidth();
        this.imageHeight = ((Part) parent).getIndividualImageHeight();
        imageView = new CosThetaImageView(this.imageWidth, this.imageHeight, null);
    }

    public String getName() {
        return name;
    }

    public GenericClientCamera getCamera() {
        return camera;
    }

    public BufferedImage getCurrentImage() {
        return camera.getCurrentImage();
    }

    public boolean isCameraAlive() {
        logger.trace("Entering isCameraAlive()");
        return camera.isActive();
    }

    public boolean connectCamera() {

        logger.trace("Entering connectCamera()");
        return camera.connectCamera();
    }

//    public void showImage() {
//        BufferedImage currentImage = camera.getCurrentImage();
//        showImage(currentImage);
//    }

    // this method is to be called after the InspectionPoint has updated the
    // input image with the results obstained from inspection
    public void showImage(BufferedImage image) {
        logger.trace("Entering showImage() with " + image);
        if (image == null) {
            System.out.println("Image is null. Hence, getting random image");
            imageView.setImage(DefaultImages.getNextRandomImage(imageWidth, imageHeight));
            return;
        }
        Image convertedImage = camera.convertImage(image);
        imageView.setImage(convertedImage);
        imageView.requestLayout();
    }

    public void showImage() {
        logger.trace("Entering no-argument showImage()");
        if (currentImage == null) {
            logger.trace("Current image is null. Hence, getting random image");
            imageView.setImage(DefaultImages.getNextRandomImage(imageWidth, imageHeight));
            return;
        }
        Image convertedImage = camera.convertImage(currentImage);
        imageView.setImage(convertedImage);
    }

    public void addImageProcessor(ImageProcessor imageProcessor) {
        if (parent == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("In Inspecion Point : ").append(getName()).append(" addImageProcessor() - ");
            sb.append("An Inspection Point needs to have a parent Part before one can add an ImageProcessor to it.");
            logger.trace(sb.toString());
            throw new IllegalStateException(sb.toString());
        }
        logger.trace("Entering addImageProcessor() with " + imageProcessor);
        if (imageProcessors.contains(imageProcessor)) {
            logger.info("Attempt to add same processor " + imageProcessor + " again to the Inspection Point " + this + ". Did not add.");
            return;
        }
        for (ImageProcessor processor : imageProcessors) {
            BaseImageProcessor bProcessor = (BaseImageProcessor) processor;
            if (bProcessor.getProcessorName() != null) {
                if (bProcessor.getProcessorName().equals(((BaseImageProcessor) imageProcessor).getProcessorName())) {
                    logger.error("Attempt to add a processor with duplicate name " + bProcessor.getProcessorName() + " to the Inspection Point " + this + ". ImageProcessors associated with an Inspection Point need to have separate names. Did not add the processor.");
                    return;
                }
            }
        }
        imageProcessors.add(imageProcessor);
        imageProcessor.setParent(this);
    }

    @Override
    public ArrayList<ProcessingResult> inspect() {
        Instant start = Instant.now();
        logger.debug("Entering inspect()");
        currentResults.clear();
        boolean connected = isCameraAlive();
        if (!connected) {
            connected = connectCamera();
        }
        if (connected) {
            boolean successfulRequest = camera.requestImage();
            if (!successfulRequest) {
                // try again
                successfulRequest = camera.requestImage();
            }
            if (!successfulRequest) {
                currentResults.add(ProcessingResult.EMPTY_PROCESSING_RESULT);
                logger.info("In inspect() : result of requestImage() is " + successfulRequest);
                return currentResults;
            }
            logger.info("In inspect() : result of requestImage() is " + successfulRequest);
            int counter = 0;
            while (!camera.isNewPictureReceived()) {
                try {
                    TimeUnit time = TimeUnit.MILLISECONDS;
                    time.sleep(200);
                    ++counter;
                    if (counter > 150) { // wait for 30 seconds to get and process image
                        break;
                    }
                } catch (Exception e) {

                }
            }
        }
        Instant picReceivedTime = Instant.now();
        if (!camera.isNewPictureReceived()) {
            // if no picture received after even 20 seconds, return empty processing result
            currentResults.add(ProcessingResult.EMPTY_PROCESSING_RESULT);
            return currentResults;
        }
        currentImage = camera.getCurrentImage();
        new Thread() {
            public void run() {
                if (saveImage) {
                    saveCurrentImage(pictureCount, false);
                }
            }
        }.start();
        showImage(currentImage);
        for (int i = 0; i < imageProcessors.size(); ++i) {
            currentResults.add(imageProcessors.get(i).process(currentImage));
        }
        BufferedImage processedImage = updateImage(currentImage, currentResults);
        new Thread() {
            public void run() {
                if (saveImage) {
                    saveProcessedImage(pictureCount, false);
                }
            }
        }.start();
        showImage(processedImage);
        ++pictureCount;
        Instant postProcessingTime = Instant.now();
        logger.info("Received image in " + Duration.between(start, picReceivedTime).toMillis() + " ms and processed image in " + Duration.between(picReceivedTime, postProcessingTime).toMillis() + " ms");
        return currentResults;
    }

    @Override
    public BufferedImage updateImage(BufferedImage inputImage, ArrayList<ProcessingResult> result) {
        if (inputImage == null) {
            return null;
        }
        logger.trace("Entering updateImage()");
        ArrayList<Rectangle> clipBoxes = new ArrayList<Rectangle>();
        for (ImageProcessor processor : imageProcessors) {
            ClipImageProcessor ciProcessor = (ClipImageProcessor) processor;
            ArrayList<Rectangle> clips = ciProcessor.getClipBoxRectangles();
            for (Rectangle aRec : clips) {
                clipBoxes.add(aRec);
            }
        }
        BufferedImage output = ImageUtils.copyBI(inputImage);
        Color color = Color.ORANGE;
        for (Rectangle clip : clipBoxes) {
            output = ImageUtils.drawRectangle(output, clip, color);
        }
        // To be implemented
        return output;
    }

    public ArrayList<ProcessingResult> getCurrentResults() {
        return currentResults;
    }

    @Override
    public EvaluatedResult evaluateResult(ArrayList<ProcessingResult> inputResults) {
        return null;
    }

    @Override
    public void shutdownServer() {
        logger.warn("Entering shutdownServer()");
        camera.shutdownServer();
    }

    @Override
    public void close() {
        logger.warn("Entering close()");
        camera.close();
    }

    public ArrayList<ImageProcessor> getImageProcessors() {
        return imageProcessors;
    }

    public void saveCurrentImage(int currentPictureCount, boolean incrementCount) {
        ImageUtils.writeFile(currentImage, "jpg", baseStringPathOfIPoint + "/" + currentPictureCount + "-original.jpg");
        if (incrementCount) {
            ++pictureCount;
        }
    }

    public void saveProcessedImage(int currentPictureCount, boolean incrementCount) {
        ImageUtils.writeFile(currentImage, "jpg", baseStringPathOfIPoint + "/" + currentPictureCount + "-processed.jpg");
        if (incrementCount) {
            ++pictureCount;
        }
    }

    public MachiningNode getParent() {
        return parent;
    }

    public void setParent(MachiningNode machiningNode) {
        if (parent == null) {
            this.parent = machiningNode;
            setImageDimensions();
        }
    }

    public ArrayList<MachiningNode> getChildren() {
        ArrayList<MachiningNode> children = new ArrayList<>();
        for (MachiningNode node : imageProcessors) {
            children.add(node);
        }
        return children;
    }

}
