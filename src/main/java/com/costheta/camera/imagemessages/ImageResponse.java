package com.costheta.camera.imagemessages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageResponse {

    private static final Logger logger = LogManager.getLogger(ImageResponse.class);

    private int imageWidth;
    private int imageHeight;
    private byte[] imageData;

    private static final String imageWidthText = "Image Width : ";
    private static final String imageHeightText = "Image Height : ";
    private static final String imageDataLengthText = "Image Data Length : ";
    private static final String delimiter = ";";

    public static final BufferedImage EMPTY_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);

    public static final ImageResponse RESPONSE_TO_UNKNOWN_REQUEST = getImageResponse(EMPTY_IMAGE);

    public static final ImageResponse EMPTY_IMAGE_RESPONSE = getEmptyImageResponse();

    protected ImageResponse() {

    }

    public static final ImageResponse getImageResponse(BufferedImage image) {
        logger.trace("Creating an ImageResponse in getImageResponse() with " + image);
        ImageResponse response = new ImageResponse();
        response.imageWidth = image.getHeight() * image.getWidth();
        response.imageHeight = image.getHeight();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2097152);
        try {
            ImageIO.write(image, "jpg", baos);
        } catch (Exception e) {
            System.out.println("ImageResponse.getImageResponse() : Couldn't write BufferedImage to ByteArrayOutputStream");
        }
        response.imageData = baos.toByteArray();
        // response.imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        // System.out.println("byte[] length of image is " + response.imageData.length);
        return response;
    }

    public static final BufferedImage getBufferedImage(ImageResponse response) {
        logger.trace("Entering getBufferedImage() with " + response);
//        BufferedImage image = new BufferedImage(response.imageSize / response.imageHeight, response.imageHeight,
//                BufferedImage.TYPE_INT_RGB);
//        WritableRaster raster = image.getRaster();
//        raster.setPixels(0, 0, image.getWidth(), image.getHeight(), response.imageData);
        InputStream is = new ByteArrayInputStream(response.imageData);
        BufferedImage image = null;
        try {
            image = ImageIO.read(is);
        } catch (Exception e) {
            return EMPTY_IMAGE;
        }
        // System.out.println("Created BufferedImage on client : " + image + " Width : " + image.getWidth() + " Height " + image.getHeight());
        return image;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(imageWidthText);
        buffer.append(imageWidth);
        buffer.append(delimiter);
        buffer.append(imageHeightText);
        buffer.append(imageHeight);
        buffer.append(delimiter);
        buffer.append(imageDataLengthText);
        buffer.append(imageData.length);
        return buffer.toString();
    }

    private static final ImageResponse getEmptyImageResponse() {
        logger.trace("Entering getEmptyImageResponse()");
        ImageResponse response = new ImageResponse();
        response.imageWidth = 0;
        response.imageHeight = 0;
        response.imageData = new byte[0];
        return response;
    }

    public static final boolean isEmptyResponse(ImageResponse response) {
        logger.trace("Entering isEmptyResponse() with " + response);
        if ((response.imageHeight == 0) && (response.imageWidth == 0) && (response.imageData.length == 0)) {
            return true;
        }
        return false;
    }

}
