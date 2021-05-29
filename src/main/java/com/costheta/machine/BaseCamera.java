package com.costheta.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseCamera extends BaseApplication {

    public static final int JPEG = 1;
    public static final int PNG = 2;

    public static Toolkit defaultToolKit = Toolkit.getDefaultToolkit();
    public static int screenWidth = defaultToolKit.getScreenSize().width;
    public static int screenHeight = defaultToolKit.getScreenSize().height;
    public static double screenRatio = screenHeight * 1.0 / screenWidth;

    private static final Logger logger = LogManager.getLogger(BaseCamera.class);

    protected String cameraName;
    protected int cameraID;
    protected BufferedImage currentImage;

    protected String imageDirectory = "pictures";
    public String getImageDirectory() {
        return imageDirectory;
    }
    public void setImageDirectory(String imageDirectory) {
        this.imageDirectory = imageDirectory;
    }

    protected Path imageDirectoryPath = null;
    public Path getImageDirectoryPath() {
        return imageDirectoryPath;
    }
    public void setImageDirectoryPath(Path imageDirectoryPath) {
        this.imageDirectoryPath = imageDirectoryPath;
    }

    public static final String CAMERA_ID_KEY = "camera.Id";

    protected BaseCamera() {

    }

    public BaseCamera(String cameraName, int cameraID) {
        logger.trace("Entering constructor() with cameraName : " + cameraName + ", camera Id : " + cameraID);
        this.cameraName = cameraName;
        this.cameraID = cameraID;
        initialise(initialisationArgs);
    }

    public static void initialise(String[] args) {
        logger.trace("Entering initialise() with arguments " + args);
        // get the cameraId, then delegate rest of the initialisation to the superclass
        String cameraIDInput =  getValueOfArguments(args, CAMERA_ID_KEY);
        if (!EMPTY_STRING.equals(cameraIDInput)) {
            logger.trace("A camera Id has been provided, which is " + cameraIDInput);
            try {
                int cameraID = Integer.parseInt(cameraIDInput);
                if (cameraID <= 0) {
                    System.out.println(serverErrorStatement);
                    System.out.println(separatorLine);
                    System.setProperty(CAMERA_ID_KEY,"0");
                } else {
                    System.setProperty(CAMERA_ID_KEY,cameraIDInput);
                }
            } catch (NumberFormatException nfe) {
                System.out.println(serverErrorStatement);
                System.out.println(separatorLine);
                System.setProperty(CAMERA_ID_KEY,"0");
            }
        }
        // super.initialise(args);
        BaseApplication.initialise(args);
        logger.trace("Base directory is " + System.getProperty(BASE_DIR_KEY));
        System.out.println("Base directory is " + System.getProperty(BASE_DIR_KEY));
    }

    // needed for RemoteCameras to be able to save images
    protected void createImageSubdirectory() {
        logger.trace("Entering createImageSubdirectory()");
        if (imageDirectoryPath != null) {
            return;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String tempDirName = dtf.format(now);
        tempDirName = tempDirName.replace("/","");
        tempDirName = tempDirName.replace(" ","");
        tempDirName = tempDirName.replace(":","");
        imageDirectoryPath = Paths.get(getBaseDir(), imageDirectory + "/" + tempDirName);
        try {
            Files.createDirectories(imageDirectoryPath);
        } catch (FileAlreadyExistsException e) {
            // do nothing
        } catch (IOException e) {
            // something else went wrong
            logger.info(e.getMessage());
        }
        imageDirectory = imageDirectoryPath.toAbsolutePath().toString();
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraID(int cameraID) {
        this.cameraID = cameraID;
    }

    public int getCameraID() {
        return cameraID;
    }

    public BufferedImage getCurrentImage() {
        logger.trace("Entering getCurrentImage()");
        return currentImage;
    }

    public void setCurrentImage(BufferedImage image) {
        currentImage = image;
    }

    public void saveImage(BufferedImage image, String imageFilePath) {
        logger.trace("Entering saveImage() with " + image + " and " + imageFilePath);
        try {
            ImageIO.write(image, "jpg", new File(imageFilePath));
        } catch (IOException ioe) {

        }
    }

}
