package com.costheta.tests;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import javafx.scene.shape.Rectangle;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestOtsu1 {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\Test pictures";
    private static String debugDirectory = inputDirectory + "/debug/";
    private static String ext = "png";

    public static void main(String[] args) throws TesseractException, IOException {

        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        List<String> files = ImageUtils.filterFiles(ImageUtils.listFiles(inputDirectory, 1), ext, true);
        Instant t = Instant.now();
        for (String filePathString : files) {
            Path filePath = Paths.get(filePathString);
            String fileNameWithExt = filePath.getFileName().toString();
            System.out.println("Processing " + fileNameWithExt);
            System.out.println("----------------------------");
            int indexOfDot = fileNameWithExt.indexOf(".");
            String fileName = fileNameWithExt.substring(0,indexOfDot);
            BufferedImage image = ImageIO.read(new File(filePathString));
            int average = ImageUtils.getAverage(image);
            double requiredGamma = ImageUtils.getGamma(average);
            double edgeGamma = requiredGamma;
            BufferedImage geImage = ImageUtils.constantGammaEnhancement(image, edgeGamma);
            geImage = ImageUtils.stretchHistogram(geImage);
            // ImageUtils.writeFile(image, "png", debugDirectory + fileName + "-" + "original.png");
            Pix pix = ImageUtils.getDepth8Pix(geImage);
            Pix normBackPix = Leptonica1.pixBackgroundNormFlex(pix, 7, 7, 2, 2, 160);
            Pix contNormPix = Leptonica1.pixContrastNorm(null, normBackPix, normBackPix.w / 2, normBackPix.h / 4, 60, 2, 2);
            // BufferedImage nbImageOriginal = ImageUtils.convertPixToImage(pix);
            BufferedImage nbImageOriginal = ImageUtils.convertPixToImage(normBackPix); // contNormPix
            ImageUtils.writeFile(geImage, "png", debugDirectory + fileName + "-" + "original.png");
            BufferedImage medianFilterImage = ImageUtils.medianFilter(nbImageOriginal, 3, 3);
            // ImageUtils.writeFile(medianFilterImage, "png", debugDirectory + fileName + "-" + "medianFilter.png");
            // nbImage = ImageUtils.medianFilter(nbImage, 3, 3);
            BufferedImage sobelEdgeImage = ImageUtils.findEdgesAggressively(medianFilterImage, true);
            sobelEdgeImage = ImageUtils.invert(sobelEdgeImage);
            // ImageUtils.writeFile(sobelEdgeImage, "png", debugDirectory + fileName + "A-" + "sobelEdgesRaw.png");
            sobelEdgeImage = ImageUtils.binarize(sobelEdgeImage, 64); // conservative
            ImageUtils.writeFile(sobelEdgeImage, "png", debugDirectory + fileName + "A-" + "sobelEdges.png");
            GrayU8 gray = ConvertBufferedImage.convertFrom(medianFilterImage, (GrayU8) null);
            GrayU8 edgeGray = gray.createSameShape();

            // Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
            // It has also been configured to save the trace as a graph.  This is the graph created while performing
            // hysteresis thresholding.
            CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(1, true, true, GrayU8.class, GrayS16.class);
            // The edge image is actually an optional parameter.  If you don't need it just pass in null
            canny.process(gray, 0.1f, 0.3f, edgeGray);
            BufferedImage edgeImage = new BufferedImage(edgeGray.width, edgeGray.height, BufferedImage.TYPE_INT_ARGB);
            edgeImage = ConvertBufferedImage.convertTo(edgeGray, null);
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + "originalEdgeImage.png");
            edgeImage = ImageUtils.fullImageGammaEnhancementWithBias(edgeImage);
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + "originalEdgeGE.png");
            edgeImage = ImageUtils.invert(edgeImage);
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + "edgeImage_GE.png");
            average = ImageUtils.getAverage(edgeImage);
            edgeImage = ImageUtils.binarize(edgeImage, average);
            ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "B-" + "originalEdge.png");
            edgeImage = ImageUtils.imageBinaryAnd(edgeImage, sobelEdgeImage);
            ArrayList<CleaningKernel> cleaningKernels = new ArrayList<>();
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(3,5));
            cleaningKernels.add(new CleaningKernel(5,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(edgeImage.getWidth() / 3,5));
            cleaningKernels.add(new CleaningKernel(edgeImage.getWidth() / 3,5));
            cleaningKernels.add(new CleaningKernel(5,edgeImage.getHeight() / 2));
            cleaningKernels.add(new CleaningKernel(5,edgeImage.getHeight() / 2));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(11,3));
            edgeImage = ImageUtils.removeSmallTrails(edgeImage,cleaningKernels,true);
            ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "C-" + "combinedEdgeCleaned.png");
            Rectangle characterBounds = ImageUtils.getBoundingBoxEnvelope(edgeImage, 20, 21);
            // expand the bounds of the rectangle by 1 pixel up and down and 2 pixels to the left and right
            characterBounds = new Rectangle(characterBounds.getX() - 2, characterBounds.getY() - 1, characterBounds.getWidth() + 4, characterBounds.getHeight() + 2);
            BufferedImage relevantEdges = ImageUtils.retainClip(edgeImage, characterBounds);
            ImageUtils.writeFile(relevantEdges, "png", debugDirectory + fileName + "D-" + "relevantEdges.png");
            ImageUtils.writeFile(geImage, "png", debugDirectory + fileName + "E-" + "originalGE.png");
            // BufferedImage regionOfInterest = ImageUtils.clipBI(geImage, characterBounds);
            BufferedImage regionOfInterest = ImageUtils.clipBI(image, characterBounds);
            ImageUtils.writeFile(regionOfInterest, "png", debugDirectory + fileName + "F-" + "originalGE.png");
            BufferedImage gammaCorrectedRoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(regionOfInterest, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 50, 242);
            ImageUtils.writeFile(gammaCorrectedRoI, "png", debugDirectory + fileName + "G-" + "gammaCorrectedRoI.png");
            // BufferedImage otsuImage = OtsuThreshold.getLocalOtsuImageStandard(nbImageOriginal, (nbImageOriginal.getWidth() + 1) / 2, (nbImageOriginal.getHeight() + 1) / 2);
            // BufferedImage gammaCorrectedOriginal = ImageUtils.relativeGammaContrastEnhancementWithPercentTolerance(geImage, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 5);
            // BufferedImage gammaCorrectedOriginal = ImageUtils.relativeGammaContrastEnhancement(geImage, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 75);
            // ImageUtils.writeFile(gammaCorrectedOriginal, "png", debugDirectory + fileName + "G-" + "otsuInput.png");
            // BufferedImage otsuImage = OtsuThreshold.getLocalOtsuImageStandard(gammaCorrectedOriginal, (geImage.getWidth() + 1) / 2, (geImage.getHeight() + 1) / 2);
            // BufferedImage otsuImage = ImageUtils.binarize(gammaCorrectedOriginal, 8);
            // ImageUtils.writeFile(nbImageOriginal, "png", debugDirectory + fileName + "F-" + "otsuInput.png");
            BufferedImage otsuImage = ImageUtils.binarize(gammaCorrectedRoI, 16);
            ImageUtils.writeFile(otsuImage, "png", debugDirectory + fileName + "H-" + "otsuImage.png");
            BufferedImage finalImage = ImageUtils.retainClip(otsuImage, characterBounds);
            ImageUtils.writeFile(finalImage, "png", debugDirectory + fileName + "I-" + "finalImage.png");
            LeptUtils.dispose(pix);
            LeptUtils.dispose(normBackPix);
            LeptUtils.dispose(contNormPix);
        }
        long timeElapsed = Duration.between(t, Instant.now()).toMillis();
        System.out.println("Time taken per image = " + timeElapsed / (files.size() > 0 ? files.size() : 1));
        System.out.println("Done");
    }



}
