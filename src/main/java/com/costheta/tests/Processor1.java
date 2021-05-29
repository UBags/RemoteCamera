package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.image.utils.ImageUtils;
import com.costheta.tesseract.TesseractUtils;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.TesseractException;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javafx.scene.shape.Rectangle;

public class Processor1 extends PartProcessor {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\100001\\20210406201737\\Inspection Point 1";
    private static String fileName = "5-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    public static void main(String[] args) throws TesseractException, IOException {

        System.out.println("Running Processor 1");
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        debugDirectory = debugDirectory + "/";

        // System.out.println("Reached here - 1");
        BufferedImage image = ImageIO.read(new File(inputDirectory + "/" + fileName));
        ArrayList<String> patterns1 = new ArrayList<>();
        patterns1.add("DDA");
        patterns1.add("AADD");
        patterns1.add("DDDDD");
        patterns1.add("DDDDD");
        Processor1 aProcessor = new Processor1(patterns1);
        aProcessor.process(image);
        System.exit(0);
    }

    public Processor1(ArrayList<String> patterns) {
        super(patterns);
    }

    @Override
    public void process(BufferedImage image) {
        Pix originalPix = ImageUtils.convertImageToPix(image);
        // Pix originalPix = Leptonica.INSTANCE.pixRead(inputDirectory + "/" + fileName);
        // System.out.println("originalPix = " + originalPix);

        // Assumes 12 MP picture input
        // Hence, downsampling twice
        Pix originalPixReduced_Temp = null;
        if (originalPix.w > 3800) {
            originalPixReduced_Temp = Leptonica1.pixScaleAreaMap2(originalPix);
        } else {
            originalPixReduced_Temp = Leptonica1.pixCopy(null, originalPix);
        }
        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPixReduced_Temp);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        Leptonica1.pixWrite(debugDirectory + "1 - Original8.png", originalReduced8, ILeptonica.IFF_PNG);

        LeptUtils.dispose(originalPixReduced_Temp);
        LeptUtils.dispose(originalPixReduced);

        Pix pixSobel = Leptonica1.pixSobelEdgeFilter(originalReduced8, ILeptonica.L_ALL_EDGES);
        Pix pixSobelInvert = Leptonica1.pixInvert(null, pixSobel);
        Pix mask1_Input_8bpp = Leptonica1.pixGammaTRC(null, pixSobelInvert, 0.5f, 0, 255);
        Leptonica1.pixWrite(debugDirectory + "2 - Mask 1 Input.png", mask1_Input_8bpp, ILeptonica.IFF_PNG);
        Pix mask1_Input_1bpp = ImageUtils.getDepth1Pix(mask1_Input_8bpp, 236);
        Pix mask1_1bpp = Leptonica1.pixCloseBrick(null, mask1_Input_1bpp,31,15);
        Pix mask1_1bpp_Temp1 = Leptonica1.pixOpenBrick(null, mask1_1bpp,17,1);
        Pix mask1_1bpp_Temp2 = Leptonica1.pixOpenBrick(null, mask1_1bpp_Temp1,1,13);

        LeptUtils.dispose(pixSobel);
        LeptUtils.dispose(pixSobelInvert);
        LeptUtils.dispose(mask1_Input_8bpp);
        LeptUtils.dispose(mask1_Input_1bpp);
        LeptUtils.dispose(mask1_1bpp);
        LeptUtils.dispose(mask1_1bpp_Temp1);

        int connectivity = 4;
        Boxa boxes = Leptonica1.pixConnCompBB(mask1_1bpp_Temp2, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes);
        Pix copyOfMask1 = Leptonica1.pixCreate(Leptonica1.pixGetWidth(mask1_1bpp_Temp2),
                Leptonica1.pixGetHeight(mask1_1bpp_Temp2), 1);
        Leptonica1.pixSetBlackOrWhite(copyOfMask1, ILeptonica.L_SET_WHITE);

        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes, i, ILeptonica.L_CLONE);
            if ((aBox.w < 40) || (aBox.h < 15)) {
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(mask1_1bpp_Temp2, aBox, null);
            Leptonica1.pixRasterop(copyOfMask1, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);
        }

        LeptUtils.dispose(mask1_1bpp_Temp2);
        LeptUtils.dispose(boxes);

//        Pix pixClipped = Leptonica1.pixRotate(pixClippedUnrotated, (float) rotationAngle,
//                ILeptonica.L_ROTATE_AREA_MAP, ILeptonica.L_BRING_IN_WHITE, 0, 0);

        Pix mask1_8bpp = ImageUtils.getDepth8Pix(copyOfMask1);
        Leptonica1.pixWrite(debugDirectory + "3 - Mask 1.png", mask1_8bpp, ILeptonica.IFF_PNG);
        LeptUtils.dispose(copyOfMask1);

        Pix original_masked_1_8bpp = Leptonica1.pixOr(null, originalReduced8, mask1_8bpp);
        Leptonica1.pixWrite(debugDirectory + "4 - Original with Mask 1.png", original_masked_1_8bpp, ILeptonica.IFF_PNG);
        LeptUtils.dispose(mask1_8bpp);

        // Leptonica1.pixWrite(debugDirectory + "Sobel.png", pixSobelInvert, ILeptonica.IFF_PNG);
        // Leptonica1.pixWrite(debugDirectory + "Gamma.png", pixGamma, ILeptonica.IFF_PNG);
        // Leptonica1.pixWrite(debugDirectory + "Binarized.png", pixBinarized, ILeptonica.IFF_PNG);
        // Pix pixMasked = Leptonica1.pixCopy(null, originalPix8);
        // int result = Leptonica1.pixCombineMasked(pixMasked,originalPix8, pixMask);

//        System.out.println("Reached here - 2");
//        Pix pixBgNorm = Leptonica1.pixBackgroundNormFlex(maskedOriginal, 7, 7, 1, 1, 160);
//        Leptonica1.pixWrite(debugDirectory + "Background Normalised.png", pixBgNorm, ILeptonica.IFF_PNG);
//        BufferedImage bgImage = ImageUtils.convertPixToImage(pixBgNorm);
//
//        System.out.println("Reached here - 3");
//        BufferedImage GEUMImage1 = ImageUtils.relativeScaledGammaContrastEnhancement(bgImage, 11, 11,
//                98);
//        System.out.println("Reached here - 4");
//        BufferedImage GEUMImage2 = ImageUtils.relativeScaledGammaContrastEnhancement(bgImage, 21, 21,
//                98);

//        System.out.println("Reached here - 5");
//        ImageUtils.writeFile(GEUMImage1, "png", debugDirectory + "GE_11_11_98.png");
//        ImageUtils.writeFile(GEUMImage2, "png", debugDirectory + "GE_21_21_98.png");

//        System.out.println("Reached here - 6");
//        Pix mask2_Start = ImageUtils.getDepth8Pix(GEUMImage2);
//        Pix mask2_RelevantArea = Leptonica1.pixOr(null, mask2_Start, mask1_8bpp);
//        Pix mask2_RA_Closed1 = Leptonica1.pixCloseGray(mask2_RelevantArea, 31, 5);
//        Pix mask2_RA_Closed2 = Leptonica1.pixCloseGray(mask2_RA_Closed1, 1, 27);
//        Leptonica1.pixWrite(debugDirectory + "Mask 2 Input.png", mask2_RA_Closed2, ILeptonica.IFF_PNG);
//        Pix Mask2_Binarized = ImageUtils.getDepth1Pix(mask2_RA_Closed2, 254);
//        Pix Mask2_d8 = ImageUtils.getDepth8Pix(Mask2_Binarized);
//        Leptonica1.pixWrite(debugDirectory + "Mask 2 Temp.png", Mask2_d8, ILeptonica.IFF_PNG);
//        Pix Mask2_d8_Inverted = Leptonica1.pixInvert(null,Mask2_d8);
//        Pix finalMask_Temp1_1 = Leptonica1.pixOr(null, Mask2_d8_Inverted, mask1_pix8);
//        Pix finalMask_1 = Leptonica1.pixOr(null, Mask2_d8, mask1_8bpp);
//        Pix finalMask = Leptonica1.pixOr(null, Mask2_d8, mask1_8bpp);

//        BufferedImage finalMask_1_BI = ImageUtils.convertPixToImage(finalMask_1);
//        ArrayList<CleaningKernel> cleaningK = new ArrayList<>();
//        cleaningK.add(new CleaningKernel(15, 9));
//        cleaningK.add(new CleaningKernel(19, 11));
//        cleaningK.add(new CleaningKernel(25, 13));
//        BufferedImage finalMask_2_BI = ImageUtils.removeSmallTrails(finalMask_1_BI, cleaningK, false);
//        Pix finalMask = ImageUtils.getDepth8Pix(finalMask_2_BI);

//        System.out.println("Reached here - 8");

//        Leptonica1.pixWrite(debugDirectory + "Mask 2 Temp 1.png", finalMask_2, ILeptonica.IFF_PNG);
//        Pix finalMask_Temp2 = Leptonica1.pixCloseGray(finalMask_2, 1, 19);
//        Leptonica1.pixWrite(debugDirectory + "Mask 2 Temp 2.png", finalMask_Temp2, ILeptonica.IFF_PNG);
//        Pix finalMask_Temp3_1 = Leptonica1.pixCloseGray(finalMask_Temp2, 33, 1);
//        Pix finalMask = Leptonica1.pixCloseGray(finalMask_Temp3_1, 1, 23);
//        Pix finalMask = Leptonica1.pixCloseGray(finalMask_2, 1, 23);

//        System.out.println("Reached here - 9");

        // System.out.println(GEPix1);
//        Pix GEPix = ImageUtils.getDepth8Pix(GEUMImage1);
//        Pix pixCnNorm = Leptonica1.pixContrastNorm(null, GEPix, 24, 24, 100, 2,
//                2);
//        Pix pixUnMaGray = Leptonica1.pixUnsharpMaskingGray(pixCnNorm, 5, 0.7f);
//        Pix pixUnMaGrayMasked = Leptonica1.pixOr(null, pixUnMaGray, finalMask);
        // Pix originalMasked = Leptonica1.pixOr(null, originalReduced8, mask1_8bpp);

        BufferedImage originalMask1BI = ImageUtils.convertPixToImage(original_masked_1_8bpp);
        BufferedImage original_Masked_Morphed = ImageUtils.dilateGrayBIWithFilter(originalMask1BI, 5, 5 ,180, ImageUtils.BLACK);
        ImageUtils.writeFile(original_Masked_Morphed, "png", debugDirectory + "5 - Original Mask 1 Dilated.png");

        original_Masked_Morphed = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(original_Masked_Morphed,41,1,95,236);
        ImageUtils.writeFile(original_Masked_Morphed, "png", debugDirectory + "6 - Original Mask 1 GE.png");

        original_Masked_Morphed = ImageUtils.openGrayBI(original_Masked_Morphed, 9,9);
        ImageUtils.writeFile(original_Masked_Morphed, "png", debugDirectory + "7 - Original Mask 1 Opened.png");

        //        Pix originalMask1_Eroded = Leptonica1.pixCloseGray(original_masked_1_8bpp, 5,5);
        //        Leptonica1.pixWrite(debugDirectory + "5 - Original Mask 1 Closed.png", originalMask1_Eroded, ILeptonica.IFF_PNG);

        // BufferedImage maskedOriginalErodedBI = ImageUtils.convertPixToImage(originalMask1_Eroded);
//        BufferedImage maskedOriginalGE = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(original_Masked_Morphed, 13, 13,
//                95, 248);
//        maskedOriginalGE = ImageUtils.openGrayBI(maskedOriginalGE,9,9);
//        ImageUtils.writeFile(maskedOriginalGE, "png", debugDirectory + "6 - Original Mask 1 GE.png");
        Pix newMask_1bpp = ImageUtils.getDepth1Pix(original_Masked_Morphed, 144);
        Pix newMask_8bpp = ImageUtils.getDepth8Pix(newMask_1bpp);
        Leptonica1.pixWrite(debugDirectory + "8 - Pre-Final Mask.png", newMask_8bpp, ILeptonica.IFF_PNG);
        Pix finalMask = Leptonica1.pixErodeGray(newMask_8bpp, 3, 7);
        Leptonica1.pixWrite(debugDirectory + "9 - Final Mask.png", finalMask, ILeptonica.IFF_PNG);
        LeptUtils.dispose(original_masked_1_8bpp);
        LeptUtils.dispose(newMask_1bpp);
        LeptUtils.dispose(newMask_8bpp);

        Pix originalFinalMasked =  Leptonica1.pixOr(null, originalReduced8, finalMask);
        Leptonica1.pixWrite(debugDirectory + "10 - Original with Final Mask.png", originalFinalMasked, ILeptonica.IFF_PNG);

        BufferedImage originalMaskedBI = ImageUtils.convertPixToImage(originalFinalMasked);
        LeptUtils.dispose(originalFinalMasked);

        BufferedImage originalMaskedBI2 = ImageUtils.lightenCentreCellsYAxis(originalMaskedBI, 1, 5);
        BufferedImage originalMaskedBI1 = ImageUtils.lightenCentreCellsXAxis(originalMaskedBI, 5, 1);
        BufferedImage originalMaskedBI3 = ImageUtils.imageAverage(originalMaskedBI1, originalMaskedBI2);
        ImageUtils.writeFile(originalMaskedBI3, "png", debugDirectory + "10A - Original Lightened.png");

        BufferedImage GEUMImage3 = ImageUtils.relativeGammaContrastEnhancementWithPercentTolerance(originalMaskedBI3, 13, 5, 0.075);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "10B - RGE_PercentTol.png");

        GEUMImage3 = ImageUtils.relativeGammaContrastEnhancementWithPercentTolerance(GEUMImage3, 13, 13, 0.075);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "10C - RGE_PercentTol.png");

        GEUMImage3 = ImageUtils.dilateGrayBIWithFilter(GEUMImage3, 7,5, 180, ImageUtils.BLACK);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11 - Original Dilated.png");

        GEUMImage3 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GEUMImage3, 35, 13,
                60, 180);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11B - Original GE.png");

        GEUMImage3 = ImageUtils.binarize(GEUMImage3, 8);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11C - Original GE.png");

        GEUMImage3 = ImageUtils.eliminateNonCharactersInNearStraightLineImages(GEUMImage3, 19);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11D - Original GE.png");

        GEUMImage3 = ImageUtils.eliminateBlackBridges(GEUMImage3, 1, 32, ImageUtils.BOTH_DIRECTION);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11E - Original GE.png");

        GEUMImage3 = ImageUtils.normaliseBackground(GEUMImage3, 21, 7,
                236, 30);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "11F - Original GE.png");

        GEUMImage3 = ImageUtils.erodeGrayBIWithFilter(GEUMImage3, 3, 1,
                64, ImageUtils.BLACK);
        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "12 - Original GE.png");

        BufferedImage originalWithNewMask = ImageUtils.imageMask(originalMaskedBI, GEUMImage3);
        ImageUtils.writeFile(originalWithNewMask, "png", debugDirectory + "12A - Original Masked.png");

        GEUMImage3 = ImageUtils.dilateGrayBIWithFilter(GEUMImage3, 3, 1,
                180, ImageUtils.WHITE);
        GEUMImage3 = ImageUtils.eliminateBlackBridgesIteratively(GEUMImage3, 4, 128,
                ImageUtils.VERTICAL_DIRECTION);

        ImageUtils.writeFile(GEUMImage3, "png", debugDirectory + "13 - Original GE.png");

//        BufferedImage GEUMImage4 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhitesAndBlacks(GEUMImage3, 11, 3,
//                50, 128, 32);
////        BufferedImage GEUMImage4 = ImageUtils.dilateGrayBIWithFilter(GEUMImage3,13,7,112);
//        ImageUtils.writeFile(GEUMImage4, "png", debugDirectory + "10 - Original GE Modified.png");

//        Pix binarizedPix = ImageUtils.getDepth1Pix(GEUMImage3, 128);
//        Leptonica1.pixWrite(debugDirectory + "11 - GE Binarized.png", binarizedPix, ILeptonica.IFF_PNG);
//        BufferedImage binarizedBI = ImageUtils.convertPixToImage(binarizedPix);
        BufferedImage binarizedBI = ImageUtils.binarizeWithCutOff(GEUMImage3,128);
        Pix binarizedPix = ImageUtils.getDepth1Pix(binarizedBI, 128);

        Pix cleanedBinarizedPix1 = ImageUtils.removeLines(binarizedPix,ImageUtils.VERTICAL_DIRECTION, 40);
        Leptonica1.pixWrite(debugDirectory + "15 - Cleaned Binarized Pix - 1.png", cleanedBinarizedPix1, ILeptonica.IFF_PNG);
        LeptUtils.dispose(binarizedPix);

        Pix cleanedBinarizedPix = ImageUtils.removeLines(cleanedBinarizedPix1,ImageUtils.HORIZONTAL_DIRECTION, 40);
        Leptonica1.pixWrite(debugDirectory + "16 - Cleaned Binarized Pix - 2.png", cleanedBinarizedPix, ILeptonica.IFF_PNG);
        LeptUtils.dispose(cleanedBinarizedPix1);

        Boxa boxes1 = Leptonica1.pixConnCompBB(cleanedBinarizedPix, connectivity);
        numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix copy = Leptonica1.pixCreate(Leptonica1.pixGetWidth(cleanedBinarizedPix),
                Leptonica1.pixGetHeight(cleanedBinarizedPix), 1);
        Leptonica1.pixSetBlackOrWhite(copy, ILeptonica.L_SET_WHITE);

        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);

            if ((aBox.w < 5) || (aBox.h < 15)) {
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(cleanedBinarizedPix, aBox, null);
            Leptonica1.pixRasterop(copy, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);

        }
        LeptUtils.dispose(boxes1);
        LeptUtils.dispose(cleanedBinarizedPix);

        Leptonica1.pixWrite(debugDirectory + "17 - Small bits cleaned.png", copy, ILeptonica.IFF_PNG);

        float rotationAngle = ImageUtils.findSkewAngle(copy, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(copy, rotationAngle, true);
        Pix rotatedPixTemp = ImageUtils.getDepth1Pix(rotatedPix8, 128);
        Pix rotatedPix = ImageUtils.eliminateBlackBridgesIteratively(rotatedPixTemp, 3, 128, ImageUtils.VERTICAL_DIRECTION);

        Leptonica1.pixWrite(debugDirectory + "18 - Rotated Binarized.png", rotatedPix, ILeptonica.IFF_PNG);
        LeptUtils.dispose(copy);
        LeptUtils.dispose(rotatedPix8);
        LeptUtils.dispose(rotatedPixTemp);

        // System.out.println("Reached this point before calculation of lines");
        //*********************************************************************

        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, 15, 5, 1, 15, 15);
        System.out.println("Lines found from rotatedPix are : " + lines);
        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        System.out.println("Lines found from force fitting into 2 lines are : " + lines);
        System.out.println("Lines are : " + lines);

//        BufferedImage binarizedBIBridgesCleaned = ImageUtils.eliminateBlackBridges(binarizedBI,4,10,ImageUtils.VERTICAL_DIRECTION);
//        ImageUtils.writeFile(binarizedBIBridgesCleaned, "png", debugDirectory + "14 - GE Binarized - No thin bridges.png");

//        ArrayList<Integer> heightStats = new ArrayList<>();
//
//        Boxa boxes2 = Leptonica1.pixConnCompBB(rotatedPix, connectivity);
//        numberOfBoxes = Leptonica1.boxaGetCount(boxes2);
//        ArrayList<Integer> yStarts = new ArrayList<>();
//        System.out.println("Boxes in rotated image = " + numberOfBoxes);
//        for (int i = 0; i < numberOfBoxes; ++i) {
//            Box aBox = Leptonica1.boxaGetBox(boxes2, i, ILeptonica.L_COPY);
//            if (aBox.w <= 5) {
//                LeptUtils.dispose(aBox);
//                continue;
//            }
//            heightStats.add(aBox.h);
//            yStarts.add(aBox.y);
//            LeptUtils.dispose(aBox);
//        }
//
//        int[] hStats = heightStats.stream().mapToInt(i -> i).toArray();
//        Arrays.sort(hStats);
//        heightStats.clear();
//        int[] histogramHeights = new int[100];
//        int bucketSize = 5;
//        int total = 0;
//        int n = 0;
//        for (int i = 0; i < hStats.length; ++i) {
//            if ((i == 0) || (i == hStats.length - 1)) {
//                continue;
//            }
//            if (hStats[i] < 15) {
//                continue;
//            }
//            System.out.println("Adding Height " + hStats[i]);
//            heightStats.add(hStats[i]);
//            int bucket = hStats[i] / bucketSize;
//            histogramHeights[bucket]++;
//            total += hStats[i];
//            ++n;
//        }
//        double averageHeight = total *1.0/ n;
//        int highestBucket = 0;
//        int highestBucketValue = 0;
//        for (int i = 0; i < histogramHeights.length; ++i) {
//            if (histogramHeights[i] > highestBucketValue) {
//                highestBucketValue = histogramHeights[i];
//                highestBucket = i;
//            }
//        }
//        int likelyHeight = Math.max(highestBucket * bucketSize + bucketSize / 2, (int) averageHeight);
//        int tolerance = 10;
//
//        System.out.println("Likely Height = " + likelyHeight);
//
//        int[] histogramYStarts = new int[100];
//        for (int i = 0; i < yStarts.size(); ++i) {
//            int bucket = yStarts.get(i) / 10;
//            histogramYStarts[bucket]++;
//        }
//
//        int mode1 = 0;
//        int mode2 = 0;
//        int mode3 = 0;
//        int mode4 = 0;
//
//        for (int i = 0; i < histogramYStarts.length; ++i) {
//            if (histogramYStarts[i] >= histogramYStarts[mode1]) {
//                mode4 = mode3;
//                mode3 = mode2;
//                mode2 = mode1;
//                mode1 = i;
//            } else {
//                if (histogramYStarts[i] >= histogramYStarts[mode2]) {
//                    mode4 = mode3;
//                    mode3 = mode2;
//                    mode2 = i;
//                } else {
//                    if (histogramYStarts[i] >= histogramYStarts[mode3]) {
//                        mode4 = mode3;
//                        mode3 = i;
//                    } else {
//                        if (histogramYStarts[i] >= histogramYStarts[mode4]) {
//                            mode4 = i;
//                        }
//                    }
//                }
//            }
//        }
//
//        int line1YStart = histogramYStarts[mode1] > 2 ? mode1 * 10 + 5 : 0 ;
//        int line2YStart = histogramYStarts[mode2] > 2 ? mode2 * 10 + 5 : 0 ;
//        int line3YStart = histogramYStarts[mode3] > 2 ? mode3 * 10 + 5 : 0 ;
//        int line4YStart = histogramYStarts[mode4] > 2 ? mode4 * 10 + 5 : 0 ;
//
//        System.out.println("Line 1 Starts at " + line1YStart + "; number of boxes = " + histogramYStarts[mode1]);
//        System.out.println("Line 2 Starts at " + line2YStart + "; number of boxes = " + histogramYStarts[mode2]);
//        System.out.println("Line 3 Starts at " + line3YStart + "; number of boxes = " + histogramYStarts[mode3]);
//        System.out.println("Line 4 Starts at " + line4YStart + "; number of boxes = " + histogramYStarts[mode4]);


//        if (histogramYStarts[mode1 - 1] > 1) {
//            line1YStart = mode1 * 10;
//        } else {
//            if (histogramYStarts[mode1 + 1] > 1) {
//                line1YStart = mode1 * 10 + 10;
//            }
//        }
//
//        if (histogramYStarts[mode2 - 1] > 1) {
//            line2YStart = mode2 * 10;
//        } else {
//            if (histogramYStarts[mode2 + 1] > 1) {
//                line1YStart = mode1 * 10 + 10;
//            }
//        }

//        Pix cleanedPix = Leptonica1.pixCreate(Leptonica1.pixGetWidth(rotatedPix),
//                Leptonica1.pixGetHeight(rotatedPix), 1);
//        Leptonica1.pixSetBlackOrWhite(cleanedPix, ILeptonica.L_SET_WHITE);
//        DescriptiveStatistics widthStats = new DescriptiveStatistics();
//
//        int lineStartTolerance = 7;
//        for (int i = 0; i < numberOfBoxes; ++i) {
//            Box aBox = Leptonica1.boxaGetBox(boxes2, i, ILeptonica.L_COPY);
//            if (aBox.w <= 5) {
//                LeptUtils.dispose(aBox);
//                continue;
//            }
//            if (!(((aBox.y >= line1YStart - lineStartTolerance) && (aBox.y <= line1YStart + lineStartTolerance)) ||
//            ((aBox.y >= line2YStart - lineStartTolerance) && (aBox.y <= line2YStart + lineStartTolerance)) ||
//            ((aBox.y >= line3YStart - lineStartTolerance) && (aBox.y <= line3YStart + lineStartTolerance)) ||
//            ((aBox.y >= line4YStart - lineStartTolerance) && (aBox.y <= line4YStart + lineStartTolerance)))) {
//                LeptUtils.dispose(aBox);
//                continue;
//            }
//
//            if ((aBox.h < (0.7 * likelyHeight)) || (aBox.h > 1.5 * likelyHeight)) {
//                LeptUtils.dispose(aBox);
//                continue;
//            }
//            widthStats.addValue(aBox.w);
//            Pix pixOriginalX = Leptonica1.pixClipRectangle(rotatedPix, aBox, null);
//            Leptonica1.pixRasterop(cleanedPix, aBox.x, aBox.y,
//                    aBox.w, aBox.h,
//                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
//            LeptUtils.dispose(aBox);
//            LeptUtils.dispose(pixOriginalX);
//        }
//        LeptUtils.dispose(boxes2);
//        LeptUtils.dispose(rotatedPix);
//        Leptonica1.pixWrite(debugDirectory + "19 - Cleaned Image.png", cleanedPix, ILeptonica.IFF_PNG);

//        Pix cleanedPix = ImageUtils.getDerivativeImage(rotatedPix, lines);
//        Leptonica1.pixWrite(debugDirectory + "19 - Cleaned Image.png", cleanedPix, ILeptonica.IFF_PNG);
//
//        DescriptiveStatistics hStats = new DescriptiveStatistics();
//        DescriptiveStatistics wStats = new DescriptiveStatistics();
//        for (int i = 0; i < lines.size(); ++i) {
//            for (int j = 0; j < lines.get(i).size(); ++ j) {
//                int height = lines.get(i).get(j).height;
//                int width = lines.get(i).get(j).width;
//                hStats.addValue(height);
//                wStats.addValue(width);
//            }
//        }
//
//        int likelyWidth = (int) wStats.getPercentile(50);
//        System.out.println("Likely Width = " + likelyWidth);
//        int likelyHeight = (int) hStats.getPercentile(50);
//        System.out.println("Likely Height = " + likelyHeight);
//
//        float widthScalingFactor = (float) (((likelyHeight * 1.0) / 1.7) / (likelyWidth * 1.0));
//        Pix cleanedScaledPix = Leptonica1.pixScale(cleanedPix, widthScalingFactor, 1.0f);
//        Leptonica1.pixWrite(debugDirectory + "20 - Cleaned Scaled Image.png", cleanedScaledPix, ILeptonica.IFF_PNG);
//
//        LeptUtils.dispose(rotatedPix);
//        LeptUtils.dispose(cleanedPix);
//
//        Pix binarizedDilated = Leptonica1.pixErodeBrick(null, cleanedScaledPix, 3, 3);
//        Leptonica1.pixWrite(debugDirectory + "21 - Rotated Dilated.png", binarizedDilated, ILeptonica.IFF_PNG);

//        System.out.println("Reached here - 11");

//        Leptonica1.pixWrite(debugDirectory + "Cn Norm.png", pixCnNorm, ILeptonica.IFF_PNG);
//        Leptonica1.pixWrite(debugDirectory + "Un Ma.png", pixUnMaGray, ILeptonica.IFF_PNG);
//        Leptonica1.pixWrite(debugDirectory + "Final pix.png", pixUnMaGrayMasked, ILeptonica.IFF_PNG);

        double[] scalingFactors = new double[] {0.85, 1.0, 1.15};
        int erodeOrDilate = ImageUtils.DILATE;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(rotatedPix, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, debugDirectory, 0, TraceLevel.INFO);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        int numberOfLines = lines.size();
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < numberOfLines; ++i) {
            String[] strings = new String[repeats];
            for (int j = 0; j < repeats; ++j) {
                strings[j] = CharacterUtils.removeSpaces(CharacterUtils.getOnlyAlphabetsAndNumbers(results.get(i * repeats + j)));
            }
            finalStrings.add(CharacterUtils.getCommonString(strings));
        }

        finalStrings = rectifyResults(finalStrings);

        System.out.println("Final Strings are : " + finalStrings);


//        ITesseract instance = new Tesseract();  // JNA Interface Mapping
//        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
//        instance.setDatapath("E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\External Libraries\\tessdata"); // path to tessdata directory
//
//        // BufferedImage tessImage = ImageUtils.convertPixToImage(pixUnMaGrayMasked);
//        BufferedImage tessImage = ImageUtils.convertPixToImage(binarizedDilated);
//
//        try {
//            String result = instance.doOCR(tessImage);
//            System.out.println(CharacterUtils.getOnlyAlphabetsAndNumbers(result));
//        } catch (TesseractException e) {
//            System.err.println(e.getMessage());
//        }
//        // System.out.println("pixMasked = " + pixMasked);
//
//         tessImage = ImageUtils.convertPixToImage(cleanedScaledPix);
//
//        try {
//            String result = instance.doOCR(tessImage);
//            System.out.println(CharacterUtils.getOnlyAlphabetsAndNumbers(result));
//        } catch (TesseractException e) {
//            System.err.println(e.getMessage());
//        }


        // Leptonica1.pixWrite(debugDirectory + "BgNorm.png", pixBgNorm, ILeptonica.IFF_PNG);
//        LeptUtils.dispose(cleanedScaledPix);
//        LeptUtils.dispose(binarizedDilated);

        // System.out.println(results);

        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(finalMask);
        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(pixArrayForTesseract);

        System.out.println("Done");
    }
}
