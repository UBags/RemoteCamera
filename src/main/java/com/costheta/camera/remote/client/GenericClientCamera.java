package com.costheta.camera.remote.client;

import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.tesseract.CosThetaTesseract;
import com.costheta.tesseract.CosThetaTesseractHandlePool;
import com.costheta.tesseract.TesseractInitialiser;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;

public abstract class GenericClientCamera extends BaseCamera implements RemoteShutdown {

    private static final Logger logger = LogManager.getLogger(GenericClientCamera.class);

    private static final String nameString = "Camera Name : ";
    private static final String idString = "Camera Id : ";
    private static final String semicolon = "; ";

    private static final String TESSERACT_DATAPATH_KEY = "tesseract.datapath";

    protected volatile boolean isActive;
    protected volatile boolean newPictureReceived = false;

    protected static int pictureCount = 1;

    public GenericClientCamera(String cameraName, int cameraID) {
        super(cameraName, cameraID);
        logger.trace("After calling super() in constructor");
        TesseractInitialiser.initialise(); // load lept and tess libs
        String tesseractDatapath = System.getProperty(TESSERACT_DATAPATH_KEY);
        if (tesseractDatapath == null) {
            tesseractDatapath = System.getProperty("user.dir") + "/tesseract";
        }
        CosThetaTesseract.changeDatapath(tesseractDatapath);
        CosThetaTesseractHandlePool.initialise(); // create Tesseract objects
        ImageUtils.initialise(); // initialise the tables in ImageUtils
        logger.trace("Exiting constructor()");
    }

    public BufferedImage getCurrentImage() {
        logger.trace("Entering getCurrentImage()");
        if (currentImage != null) {
            setNewPictureReceived(false);
        }
        logger.trace("Exiting getCurrentImage()");
        return currentImage;
    }

    public void setCurrentImage(BufferedImage image) {
        logger.trace("Entering setCurrentImage() with " + image);
        if (image != null) {
            setNewPictureReceived(true);
        }
        this.currentImage = image;
        logger.trace("Exiting setCurrentImage()");
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(nameString);
        builder.append(getCameraName());
        builder.append(semicolon);
        builder.append(idString);
        builder.append(getCameraID());
        return builder.toString();
    }

    public final Image convertImage(BufferedImage image) {
        logger.trace("Entering convertImage() with " + image);
        Image nextImage = SwingFXUtils.toFXImage(image,null);
        return nextImage;
    }

    public boolean isActive() {
        logger.trace("Entering isActive()");
        return isActive;
    }

    public void setActive(boolean active) {
        logger.trace("Entering setActive() with " + active);
        logger.trace("Entering setActive() with " + active);
        this.isActive = active;
    }

    public abstract boolean requestImage();

    public abstract boolean connectCamera();

    public boolean isNewPictureReceived() {
        logger.trace("Entering isNewPictureReceived()");
        return newPictureReceived;
    }

    public void setNewPictureReceived(boolean newPictureReceived) {
        logger.trace("Entering setNewPictureReceived() with " + newPictureReceived);
        this.newPictureReceived = newPictureReceived;
    }

    protected void saveImage() {
        saveImage(currentImage, getImageDirectory() + "/" + pictureCount++ + ".jpg");
    }

}
