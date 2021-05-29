package com.costheta.tests;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class TestImageCopyAlternatives {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\KEMS\\Pictures";
    private static String fileName = "6-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    public static void main(String[] args) {

        System.out.println("Testing ImageCopyAlternatives");

        BufferedImage original = null;
        try {
            original = ImageIO.read(new File(inputDirectory + "/" + fileName));
        } catch (Exception e) {

        }

        System.out.println("Image dimensions are : " + original.getWidth() + " and " + original.getHeight());
        BufferedImage copy = null;

        Rectangle clip = new Rectangle(0, 0, original.getWidth(), original.getHeight());
        int repeats = 1000;
        Instant before = Instant.now();
        for (int i = 0; i < repeats; ++i) {
            copy = alternative1(original, clip);
        }
        Instant after = Instant.now();
        long timeTaken = Duration.between(before, after).toMillis();
        System.out.println("Total time taken by alternative1 = " + timeTaken);
        System.out.println("Time taken per image by alternative1 = " + timeTaken / repeats);

        before = Instant.now();
        for (int i = 0; i < repeats; ++i) {
            copy = alternative2(original, clip);
        }
        after = Instant.now();
        timeTaken = Duration.between(before, after).toMillis();
        System.out.println("Total time taken by alternative2 = " + timeTaken);
        System.out.println("Time taken per image by alternative2 = " + timeTaken / repeats);

        before = Instant.now();
        for (int i = 0; i < repeats; ++i) {
            copy = alternative3(original, clip);
        }
        after = Instant.now();
        timeTaken = Duration.between(before, after).toMillis();
        System.out.println("Total time taken by alternative3 = " + timeTaken);
        System.out.println("Time taken per image by alternative3 = " + timeTaken / repeats);


    }

    private static BufferedImage alternative1(BufferedImage image, Rectangle rectangle) {
        // Get sub-raster, cast to writable and translate it to 0,0
        WritableRaster data = ((WritableRaster) image.getData(rectangle)).createWritableTranslatedChild(0, 0);

        // Create new image with data
        BufferedImage subOne = new BufferedImage(image.getColorModel(), data, image.isAlphaPremultiplied(), null);
        return subOne;
    }

    private static BufferedImage alternative2(BufferedImage image, Rectangle rectangle) {
        // Get subimage "normal way"
        BufferedImage subimage = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);

        // Create empty compatible image
        BufferedImage subTwo = new BufferedImage(image.getColorModel(), image.getRaster().createCompatibleWritableRaster(rectangle.width, rectangle.height), image.isAlphaPremultiplied(), null);

        // Copy data into the new, empty image
        subimage.copyData(subTwo.getRaster());
        return subTwo;
    }

    private static BufferedImage alternative3(BufferedImage image, Rectangle rectangle) {
        // Get subimage "normal way"
        BufferedImage subimage = image.getSubimage(rectangle.x, rectangle.y, rectangle.width, rectangle.height);

        // Create new empty image of same type
        BufferedImage subThree = new BufferedImage(rectangle.width, rectangle.height, image.getType());

        // Draw the subimage onto the new, empty copy
        Graphics2D g = subThree.createGraphics();
        try {
            g.drawImage(subimage, 0, 0, null);
        }
        finally {
            g.dispose();
        }
        return subThree;
    }

}
