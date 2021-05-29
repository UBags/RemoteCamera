package com.costheta.camera.imagemessages;

import com.costheta.utils.GeneralUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageBytes {

    private static final Logger logger = LogManager.getLogger(ImageBytes.class);
    public static final BufferedImage EMPTY_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);

    public static final ImageBytes RESPONSE_TO_UNKNOWN_REQUEST = new ImageBytes(new byte[0]);
    public static final ImageBytes EMPTY_BYTES = new ImageBytes(getImageBytes(EMPTY_IMAGE));

    public byte[] imageBytes;

    public ImageBytes() {

    }

    public void setBytes(byte[] bytes) {
        this.imageBytes = bytes;
        // this.imageBytes = GeneralUtils.compress(bytes);
    }

    public ImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
        // this.imageBytes = GeneralUtils.compress(imageBytes);
    }

    private static final ImageBytes getEmptyImageBytes() {
        logger.trace("Entering getEmptyImageBytes()");
        return RESPONSE_TO_UNKNOWN_REQUEST;
    }

    public static final boolean isEmptyImageBytes(ImageBytes response) {
        logger.trace("Entering isEmptyImageBytes() with " + response);
        // byte[] bytes = GeneralUtils.decompress(response.imageBytes);
        byte[] bytes = response.imageBytes;
        if (bytes.length == 0) {
            return true;
        }
        return false;
    }

    public static final byte[] getImageBytes(BufferedImage image){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", baos);
        } catch (Exception e) {

        }
        return baos.toByteArray();
    }

    public static BufferedImage getBufferedImage(ImageBytes imageBytes) {
        byte[] properBytes = imageBytes.imageBytes;
        // byte[] properBytes = GeneralUtils.decompress(imageBytes.imageBytes);
        InputStream is = new ByteArrayInputStream(properBytes);
        BufferedImage image = null;
        try {
            image = ImageIO.read(is);
        } catch (Exception e) {
            return EMPTY_IMAGE;
        }
        return image;
    }

    public String toString() {
        return new StringBuilder().append(imageBytes.length).toString();
    }

}
