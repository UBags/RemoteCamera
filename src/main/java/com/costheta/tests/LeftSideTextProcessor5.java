package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseClipImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import com.costheta.text.utils.CharacterUtils;
import com.sun.jna.ptr.PointerByReference;
import javafx.geometry.Dimension2D;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.regex.Pattern;
import javafx.scene.shape.Rectangle;


public class LeftSideTextProcessor5 extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(LeftSideTextProcessor5.class);
    private int imageCounter = 1;
    private static int connectivity = 4;

    public LeftSideTextProcessor5(String name, ArrayList<String> patterns) {
        super(name, patterns);
        logger.trace("After calling super() in constructor");
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
        Box clipBox = Leptonica1.boxCreate((int) clipBoxRectangles.get(0).getX(), (int) clipBoxRectangles.get(0).getY(), (int) clipBoxRectangles.get(0).getWidth(), (int) clipBoxRectangles.get(0).getHeight());
        Pix originalPix = Leptonica1.pixClipRectangle(originalPixLarge, clipBox, null);

        LeptUtils.dispose(originalPixLarge);

        // If 12 MP picture input, downsample twice
        // If 3 MP pictureinput, downsample once

        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);

        BufferedImage nbImage = ImageUtils.convertPixToImage(originalReduced8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        LeptUtils.dispose(clipBox);
        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        // get the character dimensions from the properties file
        Dimension2D dimensions = getCharacterDimensions();

        // Get background normalised and contrast normalised image
        Pix bnPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bnPix.png", bnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix cnPix = Leptonica1.pixContrastNorm(null, bnPix, bnPix.w / 4,
                bnPix.h / 8, 100, 2, 2); // 2, 2
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - cnPix.png", cnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix BLG1 = Leptonica1.pixBilateralGray(cnPix, 25f, 50f, 16, 4);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - BLG1.png", BLG1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        PointerByReference pbrSauvola1 = new PointerByReference();
        int success = 1;
        success = Leptonica1.pixSauvolaBinarizeTiled(BLG1, 21, 0.05f,
                8,
                8, null, pbrSauvola1);
        Pix sauvola1 = new Pix(pbrSauvola1.getValue());
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - SauvolaBLG1.png", sauvola1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Again, background normalise the contrast normalised image
        Pix bnPix1 = Leptonica1.pixBackgroundNormFlex(cnPix, 7, 7, 2, 2, 60); // 100
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Pix.png", bnPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix BLG2 = Leptonica1.pixBilateralGray(bnPix1, 25f, 50f, 16, 4);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - BLG2.png", BLG2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        PointerByReference pbrSauvola2 = new PointerByReference();
        success = 1;
        success = Leptonica1.pixSauvolaBinarizeTiled(BLG2, 21, 0.05f,
                8,
                8, null, pbrSauvola2);
        Pix sauvola2 = new Pix(pbrSauvola2.getValue());
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - SauvolaBLG2.png", sauvola2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(BLG1);
        LeptUtils.dispose(BLG2);
        Leptonica1.pixDestroy(pbrSauvola1);
        Leptonica1.pixDestroy(pbrSauvola2);

        BufferedImage cnImage = ImageUtils.convertPixToImage(cnPix);

        // This will be the go forward image for additional work
        BufferedImage bnImage1 = ImageUtils.convertPixToImage(bnPix1);
        bnImage1 = ImageUtils.rankFilter(bnImage1, 3,3,50);
        bnImage1 = ImageUtils.rankFilter(bnImage1, 3,3,50);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Pix_MedianFilter.png", bnImage1, BaseCamera.PNG, TraceLevel.TRACE);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(bnPix);
        LeptUtils.dispose(cnPix);
        LeptUtils.dispose(bnPix1);

        nbImage = ImageUtils.normaliseBackground(nbImage);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - normalisedBackground.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        nbImage = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(nbImage, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0),30, 254);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_NBImage_1.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        nbImage = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(nbImage, (int) (dimensions.getWidth() * 1.0), (int) (dimensions.getHeight() * 1.0),40, 254);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_NBImage_2.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        // small kernel GE, based on average
        BufferedImage baseImageInterimMask = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnImage1, (int) (dimensions.getWidth() * 0.5), (int) (dimensions.getHeight() * 0.5),242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImageInterimForMask.png", baseImageInterimMask, BaseCamera.PNG, TraceLevel.TRACE);

        // large kernel, 20 percentile, skip whites
        BufferedImage baseImageMask = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(baseImageInterimMask, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), 20, 242); // 10
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImageForMask.png", baseImageMask, BaseCamera.PNG, TraceLevel.TRACE);

        // ------ BASE IMAGE GOT --------

        // ---Get a Mask. Even though the base image for mask looks good enough for the final image,
        // it is better shape to get a mask and work forward from there, as it generally
        // helps overcome sticky corner cases

        BufferedImage blurredImageForMask = ImageUtils.blur(baseImageMask,5,5);
        // blurredImageForMask = ImageUtils.blur(blurredImageForMask,5,5);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - blurredForMask.png", blurredImageForMask, BaseCamera.PNG, TraceLevel.TRACE);

/*

        // gets a rough mask, then masks the original with this mask,
        // then does relative gamma enhancement (with 'skipWhites') of the relevant parts
        BufferedImage makeMask_1 = ImageUtils.gammaEnhancementWithMask(blurredImageForMask, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), (int) dimensions.getWidth(), (int) dimensions.getHeight(), 30); // cnImage, (* 1.5 each), and 40 percentile
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask1.png", makeMask_1, BaseCamera.PNG, TraceLevel.TRACE);

*/
        // binarize
        BufferedImage makeMask_1 = ImageUtils.binarize(blurredImageForMask, 112);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask1.png", makeMask_1, BaseCamera.PNG, TraceLevel.TRACE);

        // remove horizontal lines
        BufferedImage makeMask_2 = ImageUtils.removeLines(makeMask_1, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask2.png", makeMask_2, BaseCamera.PNG, TraceLevel.TRACE);

        // remove vertical lines
        makeMask_2 = ImageUtils.removeLines(makeMask_2, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() + 2));
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask3.png", makeMask_2, BaseCamera.PNG, TraceLevel.TRACE);

        // clean the image of small black trails
        ArrayList<CleaningKernel> cKernels = new ArrayList<>();
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,3));
        cKernels.add(new CleaningKernel(3,5));
        cKernels.add(new CleaningKernel(3,3));
        makeMask_2 = ImageUtils.removeSmallTrails(makeMask_2, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask4.png", makeMask_2, BaseCamera.PNG, TraceLevel.TRACE);

        // erode to get a more accurate mask
        BufferedImage mask = ImageUtils.erodeGrayBI(makeMask_2, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0)); // height was divided by 2 earlier
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask.png", mask, BaseCamera.PNG, TraceLevel.TRACE);

        // mask the background BN-CN image
        BufferedImage bnCnMasked = ImageUtils.imageMask(bnImage1, mask);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bnCnMasked.png", bnCnMasked, BaseCamera.PNG, TraceLevel.TRACE);

        // BufferedImage baseImageInterim = ImageUtils.harmonicFilter(bnCnMasked, 3, 3,2, 254);
        // imageWrite(imageCounter + " - " + imageSerialCounter++ + " - harmonicFilter_2.png", baseImageInterim, BaseCamera.PNG, TraceLevel.TRACE);

        // small kernel GE, based on average
        BufferedImage baseImageInterim = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 0.5), (int) (dimensions.getHeight() * 0.5),254, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        // done twice, which is a difference from the sequence followed to create the mask
        baseImageInterim = ImageUtils.relativeGammaAverageEnhancementSkipWhites(baseImageInterim, (int) (dimensions.getWidth() * 1.0), (int) (dimensions.getHeight() * 1.0),254, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImageInterim.png", baseImageInterim, BaseCamera.PNG, TraceLevel.TRACE);

        // large kernel, 15 percentile, skip whites
        // changed from (2.0, 2.0, 15) which is for the mask
        // to (1.5, 1.5, 25)
        BufferedImage baseImage = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(baseImageInterim, (int) (dimensions.getWidth() * 1.5), (int) (dimensions.getHeight() * 1.5), 25, 242);

        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - baseImage.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        // -----BASE IMAGE got for extracting letters

        // binarize - currently with a fixed number - maybe changed later to local sauvola or local otsu later
        BufferedImage finalImage = ImageUtils.binarize(baseImage,144);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_Bin.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // clean small trails and remove lines
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

        finalImage = ImageUtils.removeSmallTrails(finalImage, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - forTesseract.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // this is used for extracting the tesseract array,
        // because removeLines removes characters if they overlap with large cracks
        BufferedImage joinedLinesImage = ImageUtils.joinDisjointedBridges(finalImage);
        Pix pixForTesseractImages = ImageUtils.getDepth1Pix(joinedLinesImage, 144);

        finalImage = ImageUtils.removeLines(finalImage, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - horLinesRemoved.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        finalImage = ImageUtils.removeLines(finalImage, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - verLinesRemoved.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // Make the image fit for connected components.
        // If there are images with thin 1 pixel lines, they may not be connected...this routine corrects
        // that so that the connected components are properly formed
        finalImage = ImageUtils.joinDisjointedBridges(finalImage);
        imageWrite(imageCounter + " - 20 - bridgesJoined.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        Pix connCompReadyPix = ImageUtils.getDepth1Pix(finalImage);

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

        Pix tesseractRotatedPix8 = ImageUtils.rotatePix(pixForTesseractImages, rotationAngle, true);
        Pix tesseractRotatedPix = ImageUtils.getDepth1Pix(tesseractRotatedPix8);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedTesseractPix.png", tesseractRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        ArrayList<ArrayList<Rectangle>> lines = getLines(rotatedPix, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);

        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);
        LeptUtils.dispose(smallAndLargeBitsCleaned);
        LeptUtils.dispose(rotatedPix8);
        LeptUtils.dispose(rotatedPix);
        LeptUtils.dispose(pixForTesseractImages);
        LeptUtils.dispose(tesseractRotatedPix8);

        ArrayList<ArrayList<Rectangle>> twoLines = TesseractUtils.arrangeInto2Lines(lines);
        logger.debug("Lines found from arranging into 2 lines are : " + lines);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(tesseractRotatedPix, twoLines);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix E2ECleaned1 = ImageUtils.cleanThenJoinDisjointedBridges(pixWithE2ELines);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - E2ECleaned.png", E2ECleaned1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Note: Horizontal lines cleaning has higher tolerance the second time i.e. this time
        Pix E2ECleaned2 = ImageUtils.removeLines(E2ECleaned1, ImageUtils.HORIZONTAL_DIRECTION, (int) dimensions.getWidth() * 2);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - E2ECleaned-HorLineRemoved.png", E2ECleaned2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Note: Vertical lines cleaning has higher tolerance the second time i.e. this time
        Pix E2ECleaned = ImageUtils.removeLines(E2ECleaned2, ImageUtils.VERTICAL_DIRECTION, (int) dimensions.getHeight() + 5);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - E2ECleaned-VertLineRemoved.png", E2ECleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(tesseractRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);

        // we cannot use the lines found in the previous image because larger boxes are removed by the
        // remove lines method. These boxes get reinstated back by the repeated process which clips only the relevant
        // rectangle from the image

        lines = getLines(E2ECleaned, 0.375, 2.75, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);

        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        logger.trace("Lines are : " + lines);
        drawBoundingBoxesOnPix(E2ECleaned, lines, imageCounter + " - " + imageSerialCounter + "- E2ECleaned-forceFitInto2.png", TraceLevel.TRACE);

        double[] scalingFactors = new double[]{1.0, 1.2, 1.4};
        int erodeOrDilate = ImageUtils.LEAVE_UNCHANGED;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(E2ECleaned, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        LeptUtils.dispose(E2ECleaned1);
        LeptUtils.dispose(E2ECleaned2);
        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(pixArrayForTesseract);

        logger.debug("Strings after OCR are : " + results);
        System.out.println("Strings after OCR are : " + results);
        int numberOfLines = lines.size();
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < Math.min(numberOfLines, regexPatterns.size()); ++i) {
            ArrayList<String> matchedStrings = new ArrayList<>();
            System.out.println("Regex patterns are " + regexPatterns);
            // ArrayList<Pattern> toBeMatchedPatterns = regexPatterns.get(i);
            Pattern toBeMatchedPattern = regexPatterns.get(i);
            for (int j = 0; j < repeats; ++j) {
                String ocrString = CharacterUtils.removeSpaces(CharacterUtils.getOnlyAlphabetsAndNumbers(results.get(i * repeats + j)));
//                for (Pattern aPatternToBeMatched : toBeMatchedPatterns) {
//                    ArrayList<String> matchingStrings = matchPattern(ocrString, aPatternToBeMatched);
//                    for (String aString : matchingStrings) {
//                        matchedStrings.add(aString);
//                    }
//                }
                ArrayList<String> matchingStrings = matchPattern(ocrString, toBeMatchedPattern);
                for (String aString : matchingStrings) {
                    matchedStrings.add(aString);
                }
            }
            // logger.debug("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            // System.out.println("Matching strings at index : " + i + " = " + Arrays.deepToString(matchedStrings.stream().toArray()));
            logger.debug("Matching strings at index : " + i + " = " + matchedStrings);
            System.out.println("Matching strings at index : " + i + " = " + matchedStrings);
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
}
