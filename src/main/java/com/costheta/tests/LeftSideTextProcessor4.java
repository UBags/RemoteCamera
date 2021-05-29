package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseClipImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import javafx.geometry.Dimension2D;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.scene.shape.Rectangle;

public class LeftSideTextProcessor4 extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(LeftSideTextProcessor4.class);
    private int imageCounter = 1;
    private static int connectivity = 4;

    public LeftSideTextProcessor4(String name, ArrayList<String> patterns) {
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
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        LeptUtils.dispose(clipBox);
        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        // get the character dimensions from the properties file
        Dimension2D dimensions = getCharacterDimensions();

        // Get background normalised and contrast normalised image
        Pix bnPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        Pix cnPix = Leptonica1.pixContrastNorm(null, bnPix, bnPix.w / 8,
                bnPix.h / 4, 100, 2, 2); // 2, 2
        // Again, background normalise the contrast normalised image
        Pix bnPix1 = Leptonica1.pixBackgroundNormFlex(cnPix, 7, 7, 2, 2, 60); // 100

        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bnPix.png", bnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - cnPix.png", cnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - bn_cn_Pix.png", bnPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage cnImage = ImageUtils.convertPixToImage(cnPix);

        // This will be the go forward image for additional work
        BufferedImage bnImage1 = ImageUtils.convertPixToImage(bnPix1);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(bnPix);
        LeptUtils.dispose(cnPix);
        LeptUtils.dispose(bnPix1);

        BufferedImage blurredImageForMask = ImageUtils.blur(bnImage1,5,5);
        // blurredImageForMask = ImageUtils.blur(blurredImageForMask,5,5);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - blurredForMask.png", blurredImageForMask, BaseCamera.PNG, TraceLevel.TRACE);

        // gets a rough mask, then masks the original with this mask,
        // then does relative gamma enhancement (with 'skipWhites') of the relevant parts
        BufferedImage makeMask_1 = ImageUtils.gammaEnhancementWithMask(blurredImageForMask, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), (int) dimensions.getWidth(), (int) dimensions.getHeight(), 30); // cnImage, (* 1.5 each), and 40 percentile
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask1.png", makeMask_1, BaseCamera.PNG, TraceLevel.TRACE);

        // binarize
        BufferedImage makeMask_2 = ImageUtils.binarize(makeMask_1, 112);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask2.png", makeMask_2, BaseCamera.PNG, TraceLevel.TRACE);

        // remove horizontal lines
        BufferedImage makeMask_3 = ImageUtils.removeLines(makeMask_2, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask3.png", makeMask_3, BaseCamera.PNG, TraceLevel.TRACE);

        // remove vertical lines
        makeMask_3 = ImageUtils.removeLines(makeMask_3, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() + 2));
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask4.png", makeMask_3, BaseCamera.PNG, TraceLevel.TRACE);

        // clean the image of small black trails
        ArrayList<CleaningKernel> cKernels = new ArrayList<>();
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,3));
        cKernels.add(new CleaningKernel(3,5));
        cKernels.add(new CleaningKernel(3,3));
        makeMask_3 = ImageUtils.removeSmallTrails(makeMask_3, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask5.png", makeMask_3, BaseCamera.PNG, TraceLevel.TRACE);

        // erode to get a more accurate mask
        BufferedImage mask = ImageUtils.erodeGrayBI(makeMask_3, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0)); // height was divided by 2 earlier
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - mask.png", mask, BaseCamera.PNG, TraceLevel.TRACE);

        // mask the background BN-CN image
        BufferedImage bnCnMasked = ImageUtils.imageMask(bnImage1, mask);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - bnCnMasked.png", bnCnMasked, BaseCamera.PNG, TraceLevel.TRACE);

        // BufferedImage GE_OverallAverage = ImageUtils.gammaAverageEnhancementSkipWhites(bnCnMasked, 242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        // imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_OverallAverage.png", GE_OverallAverage, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_RelativeAverage = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0),242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage2020.png", GE_RelativeAverage, BaseCamera.PNG, TraceLevel.TRACE);

        GE_RelativeAverage = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_RelativeAverage, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), 15, 242);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage2020.png", GE_RelativeAverage, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_RelativeAverage1 = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 1.5), (int) (dimensions.getHeight() * 1.5),242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage1515.png", GE_RelativeAverage1, BaseCamera.PNG, TraceLevel.TRACE);

        GE_RelativeAverage1 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_RelativeAverage1, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), 15, 242);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage1515.png", GE_RelativeAverage1, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_RelativeAverage2 = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 1.0), (int) (dimensions.getHeight() * 1.0),242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage1010.png", GE_RelativeAverage2, BaseCamera.PNG, TraceLevel.TRACE);

        GE_RelativeAverage2 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_RelativeAverage2, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), 15, 242);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage1010.png", GE_RelativeAverage2, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_RelativeAverage3 = ImageUtils.relativeGammaAverageEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 0.5), (int) (dimensions.getHeight() * 0.5),242, ImageUtils.GAMMA_ENHANCEMENT_EXTREME);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage0505.png", GE_RelativeAverage3, BaseCamera.PNG, TraceLevel.TRACE);

        GE_RelativeAverage3 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_RelativeAverage3, (int) (dimensions.getWidth() * 2.0), (int) (dimensions.getHeight() * 2.0), 15, 242);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_RelativeAverage0505.png", GE_RelativeAverage3, BaseCamera.PNG, TraceLevel.TRACE);

        bnCnMasked = ImageUtils.fullImageGammaEnhancementWithBias(bnCnMasked);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_bnCnMasked.png", bnCnMasked, BaseCamera.PNG, TraceLevel.TRACE);

        // relative gamma enhancement (with 'skipWhites'), large kernels, 75 percentile
        BufferedImage GE = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(bnCnMasked, (int) (dimensions.getWidth() * 1.5), (int) (dimensions.getHeight() * 1.5), 85, 242); // (1.5, 1.5, 75); (1.5, 1.5, 60); (2.0, 2.0, 40)
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE.png", GE, BaseCamera.PNG, TraceLevel.TRACE);

        // sharpen the image (Unsharp Filter)
        // GE = ImageUtils.sharpen(GE, 3);
        GE = ImageUtils.copyBI(GE);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_Copy (Unsharp Removed).png", GE, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_thinLinesDarkened = ImageUtils.darkenThinLightHorizontalLines(GE, 3, 5, 70);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_ThinLinesDarkened.png", GE_thinLinesDarkened, BaseCamera.PNG, TraceLevel.TRACE);


        // do relative gamma enhancement of the sharpened image, character area, 75 percentile
        BufferedImage sharp_GE = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_thinLinesDarkened, (int) (dimensions.getWidth() * 4.0/ 4), (int) (dimensions.getHeight() * 4.0 / 4), 40, 248); // 75 // 50
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_Sharp_1.png", sharp_GE, BaseCamera.PNG, TraceLevel.TRACE);

        // do relative gamma enhancement with skipWhites, small kernel, 40 percentile
        BufferedImage sharp_GE_GE = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(sharp_GE,(int) (dimensions.getWidth() * 2.0/ 4), (int) (dimensions.getHeight() * 2.0 / 4), 20, 254); // 40
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_SS.png", sharp_GE_GE, BaseCamera.PNG, TraceLevel.TRACE);

        // binarize - currently with a fixed number - maybe changed to local sauvola or local otsu later
        BufferedImage finalBinarized = ImageUtils.binarize(sharp_GE_GE,192);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - GE_Bin.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

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

        finalBinarized = ImageUtils.removeSmallTrails(finalBinarized, cKernels, true);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - forTesseract.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        // this is used for extracting the tesseract array,
        // because removeLines removes characters if they overlap with large cracks
        Pix pixForTesseractImages = ImageUtils.getDepth1Pix(finalBinarized, 128);

        finalBinarized = ImageUtils.removeLines(finalBinarized, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - horLinesRemoved.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        finalBinarized = ImageUtils.removeLines(finalBinarized, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight()) + 2);
        imageWrite(imageCounter + " - " + imageSerialCounter++ + " - verLinesRemoved.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        // make the image fit for connected components
        finalBinarized = ImageUtils.joinDisjointedBridges(finalBinarized);
        imageWrite(imageCounter + " - 20 - bridgesJoined.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        Pix connCompReadyPix = ImageUtils.getDepth1Pix(finalBinarized, 128);

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
        Pix rotatedPix = ImageUtils.getDepth1Pix(rotatedPix8, 128);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix tesseractRotatedPix8 = ImageUtils.rotatePix(pixForTesseractImages, rotationAngle, true);
        Pix tesseractRotatedPix = ImageUtils.getDepth1Pix(tesseractRotatedPix8, 128);
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

        // change to cleanAndJoin after removing the large kernels from cleanAndJoin
        Pix E2ECleaned = ImageUtils.cleanThenJoinDisjointedBridges(pixWithE2ELines);
        pixWrite(imageCounter + " - " + imageSerialCounter++ + " - E2ECleaned.png", E2ECleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(tesseractRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);

        lines = getLines(E2ECleaned, 0.375, 2.50, dimensions, imageCounter, imageSerialCounter++, TraceLevel.TRACE);

        // saves the day sometimes when the splitline routine above does not return the right line configuration
        lines = TesseractUtils.forceFitIntoTwoLinesMultiColumns(lines, patterns);
        logger.trace("Lines are : " + lines);

        double[] scalingFactors = new double[]{1.0, 1.2, 1.4};
        int erodeOrDilate = ImageUtils.LEAVE_UNCHANGED;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(E2ECleaned, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), imageCounter, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(pixArrayForTesseract);

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
            String commonString = CharacterUtils.getCommonString(strings, (i < patterns.size() ? patterns.get(i) : ""));
            logger.debug("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
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
