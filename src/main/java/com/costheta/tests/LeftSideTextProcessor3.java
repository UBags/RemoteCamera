package com.costheta.tests;

import com.costheta.image.TraceLevel;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.*;
import com.costheta.tesseract.TesseractUtils;
import com.sun.jna.ptr.PointerByReference;
import javafx.geometry.Dimension2D;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.scene.shape.Rectangle;


public class LeftSideTextProcessor3 extends BaseClipImageProcessor {

    private static final Logger logger = LogManager.getLogger(LeftSideTextProcessor3.class);
    private int imageCounter = 1;
    private static int connectivity = 4;

    public LeftSideTextProcessor3(String name, ArrayList<String> patterns) {
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

        Pix bnPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 2, 2, 160); // 2, 2
        Pix cnPix = Leptonica1.pixContrastNorm(null, bnPix, bnPix.w / 4,
                bnPix.h / 2, 100, 1, 1); // (/ 4),( /3), 2, 2
        pixWrite(imageCounter + " - 2 - bnPix.png", bnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);
        pixWrite(imageCounter + " - 3 - cnPix.png", cnPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // Background normalise the contrast normalised image
        Pix bnPix1 = Leptonica1.pixBackgroundNormFlex(cnPix, 7, 7, 2, 2, 60); // 100
        pixWrite(imageCounter + " - 3A - bn_cn_Pix.png", bnPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage bnImage1 = ImageUtils.convertPixToImage(bnPix1);

        BufferedImage cnImage = ImageUtils.convertPixToImage(cnPix);
        Dimension2D dimensions = getCharacterDimensions();

        BufferedImage GE = ImageUtils.gammaEnhancementWithMask(cnImage, (int) dimensions.getWidth() * 2, (int) dimensions.getHeight() * 2, (int) dimensions.getWidth(), (int) dimensions.getHeight(), 40);
        imageWrite(imageCounter + " - 0 - GE - 1.png", GE, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_bin = ImageUtils.binarize(GE, 80);
        imageWrite(imageCounter + " - 0 - GE - 2.png", GE_bin, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GECleaned = ImageUtils.removeLines(GE_bin, ImageUtils.HORIZONTAL_DIRECTION, 19);
        imageWrite(imageCounter + " - 0 - GE - 2A.png", GECleaned, BaseCamera.PNG, TraceLevel.TRACE);

        GECleaned = ImageUtils.removeLines(GECleaned, ImageUtils.VERTICAL_DIRECTION, 13);
        imageWrite(imageCounter + " - 0 - GE - 2B.png", GECleaned, BaseCamera.PNG, TraceLevel.TRACE);

        ArrayList<CleaningKernel> cKernels = new ArrayList<>();
        cKernels.add(new CleaningKernel(3,3));
        cKernels.add(new CleaningKernel(5,3));
        cKernels.add(new CleaningKernel(3,5));
        cKernels.add(new CleaningKernel(3,3));
        GECleaned = ImageUtils.removeSmallTrails(GECleaned, cKernels, true);
        imageWrite(imageCounter + " - 0 - GE - 3.png", GECleaned, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_mask = ImageUtils.erodeGrayBI(GECleaned, (int) (dimensions.getWidth() * 1.25), (int) dimensions.getHeight() / 2);
        imageWrite(imageCounter + " - 0 - GE - 4.png", GE_mask, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_mask_original = ImageUtils.imageMask(bnImage1, GE_mask);
        imageWrite(imageCounter + " - 0 - GE - 5.png", GE_mask_original, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage GE_final = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_mask_original, (int) (dimensions.getWidth() * 3.0), (int) (dimensions.getHeight() *2.0), 75, 242);
        imageWrite(imageCounter + " - 0 - GE - 6.png", GE_final, BaseCamera.PNG, TraceLevel.TRACE);

        // BufferedImage bnImage2 = ImageUtils.relativeGammaContrastEnhancement(cnImage, (int) dimensions.getWidth() * 2, (int) dimensions.getHeight(), 20);
        BufferedImage bnImage2 = ImageUtils.relativeGammaContrastEnhancement(cnImage, (int) (dimensions.getWidth() * 12.0/4), (int) (dimensions.getHeight() * 12.0 / 4), 15);
        BufferedImage bnImage3 = ImageUtils.imageAverage(bnImage1, bnImage2);
        imageWrite(imageCounter + " - 3B - RGE_cn_Image-25.png", bnImage2, BaseCamera.PNG, TraceLevel.TRACE);
        imageWrite(imageCounter + " - 3C - Average.png", bnImage3, BaseCamera.PNG, TraceLevel.TRACE);

        LeptUtils.dispose(bnPix1);

        BufferedImage RGE_CNImage = ImageUtils.relativeGammaContrastEnhancement(bnImage3, (int) (dimensions.getWidth() * 12.0/ 4), (int) (dimensions.getHeight() * 8.0 / 4), 25);
        imageWrite(imageCounter + " - 4 - RGE_CNImage.png", RGE_CNImage, BaseCamera.PNG, TraceLevel.TRACE);

        // BufferedImage averaged = ImageUtils.imageAverageSkipWhites(GE_final,RGE_CNImage);
        // BufferedImage averaged = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(GE_final, (int) (dimensions.getWidth() * 4.0/ 4), (int) (dimensions.getHeight() * 4.0 / 4), 40, 255);
        GE_final = ImageUtils.sharpen(GE_final, 3);
        BufferedImage averaged = ImageUtils.relativeScaledGammaContrastEnhancement(GE_final, (int) (dimensions.getWidth() * 4.0/ 4), (int) (dimensions.getHeight() * 4.0 / 4), 75);
        imageWrite(imageCounter + " - 0 - GE - 7.png", averaged, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage final1 = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(averaged,(int) (dimensions.getWidth() * 2.0/ 4), (int) (dimensions.getHeight() * 2.0 / 4), 40, 254);
        imageWrite(imageCounter + " - 0 - GE - 8.png", final1, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage finalBinarized = ImageUtils.binarize(final1,192);
        imageWrite(imageCounter + " - 0 - GE - 9.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

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
        imageWrite(imageCounter + " - 0 - GE - 10.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        // finalBinarized = ImageUtils.removeLines(finalBinarized, ImageUtils.BOTH_DIRECTION, (int) Math.max(dimensions.getHeight(), dimensions.getWidth()) + 3);
        finalBinarized = ImageUtils.removeLines(finalBinarized, ImageUtils.HORIZONTAL_DIRECTION, 17);
        imageWrite(imageCounter + " - 0 - GE - 11.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        finalBinarized = ImageUtils.removeLines(finalBinarized, ImageUtils.VERTICAL_DIRECTION, 13);
        imageWrite(imageCounter + " - 0 - GE - 12.png", finalBinarized, BaseCamera.PNG, TraceLevel.TRACE);

        LeptUtils.dispose(originalReduced8);
        LeptUtils.dispose(bnPix);
        LeptUtils.dispose(cnPix);

        // create mask
        BufferedImage RGE_CNImage_Adjusted = ImageUtils.lightenCentreCellsYAxis(RGE_CNImage, 5, 5);
        imageWrite(imageCounter + " - 5 - RGE_CN_Lightened.png", RGE_CNImage_Adjusted, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage closedBI = ImageUtils.closeGrayBI (RGE_CNImage_Adjusted, (int) dimensions.getWidth(), ((int) dimensions.getWidth()) / 3);
        imageWrite(imageCounter + " - 6 - closedBI.png", closedBI, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage bin_closedBI_Temp = ImageUtils.binarize(closedBI, 128);
        imageWrite(imageCounter + " - 7 - bin_closedBI.png", bin_closedBI_Temp, BaseCamera.PNG, TraceLevel.TRACE);

        BufferedImage aMaskBI = ImageUtils.invert(bin_closedBI_Temp);
        imageWrite(imageCounter + " - 8 - Mask.png", aMaskBI, BaseCamera.PNG, TraceLevel.TRACE);

        Pix RGE_CN_Pix32 = ImageUtils.convertImageToPix(RGE_CNImage_Adjusted);
        Pix RGE_CN_Pix = ImageUtils.getDepth8Pix(RGE_CN_Pix32);

        PointerByReference pbrSauvola = new PointerByReference();
        int success = Leptonica1.pixSauvolaBinarizeTiled(RGE_CN_Pix, 11, 0.20f,
                (RGE_CN_Pix.w / 3) + 1,4, null, pbrSauvola);
        logger.trace("Sauvola success of " + processorName + " = " + success);
        Pix pixSauvola = new Pix(pbrSauvola.getValue());
        pixWrite(imageCounter + " - 9 - sauvolaPix.png", pixSauvola, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(RGE_CN_Pix32);
        LeptUtils.dispose(RGE_CN_Pix);

        Pix pixSauvolaCleaned1 = ImageUtils.removeSaltPepper(pixSauvola);
        pixWrite(imageCounter + " - 10 - sauvolaSaltPepper.png", pixSauvolaCleaned1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        //BufferedImage sauvolaCleanedBI = ImageUtils.convertPixToImage(pixSauvolaCleaned1);
        //BufferedImage diagonalsCleanedBI = ImageUtils.eliminateBlackBridgesIteratively(sauvolaCleanedBI, 1, 128, ImageUtils.VERTICAL_DIRECTION);
        //imageWrite(imageCounter + " - 10A - diagonalsCleaned.png", diagonalsCleanedBI, BaseCamera.PNG, TraceLevel.TRACE);

        //Pix pixSauvolaCleaned32 = ImageUtils.convertImageToPix(diagonalsCleanedBI);
        //Pix pixSauvolaCleaned = ImageUtils.getDepth1Pix(pixSauvolaCleaned32, 128);

        //LeptUtils.dispose(pixSauvolaCleaned1);
        //LeptUtils.dispose(pixSauvolaCleaned32);

        // Pix pixSauvolaLinesRemoved_H = ImageUtils.removeLines(pixSauvolaCleaned, ImageUtils.VERTICAL_DIRECTION, 19);
        Pix pixSauvolaLinesRemoved_H = ImageUtils.removeLines(pixSauvolaCleaned1, ImageUtils.VERTICAL_DIRECTION, (int) Math.max(dimensions.getHeight(), dimensions.getWidth()) + 3);
        Pix pixSauvolaLinesRemoved = ImageUtils.removeLines(pixSauvolaLinesRemoved_H, ImageUtils.HORIZONTAL_DIRECTION, (int) dimensions.getWidth() + 3);
        pixWrite(imageCounter + " - 11 - sauvolaRemoveLines.png", pixSauvolaLinesRemoved, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        BufferedImage cleanedBI = ImageUtils.convertPixToImage(pixSauvolaLinesRemoved);
        BufferedImage processedAndMaskedBI = ImageUtils.imageMask(cleanedBI, aMaskBI);
        imageWrite(imageCounter + " - 12 - maskedOriginal.png", processedAndMaskedBI, BaseCamera.PNG, TraceLevel.TRACE);

        Pix cleanedBinarizedPix = ImageUtils.getDepth1Pix(processedAndMaskedBI, 128);
        pixWrite(imageCounter + " - 13 - cleanedBinarizedPix.png", cleanedBinarizedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix connCompReadyPix = ImageUtils.cleanThenJoinDisjointedBridges(cleanedBinarizedPix);
        pixWrite(imageCounter + " - 14 - bridgesJoined.png", connCompReadyPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);
        Pix smallAndLargeBitsCleaned = Leptonica1.pixCreate(Leptonica1.pixGetWidth(connCompReadyPix),
                Leptonica1.pixGetHeight(connCompReadyPix), 1);
        Leptonica1.pixSetBlackOrWhite(smallAndLargeBitsCleaned, ILeptonica.L_SET_WHITE);

        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);

            if ((aBox.w < dimensions.getWidth() / 5) || (aBox.h < dimensions.getHeight() / 2) || (aBox.h > dimensions.getHeight() * 1.3) || (aBox.w > dimensions.getWidth() * 2)) {
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
        LeptUtils.dispose(pixSauvola);
        LeptUtils.dispose(pixSauvolaLinesRemoved_H);
        LeptUtils.dispose(pixSauvolaLinesRemoved);
        LeptUtils.dispose(cleanedBinarizedPix);
        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);

        pixWrite(imageCounter + " - 15 - smallAndLargeBitsCleaned.png", smallAndLargeBitsCleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        float rotationAngle = ImageUtils.findSkewAngle(smallAndLargeBitsCleaned, 128);
        Pix rotatedPix8 = ImageUtils.rotatePix(smallAndLargeBitsCleaned, rotationAngle, true);
        Pix rotatedPixTemp = ImageUtils.getDepth1Pix(rotatedPix8, 128);
        // Pix rotatedPix = ImageUtils.eliminateBlackBridgesIteratively(rotatedPixTemp, 2, 128, ImageUtils.VERTICAL_DIRECTION);
        Pix rotatedPix = Leptonica1.pixCopy(null, rotatedPixTemp);
        pixWrite(imageCounter + " - 16 - rotatedPix.png", rotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        //Pix sauvolaRotatedPix8 = ImageUtils.rotatePix(pixSauvolaCleaned, rotationAngle, true);
        Pix sauvolaRotatedPix8 = ImageUtils.rotatePix(pixSauvolaCleaned1, rotationAngle, true);
        Pix sauvolaRotatedPixTemp = ImageUtils.getDepth1Pix(sauvolaRotatedPix8, 128);
        // Pix sauvolaRotatedPix = ImageUtils.eliminateBlackBridgesIteratively(sauvolaRotatedPixTemp, 2, 128, ImageUtils.VERTICAL_DIRECTION);
        Pix sauvolaRotatedPix = Leptonica1.pixCopy(null, sauvolaRotatedPixTemp);
        pixWrite(imageCounter + " - 17 - rotatedSauvolaCleanedPix.png", sauvolaRotatedPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(rotatedPix8);
        LeptUtils.dispose(rotatedPixTemp);
        LeptUtils.dispose(sauvolaRotatedPix8);
        LeptUtils.dispose(sauvolaRotatedPixTemp);

        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, (int) (dimensions.getHeight() / 2), (int) (dimensions.getWidth() / 4), getMinimumCharactersPerLine(), (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.debug("Lines found from rotatedPix are : " + lines);
        Pix linesPix = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, lines);
        pixWrite(imageCounter + " - 17A - rotatedPixWithLines.png", linesPix, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // lines = LineArrangementUtils.rearrangeLines(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth(), getMinimumCharactersPerLine());
        ArrayList<ArrayList<Rectangle>> tempLines = TesseractUtils.sortLines(lines, true);
        ArrayList<ArrayList<Rectangle>> mergedLines = TesseractUtils.mergeLinesBasedOnYOverlap(tempLines, 0.375);
        ArrayList<ArrayList<Rectangle>> orderedAndMergedLines = TesseractUtils.sortLines(mergedLines, false);
        Pix linesPix_2 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, orderedAndMergedLines);
        pixWrite(imageCounter + " - 17B - orderAndArrangeLines.png", linesPix_2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        ArrayList<ArrayList<Rectangle>> noOverlappingBoxLines = TesseractUtils.dropOverlappingBoxesInLines(orderedAndMergedLines, dimensions.getHeight() * dimensions.getWidth() * 0.075);
        Pix linesPix_3 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, noOverlappingBoxLines);
        pixWrite(imageCounter + " - 17C - orderAndArrangeLines.png", linesPix_3, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        ArrayList<ArrayList<Rectangle>> okSizedBoxes = TesseractUtils.dropShortOrWideBoxes(noOverlappingBoxLines, dimensions.getHeight() * 0.75, dimensions.getWidth() * 1.3);
        Pix linesPix_4 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, okSizedBoxes);
        pixWrite(imageCounter + " - 17D - orderAndArrangeLines.png", linesPix_4, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        ArrayList<ArrayList<Rectangle>> linesSplit = TesseractUtils.splitLinesBasedOnGap(okSizedBoxes, dimensions.getWidth() * 2.75);
        Pix linesPix_5 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, linesSplit);
        pixWrite(imageCounter + " - 17E - orderAndArrangeLines.png", linesPix_5, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        // lines = TesseractUtils.orderAndArrangeLines(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.trace("Boxes after orderAndArrangeLines() into lines = " + lines);
        //Pix linesPix_2 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, lines);

        lines = TesseractUtils.reallocateLines(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.trace("Boxes after reallocateLines() into lines = " + lines);
        Pix linesPix_6 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, lines);
        pixWrite(imageCounter + " - 17F - reallocateLines.png", linesPix_6, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.reallocateLinesAgain(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.debug("Boxes after reallocateLinesAgain() into lines = " + lines);
        Pix linesPix_7 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, lines);
        pixWrite(imageCounter + " - 17G - reallocateLinesAgain.png", linesPix_7, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.segregateLoneBoxesIntoLines(lines);
        logger.debug("Boxes after segregation of lone boxes into lines = ");
        Pix linesPix_8 = ImageUtils.drawBoundingBoxesOnPix(rotatedPix, lines);
        pixWrite(imageCounter + " - 17H - segregateLoneBoxesIntoLines.png", linesPix_8, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(linesPix);
        LeptUtils.dispose(linesPix_2);
        LeptUtils.dispose(linesPix_3);
        LeptUtils.dispose(linesPix_4);
        LeptUtils.dispose(linesPix_5);
        LeptUtils.dispose(linesPix_6);
        LeptUtils.dispose(linesPix_7);
        LeptUtils.dispose(linesPix_8);

        ArrayList<ArrayList<Rectangle>> twoLines = TesseractUtils.arrangeInto2Lines(lines);
        logger.debug("Lines found from arranging into 2 lines are : " + lines);

        Pix pixWithE2ELines = TesseractUtils.getPixWithE2ELines(sauvolaRotatedPix, twoLines);
        pixWrite(imageCounter + " - 18 - pixWithE2ELines.png", pixWithE2ELines, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        Pix E2ECleaned = ImageUtils.cleanThenJoinDisjointedBridges(pixWithE2ELines);
        pixWrite(imageCounter + " - 18A - E2ECleaned.png", E2ECleaned, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.getBoundingBoxes(E2ECleaned, (int) (dimensions.getHeight() / 2), (int) (dimensions.getWidth() / 4), getMinimumCharactersPerLine(), (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.trace("Lines found from E2ECleaned are : " + lines);
        Pix linesPix1 = ImageUtils.drawBoundingBoxesOnPix(E2ECleaned, lines);
        pixWrite(imageCounter + " - 18B - pixWithE2ELinesBB.png", linesPix1, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.orderAndArrangeLines(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.trace("Boxes after orderAndArrangeLines() into lines = " + lines);
        Pix linesPix2 = ImageUtils.drawBoundingBoxesOnPix(E2ECleaned, lines);
        pixWrite(imageCounter + " - 18C - pixWithE2ELinesBB.png", linesPix2, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.reallocateLines(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.trace("Boxes after reallocateLines() into lines = " + lines);
        Pix linesPix3 = ImageUtils.drawBoundingBoxesOnPix(E2ECleaned, lines);
        pixWrite(imageCounter + " - 18D - pixWithE2ELinesBB.png", linesPix3, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.reallocateLinesAgain(lines, (int) dimensions.getHeight(), (int) dimensions.getWidth());
        logger.debug("Boxes after reallocateLinesAgain() into lines = " + lines);
        Pix linesPix4 = ImageUtils.drawBoundingBoxesOnPix(E2ECleaned, lines);
        pixWrite(imageCounter + " - 18E - pixWithE2ELinesBB.png", linesPix4, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        lines = TesseractUtils.segregateLoneBoxesIntoLines(lines);
        logger.debug("Boxes after segregation of lone boxes into lines = ");
        Pix linesPix5 = ImageUtils.drawBoundingBoxesOnPix(E2ECleaned, lines);
        pixWrite(imageCounter + " - 18F - pixWithE2ELinesBB.png", linesPix5, ILeptonica.IFF_PNG, TraceLevel.TRACE);

        LeptUtils.dispose(E2ECleaned);
        LeptUtils.dispose(linesPix1);
        LeptUtils.dispose(linesPix2);
        LeptUtils.dispose(linesPix3);
        LeptUtils.dispose(linesPix4);
        LeptUtils.dispose(linesPix5);

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
            String commonString = CharacterUtils.getCommonString(strings, (i < patterns.size() ? patterns.get(i) : ""));
            logger.debug("After matching with pattern " + (i < patterns.size() ? patterns.get(i) : "") + " the final commonString is " + commonString);
            finalStrings.add(commonString);
        }

        finalStrings = rectifyResults(finalStrings);

        logger.trace("Final Strings are : " + finalStrings);

        // LeptUtils.dispose(pixSauvolaCleaned);
        LeptUtils.dispose(pixSauvolaCleaned1);
        LeptUtils.dispose(smallAndLargeBitsCleaned);
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
