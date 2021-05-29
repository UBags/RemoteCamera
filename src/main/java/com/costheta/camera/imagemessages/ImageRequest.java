package com.costheta.camera.imagemessages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ImageRequest {

    private static final Logger logger = LogManager.getLogger(ImageRequest.class);

    private static final String SEND_PICTURE = "Send Picture";
    private static final String SHUTDOWN = "Shutdown";
    private static final String EMPTY_STRING = "Empty Request";

    public static final ImageRequest DEFAULT_REQUEST = getImageRequest(SEND_PICTURE);
    public static final ImageRequest SEND_PICTURE_REQUEST = getImageRequest(SEND_PICTURE);
    public static final ImageRequest SHUTDOWN_REQUEST = getImageRequest(SHUTDOWN);
    public static final ImageRequest EMPTY_REQUEST = getImageRequest(EMPTY_STRING);

    private String request;

    protected ImageRequest() {

    }

    public static final ImageRequest getImageRequest(String request) {
        logger.trace("Creating an ImageRequest in getImageRequest() with " + request);
        if (EMPTY_STRING.equals(request)) {
            return DEFAULT_REQUEST;
        }
        if (request == null) {
            return DEFAULT_REQUEST;
        }
        ImageRequest imageRequest = new ImageRequest();
        imageRequest.request = request;
        logger.trace("Exiting getImageRequest()");
        return imageRequest;
    }

    public final boolean isImageRequestForNextPicture() {
        logger.trace("Entering isImageRequestForNextPicture()");
        return SEND_PICTURE.equals(request);
    }

    public final boolean isShutdownRequest() {
        logger.trace("Entering isShutdownRequest()");
        return SHUTDOWN.equals(request);
    }

    @Override
    public String toString() {
        return request;
    }
    
}
