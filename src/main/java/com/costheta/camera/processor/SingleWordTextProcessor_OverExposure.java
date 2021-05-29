package com.costheta.camera.processor;

import com.costheta.image.TraceLevel;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.machine.BaseClipImageProcessor;
import com.costheta.machine.ProcessingResult;
import com.costheta.tesseract.TesseractUtils;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.text.utils.PatternMatchedStrings;
import javafx.geometry.Dimension2D;
import javafx.scene.shape.Rectangle;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class SingleWordTextProcessor_OverExposure extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(SingleWordTextProcessor_OverExposure.class);
    private int runCounter = 1;
    private static int connectivity = 4;

    public SingleWordTextProcessor_OverExposure(String name, ArrayList<String> patterns) {
        this(name, patterns, patterns.size());
        logger.trace("Leaving constructor");
    }

    public SingleWordTextProcessor_OverExposure(String name, ArrayList<String> patterns, int assessmentRegions) {
        super(name, patterns, assessmentRegions);
        if (patterns.size() != assessmentRegions) {
            throw new IllegalStateException("In a SingleWordTextProcessor, the number of patterns must be equal to the number of assessment regions."
            + "In this case, number of patterns is " + patterns.size() + " and the number of assessment regions is " + assessmentRegions);
        }
        System.out.println("In SingleWordTextProcessor_FasterApproach() processor with assessmentRegions = " + assessmentRegions);
        logger.trace("Leaving constructor");
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

        // clip the relevant part of the picture

        Box[] clipBoxes = new Box[clipBoxRectangles.size()];
        Pix[] pixCopies = new Pix[clipBoxRectangles.size()];
        for (int i = 0; i < clipBoxes.length; ++i) {
            clipBoxes[i] = Leptonica1.boxCreate((int) clipBoxRectangles.get(i).getX(), (int) clipBoxRectangles.get(i).getY(), (int) clipBoxRectangles.get(i).getWidth(), (int) clipBoxRectangles.get(i).getHeight());
            pixCopies[i] = Leptonica1.pixCopy(null, originalPixLarge);
        }

        final ArrayList<CompletableFuture<ProcessingResult>> processingThreads = new ArrayList<CompletableFuture<ProcessingResult>>(
                clipBoxRectangles.size());
        for (int i = 0; i < clipBoxRectangles.size(); ++i) {
            final int index = i;
            processingThreads.add(CompletableFuture.supplyAsync(() -> {
                ProcessingResult result = processForEach(pixCopies[index], clipBoxes[index], patterns.get(index), regexPatterns.get(index), index);
                return result;
            }, parallelThreadPool));
        }
        CompletableFuture.allOf(processingThreads.toArray(new CompletableFuture[processingThreads.size()])).join();

        for (int i = 0; i < clipBoxRectangles.size(); ++i) {
            System.out.println("Disposing in cycle " + i);
            LeptUtils.dispose(clipBoxes[i]);
            LeptUtils.dispose(pixCopies[i]);
        }
        LeptUtils.dispose(originalPixLarge);
        ProcessingResult result = null;
        try {
            result = processingThreads.get(0).get();
        } catch (Exception e) {

        }
        ++runCounter;
        return result;
    }

    private ProcessingResult processForEach(Pix originalLargePix, Box clipBox, String stringPattern, Pattern pattern, int threadNumber) {

        int imageSerialCounter = 1;

        Pix originalPix = Leptonica1.pixClipRectangle(originalLargePix, clipBox, null);

        // If 12 MP picture input, downsample twice
        // If 3 MP pictureinput, downsample once

        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - Original8.png", originalReduced8, ILeptonica.IFF_PNG, TraceLevel.INFO);

        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        // get the character dimensions from the properties file
        Dimension2D dimensions = getCharacterDimensions();

        // Get background normalised and contrast normalised image
        Pix normBackPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160);
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - normBackPix.png", normBackPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage nbImage = ImageUtils.convertPixToImage(normBackPix);
        // nbImage = ImageUtils.relativeGammaContrastEnhancement(nbImage,(int) dimensions.getWidth() / 2, (int) dimensions.getHeight() / 2);
        // nbImage = ImageUtils.gammaContrastEnhancementSkipWhites(nbImage,192, ImageUtils.GAMMA_ENHANCEMENT_NORMAL);
        nbImage = ImageUtils.gammaContrastEnhancementSkipWhites(nbImage,ImageUtils.GAMMA_ENHANCEMENT_NORMAL);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm1.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        nbImage = ImageUtils.gammaContrastEnhancement(nbImage);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm2.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        nbImage = ImageUtils.relativeGammaContrastEnhancement(nbImage,(int) dimensions.getWidth(), (int) dimensions.getHeight(), 60);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm3.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        nbImage = ImageUtils.relativeGammaContrastEnhancement(nbImage,(int) dimensions.getWidth() / 2, (int) dimensions.getHeight() / 2, 40);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm4.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        // nbImage = ImageUtils.lightenIntermediates(nbImage);
        // imageWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm3.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

        // nbImage = ImageUtils.relativeGammaContrastEnhancement(nbImage,(int) dimensions.getWidth() / 2, (int) dimensions.getHeight() / 2);
//        nbImage = ImageUtils.relativeGammaContrastEnhancement(nbImage,5, 5);
//        imageWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNorm5.png", nbImage, BaseCamera.PNG, TraceLevel.TRACE);

//        Pix geNormBackPix = ImageUtils.getDepth8Pix(nbImage);
//        pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - geNormBackPix.png", geNormBackPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        // Pix bilateralGrayPix = Leptonica1.pixBilateralGray(normBackPix, (Math.min(normBackPix.w, normBackPix.h) - 5) * 1.0f, 50f, 16, 4);
        // pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - bilateralGrayPix.png", bilateralGrayPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

//        Pix contNormPix = Leptonica1.pixContrastNorm(null, geNormBackPix, normBackPix.w / 2,
//                normBackPix.h / 2, 60, 2, 2); // 2, 2
//        pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - cnPix.png", contNormPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Pix sharpenedPix = Leptonica1.pixUnsharpMaskingGray(contNormPix, 5, 0.7f);
        // pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - sharpenedPix.png", sharpenedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // BufferedImage sharpenedImage = ImageUtils.convertPixToImage(sharpenedPix);
//        int bucketSize = 3;
//        int[] histogram = new int[256/bucketSize + 1];
//        int[][] pixels = new int[sharpenedImage.getHeight()][sharpenedImage.getWidth()];
//        for (int j = 0; j < sharpenedImage.getHeight(); ++j) {
//            for (int i = 0; i < sharpenedImage.getWidth(); ++i) {
//                int pixel = sharpenedImage.getRGB(i,j) & 0x000000FF;
//                pixels[j][i] = pixel;
//                histogram[pixel / bucketSize]++;
//            }
//        }
//        // smoothen the histogram
//        int smootheningPeriod = 3;
//        double smootheningMultiplier = 2.0 / (smootheningPeriod + 1);
//        double[] smoothedHistogram = new double[histogram.length];
//        smoothedHistogram[0] = histogram[0];
//        for (int i = 1; i < histogram.length; ++i) {
//            smoothedHistogram[i] = smoothedHistogram[i-1] * (1 - smootheningMultiplier) + smoothedHistogram[i] * smootheningMultiplier;
//        }
//        int minimaIndex = 4;
//        double minima = smoothedHistogram[minimaIndex];
//        for (int i = minimaIndex + 2; i < smoothedHistogram.length; ++i) {
//            if ((smoothedHistogram[i] > smoothedHistogram[i - 1]) && ((smoothedHistogram[i - 1] > minima))){
//                minimaIndex = i - 1;
//                break;
//            } else {
//                minima = smoothedHistogram[i - 1];
//            }
//        }
//        int binarizationPixel = minimaIndex * bucketSize + ((bucketSize + 1) / 2);
//        System.out.println("Found minima / binarization cutoff at pixel value " +  binarizationPixel);
//
//        BufferedImage binarizedImage = ImageUtils.binarize(sharpenedImage, binarizationPixel);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(normBackPix);
        // LeptUtils.dispose(bilateralGrayPix);
        // LeptUtils.dispose(contNormPix);

//        PointerByReference pbrSauvola = new PointerByReference();
//        int widthDivisions = 2;
//        int heightDivisions = 2;
//        int success = 1;
        //success = Leptonica1.pixSauvolaBinarizeTiled(sharpenedPix, Math.min(25, Math.min(sharpenedPix.h / heightDivisions, sharpenedPix.w / widthDivisions)), 0.70f,
        //        widthDivisions,
        //       heightDivisions, null, pbrSauvola);
//        success = Leptonica1.pixSauvolaBinarizeTiled(contNormPix, Math.min(25, Math.min(normBackPix.h / heightDivisions, normBackPix.w / widthDivisions)), 0.70f,
//                widthDivisions,
//                heightDivisions, null, pbrSauvola);
//        Pix binarized = new Pix(pbrSauvola.getValue());
//        pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - Sauvola.png", binarized, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // LeptUtils.dispose(sharpenedPix);
//        LeptUtils.dispose(contNormPix);

        // Pix binarized = ImageUtils.getDepth1Pix(binarizedImage);

        BufferedImage binarizedImage = ImageUtils.binarize(nbImage, 80); // 80 is from experience of looking at histograms
        Pix binarized = ImageUtils.getDepth1Pix(binarizedImage);
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - binarized.png", binarized, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // remove horizontal lines
        // note that ImageUtils adds 2 pixels as a matter of abundant caution
        Pix removeHorLines = ImageUtils.removeLines(binarized, ImageUtils.HORIZONTAL_DIRECTION, (int) (dimensions.getWidth()) * 3);
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - removeHorLines.png", removeHorLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // remove vertical lines
        // note that ImageUtils adds 2 pixels as a matter of abundant caution
        Pix removeVertLines = ImageUtils.removeLines(removeHorLines, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() * 1.5));
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - removeVertLines.png", removeVertLines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage baseImage = ImageUtils.convertPixToImage(removeVertLines);

        // clean the image of small black trails
        ArrayList<CleaningKernel> cKernels = new ArrayList<>();

        for (int i = 3; i < dimensions.getWidth() * 2.0 / 3; i = i + 2) {
            for (int j = 3; j < dimensions.getHeight() * 2.0 / 3; j = j + 2) {
                cKernels.add(new CleaningKernel(i, j));
            }
        }
        baseImage = ImageUtils.removeSmallTrails(baseImage, cKernels, true);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - smallTrailsCleaned.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        baseImage = ImageUtils.joinDisjointedBridges(baseImage);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - baseImage.png", baseImage, BaseCamera.PNG, TraceLevel.TRACE);

        // -----BASE IMAGE got for extracting letters

        Pix connCompReadyPix = ImageUtils.getDepth1Pix(baseImage);

        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix smallBitsCleaned = Leptonica1.pixCreate(Leptonica1.pixGetWidth(connCompReadyPix),
                Leptonica1.pixGetHeight(connCompReadyPix), 1);
        Leptonica1.pixSetBlackOrWhite(smallBitsCleaned, ILeptonica.L_SET_WHITE);
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);
            //if ((aBox.w < dimensions.getWidth() / 5) || (aBox.h < dimensions.getHeight() / 2) || (aBox.h > dimensions.getHeight() * 1.3) || (aBox.w > dimensions.getWidth() * 2.5)) {
            if ((aBox.w < dimensions.getWidth() / 5) || (aBox.h < dimensions.getHeight() / 2) || (aBox.w > dimensions.getWidth() * 3)) {
                logger.trace("Creating smallBitsCleaned : Dropping box at " + aBox.x + "," + aBox.y + "," + aBox.w + "," + aBox.h);
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(connCompReadyPix, aBox, null);
            Leptonica1.pixRasterop(smallBitsCleaned, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);
        }
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - smallBitsCleaned.png", smallBitsCleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(removeHorLines);
        LeptUtils.dispose(removeVertLines);
        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);

        ArrayList<ArrayList<Rectangle>> lines = getWord(smallBitsCleaned, 0.375, 2.75, dimensions, runCounter, imageSerialCounter++, threadNumber, TraceLevel.TRACE);
        logger.debug("Lines found : " + lines);

        LeptUtils.dispose(smallBitsCleaned);

        // float rotationAngle = ImageUtils.findSkewAngle(smallBitsCleaned, 128);
        // Pix rotatedPix8 = ImageUtils.rotatePix(smallBitsCleaned, rotationAngle, true);
        // Pix rotatedPix = ImageUtils.getDepth1Pix(rotatedPix8);
        // pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Pix tesseractRotatedPix8 = ImageUtils.rotatePix(binarized, rotationAngle, true);
        // Pix tesseractRotatedPix = ImageUtils.getDepth1Pix(tesseractRotatedPix8);
        // pixWrite(imageCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - rotatedTesseractPix.png", tesseractRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(binarized, lines);
        pixWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Note: Vertical lines cleaning has higher tolerance this time
        // Note: No cleaning of horizontal lines needed this time
        BufferedImage finalImage = ImageUtils.convertPixToImage(pixWithE2ELines);
        finalImage = ImageUtils.removeLines(finalImage, ImageUtils.VERTICAL_DIRECTION, (int) (dimensions.getHeight() * 1.4));
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - finalVerLinesRemoved.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);

        // Make the image fit for connected components.
        // If there are images with thin 1 pixel lines, they may not be connected...this routine corrects
        // that so that the connected components are properly formed
        finalImage = ImageUtils.cleanThenJoinDisjointedBridges(finalImage);
        imageWrite(runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + " - bridgesJoined.png", finalImage, BaseCamera.PNG, TraceLevel.TRACE);
        Pix E2ECleaned = ImageUtils.getDepth1Pix(finalImage, 144);

        // LeptUtils.dispose(rotatedPix);
        // LeptUtils.dispose(tesseractRotatedPix8);
        // LeptUtils.dispose(tesseractRotatedPix);
        LeptUtils.dispose(pixWithE2ELines);

        // Note: We cannot use the lines found in the previous image because larger boxes are removed by the
        // remove lines method. These boxes get reinstated back by the repeated process which clips only the relevant
        // rectangle from the image

        lines = getWord(E2ECleaned, 0.375, 2.75, dimensions, runCounter, imageSerialCounter++, threadNumber, TraceLevel.TRACE);

        logger.trace("Lines are : " + lines);
        drawBoundingBoxesOnPix(E2ECleaned, lines, runCounter + " - " + threadNumber + " - " + imageSerialCounter++ + "- E2ECleaned-forceFitInto2.png", TraceLevel.TRACE);

        double[] scalingFactors = new double[]{0.7, 0.85, 1.0, 1.1, 1.2};
        int erodeOrDilate = ImageUtils.LEAVE_UNCHANGED;

        Pixa pixArrayForTesseract = ImageUtils.getDerivativeImages(E2ECleaned, lines, scalingFactors, erodeOrDilate);
        ImageUtils.printPixArray(pixArrayForTesseract, getBaseStringPathOfProcessorImages(), runCounter, threadNumber, TraceLevel.DEBUG);
        ArrayList<String> results = TesseractUtils.doOCR(pixArrayForTesseract);

        // LeptUtils.dispose(binarized);
//        Leptonica1.pixDestroy(pbrSauvola);
        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(pixArrayForTesseract);

        logger.debug("Strings after OCR are : " + results);
        System.out.println("Strings after OCR are : " + results);
        int numberOfLines = lines.size(); // can be 0 or 1, hence better to keep it as lines.size() instead of specifying 1
        int repeats = scalingFactors.length;
        ArrayList<String> finalStrings = new ArrayList<>(numberOfLines);

        for (int i = 0; i < Math.min(numberOfLines, 1); ++i) {
            PatternMatchedStrings matchedStrings = new PatternMatchedStrings();
            System.out.println("Regex pattern is " + pattern);
            // ArrayList<Pattern> toBeMatchedPatterns = regexPatterns.get(i);
            Pattern toBeMatchedPattern = pattern;
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
            logger.debug("After matching with pattern " + stringPattern + " the final commonString is " + commonString);
            System.out.println("After matching with pattern " + stringPattern + " the final commonString is " + commonString);
            finalStrings.add(commonString);
        }

        finalStrings = rectifyResults(finalStrings);
        logger.trace("Final Strings are : " + finalStrings);

        ++runCounter;

        ArrayList<ArrayList<String>> strings = CharacterUtils.forceFitIntoMultiLineMultiColumn(finalStrings, 1);
        ProcessingResult pResult = new ProcessingResult(lines, new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), strings, true);
        logger.trace("Done");
        return pResult;
    }
}
