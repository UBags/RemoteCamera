package com.costheta.camera.remote.image;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

public class DefaultImages {

    private static final Logger logger = LogManager.getLogger(DefaultImages.class);

    public static final BufferedImage EMPTY_BUFFERED_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
    public static byte[] EMPTY_BYTES;
    static {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(EMPTY_BUFFERED_IMAGE, "jpg", baos);
            EMPTY_BYTES = baos.toByteArray();
        } catch (Exception e) {

        }

    }

    public static final Image EMPTY_IMAGE = SwingFXUtils.toFXImage(EMPTY_BUFFERED_IMAGE, null);

    public static final Hashtable<String, BufferedImage[]> DEFAULT_BUFFERED_IMAGES = new Hashtable<>();

    public static final Hashtable<String, Image[]> DEFAULT_IMAGES = new Hashtable<>();

    public static final void createImages(Hashtable<Integer, Integer> screenCombinations) {
        logger.trace("Entering createImages()");

        for (Map.Entry<Integer, Integer> entry : screenCombinations.entrySet()) {
            int width = entry.getKey();
            int height = entry.getValue();
            String key = "" + width + "x" + height;
            BufferedImage[] bufferedImages = new BufferedImage[12];
            Image[] images = new Image[12];
            for (int i = 0; i < bufferedImages.length; ++i) {
                bufferedImages[i] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = bufferedImages[i].createGraphics();
                graphics.setColor(new Color(i < 4 ? 40 + (int) (160 * i * 1.0 / 4) : 255, i > 7 ? 40 + (int) (160 * (i - 7) * 1.0 / 4) : 255, 40 + (int) (160 * i * 1.0 / 12)));
                graphics.fillRect(0, 0, 240, 160);
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font("Arial Black", Font.ITALIC, 18));
                graphics.drawString("Dummy Image " + (i + 1), 40, 90);
                graphics.dispose();
                images[i] = SwingFXUtils.toFXImage(bufferedImages[i], null);
            }
            DEFAULT_BUFFERED_IMAGES.put(key, bufferedImages);
            DEFAULT_IMAGES.put(key, images);
        }
    }

    public static final BufferedImage getNextRandomBufferedImage(int width, int height) {
        String key = "" + width + "x" + height;
        BufferedImage[] bufferedImages = DEFAULT_BUFFERED_IMAGES.get(key);
        if (bufferedImages == null) {
            return EMPTY_BUFFERED_IMAGE;
        }
        int random = new Random().nextInt(12);
        return bufferedImages[random];
    }

    public static final Image getNextRandomImage(int width, int height) {
        String key = "" + width + "x" + height;
        Image[] images = DEFAULT_IMAGES.get(key);
        if (images == null) {
            return EMPTY_IMAGE;
        }
        int random = new Random().nextInt(12);
        return images[random];
    }

}
