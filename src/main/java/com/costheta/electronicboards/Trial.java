package com.costheta.electronicboards;

import com.costheta.image.utils.ImageUtils;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Trial {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\Electronic Board Inspection";
    private static String fileName = "GreenBoard - 1.png";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    static {
        ImageUtils.initialise();
    }

    public static void main(String[] args) throws TesseractException, IOException {

        System.out.println("Running ProcessGreenBoard");
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        debugDirectory = debugDirectory + "/";

        // System.out.println("Reached here - 1");
        BufferedImage image = ImageIO.read(new File(inputDirectory + "/" + fileName));
        BufferedImage copy = ImageUtils.copyBI(image);
        for (int y = 0; y < copy.getHeight(); ++y) {
            for (int x = 0; x < copy.getWidth(); ++x) {
                int pixel = copy.getRGB(x,y);
                System.out.println("alpha = " + ((pixel >> 24) & 0xFF));
            }
        }
        ImageUtils.writeFile(copy, "png", debugDirectory + "Copy.png");

//        BufferedImage lightColoursRemoved = ElectronicBoardUtils.replaceLightColouredWithWhite(greensRemoved);
//        ImageUtils.writeFile(lightColoursRemoved, "jpg", debugDirectory + "LightColoursRemoved.jpg");
//
//        BufferedImage copperColoursRemoved = ElectronicBoardUtils.replaceCopperColoursWithWhite(lightColoursRemoved);
//        ImageUtils.writeFile(copperColoursRemoved, "jpg", debugDirectory + "CopperColoursRemoved.jpg");
//
//        BufferedImage nearWhitesRemoved = ElectronicBoardUtils.replaceNearWhitesWithWhite(copperColoursRemoved);
//        ImageUtils.writeFile(nearWhitesRemoved, "jpg", debugDirectory + "NearWhitesRemoved.jpg");
//
//        BufferedImage nearGraysRemoved = ElectronicBoardUtils.replaceNearGraysWithWhite(nearWhitesRemoved);
//        ImageUtils.writeFile(nearGraysRemoved, "jpg", debugDirectory + "NearGraysRemoved.jpg");

        System.out.println("Done");
        System.exit(0);
    }

}
