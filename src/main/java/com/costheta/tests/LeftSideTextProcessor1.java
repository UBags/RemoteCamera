package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javafx.scene.shape.Rectangle;

public class LeftSideTextProcessor1 extends BaseImageProcessor {

    private static final Logger logger = LogManager.getLogger(LeftSideTextProcessor1.class);
    private int imageCounter = 1;

    public LeftSideTextProcessor1(String name, ArrayList<String> patterns) {
        super(name, patterns);
        logger.trace("After calling super() in constructor");
    }

    @Override
    public ProcessingResult process(BufferedImage image) {
        logger.trace("Entering process() with " + image);
        if (image == null) {
            return ProcessingResult.EMPTY_PROCESSING_RESULT;
        }
        logger.trace("Entering process() with " + image);
        Pix originalPix = ImageUtils.convertImageToPix(image);
        if (originalPix == null) {
            return ProcessingResult.EMPTY_PROCESSING_RESULT;
        }
        logger.trace("originalPix = " + originalPix);
        // Assumes 12 MP picture input
        // Hence, downsampling twice
        Pix originalPixReduced_Temp = null;
        if (originalPix.w > 3800) {
            originalPixReduced_Temp = Leptonica1.pixScaleAreaMap2(originalPix);
        } else {
            originalPixReduced_Temp = Leptonica1.pixCopy(null, originalPix);
        }
        logger.trace("originalPixReduced_Temp = " + originalPixReduced_Temp);
        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPixReduced_Temp);
        logger.trace("originalPixReduced = " + originalPixReduced);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        pixWrite(imageCounter + " - 1 - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        LeptUtils.dispose(originalPixReduced_Temp);
        LeptUtils.dispose(originalPixReduced);

        Pix pixSobel = Leptonica1.pixSobelEdgeFilter(originalReduced8, ILeptonica.L_ALL_EDGES);
        Pix pixSobelInvert = Leptonica1.pixInvert(null, pixSobel);
        Pix mask1_Input_8bpp = Leptonica1.pixGammaTRC(null, pixSobelInvert, 0.5f, 0, 255);
        pixWrite(imageCounter + " - 2 - Mask 1 Input.png", mask1_Input_8bpp, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        Pix mask1_Input_1bpp = ImageUtils.getDepth1Pix(mask1_Input_8bpp, 236);
        Pix mask1_1bpp = Leptonica1.pixCloseBrick(null, mask1_Input_1bpp, 31, 15);
        Pix mask1_1bpp_Temp1 = Leptonica1.pixOpenBrick(null, mask1_1bpp, 17, 1);
        Pix mask1_1bpp_Temp2 = Leptonica1.pixOpenBrick(null, mask1_1bpp_Temp1, 1, 13);

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
        pixWrite(imageCounter + " - 3 - Mask 1.png", mask1_8bpp, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        LeptUtils.dispose(copyOfMask1);

        Pix original_masked_1_8bpp = Leptonica1.pixOr(null, originalReduced8, mask1_8bpp);
        pixWrite(imageCounter + " - 4 - Original with Mask 1.png", original_masked_1_8bpp, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        LeptUtils.dispose(mask1_8bpp);

        BufferedImage originalMask1BI = ImageUtils.convertPixToImage(original_masked_1_8bpp);
        BufferedImage original_Masked_Morphed = ImageUtils.dilateGrayBIWithFilter(originalMask1BI, 5, 5, 180, ImageUtils.BLACK);
        imageWrite(imageCounter + " - 5 - Original Mask 1 Dilated.png", original_Masked_Morphed, BaseCamera.PNG, TraceLevel.TRACE);

        original_Masked_Morphed = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(original_Masked_Morphed, 41, 1, 95, 236);
        imageWrite(imageCounter + " - 6 - Original Mask 1 GE.png", original_Masked_Morphed, BaseCamera.PNG, TraceLevel.TRACE);

        original_Masked_Morphed = ImageUtils.openGrayBI(original_Masked_Morphed, 9, 9);
        imageWrite(imageCounter + " - 7 - Original Mask 1 Opened.png", original_Masked_Morphed, BaseCamera.PNG, TraceLevel.TRACE);

        Pix newMask_1bpp = ImageUtils.getDepth1Pix(original_Masked_Morphed, 144);
        Pix newMask_8bpp = ImageUtils.getDepth8Pix(newMask_1bpp);
        pixWrite(imageCounter + " - 8 - Pre-Final Mask.png", newMask_8bpp, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        Pix finalMask = Leptonica1.pixErodeGray(newMask_8bpp, 3, 7);
        pixWrite(imageCounter + " - 9 - Final Mask.png", finalMask, ILeptonica.IFF_PNG, TraceLevel.DEBUG);
        LeptUtils.dispose(original_masked_1_8bpp);
        LeptUtils.dispose(newMask_1bpp);
        LeptUtils.dispose(newMask_8bpp);

        Pix originalFinalMasked = Leptonica1.pixOr(null, originalReduced8, finalMask);
        pixWrite(imageCounter + " - 10 - Original with Final Mask.png", originalFinalMasked, ILeptonica.IFF_PNG, TraceLevel.INFO);

        BufferedImage originalMaskedBI = ImageUtils.convertPixToImage(originalFinalMasked);
        LeptUtils.dispose(originalFinalMasked);

        int number1 = Integer.parseInt(System.getProperty("GEIm3.1.whiteCutoff","180"));
        BufferedImage GEUMImage3 = ImageUtils.dilateGrayBIWithFilter(originalMaskedBI, 5, 3, number1, ImageUtils.BLACK);
        imageWrite(imageCounter + " - 11A - Original Dilated.png", GEUMImage3, BaseCamera.PNG, TraceLevel.TRACE);

        int number2 = Integer.parseInt(System.getProperty("GEIm3.2.percentile","60"));
        int number3 = Integer.parseInt(System.getProperty("GEIm3.2.whiteCutoff","170"));
        GEUMImage3 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GEUMImage3, 35, 13,
                number2, number3);
        imageWrite(imageCounter + " - 11B - Original GE.png", GEUMImage3, BaseCamera.PNG, TraceLevel.DEBUG);

        int number4 = Integer.parseInt(System.getProperty("GEIm3.3.blackCutoff","64"));
        GEUMImage3 = ImageUtils.erodeGrayBIWithFilter(GEUMImage3, 3, 1,
                number4, ImageUtils.BLACK);
        imageWrite(imageCounter + " - 12 - Original GE.png", GEUMImage3, BaseCamera.PNG, TraceLevel.INFO);

        int number5 = Integer.parseInt(System.getProperty("GEIm3.4.whiteCutoff","180"));
        GEUMImage3 = ImageUtils.dilateGrayBIWithFilter(GEUMImage3, 3, 1,
                number5, ImageUtils.WHITE);
        GEUMImage3 = ImageUtils.eliminateBlackBridgesIteratively(GEUMImage3, 4, 128,
                ImageUtils.VERTICAL_DIRECTION);

        imageWrite(imageCounter + " - 13 - Original GE.png", GEUMImage3, BaseCamera.PNG, TraceLevel.DEBUG);

        BufferedImage binarizedBI = ImageUtils.binarizeWithCutOff(GEUMImage3, 128);
        Pix binarizedPix = ImageUtils.getDepth1Pix(binarizedBI, 128);

        Pix cleanedBinarizedPix1 = ImageUtils.removeLines(binarizedPix, ImageUtils.VERTICAL_DIRECTION, 40);
        pixWrite(imageCounter + " - 15 - Cleaned Binarized Pix - 1.png", cleanedBinarizedPix1, ILeptonica.IFF_PNG, TraceLevel.DEBUG);
        LeptUtils.dispose(binarizedPix);

        Pix cleanedBinarizedPix = ImageUtils.removeLines(cleanedBinarizedPix1, ImageUtils.HORIZONTAL_DIRECTION, 40);
        pixWrite(imageCounter + " - 16 - Cleaned Binarized Pix - 2.png", cleanedBinarizedPix, ILeptonica.IFF_PNG, TraceLevel.DEBUG);
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

        pixWrite(imageCounter + " - 17 - Small bits cleaned.png", copy, ILeptonica.IFF_PNG, TraceLevel.DEBUG);

        float rotationAngle = ImageUtils.findSkewAngle(copy, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(copy, rotationAngle, true);
        Pix rotatedPixTemp = ImageUtils.getDepth1Pix(rotatedPix8, 128);
        Pix rotatedPix = ImageUtils.eliminateBlackBridgesIteratively(rotatedPixTemp, 3, 128, ImageUtils.VERTICAL_DIRECTION);

        pixWrite(imageCounter + " - 18 - Rotated Binarized.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.INFO);
        LeptUtils.dispose(copy);
        LeptUtils.dispose(rotatedPix8);
        LeptUtils.dispose(rotatedPixTemp);

        // logger.trace("Reached this point before calculation of lines");
        //*********************************************************************

        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, 15, 5, 1, 15, 15);
        System.out.println("Lines found from rotatedPix are : " + lines);
        logger.debug("Bounding boxes for the lines in the image are : " + lines);
        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        System.out.println("After force fitting the lines into 2 lines multi-columns : " + lines);
        logger.debug("After force fitting the lines into 2 lines multi-columns, they are : " + lines);

        double[] scalingFactors = new double[]{0.85, 1.0, 1.15};
        int erodeOrDilate = ImageUtils.DILATE;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(rotatedPix, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
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

        logger.trace("Final Strings are : " + finalStrings);

        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(finalMask);
        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(pixArrayForTesseract);
        ++imageCounter;

        ArrayList<ArrayList<String>> strings = CharacterUtils.forceFitIntoMultiLineMultiColumn(finalStrings, 2);
        ProcessingResult pResult = new ProcessingResult(lines, new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), strings, true);
        logger.trace("Done");
        return pResult;
    }
}
