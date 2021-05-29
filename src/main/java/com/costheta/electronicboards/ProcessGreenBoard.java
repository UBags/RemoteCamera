package com.costheta.electronicboards;

import com.costheta.image.utils.ImageUtils;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ProcessGreenBoard {

    private static final Logger logger = LogManager.getLogger(ProcessGreenBoard.class);

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\Electronic Board Inspection";
    // private static String fileName = "GreenBoard - 1.png";
    // private static String fileName = "EB-1.png";
    // private static String fileName = "Trial Picture - 1.jpg";
    private static String fileName = "A random file.jpg";
    // private static String fileName = "Trial Pic - 2.png";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    private static final String EMPTY_STRING = "";

    static {
        ImageUtils.initialise();
    }

    public static void main(String[] args) throws TesseractException, IOException {

        System.out.println("Running ProcessGreenBoard");

        if (args.length != 4) {
            System.out.println("Expecting 4 arguments: fileName={path to file} cutoff={number between 1-100} widthDivisions={number of divisions of width} heightDivisions={number of divisions of height}");
            System.exit(0);
        }
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        debugDirectory = debugDirectory + "/";
        String newFileName = getValueOfArguments(args, "fileName");
        if (!EMPTY_STRING.equals(newFileName)) {
            fileName = newFileName;
        }
        int cutoff = 30;
        try {
            cutoff = Integer.parseInt(getValueOfArguments(args, "cutoff"));
        } catch (Exception e) {

        }
        int widthDivisions = 10;
        try {
            widthDivisions = Integer.parseInt(getValueOfArguments(args, "widthDivisions"));
        } catch (Exception e) {

        }

        int heightDivisions = 5;
        try {
            heightDivisions = Integer.parseInt(getValueOfArguments(args, "heightDivisions"));
        } catch (Exception e) {

        }


        // System.out.println("Reached here - 1");
        BufferedImage image = ImageIO.read(new File(inputDirectory + "/" + fileName));
        ImageUtils.writeFile(image, "png", debugDirectory + "0 - original.png");

//        BufferedImage greensRemoved = ElectronicBoardUtils.replaceGreensWithWhite(image);
//        ImageUtils.writeFile(greensRemoved, "png", debugDirectory + "1 - GreensRemoved.png");
//
//        BufferedImage lightColoursRemoved = ElectronicBoardUtils.replaceLightColouredWithWhite(greensRemoved);
//        ImageUtils.writeFile(lightColoursRemoved, "png", debugDirectory + "2 - LightColoursRemoved.png");
//
//        BufferedImage copperColoursRemoved = ElectronicBoardUtils.replaceCopperColoursWithWhite(lightColoursRemoved);
//        ImageUtils.writeFile(copperColoursRemoved, "png", debugDirectory + "3 - CopperColoursRemoved.png");
//
//        BufferedImage nearWhitesRemoved = ElectronicBoardUtils.replaceNearWhitesWithWhite(copperColoursRemoved);
//        ImageUtils.writeFile(nearWhitesRemoved, "png", debugDirectory + "4 - NearWhitesRemoved.png");
//
//        BufferedImage nearGraysRemoved = ElectronicBoardUtils.replaceNearGraysWithWhite(nearWhitesRemoved);
//        ImageUtils.writeFile(nearGraysRemoved, "png", debugDirectory + "5 - NearGraysRemoved.png");
//
//        BufferedImage vertLinesRemoved = ElectronicBoardUtils.eliminateBlackBridges(nearGraysRemoved, 2, 208, ImageUtils.VERTICAL_DIRECTION);
//        ImageUtils.writeFile(vertLinesRemoved, "png", debugDirectory + "6 - VertLinesRemoved.png");
//
//        BufferedImage horLinesRemoved = ElectronicBoardUtils.eliminateBlackBridges(vertLinesRemoved, 2, 208, ImageUtils.HORIZONTAL_DIRECTION);
//        ImageUtils.writeFile(horLinesRemoved, "png", debugDirectory + "7 - HorLinesRemoved.png");
//
//        BufferedImage circlesFound1 = ElectronicBoardUtils.findAndHighlightSmallCircles(greensRemoved, 11, 8, 1.11);
//        ImageUtils.writeFile(circlesFound1, "png", debugDirectory + "8 - CirclesFound - 1.png");
//
//        BufferedImage circlesFound2 = ElectronicBoardUtils.findAndHighlightSmallCircles(circlesFound1, 13, 8, 1.12);
//        ImageUtils.writeFile(circlesFound2, "png", debugDirectory + "9 - CirclesFound - 2.png");
//
//        BufferedImage circlesFound3 = ElectronicBoardUtils.findAndHighlightSmallCircles(circlesFound2, 15, 8, 1.13);
//        ImageUtils.writeFile(circlesFound3, "png", debugDirectory + "10 - CirclesFound - 3.png");

//        BufferedImage holesIsolated = ElectronicBoardUtils.enhanceWhitesSuppressOthers(image);
//        ImageUtils.writeFile(holesIsolated, "png", debugDirectory + "1 - HolesIsolated.png");

//         BufferedImage holesIsolated = ElectronicBoardUtils.enhanceMaxMinusXPercent(image, 45, 5, 2); - Small board
//        BufferedImage holesIsolated = ElectronicBoardUtils.enhanceMaxMinusXPercent(image, 30, 16, 2); - Larger board
        BufferedImage holesIsolated = ElectronicBoardUtils.enhanceMaxMinusXPercent(image, cutoff, widthDivisions, heightDivisions);
        ImageUtils.writeFile(holesIsolated, "png", debugDirectory + "1 - HolesIsolated.png");

        BufferedImage eroded = ElectronicBoardUtils.erodeGrayBI(holesIsolated, 3,3);
        ImageUtils.writeFile(eroded, "png", debugDirectory + "2 - HolesDemarcated1.png");

        BufferedImage opened = ElectronicBoardUtils.openGrayBI(eroded, 5,5);
        ImageUtils.writeFile(eroded, "png", debugDirectory + "3 - HolesDemarcated2.png");

        System.out.println("Done");
        System.exit(0);
    }

    private static String getValueOfArguments(String[] args, String key) {
        if (args == null) {
            return EMPTY_STRING;
        }
        if (args.length == 0) {
            return EMPTY_STRING;
        }
        if (key == null) {
            return EMPTY_STRING;
        }
        if (EMPTY_STRING.equals(key)) {
            return EMPTY_STRING;
        }
        for (int i = 0; i < args.length; ++i) {
            String[] keyValuePair = args[i].split("=");
            if (keyValuePair.length != 2) {
                continue;
            }
            String thisKey = keyValuePair[0];
            if (key.equals(thisKey)) {
                return keyValuePair[1].trim();
            }
        }
        // no match found
        return EMPTY_STRING;
    }


}
