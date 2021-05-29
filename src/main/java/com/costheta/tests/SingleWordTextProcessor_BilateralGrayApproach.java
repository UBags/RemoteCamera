package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseClipImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.text.utils.PatternMatchedStrings;
import com.sun.jna.ptr.PointerByReference;
import javafx.geometry.Dimension2D;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.regex.Pattern;

import javafx.scene.shape.Rectangle;

public class SingleWordTextProcessor_BilateralGrayApproach extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(SingleWordTextProcessor_BilateralGrayApproach.class);
    private int imageCounter = 1;
    private static int connectivity = 4;

    public SingleWordTextProcessor_BilateralGrayApproach(String name, ArrayList<String> patterns) {
        super(name, patterns);
        logger.trace("Leaving constructor");
    }

    public SingleWordTextProcessor_BilateralGrayApproach(String name, ArrayList<String> patterns, int assessmentRegions) {
        super(name, patterns, assessmentRegions);
        logger.trace("Leaving constructor");
    }

    @Override
    public ProcessingResult process(BufferedImage image) {
        logger.trace("Entering process() with " + image);

        int imageSerialCounter = 1;

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

        // clip the relevant part of the picture

        Box[] clipBoxes = new Box[clipBoxRectangles.size()];
        for (int i = 0; i < clipBoxes.length; ++i) {
            clipBoxes[i] = Leptonica1.boxCreate((int) clipBoxRectangles.get(i).getX(), (int) clipBoxRectangles.get(i).getY(), (int) clipBoxRectangles.get(i).getWidth(), (int) clipBoxRectangles.get(i).getHeight());
        }
        Box clipBox = Leptonica1.boxCreate((int) clipBoxRectangles.get(0).getX(), (int) clipBoxRectangles.get(0).getY(), (int) clipBoxRectangles.get(0).getWidth(), (int) clipBoxRectangles.get(0).getHeight());
        Pix originalPix = Leptonica1.pixClipRectangle(originalPixLarge, clipBox, null);

        LeptUtils.dispose(originalPixLarge);
        LeptUtils.dispose(clipBox);

        // If 12 MP picture input, downsample twice
        // If 3 MP pictureinput, downsample once

        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        BufferedImage normBackImage = ImageUtils.convertPixToImage(originalReduced8);

        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        // get the character dimensions from the properties file
        Dimension2D dimensions = getCharacterDimensions();

        // Get background normalised and contrast normalised image
        Pix normBackPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - normBackPix.png", normBackPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix contNormPix = Leptonica1.pixContrastNorm(null, normBackPix, normBackPix.w / 4,
                normBackPix.h / 8, 100, 2, 2); // 2, 2
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - cnPix.png", contNormPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Again, background normalise the contrast normalised image
        Pix normBackPix1 = Leptonica1.pixBackgroundNormFlex(contNormPix, 7, 7, 2, 2, 60); // 100
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Pix.png", normBackPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix BLG1 = Leptonica1.pixBilateralGray(normBackPix1, 25f, 50f, 16, 4);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - BL_Gray_1.png", BLG1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

//        BufferedImage bnImage1 = ImageUtils.convertPixToImage(normBackPix1);
//        bnImage1 = ImageUtils.rankFilter(bnImage1, 3,3,50);
//        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Image_MedianFilter.png", bnImage1, BaseCamera.PNG, TraceLevel.TRACE);
//
//        Pix medianPix = ImageUtils.getDepth8Pix(bnImage1);
//        Pix BLG2 = Leptonica1.pixBilateralGray(medianPix, 25f, 50f, 16, 4);
//        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - BLG2.png", BLG2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        PointerByReference pbrSauvola1 = new PointerByReference();
        int success = 1;
        success = Leptonica1.pixSauvolaBinarizeTiled(BLG1, 25, 0.05f,
                8,
                8, null, pbrSauvola1);
        Pix sauvola1 = new Pix(pbrSauvola1.getValue());
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - SauvolaBLG1.png", sauvola1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

//        PointerByReference pbrSauvola2 = new PointerByReference();
//        success = 1;
//        success = Leptonica1.pixSauvolaBinarizeTiled(BLG2, 21, 0.05f,
//                8,
//                8, null, pbrSauvola2);
//        Pix sauvola2 = new Pix(pbrSauvola2.getValue());
//        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - SauvolaBLG2.png", sauvola2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(normBackPix);
        LeptUtils.dispose(contNormPix);
        LeptUtils.dispose(normBackPix1);
        LeptUtils.dispose(BLG1);
//        LeptUtils.dispose(medianPix);
//        LeptUtils.dispose(BLG2);

        // remove horizontal lines
        Pix removeHorLines = ImageUtils.removeLines(sauvola1, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 2);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - removeHorLines.png", removeHorLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // remove vertical lines
        Pix removeVertLines = ImageUtils.removeLines(removeHorLines, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() + 2));
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - removeVertLines.png", removeVertLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage baseImage = ImageUtils.convertPixToImage(removeVertLines);

        // clean the image of small black trails
        ArrayList<CleaningKernel> cKernels = new ArrayList<>();
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,3));
        cKernels.add(new CleaningKernel(3,5));
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,5));
        cKernels.add(new CleaningKernel(7,5));
        cKernels.add(new CleaningKernel(5,7));
        cKernels.add(new CleaningKernel(9,7));
        cKernels.add(new CleaningKernel(7,9));
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,5));
        cKernels.add(new CleaningKernel(11,5));
        cKernels.add(new CleaningKernel(13,5));
        cKernels.add(new CleaningKernel(9,7));
        cKernels.add(new CleaningKernel(11,7));
        cKernels.add(new CleaningKernel(13,7));
        baseImage = ImageUtils.removeSmallTrails(baseImage, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - smallTrailsCleaned.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        baseImage = ImageUtils.joinDisjointedBridges(baseImage);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImage.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        // -----BASE IMAGE got for extracting letters

        Pix connCompReadyPix = ImageUtils.getDepth1Pix(baseImage);

        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix smallAndLargeBitsCleaned = Leptonica1.pixCreate(Leptonica1.pixGetWidth(connCompReadyPix),
                Leptonica1.pixGetHeight(connCompReadyPix), 1);
        Leptonica1.pixSetBlackOrWhite(smallAndLargeBitsCleaned, ILeptonica.L_SET_WHITE);
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);
            if ((aBox.w < dimensions.getWidth() / 5) || (aBox.h < dimensions.getHeight() / 2) || (aBox.h > dimensions.getHeight() * 1.3) || (aBox.w > dimensions.getWidth() * 2.5)) {
                logger.trace("Creating smallAndLargeBitsCleaned : Dropping box at " + aBox.x + "," + aBox.y + "," + aBox.w + "," + aBox.h);
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(connCompReadyPix, aBox, null);
            Leptonica1.pixRasterop(smallAndLargeBitsCleaned, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);
        }
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - smallAndLargeBitsCleaned.png", smallAndLargeBitsCleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        float rotationAngle = ImageUtils.findSkewAngle(smallAndLargeBitsCleaned, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(smallAndLargeBitsCleaned, rotationAngle, true);
        Pix rotatedPix = ImageUtils.getDepth1Pix(rotatedPix8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(removeHorLines);
        LeptUtils.dispose(removeVertLines);
        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);
        LeptUtils.dispose(smallAndLargeBitsCleaned);
        LeptUtils.dispose(rotatedPix8);

        ArrayList<ArrayList<Rectangle>> lines = getLines(rotatedPix, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);
        ArrayList<ArrayList<Rectangle>> twoLines = TesseractUtils.arrangeInto2Lines(lines);
        logger.debug("Lines found from arranging into 2 lines are : " + lines);

        Pix tesseractRotatedPix8 = ImageUtils.rotatePix(sauvola1, rotationAngle, true);
        Pix tesseractRotatedPix = ImageUtils.getDepth1Pix(tesseractRotatedPix8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedTesseractPix.png", tesseractRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(tesseractRotatedPix, twoLines);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Note: Vertical lines cleaning has higher tolerance this time
        // Note: No cleaning of horizontal lines needed this time
        BufferedImage finalImage = ImageUtils.convertPixToImage(pixWithE2ELines);
        finalImage = ImageUtils.removeLines(finalImage, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight()) + 6);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - finalVerLinesRemoved.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // Make the image fit for connected components.
        // If there are images with thin 1 pixel lines, they may not be connected...this routine corrects
        // that so that the connected components are properly formed
        finalImage = ImageUtils.cleanThenJoinDisjointedBridges(finalImage);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bridgesJoined.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);
        Pix E2ECleaned = ImageUtils.getDepth1Pix(finalImage, 144);

        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(tesseractRotatedPix8);
        LeptUtils.dispose(tesseractRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);

        // Note: We cannot use the lines found in the previous image because larger boxes are removed by the
        // remove lines method. These boxes get reinstated back by the repeated process which clips only the relevant
        // rectangle from the image

        lines = getLines(E2ECleaned, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);

        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        logger.trace("Lines are : " + lines);
        drawBoundingBoxesOnPix(E2ECleaned, lines, imageCounter + " - " + imageSerialCounter + "- E2ECleaned-forceFitInto2.png", TraceLevel.TRACE);

        double[] scalingFactors = new double[]{0.7, 0.85, 1.0, 1.1, 1.2};
        int erodeOrDilate = ImageUtils.LEAVE_UNCHANGED;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(E2ECleaned, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        Leptonica1.pixDestroy(pbrSauvola1);
//        Leptonica1.pixDestroy(pbrSauvola2);
        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(pixArrayForTesseract);

        logger.debug("Strings after OCR are : " + results);
        System.out.println("Strings after OCR are : " + results);
        int numberOfLines = lines.size();
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < Math.min(numberOfLines, regexPatterns.size()); ++i) {
            PatternMatchedStrings matchedStrings = new PatternMatchedStrings();
            System.out.println("Regex pattern is " + regexPatterns.get(i));
            // ArrayList<Pattern> toBeMatchedPatterns = regexPatterns.get(i);
            Pattern toBeMatchedPattern = regexPatterns.get(i);
            String stringPattern = patterns.get(i);
            for (int j = 0; j < repeats; ++j) {
                String ocrString = CharacterUtils.removeSpaces(CharacterUtils.getOnlyAlphabetsAndNumbers(results.get(i * repeats + j)));
                PatternMatchedStrings matchingStrings = matchPattern(ocrString, toBeMatchedPattern, stringPattern);
                // matchPattern() returns the original back if no match is found
                // so, check if the length of the returned arraylist is 1 and if it's length is the same as the required pattern
//                if (matchedStrings.size() == 1) {
//                    if (matchedStrings.get(0).string.length() == patterns.get(i).length()) {
//                        matchedStrings.add(matchingStrings.get(0));
//                    }
//                } else {
//                    for (String aString : matchingStrings) {
//                        matchedStrings.add(aString);
//                    }
//                }
                matchedStrings.add(matchingStrings);
            }
            // logger.debug("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            // System.out.println("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            logger.debug("First cut : Matching strings at index : " + i + " = " + matchedStrings);
            System.out.println("First cut: Matching strings at index : " + i + " = " + matchedStrings);
            // If there is an exact match i.e. length is exactly same and
            // String commonString = CharacterUtils.getCommonString(strings, (i < patterns.size() ? patterns.get(i) : ""));
            String commonString = CharacterUtils.getCommonString(matchedStrings);
            logger.debug("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
            System.out.println("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
            finalStrings.add(commonString);
        }

        finalStrings = rectifyResults(finalStrings);
        logger.trace("Final Strings are : " + finalStrings);

        ++imageCounter;

        ArrayList<ArrayList<String>> strings = CharacterUtils.forceFitIntoMultiLineMultiColumn(finalStrings, 2);
        ProcessingResult pResult = new ProcessingResult(lines, new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), strings, true);
        logger.trace("Done");
        return pResult;
    }

    private ProcessingResult processForEach(Pix originalLargePix, Box clipBox) {

        int imageSerialCounter = 1;

        Pix originalPix = Leptonica1.pixClipRectangle(originalLargePix, clipBox, null);

        // If 12 MP picture input, downsample twice
        // If 3 MP pictureinput, downsample once

        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        BufferedImage normBackImage = ImageUtils.convertPixToImage(originalReduced8);

        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        // get the character dimensions from the properties file
        Dimension2D dimensions = getCharacterDimensions();

        // Get background normalised and contrast normalised image
        Pix normBackPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - normBackPix.png", normBackPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix contNormPix = Leptonica1.pixContrastNorm(null, normBackPix, normBackPix.w / 4,
                normBackPix.h / 8, 100, 2, 2); // 2, 2
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - cnPix.png", contNormPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Again, background normalise the contrast normalised image
        Pix normBackPix1 = Leptonica1.pixBackgroundNormFlex(contNormPix, 7, 7, 2, 2, 60); // 100
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Pix.png", normBackPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix BLG1 = Leptonica1.pixBilateralGray(normBackPix1, 25f, 50f, 16, 4);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - BL_Gray_1.png", BLG1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        PointerByReference pbrSauvola1 = new PointerByReference();
        int success = 1;
        success = Leptonica1.pixSauvolaBinarizeTiled(BLG1, 25, 0.05f,
                8,
                8, null, pbrSauvola1);
        Pix sauvola1 = new Pix(pbrSauvola1.getValue());
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - SauvolaBLG1.png", sauvola1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(normBackPix);
        LeptUtils.dispose(contNormPix);
        LeptUtils.dispose(normBackPix1);
        LeptUtils.dispose(BLG1);

        // remove horizontal lines
        // note that ImageUtils adds 2 pixels as a matter of abundant caution
        Pix removeHorLines = ImageUtils.removeLines(sauvola1, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 4);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - removeHorLines.png", removeHorLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // remove vertical lines
        // note that ImageUtils adds 2 pixels as a matter of abundant caution
        Pix removeVertLines = ImageUtils.removeLines(removeHorLines, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() + 4));
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - removeVertLines.png", removeVertLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage baseImage = ImageUtils.convertPixToImage(removeVertLines);

        // clean the image of small black trails
        ArrayList<CleaningKernel> cKernels = new ArrayList<>();

        for (int i = 3; i < dimensions.getWidth() * 2.0 / 3; i = i + 2) {
            for (int j = 3; j < dimensions.getHeight() * 2.0 / 3; j = j + 2) {
                cKernels.add(new CleaningKernel(i, j));
            }
        }
        baseImage = ImageUtils.removeSmallTrails(baseImage, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - smallTrailsCleaned.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        baseImage = ImageUtils.joinDisjointedBridges(baseImage);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImage.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        // -----BASE IMAGE got for extracting letters

        Pix connCompReadyPix = ImageUtils.getDepth1Pix(baseImage);

        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix smallAndLargeBitsCleaned = Leptonica1.pixCreate(Leptonica1.pixGetWidth(connCompReadyPix),
                Leptonica1.pixGetHeight(connCompReadyPix), 1);
        Leptonica1.pixSetBlackOrWhite(smallAndLargeBitsCleaned, ILeptonica.L_SET_WHITE);
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);
            if ((aBox.w < dimensions.getWidth() / 5) || (aBox.h < dimensions.getHeight() / 2) || (aBox.h > dimensions.getHeight() * 1.3) || (aBox.w > dimensions.getWidth() * 2.5)) {
                logger.trace("Creating smallAndLargeBitsCleaned : Dropping box at " + aBox.x + "," + aBox.y + "," + aBox.w + "," + aBox.h);
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(connCompReadyPix, aBox, null);
            Leptonica1.pixRasterop(smallAndLargeBitsCleaned, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);
        }
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - smallAndLargeBitsCleaned.png", smallAndLargeBitsCleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        float rotationAngle = ImageUtils.findSkewAngle(smallAndLargeBitsCleaned, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(smallAndLargeBitsCleaned, rotationAngle, true);
        Pix rotatedPix = ImageUtils.getDepth1Pix(rotatedPix8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(removeHorLines);
        LeptUtils.dispose(removeVertLines);
        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);
        LeptUtils.dispose(smallAndLargeBitsCleaned);
        LeptUtils.dispose(rotatedPix8);

        ArrayList<ArrayList<Rectangle>> lines = getLines(rotatedPix, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);
        logger.debug("Lines found : " + lines);

        Pix tesseractRotatedPix8 = ImageUtils.rotatePix(sauvola1, rotationAngle, true);
        Pix tesseractRotatedPix = ImageUtils.getDepth1Pix(tesseractRotatedPix8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedTesseractPix.png", tesseractRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(tesseractRotatedPix, lines);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Note: Vertical lines cleaning has higher tolerance this time
        // Note: No cleaning of horizontal lines needed this time
        BufferedImage finalImage = ImageUtils.convertPixToImage(pixWithE2ELines);
        finalImage = ImageUtils.removeLines(finalImage, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight()) + 6);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - finalVerLinesRemoved.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // Make the image fit for connected components.
        // If there are images with thin 1 pixel lines, they may not be connected...this routine corrects
        // that so that the connected components are properly formed
        finalImage = ImageUtils.cleanThenJoinDisjointedBridges(finalImage);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bridgesJoined.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);
        Pix E2ECleaned = ImageUtils.getDepth1Pix(finalImage, 144);

        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(tesseractRotatedPix8);
        LeptUtils.dispose(tesseractRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);

        // Note: We cannot use the lines found in the previous image because larger boxes are removed by the
        // remove lines method. These boxes get reinstated back by the repeated process which clips only the relevant
        // rectangle from the image

        lines = getWord(E2ECleaned, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, 0, TraceLevel.TRACE);

        logger.trace("Lines are : " + lines);
        drawBoundingBoxesOnPix(E2ECleaned, lines, imageCounter + " - " + imageSerialCounter + "- E2ECleaned-forceFitInto2.png", TraceLevel.TRACE);

        double[] scalingFactors = new double[]{0.7, 0.85, 1.0, 1.1, 1.2};
        int erodeOrDilate = ImageUtils.LEAVE_UNCHANGED;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(E2ECleaned, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        Leptonica1.pixDestroy(pbrSauvola1);
//        Leptonica1.pixDestroy(pbrSauvola2);
        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(pixArrayForTesseract);

        logger.debug("Strings after OCR are : " + results);
        System.out.println("Strings after OCR are : " + results);
        int numberOfLines = lines.size();
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < Math.min(numberOfLines, regexPatterns.size()); ++i) {
            PatternMatchedStrings matchedStrings = new PatternMatchedStrings();
            System.out.println("Regex pattern is " + regexPatterns.get(i));
            // ArrayList<Pattern> toBeMatchedPatterns = regexPatterns.get(i);
            Pattern toBeMatchedPattern = regexPatterns.get(i);
            String stringPattern = patterns.get(i);
            for (int j = 0; j < repeats; ++j) {
                String ocrString = CharacterUtils.removeSpaces(CharacterUtils.getOnlyAlphabetsAndNumbers(results.get(i * repeats + j)));
                PatternMatchedStrings matchingStrings = matchPattern(ocrString, toBeMatchedPattern, stringPattern);
                // matchPattern() returns the original back if no match is found
                // so, check if the length of the returned arraylist is 1 and if it's length is the same as the required pattern
//                if (matchedStrings.size() == 1) {
//                    if (matchedStrings.get(0).string.length() == patterns.get(i).length()) {
//                        matchedStrings.add(matchingStrings.get(0));
//                    }
//                } else {
//                    for (String aString : matchingStrings) {
//                        matchedStrings.add(aString);
//                    }
//                }
                matchedStrings.add(matchingStrings);
            }
            // logger.debug("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            // System.out.println("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            logger.debug("First cut : Matching strings at index : " + i + " = " + matchedStrings);
            System.out.println("First cut: Matching strings at index : " + i + " = " + matchedStrings);
            // If there is an exact match i.e. length is exactly same and
            // String commonString = CharacterUtils.getCommonString(strings, (i < patterns.size() ? patterns.get(i) : ""));
            String commonString = CharacterUtils.getCommonString(matchedStrings);
            logger.debug("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
            System.out.println("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
            finalStrings.add(commonString);
        }

        finalStrings = rectifyResults(finalStrings);
        logger.trace("Final Strings are : " + finalStrings);

        ++imageCounter;

        ArrayList<ArrayList<String>> strings = CharacterUtils.forceFitIntoMultiLineMultiColumn(finalStrings, 1);
        ProcessingResult pResult = new ProcessingResult(lines, new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), strings, true);
        logger.trace("Done");
        return pResult;
    }
}
