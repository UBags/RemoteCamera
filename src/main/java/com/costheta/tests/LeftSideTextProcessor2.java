package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseClipImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import com.costheta.text.utils.CharacterUtils;
import com.sun.jna.ptr.PointerByReference;
import javafx.geometry.Dimension2D;
import javafx.scene.shape.Rectangle;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class LeftSideTextProcessor2 extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(LeftSideTextProcessor2.class);
    private int imageCounter = 1;
    private static int connectivity = 4;

    public LeftSideTextProcessor2(String name, ArrayList<String> patterns) {
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
        Pix originalPixLarge = ImageUtils.convertImageToPix(image);
        if (originalPixLarge == null) {
            LeptUtils.dispose(originalPixLarge);
            return ProcessingResult.EMPTY_PROCESSING_RESULT;
        }
        logger.trace("originalPix = " + originalPixLarge);
        // Assumes 12 MP picture input
        // Hence, downsampling twice
        Box clipBox = Leptonica1.boxCreate((int) clipBoxRectangles.get(0).getX(), (int) clipBoxRectangles.get(0).getY(), (int) clipBoxRectangles.get(0).getWidth(), (int) clipBoxRectangles.get(0).getHeight());
        Pix originalPix = Leptonica1.pixClipRectangle(originalPixLarge, clipBox, null);
        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        pixWrite(imageCounter + " - 1 - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        LeptUtils.dispose(originalPixLarge);
        LeptUtils.dispose(clipBox);
        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        Pix bnPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        Pix cnPix = Leptonica1.pixContrastNorm(null, bnPix, bnPix.w / 4,
                bnPix.h / 3, 100, 2, 2);
        pixWrite(imageCounter + " - 2 - bnPix.png", bnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        pixWrite(imageCounter + " - 3 - cnPix.png", cnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage cnImage = ImageUtils.convertPixToImage(cnPix);
        Dimension2D dimensions = getCharacterDimensions();
        BufferedImage RGE_CNImage = ImageUtils.relativeGammaContrastEnhancement(cnImage, (int) dimensions.getWidth() / 3, (int) dimensions.getHeight() / 3, 30);
        imageWrite(imageCounter + " - 4A - RGE_CNImage-FirstCut.png", RGE_CNImage, BaseCamera.PNG, TraceLevel.TRACE);

        Pix bnPix1 = Leptonica1.pixBackgroundNormFlex(cnPix, 7, 7, 2, 2, 100);
        pixWrite(imageCounter + " - 3A - bnPix1.png", bnPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage bnImage1 = ImageUtils.convertPixToImage(bnPix1);
        BufferedImage bnImage2 = ImageUtils.relativeGammaContrastEnhancement(cnImage, (int) dimensions.getWidth(), (int) dimensions.getHeight(), 20);
        BufferedImage bnImage3 = ImageUtils.imageAverage(bnImage1, bnImage2);
        imageWrite(imageCounter + " - 3B - RGE_bnImage-20.png", bnImage2, BaseCamera.PNG, TraceLevel.TRACE);
        imageWrite(imageCounter + " - 3C - Average.png", bnImage3, BaseCamera.PNG, TraceLevel.TRACE);


        Pix RG8 = ImageUtils.getDepth8Pix(bnImage3);
        Pix bnPix2 = Leptonica1.pixBackgroundNormFlex(RG8, 7, 7, 2, 2, 100);
        pixWrite(imageCounter + " - 3D - bnPix2.png", bnPix2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(bnPix1);
        LeptUtils.dispose(RG8);
        LeptUtils.dispose(bnPix2);

        // RGE_CNImage = ImageUtils.relativeGammaContrastEnhancement(RGE_CNImage, 15, 19);
        // RGE_CNImage = ImageUtils.relativeGammaContrastEnhancement(RGE_CNImage, (int) (dimensions.getWidth() * 3.0/ 4), (int) (dimensions.getHeight() * 3.0 / 4));
        RGE_CNImage = ImageUtils.relativeGammaContrastEnhancement(bnImage3, (int) (dimensions.getWidth() * 3.0/ 4), (int) (dimensions.getHeight() * 3.0 / 4));
        imageWrite(imageCounter + " - 4 - RGE_CNImage.png", RGE_CNImage, BaseCamera.PNG, TraceLevel.TRACE);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(bnPix);
        LeptUtils.dispose(cnPix);

        BufferedImage RGE_CNImage_Adjusted = ImageUtils.lightenCentreCellsYAxis(RGE_CNImage, 5, 5);
        imageWrite(imageCounter + " - 5 - RGE_CN_Lightened.png", RGE_CNImage_Adjusted, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage closedBI = ImageUtils.closeGrayBI (RGE_CNImage_Adjusted, 15, 5);
        imageWrite(imageCounter + " - 6 - closedBI.png", closedBI, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage bin_closedBI_Temp = ImageUtils.binarize(closedBI, 128);
        imageWrite(imageCounter + " - 7 - bin_closedBI.png", bin_closedBI_Temp, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage aMaskBI = ImageUtils.invert(bin_closedBI_Temp);
        imageWrite(imageCounter + " - 8 - Mask.png", aMaskBI, BaseCamera.PNG, TraceLevel.TRACE);

        Pix RGE_CN_Pix32 = ImageUtils.convertImageToPix(RGE_CNImage_Adjusted);
        Pix RGE_CN_Pix = ImageUtils.getDepth8Pix(RGE_CN_Pix32);

        PointerByReference pbrSauvola = new PointerByReference();
        int success = Leptonica1.pixSauvolaBinarizeTiled(RGE_CN_Pix, 11, 0.2f,
                (RGE_CN_Pix.w / (RGE_CN_Pix.h / 2)) + 1,2, null, pbrSauvola);
        logger.trace("Sauvola success of " + processorName + " = " + success);
        Pix pixSauvola = new Pix(pbrSauvola.getValue());
        pixWrite(imageCounter + " - 9 - sauvolaPix.png", pixSauvola, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(RGE_CN_Pix32);
        LeptUtils.dispose(RGE_CN_Pix);

        Pix pixSauvolaCleaned1 = ImageUtils.removeSaltPepper(pixSauvola);
        pixWrite(imageCounter + " - 10 - sauvolaSaltPepper.png", pixSauvolaCleaned1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage sauvolaCleanedBI = ImageUtils.convertPixToImage(pixSauvolaCleaned1);
        // BufferedImage diagonalsCleanedBI = ImageUtils.eliminateDiagonalBlacks(sauvolaCleanedBI,3);
        BufferedImage diagonalsCleanedBI = ImageUtils.eliminateBlackBridgesIteratively(sauvolaCleanedBI, 1, 128, ImageUtils.VERTICAL_DIRECTION);
        // diagonalsCleanedBI = ImageUtils.eliminateDiagonalBlacks(diagonalsCleanedBI,3);
        imageWrite(imageCounter + " - 10A - diagonalsCleaned.png", diagonalsCleanedBI, BaseCamera.PNG, TraceLevel.TRACE);

        Pix pixSauvolaCleaned32 = ImageUtils.convertImageToPix(diagonalsCleanedBI);
        Pix pixSauvolaCleaned = ImageUtils.getDepth1Pix(pixSauvolaCleaned32, 128);

        LeptUtils.dispose(pixSauvolaCleaned1);
        LeptUtils.dispose(pixSauvolaCleaned32);

        Pix pixSauvolaLinesRemoved_H = ImageUtils.removeLines(pixSauvolaCleaned, ImageUtils.VERTICAL_DIRECTION, 19);
        Pix pixSauvolaLinesRemoved = ImageUtils.removeLines(pixSauvolaLinesRemoved_H, ImageUtils.HORIZONTAL_DIRECTION, 19);
        pixWrite(imageCounter + " - 11 - sauvolaRemoveLines.png", pixSauvolaLinesRemoved, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage cleanedBI = ImageUtils.convertPixToImage(pixSauvolaLinesRemoved);
        BufferedImage processedAndMaskedBI = ImageUtils.imageMask(cleanedBI, aMaskBI);
        imageWrite(imageCounter + " - 12 - maskedOriginal.png", processedAndMaskedBI, BaseCamera.PNG, TraceLevel.TRACE);

        Pix cleanedBinarizedPix = ImageUtils.getDepth1Pix(processedAndMaskedBI, 128);
        pixWrite(imageCounter + " - 13 - cleanedBinarizedPix.png", cleanedBinarizedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Boxa boxes1 = Leptonica1.pixConnCompBB(cleanedBinarizedPix, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix smallBitsCleaned = Leptonica1.pixCreate(Leptonica1.pixGetWidth(cleanedBinarizedPix),
                Leptonica1.pixGetHeight(cleanedBinarizedPix), 1);
        Leptonica1.pixSetBlackOrWhite(smallBitsCleaned, ILeptonica.L_SET_WHITE);

        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);

            if ((aBox.w < 3) || (aBox.h < 9)) {
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(cleanedBinarizedPix, aBox, null);
            Leptonica1.pixRasterop(smallBitsCleaned, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);

        }
        LeptUtils.dispose(pixSauvola);
        LeptUtils.dispose(pixSauvolaLinesRemoved_H);
        LeptUtils.dispose(pixSauvolaLinesRemoved);
        LeptUtils.dispose(cleanedBinarizedPix);
        LeptUtils.dispose(boxes1);
        pixWrite(imageCounter + " - 14 - smallBitsCleaned.png", smallBitsCleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        float rotationAngle = ImageUtils.findSkewAngle(smallBitsCleaned, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(smallBitsCleaned, rotationAngle, true);
        Pix rotatedPixTemp = ImageUtils.getDepth1Pix(rotatedPix8, 128);
        Pix rotatedPix = ImageUtils.eliminateBlackBridgesIteratively(rotatedPixTemp, 2, 128, ImageUtils.VERTICAL_DIRECTION);
        pixWrite(imageCounter + " - 15 - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix sauvolaRotatedPix8 = ImageUtils.rotatePix(pixSauvolaCleaned, rotationAngle, true);
        Pix sauvolaRotatedPixTemp = ImageUtils.getDepth1Pix(sauvolaRotatedPix8, 128);
        Pix sauvolaRotatedPix = ImageUtils.eliminateBlackBridgesIteratively(sauvolaRotatedPixTemp, 2, 128, ImageUtils.VERTICAL_DIRECTION);
        pixWrite(imageCounter + " - 16 - rotatedSauvolaCleanedPix.png", sauvolaRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(rotatedPix8);
        LeptUtils.dispose(rotatedPixTemp);
        LeptUtils.dispose(sauvolaRotatedPix8);
        LeptUtils.dispose(sauvolaRotatedPixTemp);

        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, 7, 2, 2, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.debug("Lines found from rotatedPix are : " + lines);
        ArrayList<ArrayList<Rectangle>> twoLines = TesseractUtils.arrangeInto2Lines(lines);
        logger.debug("Lines found from arranging into 2 lines are : " + lines);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(sauvolaRotatedPix, twoLines);
        pixWrite(imageCounter + " - 17 - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        lines = TesseractUtils.getBoundingBoxes(pixWithE2ELines, 7, 2, 2, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        logger.trace("Lines are : " + lines);

        double[] scalingFactors = new double[]{1.0, 1.2, 1.4};
        int erodeOrDilate = ImageUtils.DILATE;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(rotatedPix, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        logger.debug("Strings after OCR are : " + results);

        int numberOfLines = lines.size();
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < numberOfLines; ++i) {
            String[] strings = new String[repeats];
            for (int j = 0; j < repeats; ++j) {
                strings[j] = CharacterUtils.removeSpaces(CharacterUtils.getOnlyAlphabetsAndNumbers(results.get(i * repeats + j)));
            }
            logger.debug("Strings at index : " + i + " = " + Arrays.deepToString(strings));
            String commonString = CharacterUtils.getCommonString(strings, patterns.get(i));
            logger.debug("After matching with pattern " + patterns.get(i) + " the final commonString is " + commonString);
            finalStrings.add(commonString);
        }

        finalStrings = rectifyResults(finalStrings);

        logger.trace("Final Strings are : " + finalStrings);

        LeptUtils.dispose(pixSauvolaCleaned);
        LeptUtils.dispose(smallBitsCleaned);
        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(sauvolaRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);
        LeptUtils.dispose(pixArrayForTesseract);

        ++imageCounter;

        ArrayList<ArrayList<String>> strings = CharacterUtils.forceFitIntoMultiLineMultiColumn(finalStrings, 2);
        ProcessingResult pResult = new ProcessingResult(lines, new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), strings, true);
        logger.trace("Done");
        return pResult;
    }
}
