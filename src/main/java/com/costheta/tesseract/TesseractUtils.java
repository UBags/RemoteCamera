package com.costheta.tesseract;

import com.costheta.image.utils.ImageUtils;
import com.costheta.text.utils.CharacterUtils;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.scene.shape.Rectangle;

public class TesseractUtils {

    private static final Logger logger = LogManager.getLogger(TesseractUtils.class);

    public static final ExecutorService largePoolThreadService = Executors.newFixedThreadPool(30);
    public static final ExecutorService smallPoolThreadService = Executors.newFixedThreadPool(60);

    private static final CosThetaTesseractHandlePool largeTesseractPool =
            new CosThetaTesseractHandlePool(new CosThetaTesseractHandleFactory(1, 1000),
            CosThetaTesseractHandlePool.oneMachineConfig, 1, 1000);
    private static final CosThetaTesseractHandlePool smallTesseractPool = new CosThetaTesseractHandlePool(new CosThetaTesseractHandleFactory(1, 1000),
            CosThetaTesseractHandlePool.smallPoolConfig, 1, 1000);

    private static final String EMPTY_STRING = "";

    public static final ArrayList<String> doOCR(Pixa pixArray) {
        if (pixArray == null) {
            return doOCR((ArrayList<BufferedImage>) null);
        }
        ArrayList<BufferedImage> images = new ArrayList<>();
        int numberOfImages = Leptonica1.pixaGetCount(pixArray);
        for (int i = 0; i < numberOfImages; ++i) {
            Pix aPix = Leptonica1.pixaGetPix(pixArray, i, ILeptonica.L_COPY);
            BufferedImage image = ImageUtils.convertPixToImage(aPix);
            LeptUtils.dispose(aPix);
            images.add(image);
        }
        return doOCR(images);
    }

    public static final ArrayList<String> doOCR(ArrayList<BufferedImage> images) {

        if ((images == null) || (images.size() == 0)) {
            return new ArrayList<String>();
        }
        CosThetaTesseractHandlePool tempPoolVariable = null;
        ExecutorService service = null;
        if (images.size() < 10) {
            tempPoolVariable = smallTesseractPool;
            service = largePoolThreadService;
        } else {
            tempPoolVariable = largeTesseractPool;
            service = smallPoolThreadService;
        }
        final CosThetaTesseractHandlePool pool = tempPoolVariable;
        final ArrayList<String> results = new ArrayList<>(images.size());
        final ArrayList<CompletableFuture<String>> cfs = new ArrayList<CompletableFuture<String>>(images.size());
        Object interruptLock = new Object();
        for (int i = 0; i < images.size(); ++i) {
            final int currentIndex = i;
            cfs.add(CompletableFuture.supplyAsync(() -> {
                CosThetaTesseractHandle instanceHandle = null;
                try {
                    synchronized (interruptLock) {
                        instanceHandle = (CosThetaTesseractHandle) pool.borrowObject();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        synchronized (interruptLock) {
                            pool.returnObject(instanceHandle);
                        }
                    } catch (Exception e1) {
                    }
                    return EMPTY_STRING;
                }
                CosThetaTesseract instance = instanceHandle.getHandle();

                boolean success = true;
                String result = EMPTY_STRING;
                try {
                    // System.out.println("About to do OCR with " + instance);
                    BufferedImage image = ImageUtils.getGrayBI(images.get(currentIndex));
                    // BufferedImage image = images.get(currentIndex);
                    // ImageUtils.writeFile(image, "jpg", Processor1.debugDirectory + "/" + "OCR Image-" + currentIndex + ".jpg");
                    result = instance.doOCR(image);
                    // System.out.println("OCR result = " + result);
                } catch (Exception e) {
                    success = false;
                }
                synchronized (interruptLock) {
                    pool.returnObject(instanceHandle);
                }
                return result;
            }, service));
        }
        CompletableFuture.allOf(cfs.toArray(new CompletableFuture[cfs.size()])).join();
        for (int i = 0; i < cfs.size(); ++i) {
            String result = EMPTY_STRING;
            try {
                result = cfs.get(i).get();
                // results.add(CharacterUtils.getOnlyAlphabetsAndNumbers(result));
                // TesseractUtils sends the raw result;
                // rectification and modification of raw results is left to the caller
                results.add(result);
            } catch (Exception e) {
                results.add(EMPTY_STRING);
            }
        }
        return results;
    }

    public static ArrayList<ArrayList<Rectangle>> getBoundingBoxes(Pix pix, int heightCutoff, int widthCutoff, int minBoxesRequiredInLine, int characterHeight, int characterWidth) {

        String currentMergeSortProperty = System.getProperty("java.util.Arrays.useLegacyMergeSort");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        // System.out.println("Got pix in getBoundingBoxes() = " + pix);
        Rectangle[] bboxes = getDefaultBoxes(pix, heightCutoff, widthCutoff);
        // for (int i = 0; i < bboxes.length; ++i) {
        //    System.out.println(bboxes[i]);
        // }
        // System.out.println("Received array in getBoundingBoxes() from getDefaultBoxes() = " + bboxes);
        ArrayList<ArrayList<Rectangle>> lines = segregateBoxesIntoLines(bboxes, characterHeight);
        logger.debug("Boxes after segregateBoxesIntoLines() into lines = " + lines);
        logger.debug(lines);
        // remove empty lines and lines with only 1 box
/*
        // reinstate later
        for (int i = lines.size() - 1; i >= 0; --i) {
            if (lines.get(i).size() < minBoxesRequiredInLine){
                logger.trace("getBoundingBoxes() : Dropping line " + i + " with box " + lines.get(i));
                lines.remove(i);
            }
        }
*/
        // sort boxes in each line by the x-coordinate
        for (ArrayList<Rectangle> line : lines) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle r1, Rectangle r2) {
                    return (int) (r1.getX() - r2.getX());
                }

            });
        }

        if (currentMergeSortProperty != null) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", currentMergeSortProperty);
        }
        return lines;

    }

    public static Rectangle[] getDefaultBoxes(Pix pix, int heightCutoff, int widthCutoff) {

        int connectivity = 4;
        Pix pix1 = ImageUtils.getDepth1Pix(pix, 128);
        Boxa result = Leptonica1.pixConnCompBB(pix1, connectivity);
        Boxa sortedResult = Leptonica1.boxaSort(result, ILeptonica.L_SORT_BY_Y, ILeptonica.L_SORT_INCREASING, null);

        int numberOfBoxes = Leptonica1.boxaGetCount(sortedResult);
        // System.out.println("Number of boxes in getDefaultBoxes() = " + numberOfBoxes);

        // add rectangles to an arraylist, but remove small boxes of heightCutoff or below,
        // and also remove boxes of widthCutoff or below
        int absoluteHeightCutOff = heightCutoff;
        int absoluteWidthCutOff = widthCutoff;

        ArrayList<Rectangle> wordRectangles = new ArrayList<>();
        for (int j = 0; j < numberOfBoxes; ++j) {
            Box box = Leptonica1.boxaGetBox(sortedResult, j, ILeptonica.L_COPY);
            if ((box.h > absoluteHeightCutOff) && (box.w > absoluteWidthCutOff)) {
                wordRectangles.add(new Rectangle(box.x, box.y, box.w, box.h));
            } else {
                logger.trace("getDefaultBoxes() : Dropping box at [" + box.x + "," + box.y + "," + box.w + "," + box.h + "]");
            }
            LeptUtils.dispose(box);
        }

        LeptUtils.dispose(pix1);
        LeptUtils.dispose(result);
        LeptUtils.dispose(sortedResult);

        // remove boxes of dubious height
        DescriptiveStatistics hStats = new DescriptiveStatistics();
        for (Rectangle word : wordRectangles) {
            hStats.addValue(word.getHeight());
        }

        int medianHeight = (int) hStats.getPercentile(50);
        ArrayList<Rectangle> finalWords = new ArrayList<>();
        for (Rectangle word : wordRectangles) {
            if (word.getHeight() > 0.7 * medianHeight) {
                finalWords.add(word);
            } else {
                logger.trace("getDefaultBoxes(): Dropping box at [" + (int) word.getX() + "," + (int) word.getY() + "," + word.getWidth() + "," + word.getHeight() + "]");
            }
        }
        // System.out.println("Final ArrayList in getDefaultBoxes() = " + finalWords);
        return finalWords.toArray(new Rectangle[finalWords.size()]);
    }

    public static ArrayList<ArrayList<Rectangle>> segregateBoxesIntoLines(Rectangle[] letters, int characterHeight) {

        DescriptiveStatistics heightStats = new DescriptiveStatistics();
        DescriptiveStatistics widthStats = new DescriptiveStatistics();

        for (Rectangle word : letters) {
            if (word != null) {
                heightStats.addValue(word.getHeight());
            }
        }

        double medHeight = heightStats.getMean();
        heightStats.clear();

        for (Rectangle word : letters) {
            if (word != null) {
                if (word.getHeight() >= (medHeight * 0.5)) {
                    heightStats.addValue(word.getHeight());
                    widthStats.addValue(word.getWidth());
                }
            }
        }

        int numberOfModes = 0;
        ArrayList<Integer> modes = new ArrayList<>();
        int kde = 3;
        if (heightStats.getN() > 8) {
            kde = (int) (0.9 * heightStats.getStandardDeviation() * Math.pow(heightStats.getN(), -0.2));
        }
        if (kde <= 1) {
            kde = 2;
        }
        int numberOfBins = ((1600 % kde) == 0) ? 1600 / kde : ((1600 / kde) + 1);
        int histogram[] = new int[numberOfBins];
        for (Rectangle letter : letters) {
            if (letter != null) {
                if (letter.getHeight() > (medHeight * 0.5)) {
                    int binNumber = (int) (((letter.getHeight() % kde) != 0) ? (letter.getHeight() / kde) : ((letter.getHeight() / kde) - 1));
                    if (binNumber < 0) {
                        binNumber = 0;
                    }
                    ++histogram[binNumber];
                }
            }
        }

        for (int i = 1; i < (numberOfBins - 1); ++i) {
            if ((histogram[i] > 4) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
                modes.add(i);
                ++numberOfModes;
            }
        }

        if (modes.size() >= 2) {
            int mode1 = modes.get(0);
            int mode2 = modes.get(1);
            // order mode1 and mode2 in ascending order of their bin count
            if (histogram[mode1] > histogram[mode2]) {
                int temp = mode2;
                mode2 = mode1;
                mode1 = temp;
            }
            for (int i = 2; i < modes.size(); ++i) {
                if (histogram[i] > histogram[mode2]) {
                    mode1 = mode2;
                    mode2 = i;
                } else {
                    if (histogram[i] > histogram[mode1]) {
                        mode1 = i;
                    }
                }
            }
        }

        int mostLikelyHeightIndex = 1;
        int heightTotal = histogram[0] + histogram[1];
        for (int i = 1; i < (histogram.length - 2); ++i) {
            int newTotal = histogram[i - 1] + histogram[i] + histogram[i + 1];
            if ((newTotal > heightTotal) && (histogram[i] >= histogram[i - 1]) && (histogram[i] >= histogram[i + 1])) {
                mostLikelyHeightIndex = i;
                heightTotal = newTotal;
            }
        }

        int lHeight;

        final double cHeight = ((histogram[mostLikelyHeightIndex] * (mostLikelyHeightIndex + 0.5) * kde)
                + (histogram[mostLikelyHeightIndex + 1] * (mostLikelyHeightIndex + 1.5) * kde))
                / (histogram[mostLikelyHeightIndex] + histogram[mostLikelyHeightIndex + 1]);

        if (mostLikelyHeightIndex == 1) {
            lHeight = (int) cHeight;
        } else {
            if (histogram[mostLikelyHeightIndex - 1] > histogram[mostLikelyHeightIndex + 1]) {
                lHeight = (int) (((histogram[mostLikelyHeightIndex] * (mostLikelyHeightIndex + 0.5) * kde)
                        + (histogram[mostLikelyHeightIndex - 1] * (mostLikelyHeightIndex - 0.5) * kde))
                        / (histogram[mostLikelyHeightIndex] + histogram[mostLikelyHeightIndex - 1]));
            } else {
                lHeight = (int) cHeight;
            }
        }

        logger.trace("segregateBoxesIntoLines() : Height modes = " + modes.size() + " which are " + modes);
        int[] heightModalValues = new int[modes.size()];
        int idx = 0;
        for (Integer mode : modes) {
            heightModalValues[idx++] = (int) ((mode + 0.5) * kde);
            logger.trace("segregateBoxesIntoLines() : Height mode = " + (int) ((mode + 0.5) * kde));
        }

        if (modes.size() == 1) {
            lHeight = heightModalValues[0] + 1;
        }

        logger.trace("segregateBoxesIntoLines() : lHeight = " + lHeight);

        int numberOfWidthModes = 0;
        ArrayList<Integer> widthModes = new ArrayList<>();
        int kdew = 2;
        if (widthStats.getN() > 8) {
            kdew = (int) (0.9 * widthStats.getStandardDeviation() * Math.pow(widthStats.getN(), -0.2));
        }
        if (kdew <= 1) {
            kdew = 2;
        }
        // int originalKDEW = kdew;
        if (kdew > 9) {
            kdew = 9;
        }

        int numberOfWidthBins = ((1600 % kdew) == 0) ? 1600 / kdew : ((1600 / kdew) + 1);
        int wHistogram[] = new int[numberOfWidthBins];
        for (Rectangle letter : letters) {
            if (letter != null) {
                if (letter.getHeight() > (medHeight * 0.5)) {
                    int binNumber = (int) (((letter.getWidth() % kdew) != 0) ? (letter.getWidth() / kdew) : ((letter.getWidth() / kdew) - 1));
                    if (binNumber < 0) {
                        binNumber = 0;
                    }
                    ++wHistogram[binNumber];
                }
            }
        }
        for (int i = 1; i < (numberOfWidthBins - 1); ++i) {
            if ((wHistogram[i] > 4) && (wHistogram[i] >= wHistogram[i - 1]) && (wHistogram[i] >= wHistogram[i + 1])) {
                widthModes.add(i);
                ++numberOfWidthModes;
            }
        }
        int mostLikelyWidthIndex = 1;
        int widthTotal = wHistogram[0] + wHistogram[1];
        for (int i = 1; i < (wHistogram.length - 2); ++i) {
            int newTotal = wHistogram[i - 1] + wHistogram[i] + wHistogram[i + 1];
            if ((newTotal > widthTotal) && (wHistogram[i] >= wHistogram[i - 1])
                    && (wHistogram[i] >= wHistogram[i + 1])) {
                mostLikelyWidthIndex = i;
                widthTotal = newTotal;
            }
        }

        int lWidth;
        double valueOfWidth = ((wHistogram[mostLikelyWidthIndex] * (mostLikelyWidthIndex + 0.5) * kdew)
                + (wHistogram[mostLikelyWidthIndex + 1] * (mostLikelyWidthIndex + 1.5) * kdew))
                / (wHistogram[mostLikelyWidthIndex] + wHistogram[mostLikelyWidthIndex + 1]);
        if (mostLikelyWidthIndex == 1) {
            lWidth = (int) valueOfWidth;
        } else {
            if (wHistogram[mostLikelyWidthIndex - 1] > wHistogram[mostLikelyWidthIndex + 1]) {
                lWidth = (int) (((wHistogram[mostLikelyWidthIndex] * (mostLikelyWidthIndex + 0.5) * kdew)
                        + (wHistogram[mostLikelyWidthIndex - 1] * (mostLikelyWidthIndex - 0.5) * kdew))
                        / (wHistogram[mostLikelyWidthIndex] + wHistogram[mostLikelyWidthIndex - 1]));
            } else {
                lWidth = (int) valueOfWidth;
            }
        }
        logger.trace("segregateBoxesIntoLines() : Width modes = " + widthModes.size() + " which are " + widthModes);

        int[] widthModalValues = new int[widthModes.size()];
        int idx1 = 0;
        for (Integer mode : widthModes) {
            widthModalValues[idx1++] = (int) ((mode + 0.5) * kdew);
            logger.trace("segregateBoxesIntoLines() : Width mode = " + (int) ((mode + 0.5) * kdew));
        }

        if (widthModes.size() == 1) {
            lWidth = widthModalValues[0] + 1;
        }

        logger.trace("segregateBoxesIntoLines() : lWidth = " + lWidth);

        int minYDifference = (int) (lHeight / 2.5);
        double heightCutoff = 0.45;
        double widthCutoff = 0.25; // 0.5

        // System.out.println("Likely Height : " + lHeight +"; Likely Width : " + lWidth);

        ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();
        Set<Integer> lineNumbersWhereFitmentPossible = new TreeSet<>();

        mainloop: for (Rectangle letter : letters) {
            if (letter == null) {
                continue mainloop;
            }
            int index = 0;
            lineNumbersWhereFitmentPossible.clear();
            if (lines.size() == 0) { // the loop is starting
                ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
                lines.add(newLine);
                newLine.add(letter);
                continue mainloop;
            }
            loop2: for (ArrayList<Rectangle> line : lines) {
                loop3: for (Rectangle box : line) {
                    // Note: letter is the new Rectangle picked up for fitment,
                    // while box is an already slotted Rectangle in the lines ArrayList

                    // ignore the box if its dimensions are small
                    if ((box.getHeight() < (lHeight * heightCutoff)) || (box.getWidth() < (lWidth * widthCutoff))) {
                        logger.trace("segregateBoxesIntoLines() : Skipping box at [" + (int) box.getX() + "," + (int) box.getY() + "," + box.getWidth() + "," + box.getHeight() + "] due to small height or width");
                        continue loop3;
                    }
                    // ignore the box if its height is too large
                    // changed the cutoff from 2.5 to 3.5
                    if (box.getHeight() > (lHeight * 3.5)) {
                        logger.trace("segregateBoxesIntoLines() : Skipping box at [" + (int) box.getX() + "," + (int) box.getY() + "," + box.getWidth() + "," + box.getHeight() + "] due to large height");
                        continue loop3;
                    }
                    if ((Math.abs(letter.getY() - box.getY()) <= minYDifference)
                            || (Math.abs((letter.getY() + letter.getHeight()) - (box.getY() + box.getHeight())) <= minYDifference)) {
                        lineNumbersWhereFitmentPossible.add(index);
                        ++index;
                        continue loop2;
                    }
                    if ((letter.getY() >= box.getY()) && ((letter.getY() + letter.getHeight()) < (box.getY() + box.getHeight() + minYDifference))) {
                        lineNumbersWhereFitmentPossible.add(index);
                        ++index;
                        continue loop2;
                    }
                }
                ++index;
            }
            if (lineNumbersWhereFitmentPossible.size() == 0) { // based on y-coordinates, did not find a potential set
                // of words where it can fit
                ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
                lines.add(newLine);
                newLine.add(letter);
                continue mainloop;
            }

            // First, check which existing lineNumber is a better fit for the box.
            // Essentially, find the distance between the current box and the boxes in the
            // line and choose that line where the distance is minimum

            // ArrayList<Integer> minimumXDistances = new ArrayList<>();
            int bestFitLine = -1;
            int minXDistance = Integer.MAX_VALUE;
            outerloop: for (int lineNumber : lineNumbersWhereFitmentPossible) {
                ArrayList<Rectangle> line = lines.get(lineNumber); // get the current list of letters at the lineNumber
                for (Rectangle box : line) {
                    int xDistance = (int) ((letter.getX() > box.getX()) ? Math.abs(letter.getX() - (box.getX() + box.getWidth()))
                            : Math.abs(box.getX() - (letter.getX() + letter.getWidth())));
                    if (xDistance < minXDistance) {
                        minXDistance = xDistance;
                        bestFitLine = lineNumber;
                        continue outerloop;
                    }
                }
            }

            int acceptableGap = (int) (lWidth * 2.25);
            logger.trace("segregateBoxesIntoLines() : Acceptable Gap = " + acceptableGap);

            if ((bestFitLine != -1) && (minXDistance < acceptableGap)) {
                ArrayList<Rectangle> bestLine = lines.get(bestFitLine); // get the current line at bestFitLine index
                bestLine.add(0, letter);
            } else {
                // create a new line and add the letter
                ArrayList<Rectangle> newLine = new ArrayList<Rectangle>();
                lines.add(newLine);
                newLine.add(letter);
                continue mainloop;
            }

        }

        // sort each line of 'words' by x-coordinate and add to lines
        // then, return the sorted ArrayList lines

        for (ArrayList<Rectangle> line : lines) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle r1, Rectangle r2) {
                    return (int) (r1.getX() - r2.getX());
                }

            });
        }

        // remove all lines with length 0. It seems that some such lines are still in
        // the mix. Need to clear these somewhere above, but will do it here for now

        for (int k = lines.size() - 1; k >= 0; --k) {
            if ((lines.get(k) == null) || (lines.get(k).size() == 0)) {
                lines.remove(k);
            }
        }

        // sort the lines by y-coordinate
        Collections.sort(lines, new Comparator<ArrayList<Rectangle>>() {

            @Override
            public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {

                if ((line1.size() == 0) && (line2.size() == 0)) {
                    return 0;
                }
                if ((line1.size() > 0) && (line2.size() == 0)) {
                    return 1;
                }
                if ((line1.size() == 0) && (line2.size() > 0)) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) < -5) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) > 5) {
                    return 1;
                }
                return (int) (((line1.get(0).getX() - line2.get(0).getX()) > 5 ? 1 : ((line1.get(0).getX() - line2.get(0).getX()) < -5 ? -1 : 0)));
            }
        });

        return lines;
    }

    public static ArrayList<ArrayList<Rectangle>> segregateLoneBoxesIntoLines(ArrayList<ArrayList<Rectangle>> lines) {

        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        Set<Integer> lineNumbersWhereFitmentPossible = new TreeSet<>();

        DescriptiveStatistics hStats = new DescriptiveStatistics();
        DescriptiveStatistics wStats = new DescriptiveStatistics();

        for (ArrayList<Rectangle> line : lines) {
            for (Rectangle letter : line) {
                hStats.addValue(letter.getHeight());
                wStats.addValue(letter.getWidth());
            }
        }

        int likelyHeight = (int) hStats.getPercentile(55);
        int likelyWidth = (int) wStats.getPercentile(55);

        int minYDifference = (int) (likelyHeight / 3) ;
        double heightCutoff = 2.5;
        double widthCutoff = 0.35;

        for (int index = lines.size() - 1; index >= 0; --index) {
            if (lines.get(index).size() > 1) {
                // if it's a line with 2 or more boxes, go to the next line
                newLines.add(lines.get(index)); // add the line to newLines
                lines.remove(index); // remove the line from the input lines
            }
        }

        logger.debug("Identified the following single lines : " + lines);

        logger.debug("newLines has the following lines at start of loop : " + newLines);

        mainloop: for (int index = lines.size() - 1; index >= 0; --index) {
            if (lines.get(index).size() > 1) {
                continue;
            }
            ArrayList<Rectangle> singleLine = lines.get(index);
            for (Rectangle letter : singleLine) {
                if (letter == null) {
                    continue;
                }
                int lineIndex = -1;
                lineNumbersWhereFitmentPossible.clear();
                outerloop: for (ArrayList<Rectangle> line : newLines) {
                    ++lineIndex;
                    for (Rectangle box : line) {
                        // Note: letter is the new Rectangle picked up for fitment,
                        // while box is an already slotted Rectangle in the lines ArrayList
                        // ignore the box if its dimensions are small
                        if ((box.getHeight() > (likelyHeight * heightCutoff)) || (box.getWidth() < (likelyWidth * widthCutoff))) {
                            logger.trace("segregateBoxesIntoLines() : Skipping box at [" + (int) box.getX() + "," + (int) box.getY() + "," + box.getWidth() + "," + box.getHeight() + "] due to small height or width");
                            continue;
                        }
                        if ((letter.getY() < box.getY() - minYDifference)
                                && ((letter.getY() + letter.getHeight()) < (box.getY() + box.getHeight()) + minYDifference)
                                && ((letter.getY() + letter.getHeight()) > box.getY())) {
                            lineNumbersWhereFitmentPossible.add(lineIndex);
                            continue outerloop;
                        }
                        if ((letter.getY() >= box.getY()) && ((letter.getY() + letter.getHeight()) < (box.getY() + box.getHeight() + minYDifference))) {
                            lineNumbersWhereFitmentPossible.add(lineIndex);
                            continue outerloop;
                        }
                    }
                }
                logger.debug("For line " + singleLine + ", the possible line fitments are at indices - " + lineNumbersWhereFitmentPossible);
                if (lineNumbersWhereFitmentPossible.size() == 0) { // based on y-coordinates, did not find a potential set
                    // newLines.add(singleLine);
                    // drop the box
                    logger.trace("Dropping " + singleLine + " because no fitment found");
                    continue mainloop;
                }

                // First, check which existing lineNumber is a better fit for the box.
                // Essentially, find the distance between the current box and the boxes in the
                // line and choose that line where the distance is minimum

                // ArrayList<Integer> minimumXDistances = new ArrayList<>();
                int bestFitLine = -1;
                int minXDistance = Integer.MAX_VALUE;
                for (int lineNumber : lineNumbersWhereFitmentPossible) {
                    ArrayList<Rectangle> line = newLines.get(lineNumber); // get the current list of letters at the lineNumber
                    for (Rectangle box : line) {
                        int xDistance = (int) ((letter.getX() > box.getX()) ? Math.abs(letter.getX() - (box.getX() + box.getWidth()))
                                : Math.abs(box.getX() - (letter.getX() + letter.getWidth())));
                        if (xDistance < minXDistance) {
                            minXDistance = xDistance;
                            bestFitLine = lineNumber;
                            continue;
                        }
                    }
                }

                int acceptableGap = (int) (likelyWidth * 2.5);
                // System.out.println("segregateBoxesIntoLines() : Acceptable Gap = " + acceptableGap);

                if ((bestFitLine != -1) && (minXDistance < acceptableGap)) {
                    logger.debug("Allocating box at line " + index + " to line " + bestFitLine);
                    ArrayList<Rectangle> bestLine = newLines.get(bestFitLine); // get the current line at bestFitLine index
                    // singleLine.remove(letter); // remove the letter from the line
                    bestLine.add(0, letter); // add the box in the best fit line
                    // lines.remove(singleLine); // remove the existing line from the list of lines!!
                }
            }
        }

        // sort each line of 'words' by x-coordinate and add to lines
        // then, return the sorted ArrayList lines

        for (ArrayList<Rectangle> line : newLines) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle r1, Rectangle r2) {
                    return (int) (r1.getX() - r2.getX());
                }

            });
        }

        // remove all lines with length 0. It seems that some such lines are still in
        // the mix. Need to clear these somewhere above, but will do it here for now

        for (int k = newLines.size() - 1; k >= 0; --k) {
            if ((newLines.get(k) == null) || (newLines.get(k).size() == 0)) {
                newLines.remove(k);
            }
        }

        // sort the lines by y-coordinate
        Collections.sort(newLines, new Comparator<ArrayList<Rectangle>>() {

            @Override
            public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {

                if ((line1.size() == 0) && (line2.size() == 0)) {
                    return 0;
                }
                if ((line1.size() > 0) && (line2.size() == 0)) {
                    return 1;
                }
                if ((line1.size() == 0) && (line2.size() > 0)) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) < -5) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) > 5) {
                    return 1;
                }
                return (int) (((line1.get(0).getX() - line2.get(0).getX()) > 5 ? 1 : ((line1.get(0).getX() - line2.get(0).getX()) < -5 ? -1 : 0)));
            }
        });

        return newLines;
    }

    public static ArrayList<ArrayList<Rectangle>> forceFitIntoOneLineMultiColumns(ArrayList<ArrayList<Rectangle>> lines) {
        ArrayList<Rectangle> singleLine = new ArrayList<>();
        DescriptiveStatistics wStats = new DescriptiveStatistics();
        for (ArrayList<Rectangle> line : lines) {
            for (Rectangle word : line) {
                singleLine.add(word);
                wStats.addValue(word.getWidth());
            }
        }
        Collections.sort(singleLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });
        double medianWidth = wStats.getPercentile(50);
        double gapToleranceMultiple = 1.25;
        ArrayList<ArrayList<Rectangle>> readjustedLines = new ArrayList<>();
        ArrayList<Rectangle> newLine = new ArrayList<>();
        Rectangle previousWord = null;
        for (Rectangle word : singleLine) {
            if (previousWord == null) {
                newLine.add(word);
                previousWord = word;
                continue;
            }
            if ((word.getX() - previousWord.getX() - previousWord.getWidth()) < medianWidth * gapToleranceMultiple) {
                newLine.add(word);
                previousWord = word;
            } else {
                readjustedLines.add(newLine);
                newLine = new ArrayList<>();
                newLine.add(word);
                previousWord = word;
            }
        }
        if (newLine.size() > 0) {
            readjustedLines.add(newLine);
        }
        return readjustedLines;
    }

    public static ArrayList<ArrayList<Rectangle>> forceFitIntoTwoLinesMultiColumns(ArrayList<ArrayList<Rectangle>> lines, ArrayList<String> patterns) {

        System.out.println("Entering forceFitInto2LinesMultiColumns()");

        // first, check if the number of input lines matches the number of required lines
        int nLines = patterns.size();
        int currentLines = lines.size();

        if (nLines == currentLines) {
            return lines;
        }

        int minRequiredWordsPerLine = 0;
        for (String pattern : patterns) {
            if (minRequiredWordsPerLine == 0) {
                minRequiredWordsPerLine = pattern.length();
                continue;
            }
            minRequiredWordsPerLine = Math.min(minRequiredWordsPerLine, pattern.length());
        }

        // check if any of the first and

        ArrayList<Rectangle> singleLine = new ArrayList<>();

        // put everything into a single line, and also
        // get the bottoms of the first and second lines
        HashMap<Integer, Integer> yCoord_boxCount = new HashMap<>();
        DescriptiveStatistics hStats = new DescriptiveStatistics();
        for (ArrayList<Rectangle> line : lines) {
            for (Rectangle word : line) {
                hStats.addValue(word.getHeight());
            }
        }
        int medianHeight = (int) hStats.getPercentile(50);
        int heightTolerance = (medianHeight * 2) / 3 + 2;
        System.out.println("Median height = " + medianHeight + "; height tolerance = " + heightTolerance);

        int bucketSize = (medianHeight * 2) / 3;

        for (ArrayList<Rectangle> line : lines) {
            for (Rectangle word : line) {
                singleLine.add(word);
                int yBottom = (int) (word.getY() + word.getHeight());
                int yCoordBucket = yBottom / bucketSize;
                if (yCoord_boxCount.containsKey(yCoordBucket)) {
                    int boxCount = yCoord_boxCount.get(yCoordBucket);
                    yCoord_boxCount.replace(yCoordBucket, ++boxCount);
                } else {
                    yCoord_boxCount.put(yCoordBucket, 1);
                }
            }
        }

        System.out.println("The mapping of line bottoms is " + yCoord_boxCount);

        // get the top 2 modes for line bottoms
        HashMap<Integer, Integer> modes = new HashMap<>();
        yCoord_boxCount = CharacterUtils.sortDescendingByValue(yCoord_boxCount);
        System.out.println("The descending sorted mapping of line bottoms is " + yCoord_boxCount);
        List<Map.Entry<Integer, Integer>> entrySet = new LinkedList<Map.Entry<Integer, Integer>>(yCoord_boxCount.entrySet());
        for (int i = 0; i < entrySet.size(); ++i) {
            // add the first y-coord bottom to the modes Hashmap and update the count against that
            int yC = entrySet.get(i).getKey();
            if (i == 0) {
                modes.put((yC * bucketSize) + (bucketSize / 2), entrySet.get(i).getValue());
                System.out.println("To the 'modes' HashMap, added key " + (yC * bucketSize) + (bucketSize / 2) + " and value " + entrySet.get(i).getValue());
                continue;
            }
            int index = -1;
            boolean found = false;
            // find another y-bottom entry that is within height tolerance of the already added entry and
            // add its count to the previous y-Bottom
            for (Map.Entry<Integer, Integer> anEntry : modes.entrySet()) {
                ++index;
                int aMode = anEntry.getKey();
                if (Math.abs((yC * bucketSize) + (bucketSize / 2)) == aMode) {
                    continue;
                }
                // if (Math.abs((yC * bucketSize) + (bucketSize / 2) - aMode) <= bucketSize) {
                // need to have a size bugger than bucketSize, because current bucketisation is
                // already as per bucketSize
                if (Math.abs((yC * bucketSize) + (bucketSize / 2) - aMode) <= heightTolerance) {
                    found = true;
                    break;
                }
            }
            if (found) {
                Map.Entry<Integer,Integer> theEntry = (Map.Entry<Integer, Integer>) (new LinkedList<Map.Entry<Integer, Integer>> (modes.entrySet())).get(index);
                int currentCount = theEntry.getValue();
                currentCount += entrySet.get(i).getValue();
                modes.replace((yC * bucketSize) + (bucketSize / 2),currentCount);
            } else {
                // add a new value of y-Bottom modes
                modes.put((yC * bucketSize) + (bucketSize / 2), entrySet.get(i).getValue());
            }
        }

        System.out.println("The line bottom modes are " + modes);

        modes = CharacterUtils.sortDescendingByValue(modes);
        System.out.println("The sorted line bottom modes are " + modes);
        List<Map.Entry<Integer,Integer>> modesList = new LinkedList<Map.Entry<Integer, Integer>> (modes.entrySet());

        if (modesList.size() < 2) {
            System.out.println("2 lines not found; returning original lines");
            return lines;
        }
        int aBottom = modesList.get(0).getKey();
        int anotherBottom = modesList.get(1).getKey();

        int firstLineBottom = Math.min(aBottom, anotherBottom);
        int secondLineBottom = Math.max(aBottom, anotherBottom);

        System.out.println("The first line's bottom is at " + firstLineBottom + "; the second line bottom is at " + secondLineBottom);

        Collections.sort(singleLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                // compare the base instead of the top !!
                return (int) (word1.getY() + word1.getHeight() - word2.getY() - word2.getHeight());
            }
        });
        // System.out.println("All words sorted = " + singleLine);

        ArrayList<Rectangle> firstLine = new ArrayList<>();
        ArrayList<Rectangle> secondLine = new ArrayList<>();
        ArrayList<Rectangle> unallocatedWords = new ArrayList<>();

//        Rectangle lastWordAddedInLine1 = null;
//        Rectangle lastWordAddedInLine2 = null;

//        for (Rectangle word : singleLine) {
//            if (firstLine.size() == 0) {
//                firstLine.add(word);
//                lastWordAddedInLine1 = word;
//                continue;
//            }
//            if (heightOverlap(word, lastWordAddedInLine1) > 0.4) {
//                firstLine.add(word);
//                lastWordAddedInLine1 = word;
//                continue;
//            }
//            if (secondLine.size() == 0) {
//                secondLine.add(word);
//                lastWordAddedInLine2 = word;
//                continue;
//            }
//            if (heightOverlap(word, lastWordAddedInLine2) > 0.4) {
//                secondLine.add(word);
//                lastWordAddedInLine2 = word;
//                continue;
//            }
//            unallocatedWords.add(word);
//        }

        for (Rectangle word : singleLine) {
            if (Math.abs(word.getY() + word.getHeight() - firstLineBottom) <= heightTolerance) {
                firstLine.add(word);
                continue;
            }
            if (Math.abs(word.getY() + word.getHeight() - secondLineBottom) <= heightTolerance) {
                secondLine.add(word);
                continue;
            }
            unallocatedWords.add(word);
        }

        Collections.sort(firstLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        Collections.sort(secondLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        System.out.println("First Line after sorting : " + firstLine);
        System.out.println("Second Line after sorting : " + secondLine);

        // check if the unallocated words need to be fitted in as well
        HashMap<Rectangle, Integer> remainingBoxes = new HashMap<>();
        System.out.println("Checking unallocated boxes for allocation. Unallocated boxes = " + unallocatedWords);
        int lettersInLine1 = (patterns.get(0) != null ? patterns.get(0).length() : 5) + (patterns.get(1) != null ? patterns.get(1).length() : 5);
        if (firstLine.size() < lettersInLine1) {
            int shortfall = lettersInLine1 - firstLine.size();
            for (Rectangle word : unallocatedWords) {
                // increase the tolerance
                if (Math.abs(word.getY() + word.getHeight() - firstLineBottom) < medianHeight) {
                    remainingBoxes.put(word, (int) Math.abs(word.getY() + word.getHeight() - firstLineBottom));
                }
            }
            remainingBoxes = CharacterUtils.sortAscendingByValue(remainingBoxes);
            int size = remainingBoxes.size();
            while (Math.min(shortfall, size) > 0) {
                List<Map.Entry<Rectangle,Integer>> listOfBoxes= new LinkedList<Map.Entry<Rectangle, Integer>>((Set)remainingBoxes.entrySet());
                Rectangle aRectangle = (Rectangle) listOfBoxes.get(0).getKey();
                remainingBoxes.remove(aRectangle);
                unallocatedWords.remove(aRectangle);
                size = remainingBoxes.size();
                --shortfall;
                firstLine.add(aRectangle);
                System.out.println("Added word " + aRectangle + " to the first line to make up shortfall");
            }
        }

        int lettersInLine2 = (patterns.get(2) != null ? patterns.get(2).length() : 5) + (patterns.get(3) != null ? patterns.get(3).length() : 5);
        remainingBoxes = new HashMap<>();
        if (secondLine.size() < lettersInLine2) {
            int shortfall = lettersInLine2 - secondLine.size();
            for (Rectangle word : unallocatedWords) {
                if (Math.abs(word.getY() + word.getHeight() - secondLineBottom) < medianHeight * 2.0 / 3) {
                    remainingBoxes.put(word, (int) Math.abs(word.getY() + word.getHeight() - secondLineBottom));
                }
            }
            remainingBoxes = CharacterUtils.sortAscendingByValue(remainingBoxes);
            int size = remainingBoxes.size();
            while (Math.min(shortfall, size) > 0) {
                List<Map.Entry<Rectangle,Integer>> listOfBoxes= new LinkedList<Map.Entry<Rectangle, Integer>>((Set)remainingBoxes.entrySet());
                Rectangle aRectangle = (Rectangle) listOfBoxes.get(0).getKey();
                remainingBoxes.remove(aRectangle);
                size = remainingBoxes.size();
                --shortfall;
                secondLine.add(aRectangle);
                System.out.println("Added word " + aRectangle + " to the second line to make up shortfall");
            }
        }

        // sort the lines again

        Collections.sort(firstLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        // System.out.println("First line sorted = " + firstLine);

        Collections.sort(secondLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        // System.out.println("Second Line sorted = " + secondLine);
        // System.out.println("Unallocated words  = " + unallocatedWords);

        // split the lines now

        double gapToleranceMultiple = 2.25;

        DescriptiveStatistics wStats1 = new DescriptiveStatistics();
        for (Rectangle word : firstLine) {
            wStats1.addValue(word.getWidth());
        }
        double medianWidth1 = wStats1.getPercentile(50);

        DescriptiveStatistics wStats2 = new DescriptiveStatistics();
        for (Rectangle word : secondLine) {
            wStats2.addValue(word.getWidth());
        }
        double medianWidth2 = wStats2.getPercentile(50);

        ArrayList<ArrayList<Rectangle>> readjustedFirstLine = new ArrayList<>();
        if (firstLine.size() == lettersInLine1) {
            // allocate boxes from the front of the line
            ArrayList<Rectangle> aLine = new ArrayList<>();
            // add, starting from index 0, as many boxes as as spec'ed in the first pattern
            for (int i = 0; i < (patterns.get(0) != null ? patterns.get(0).length() : 5); ++i) {
                aLine.add(firstLine.get(i));
            }
            readjustedFirstLine.add(aLine);
            aLine = new ArrayList<>();
            // add, starting from index = length of pattern 0 (i.e. after skipping as many words as are there in pattern 0,
            // as many boxes as as spec'ed in the first pattern
            for (int i = (patterns.get(0) != null ? patterns.get(0).length() : 5); i < firstLine.size(); ++i) {
                aLine.add(firstLine.get(i));
            }
            readjustedFirstLine.add(aLine);
        } else {
            ArrayList<Rectangle> newLine = new ArrayList<>();
            Rectangle previousWord = null;
            for (Rectangle word : firstLine) {
                if (previousWord == null) {
                    newLine.add(word);
                    previousWord = word;
                    continue;
                }
                if ((word.getX() - previousWord.getX() - previousWord.getWidth()) < medianWidth1 * gapToleranceMultiple) {
                    newLine.add(word);
                    previousWord = word;
                } else {
                    readjustedFirstLine.add(newLine);
                    newLine = new ArrayList<>();
                    newLine.add(word);
                    previousWord = word;
                }
            }
            if (newLine.size() > 0) {
                readjustedFirstLine.add(newLine);
            }
        }

        // if the lines haven't been split because of gap intolerance issues, or
        // the number of words in the line is different from the required total of words
        if (readjustedFirstLine.size() < 2) {
            // force fit into 2 lines, starting from the reverse
            int length = firstLine.size();
            ArrayList<Rectangle> aLine = new ArrayList<>();
            for (int i = firstLine.size() - (patterns.get(1) != null ? patterns.get(1).length() : 5); (i >= 0) && (i < firstLine.size()); ++i) {
                // Index out of bounds here
                aLine.add(firstLine.get(i));
            }
            readjustedFirstLine.add(aLine);
            aLine = new ArrayList<>();
            for (int i = 0; i < firstLine.size() - (patterns.get(1) != null ? patterns.get(1).length() : 5); ++i) {
                aLine.add(firstLine.get(i));
            }
            readjustedFirstLine.add(aLine);
        }

        ArrayList<ArrayList<Rectangle>> readjustedSecondLine = new ArrayList<>();
        if (secondLine.size() == lettersInLine2) {
            ArrayList<Rectangle> aLine = new ArrayList<>();
            for (int i = 0; i < (patterns.get(2) != null ? patterns.get(2).length() : 5); ++i) {
                aLine.add(secondLine.get(i));
            }
            readjustedSecondLine.add(aLine);
            aLine = new ArrayList<>();
            for (int i = (patterns.get(3) != null ? patterns.get(3).length() : 5); i < secondLine.size(); ++i) {
                aLine.add(secondLine.get(i));
            }
            readjustedSecondLine.add(aLine);
        } else {
            ArrayList<Rectangle> newLine1 = new ArrayList<>();
            Rectangle prevWord = null;
            for (Rectangle word : secondLine) {
                if (prevWord == null) {
                    newLine1.add(word);
                    prevWord = word;
                    continue;
                }
                if ((word.getX() - prevWord.getX() - prevWord.getWidth()) < medianWidth2 * gapToleranceMultiple) {
                    newLine1.add(word);
                    prevWord = word;
                } else {
                    readjustedSecondLine.add(newLine1);
                    newLine1 = new ArrayList<>();
                    newLine1.add(word);
                    prevWord = word;
                }
            }
            if (newLine1.size() > 0) {
                readjustedSecondLine.add(newLine1);
            }
        }

        if (readjustedSecondLine.size() < 2) {
            // force fit into 2 lines, starting from the reverse
            int length = secondLine.size();
            ArrayList<Rectangle> aLine = new ArrayList<>();
            for (int i = secondLine.size() - (patterns.get(3) != null ? patterns.get(3).length() : 5); (i >= 0) && (i < secondLine.size()); ++i) {
                aLine.add(secondLine.get(i));
            }
            readjustedSecondLine.add(aLine);
            aLine = new ArrayList<>();
            for (int i = 0; i < secondLine.size() - (patterns.get(3) != null ? patterns.get(3).length() : 5); ++i) {
                aLine.add(secondLine.get(i));
            }
            readjustedSecondLine.add(aLine);
        }

        // System.out.println("First Line columnised = " + readjustedFirstLine);
        // System.out.println("Second Line columnised = " + readjustedSecondLine);

        // we could have avoided the variable readjustedSecondLine, and added the newLines into
        // the readjustedFirstLine
        // But I chose to do it this way, in case it becomes necessary later to return these 2 lines in a larger ArrayList
        for (ArrayList<Rectangle> line : readjustedSecondLine) {
            readjustedFirstLine.add(line);
        }

        // finally, drop line segments that have less than the number of minRequiredBoxes
        for (int i = readjustedFirstLine.size() - 1; i >= 0; --i) {
            ArrayList<Rectangle> line = readjustedFirstLine.get(i);
            if (line.size() < minRequiredWordsPerLine) {
                readjustedFirstLine.remove(i);
            }
        }

        return readjustedFirstLine;
    }

    public static double heightOverlap(Rectangle word1, Rectangle word2) {
        return overlapBetween2Lines(word1.getY(), word1.getY() + word1.getHeight(), word2.getY(), word2.getY() + word2.getHeight());
    }

    public static double absoluteHeightOverlap(Rectangle word1, Rectangle word2) {
        return absoluteOverlapBetween2Lines(word1.getY(), word1.getY() + word1.getHeight(), word2.getY(), word2.getY() + word2.getHeight());
    }

    public static double overlapBetween2Lines(double line1Start, double line1End, double line2Start, double line2End) {
        double totalRange = Math.max(line1End, line2End) - Math.min(line1Start, line2Start);
        double sumOfRanges = (line1End - line1Start) + (line2End - line2Start);
        double overlappingInterval = 0.0;

        if (sumOfRanges < totalRange) {
            return 0.0; // no overlap
        }
        overlappingInterval = Math.min(line1End, line2End) - Math.max(line1Start, line2Start);
        double firstLineOverlap = (overlappingInterval * 1.0) / (line1End - line1Start);
        double secondLineOverlap = (overlappingInterval * 1.0) / (line2End - line2Start);
        return Math.max(firstLineOverlap, secondLineOverlap);
    }

    public static double absoluteOverlapBetween2Lines(double line1Start, double line1End, double line2Start, double line2End) {
        double totalRange = Math.max(line1End, line2End) - Math.min(line1Start, line2Start);
        double sumOfRanges = (line1End - line1Start) + (line2End - line2Start);
        double overlappingInterval = 0.0;

        if (sumOfRanges < totalRange) {
            return 0.0; // no overlap
        }
        overlappingInterval = Math.min(line1End, line2End) - Math.max(line1Start, line2Start);
        return overlappingInterval;
    }

    public static ArrayList<ArrayList<Rectangle>> arrangeInto2Lines(ArrayList<ArrayList<Rectangle>> lines) {

        ArrayList<Rectangle> singleLine = new ArrayList<>();
        for (ArrayList<Rectangle> line : lines) {
            for (Rectangle word : line) {
                singleLine.add(word);
            }
        }
        Collections.sort(singleLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                // compare the base instead of the top !!
                return (int) (word1.getY() + word1.getHeight() - word2.getY() - word2.getHeight());
            }
        });
        // System.out.println("All words sorted = " + singleLine);

        ArrayList<Rectangle> firstLine = new ArrayList<>();
        ArrayList<Rectangle> secondLine = new ArrayList<>();
        ArrayList<Rectangle> unallocatedWords = new ArrayList<>();

        Rectangle lastWordAddedInLine1 = null;
        Rectangle lastWordAddedInLine2 = null;

        for (Rectangle word : singleLine) {
            if (firstLine.size() == 0) {
                firstLine.add(word);
                lastWordAddedInLine1 = word;
                continue;
            }
            if (heightOverlap(word, lastWordAddedInLine1) > 0.4) {
                firstLine.add(word);
                lastWordAddedInLine1 = word;
                continue;
            }
            if (secondLine.size() == 0) {
                secondLine.add(word);
                lastWordAddedInLine2 = word;
                continue;
            }
            if (heightOverlap(word, lastWordAddedInLine2) > 0.4) {
                secondLine.add(word);
                lastWordAddedInLine2 = word;
                continue;
            }
            unallocatedWords.add(word);
        }

        Collections.sort(firstLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        // System.out.println("First line sorted = " + firstLine);

        Collections.sort(secondLine, new Comparator<Rectangle>() {
            @Override
            public int compare(Rectangle word1, Rectangle word2) {
                return (int) (word1.getX() - word2.getX());
            }
        });

        ArrayList<ArrayList<Rectangle>> adjustedLines = new ArrayList<>();
        adjustedLines.add(firstLine);
        adjustedLines.add(secondLine);

        return adjustedLines;
    }

    public static final Pix getPixWithE2ELines(Pix originalPix, ArrayList<ArrayList<Rectangle>> firstCutLines) {
        Pix copy = Leptonica1.pixCreate(Leptonica1.pixGetWidth(originalPix),
                Leptonica1.pixGetHeight(originalPix), 1);
        Leptonica1.pixSetBlackOrWhite(copy, ILeptonica.L_SET_WHITE);
        ArrayList<Rectangle> lines = new ArrayList<>();
        for (ArrayList<Rectangle> aLine : firstCutLines) {
            int xStart = Integer.MAX_VALUE;
            int yStart = Integer.MAX_VALUE;
            int xEnd = Integer.MIN_VALUE;
            int yEnd = Integer.MIN_VALUE;
            for (Rectangle word : aLine) {
                xStart = (int) Math.min(xStart, word.getX());
                yStart = (int) Math.min(yStart, word.getY());
                xEnd = (int) Math.max(xEnd, word.getX() + word.getWidth());
                yEnd = (int) Math.max(yEnd, word.getY() + word.getHeight());
            }
            lines.add(new Rectangle(xStart,yStart, xEnd - xStart, yEnd-yStart));
        }

        for (Rectangle line : lines) {
            Box lineBox = Leptonica1.boxCreate((int) line.getX(), (int) line.getY(), (int) line.getWidth(), (int) line.getHeight());
            Pix pixOriginalX = Leptonica1.pixClipRectangle(originalPix, lineBox, null);
            Leptonica1.pixRasterop(copy, lineBox.x, lineBox.y,
                    lineBox.w, lineBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(lineBox);
            LeptUtils.dispose(pixOriginalX);
        }
        return copy;
    }

    public static ArrayList<ArrayList<Rectangle>> orderAndArrangeLines(ArrayList<ArrayList<Rectangle>> inputLines, int characterHeight, int characterWidth) {

        int minDifference = characterHeight / 4; // minimum pixel deviation
        double assumedWidthRatio = 0.8;
        double acceptableGapMultiple = 2.75; // multiple of width

        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        int lineIndex = 0;
        int newIndex = 0;
        mainloop: for (ArrayList<Rectangle> line : inputLines) {
            if (lineIndex == 0) {
                newLines.add(line);
                ++lineIndex;
                ++newIndex;
                continue;
            }
            ArrayList<Rectangle> previousLine = newLines.get(newIndex - 1);
            Rectangle lastBoxInPreviousLine = previousLine.get(previousLine.size() - 1);
            boolean firstWord = true;
            innerloop: for (Rectangle word : line) {
                if (firstWord) {
                    boolean sufficientDistance = (word.getX() > lastBoxInPreviousLine.getX()) && (Math
                            .abs(word.getX() - (lastBoxInPreviousLine.getX() + lastBoxInPreviousLine.getWidth())) > (characterWidth
                            * acceptableGapMultiple));
                    if (sufficientDistance) {
                        break innerloop;
                    }
                }

                firstWord = false;
                innermostloop: for (Rectangle aWord : previousLine) {
                    if (aWord.getHeight() < (characterHeight * 0.45)) {
                        continue innermostloop;
                    }
                    boolean fitFound = (Math.abs(word.getY() - aWord.getY()) <= minDifference)
                            || (Math.abs((word.getY() + word.getHeight()) - (aWord.getY() + aWord.getHeight())) <= minDifference);
                    if (fitFound) {
                        ArrayList<Rectangle> newLine = mergeAndSort(previousLine, line);
                        newLines.set(newIndex - 1, newLine);
                        ++lineIndex;
                        continue mainloop;
                    }
                }
            }
            // if the line doesn't fit into an existing line, then the code reaches here
            newLines.add(line);
            ++lineIndex;
            ++newIndex;
        }

        logger.trace("In orderAndArrangeLines : At 1 in orderAndArrangeLines()");
        logger.trace(newLines.toString());

        // lines would have been allocated properly by now
        // now, one has to split each line into separate pockets of lines depending on
        // gap

        ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
        // Rectangle previousRectangle = null;
        for (ArrayList<Rectangle> line : newLines) {
            ArrayList<Rectangle> newLine = new ArrayList<>();
            finalLines.add(newLine);
            int index = 0;
            wordloop: for (Rectangle word : line) {
                if (index == 0) {
                    newLine.add(word);
                    ++index;
                    continue wordloop;
                }
                if ((word.getX() - (newLine.get(index - 1).getX() + newLine.get(index - 1).getWidth())) < (characterHeight
                        * assumedWidthRatio * acceptableGapMultiple)) {
                    newLine.add(word);
                    ++index;
                } else {
                    newLine = new ArrayList<>();
                    finalLines.add(newLine);
                    newLine.add(word);
                    index = 1;
                }
            }
        }

        logger.trace("In orderAndArrangeLines :At 2 in orderAndArrangeLines()");
        logger.trace(finalLines.toString());

        // now check for boxes within a line whose x-coordinates are close to each
        // other. If they are, then merge them into a single box

        int xTolerance = 4;
        ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();
        class MergePair {
            Rectangle firstBox;
            Rectangle secondBox;

            MergePair(Rectangle i, Rectangle j) {
                this.firstBox = i;
                this.secondBox = j;
            }
        }
        ArrayList<MergePair> boxesToBeMerged = new ArrayList<>();

        for (ArrayList<Rectangle> line : finalLines) {
            boxesToBeMerged.clear();
            for (int i = 0; i < line.size(); ++i) {
                Rectangle box = line.get(i);
                for (int j = i + 1; j < line.size(); ++j) {
                    Rectangle otherBox = line.get(j);
                    if (Math.abs(box.getX() - otherBox.getX()) < xTolerance) {
                        boxesToBeMerged.add(new MergePair(box, otherBox));
                    }
                }
            }
            if (boxesToBeMerged.size() == 0) {
                resultLines1.add(line);
            } else {
                ArrayList<Rectangle> newRectangles = new ArrayList<>();
                Set<Rectangle> rectanglesToBeRemoved = new java.util.HashSet<Rectangle>();
                for (MergePair pair : boxesToBeMerged) {
                    int xCoord = (int) Math.min(pair.firstBox.getX(), pair.secondBox.getX());
                    int yCoord = (int) Math.min(pair.firstBox.getY(), pair.secondBox.getY());
                    int width = (int) Math.max(pair.firstBox.getX() + pair.firstBox.getWidth(), pair.secondBox.getX() + pair.secondBox.getWidth())
                            - xCoord;
                    int height = (int) Math.max(pair.firstBox.getY() + pair.firstBox.getHeight(),
                            pair.secondBox.getY() + pair.secondBox.getHeight()) - yCoord;
                    Rectangle newRectangle = new Rectangle(xCoord, yCoord, width, height);
                    newRectangles.add(newRectangle);
                    rectanglesToBeRemoved.add(pair.firstBox);
                    rectanglesToBeRemoved.add(pair.secondBox);
                }
                for (Rectangle toBeRemoved : rectanglesToBeRemoved) {
                    line.remove(toBeRemoved);
                }
                for (Rectangle toBeAdded : newRectangles) {
                    line.add(toBeAdded);
                }
                resultLines1.add(line);
            }
        }

        // now sort the boxes in each line based on the x-coordinates of the boxes

        for (ArrayList<Rectangle> line : resultLines1) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle letter1, Rectangle letter2) {
                    return (int) (letter1.getX() - letter2.getX());
                }
            });
        }

        // now sort the lines based on the y-coordinate and x-coordinate of the first
        // box in that line

        Collections.sort(resultLines1, new Comparator<ArrayList<Rectangle>>() {

            @Override
            public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
                if ((line1.size() > 0) && (line2.size() > 0)) {
                    if ((line1.get(0).getY() - line2.get(0).getY()) < -5) {
                        return -1;
                    }
                    if ((line1.get(0).getY() - line2.get(0).getY()) > 5) {
                        return 1;
                    }
                }
                if (line1.size() == 0) {
                    return -1;
                }
                if (line2.size() == 0) {
                    return 1;
                }
                return (int) (line1.get(0).getX() - line2.get(0).getX());
            }

        });

        return resultLines1;
    }

    public static ArrayList<ArrayList<Rectangle>> reallocateLines(ArrayList<ArrayList<Rectangle>> input, int characterHeight, int characterWidth) {

        // do reallocation of lines to other lines, figuring out if they are in the same
        // line

        // ArrayList<Integer> medianWidths = new ArrayList<>();
        ArrayList<Integer> medianHeights = new ArrayList<>();
        ArrayList<Integer> topEdge = new ArrayList<>();
        ArrayList<Integer> bottomEdge = new ArrayList<>();

        for (ArrayList<Rectangle> line : input) {
            DescriptiveStatistics heightStats = new DescriptiveStatistics();
            DescriptiveStatistics topEdgeStats = new DescriptiveStatistics();
            DescriptiveStatistics bottomEdgeStats = new DescriptiveStatistics();
            for (Rectangle letter : line) {
                heightStats.addValue(letter.getHeight());
                topEdgeStats.addValue(letter.getY());
                bottomEdgeStats.addValue(letter.getY() + letter.getHeight());
            }
            medianHeights.add((int) heightStats.getPercentile(50));
            topEdge.add((int) topEdgeStats.getPercentile(50));
            bottomEdge.add((int) bottomEdgeStats.getPercentile(50));
        }

        ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();
        // int topEdgeTolerance = 14;
        ArrayList<Integer> linesAccountedFor = new ArrayList<Integer>();
        double acceptableGapAsHeightMultiple = 2.75;
        double overlapCutoff = 0.375; // 33% is a pretty reasonable cutoff; 37.5% is safer

        currentLineLoop: for (int i = 0; i < input.size(); ++i) {
            if (linesAccountedFor.contains(Integer.valueOf(i))) {
                continue currentLineLoop;
            }

            ArrayList<Rectangle> currentLine = input.get(i);

            Collections.sort(currentLine, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle letter1, Rectangle letter2) {
                    return (int) (letter1.getX() - letter2.getX());
                }
            });

            if (i == (input.size() - 1)) {
                resultLines.add(currentLine);
                break currentLineLoop;
            }
            nextLineLoop: for (int j = i + 1; j < input.size(); ++j) {
                if (linesAccountedFor.contains(Integer.valueOf(j))) {
                    continue nextLineLoop;
                }
                ArrayList<Rectangle> nextLine = input.get(j);

                Collections.sort(nextLine, new Comparator<Rectangle>() {

                    @Override
                    public int compare(Rectangle letter1, Rectangle letter2) {
                        return (int) (letter1.getX() - letter2.getX());
                    }
                });

                boolean inSameLine = false;
                // find lowest box in current line within tolerance of 5 from box 0
                Rectangle lowestBoxInCurrentLine = currentLine.get(0);
                if (currentLine.get(currentLine.size() - 1).getY() > lowestBoxInCurrentLine.getY()) {
                    lowestBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
                }
                Rectangle highestBoxInCurrentLine = currentLine.get(0);
                if (currentLine.get(currentLine.size() - 1).getY() < highestBoxInCurrentLine.getY()) {
                    highestBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
                }

                // find highest box in next line within tolerance of topEdgeTolerance from it's
                // lines box 0
                Rectangle highestBoxInNextLine = nextLine.get(0);
                if (nextLine.get(nextLine.size() - 1).getY() < highestBoxInNextLine.getY()) {
                    highestBoxInNextLine = nextLine.get(nextLine.size() - 1);
                }

                Rectangle lowestBoxInNextLine = nextLine.get(0);
                if (nextLine.get(nextLine.size() - 1).getY() > lowestBoxInNextLine.getY()) {
                    lowestBoxInNextLine = nextLine.get(nextLine.size() - 1);
                }

                int bottomEnd = Math.max(bottomEdge.get(i), bottomEdge.get(j));
                int topEnd = Math.min(topEdge.get(i), topEdge.get(j));
                // overlap = h1 + h2 - d
                int overlap = Math.max(0, (medianHeights.get(i) + medianHeights.get(j)) - (bottomEnd - topEnd));
                double overlapPercent = (overlap * 1.0) / (bottomEnd - topEnd); // = overlap / fullRange

                if (overlapPercent > overlapCutoff) {
                    inSameLine = true;
                }

                if (!inSameLine) {

                    logger.trace(
                            "In reallocateLines : Found that line " + (j + 1)
                                    + " is not in the same line as line " + (i + 1));

                    // if the nextLine is the last line, then add the last line and break out of the
                    // loop
                    if (j == (input.size() - 1)) {
                        resultLines.add(currentLine);
                        linesAccountedFor.add(i);
                        logger.trace(
                                "In reallocateLines : Adding line " + (i + 1));
                    }
                    if (i == (input.size() - 2)) { // second last line reached
                        resultLines.add(nextLine);
                        linesAccountedFor.add(j);

                        logger.trace("In reallocateLines : Reached second last line for comparison and found that last line is in a different line. ");
                        logger.trace("In rellocateLines(): Adding line " + (j + 1));
                        break currentLineLoop; // add the last line and exit the loop
                    }
                    if (j < (input.size() - 1)) {
                        continue nextLineLoop;
                    }
                    continue currentLineLoop;
                }

                // if the next line looks to be in the same line, then the code continues from
                // here

                // if the next line is in the same y-zone as the current line, check if any
                // boxes in these 2 lines are close to each other. If yes, the next line can be
                // fitted into the current line
                // first, check if the medianWidth can be used or a different width setting has
                // to be applied

                double acceptableGap = (((medianHeights.get(i) + medianHeights.get(j)) * 1.0) / 2)
                        * acceptableGapAsHeightMultiple;
                logger.trace("In reallocateLines : Acceptable Gap for judging if line " + (j + 1)
                        + " can be added to line " + (i + 1) + " is " + acceptableGap);

                boolean nextLineCanBeAddedToCurrent = false;
                boxClosenessCheckLoop: for (int p = 0; p < currentLine.size(); ++p) {
                    Rectangle letterInCurrentLine = currentLine.get(p);
                    // int allowableGap = Math.max((int) (referenceWidth * acceptableWidthMultiple),
                    // (int) (referenceGap * acceptableGapMultiple));
                    double allowableGap = acceptableGap;
                    for (int q = 0; q < nextLine.size(); ++q) {
                        Rectangle letterInNextLine = nextLine.get(q);
                        if (Math.abs((letterInCurrentLine.getX() + letterInCurrentLine.getWidth())
                                - letterInNextLine.getX()) < (allowableGap)) {
                            nextLineCanBeAddedToCurrent = true;
                            break boxClosenessCheckLoop;
                        }
                        if (Math.abs((letterInNextLine.getX() + letterInNextLine.getWidth())
                                - letterInCurrentLine.getX()) < (allowableGap)) {
                            nextLineCanBeAddedToCurrent = true;
                            break boxClosenessCheckLoop;
                        }
                    }
                }
                if (nextLineCanBeAddedToCurrent) {
                    logger.trace(
                            "In reallocateLines : Determined that line " + (j + 1)
                                    + " can be added to line " + (i + 1));
                    logger.trace(
                            "In reallocateLines : Current Line before addition is : " + currentLine);
                    for (int q = 0; q < nextLine.size(); ++q) {
                        currentLine.add(nextLine.get(q));
                    }
                    linesAccountedFor.add(j);
                    logger.trace(
                            "In reallocateLines : Current Line after addition is : " + currentLine);
                    if (j == (input.size() - 1)) {
                        resultLines.add(currentLine);
                        linesAccountedFor.add(i);
                        // break currentLineLoop;
                        continue currentLineLoop;
                    }
                    continue nextLineLoop;
                }

                if (j == (input.size() - 1)) {
                    resultLines.add(currentLine);
                    linesAccountedFor.add(i);
                    continue currentLineLoop;
                }
            }
        }

        logger.trace("In reallocateLines : RESULT LINES IS - " + resultLines);

        return resultLines;

    }

    public static ArrayList<ArrayList<Rectangle>> reallocateLinesAgain(ArrayList<ArrayList<Rectangle>> input, int characterHeight, int characterWidth) {

        // do reallocation of lines to other lines again, figuring out if they are in
        // the same line

        ArrayList<ArrayList<Rectangle>> resultLines = new ArrayList<>();
        // int topEdgeTolerance = 7;
        double heightOverlapCutoff = 0.7; // At least 70% of one line needs to overlap with the other line
        ArrayList<Integer> linesAccountedFor = new ArrayList<Integer>();
        ArrayList<Integer> topEdge = new ArrayList<>();
        ArrayList<Integer> bottomEdge = new ArrayList<>();
        ArrayList<Integer> gaps = new ArrayList<>();
        ArrayList<Integer> heights = new ArrayList<>();

        for (ArrayList<Rectangle> line : input) {
            DescriptiveStatistics topEdgeStats = new DescriptiveStatistics();
            DescriptiveStatistics bottomEdgeStats = new DescriptiveStatistics();
            DescriptiveStatistics gapStats = new DescriptiveStatistics();
            DescriptiveStatistics heightStats = new DescriptiveStatistics();
            for (int i = 0; i < line.size(); ++i) {
                topEdgeStats.addValue(line.get(i).getY());
                heightStats.addValue(line.get(i).getHeight());
                if (i > 0) {
                    gapStats.addValue(line.get(i).getX() - line.get(i - 1).getX() - line.get(i - 1).getWidth());
                }
                bottomEdgeStats.addValue(line.get(i).getY() + line.get(i).getHeight());
            }
            topEdge.add((int) topEdgeStats.getPercentile(50));
            bottomEdge.add((int) bottomEdgeStats.getPercentile(50));
            gaps.add((int) gapStats.getPercentile(50));
            heights.add((int) heightStats.getPercentile(50));
        }

        logger.trace("In reallocateLinesAgain() : topEdgeStats are " + topEdge);
        logger.trace(
                "In reallocateLinesAgain() : bottomEdgeStats are " + bottomEdge);
        logger.trace("In reallocateLinesAgain() : gapStats are " + gaps);
        logger.trace("In reallocateLinesAgain() : heightStats are " + heights);

        currentLineLoop: for (int i = 0; i < input.size(); ++i) {
            logger.trace(
                    "In reallocateLinesAgain() : Evaluating line " + (i + 1));
            if (linesAccountedFor.contains(Integer.valueOf(i))) {
                logger.trace("In reallocateLinesAgain() : Skipping line "
                        + (i + 1) + " as it is already accounted for");
                continue currentLineLoop;
            }
            double acceptableGapAsHeightMultiple = 4.0;
            ArrayList<Rectangle> currentLine = input.get(i);
            Collections.sort(currentLine, new Comparator<Rectangle>() {
                @Override
                public int compare(Rectangle letter1, Rectangle letter2) {
                    return (int) (letter1.getX() - letter2.getX());
                }
            });

            if (i == (input.size() - 1)) {
                logger.trace(
                        "In reallocateLinesAgain() : Adding line " + (i + 1));
                resultLines.add(currentLine);
                break currentLineLoop;
            }
            nextLineLoop: for (int j = i + 1; j < input.size(); ++j) {
                // if the last line is the "nextLine", then the currentLine anyhow needs to be
                // added
                if (j == (input.size() - 1)) {
                    resultLines.add(currentLine);
                    linesAccountedFor.add(i);
                    logger.trace(
                            "In reallocateLinesAgain() : Adding line " + (i + 1));
                }
                if (linesAccountedFor.contains(Integer.valueOf(j))) {
                    logger.trace("In reallocateLinesAgain() : Skipping nextine "
                            + (j + 1) + " as it is already accounted for");
                    continue nextLineLoop;
                }
                int topOfRange = Math.min(topEdge.get(i), topEdge.get(j));
                int bottomOfRange = Math.max(bottomEdge.get(i), bottomEdge.get(j));
                int overlap = Math.max(0, (((bottomEdge.get(i) - topEdge.get(i)) + (bottomEdge.get(j) - topEdge.get(j)))
                        - (bottomOfRange - topOfRange))); // overlap = Math.max(h1 + h2 - d,0)
                double overlapPercent = (overlap * 1.0) / (bottomOfRange - topOfRange);
                overlapPercent = Math.max(overlapPercent, (overlap * 1.0) / (bottomEdge.get(i) - topEdge.get(i)));
                overlapPercent = Math.max(overlapPercent, (overlap * 1.0) / (bottomEdge.get(j) - topEdge.get(j)));
                if (overlapPercent < heightOverlapCutoff) {
                    logger.trace("In reallocateLinesAgain() : Skipping nextLine " + (j + 1) + " as overlap is "
                            + overlapPercent + " between lines " + (i + 1) + " and " + (j + 1));
                    continue nextLineLoop;
                } else {
                    logger.trace("In reallocateLinesAgain() : Overlap of "
                            + overlapPercent + " between lines " + (i + 1) + " and " + (j + 1));
                }
                logger.trace("In reallocateLinesAgain() : nextLine " + (j + 1)
                        + " is not excluded. Hence, evaluating gap for inclusion in line " + (i + 1));
                ArrayList<Rectangle> nextLine = input.get(j);
                Collections.sort(nextLine, new Comparator<Rectangle>() {
                    @Override
                    public int compare(Rectangle letter1, Rectangle letter2) {
                        return (int) (letter1.getX() - letter2.getX());
                    }
                });

                boolean inSameLine = false;
                // find last box in current line within tolerance of 5 from box 0
                Rectangle lastBoxInCurrentLine = currentLine.get(currentLine.size() - 1);
                Rectangle firstBoxInCurrentLine = currentLine.get(0);

                logger.trace("In reallocateLinesAgain() : all boxes in line "
                        + (i + 1) + " are " + currentLine);
                logger.trace("In reallocateLinesAgain() : last box in line "
                        + (i + 1) + " is " + lastBoxInCurrentLine);
                // find first box in next line
                Rectangle firstBoxInNextLine = nextLine.get(0);
                Rectangle lastBoxInNextLine = nextLine.get(nextLine.size() - 1);

                logger.trace("In reallocateLinesAgain() : all boxes in line "
                        + (j + 1) + " are " + nextLine);
                logger.trace(
                        "In reallocateLinesAgain() : first box in nextLine "
                                + (j + 1) + " is " + firstBoxInNextLine);

                double averageHeight = ((heights.get(i) + heights.get(j)) * 1.0) / 2;

                if (Math.abs((lastBoxInCurrentLine.getX() + lastBoxInCurrentLine.getWidth())
                        // - firstBoxInNextLine.getX()) <= (acceptableGapMultiple * medianGap)) { ***Removed
                        // gapMultiple as it is unreliable. Height multiple is a more consistent
                        - firstBoxInNextLine.getX()) <= (acceptableGapAsHeightMultiple * averageHeight)) {
                    logger.trace("In reallocateLinesAgain() : acceptableGap in line " + (i + 1) + " and line "
                            + (j + 1) + " is " + (acceptableGapAsHeightMultiple * averageHeight));

                    inSameLine = true;
                }

                // added this block of code
                // ***********
                if (Math.abs((lastBoxInNextLine.getX() + lastBoxInNextLine.getWidth())
                        // - firstBoxInNextLine.getX()) <= (acceptableGapMultiple * medianGap)) { ***Removed
                        // gapMultiple as it is unreliable. Height multiple is a more consistent
                        - firstBoxInCurrentLine.getX()) <= (acceptableGapAsHeightMultiple * averageHeight)) {
                    logger.trace("In reallocateLinesAgain() : acceptableGap in line " + (i + 1) + " and line "
                            + (j + 1) + " is " + (acceptableGapAsHeightMultiple * averageHeight));
                    inSameLine = true;
                }
                // ***********

                if (!inSameLine) {
                    logger.trace("In reallocateLinesAgain() : Found that line "
                            + (i + 1) + " is too far from the next line " + (j + 1));
                    // if the nextLine is the last line, then add the last line and break out of the
                    // loop
                    if (i == (input.size() - 2)) { // currentLine is second last line reached
                        // Note: current line does not need to be added as it is added as soon as last
                        // line is being evaluated
                        resultLines.add(nextLine);
                        linesAccountedFor.add(j); // not needed as both evaluation loops are over, but kept for
                        // completeness
                        logger.trace("In reallocateLinesAgain() : Reached second last line for comparison and found that last line is in a different line. ");
                        logger.trace("Adding line " + (j + 1));
                        break currentLineLoop; // add the last line and exit the loop
                    }
                    if (j < (input.size() - 1)) {
                        continue nextLineLoop;
                    }
                    continue currentLineLoop;
                }

                // if the next line looks to be in the same line, then the code continues from
                // here

                logger.trace("In reallocateLinesAgain() : Determined that line "
                        + (j + 1) + " can be added to line " + (i + 1));
                logger.trace("In reallocateLinesAgain() : Current Line "
                        + (i + 1) + " before addition is : " + currentLine);
                for (int q = 0; q < nextLine.size(); ++q) {
                    currentLine.add(nextLine.get(q));
                }
                linesAccountedFor.add(j);
                logger.trace("In reallocateLinesAgain() : Current Line "
                        + (i + 1) + " before addition is : " + currentLine);
                continue nextLineLoop;
            }
        }

        // The ABOVE code MERGES 2 or more lines into 1.
        // The code BELOW SEPARATES 1 line into 2 or more lines

        // Do a splitting of lines if there is an inordinate gap between words
        // in a line. Split only those lines which are longer than 8 words in length.
        // Since it is difficult to know at this juncture how many bounding boxes
        // correspond to 8 words,
        // making a guess that the routine should be executed only if there are at least
        // 7 boxes in the line
        // This is to ensure that the bounding boxes of a typical price line of Rs112000
        // (Rs.1120.00) is not split up

        // Also, split lines that have a considerable gap in between and the number of
        // words on either side is <= 2. Determine number of words as a multiple of
        // width / (referenceWidth)

        ArrayList<ArrayList<Rectangle>> interimLines1 = new ArrayList<>();
        // double acceptableHeightMultiple = 3.25;
        double acceptableHeightMultiple = 4.25; // height multiple seems to be more consistent than gap and width
        // multiples
        // double acceptableHeightMultipleForEdgeWords = 2.25;
        double acceptableHeightMultipleForEdgeWords = 3.25;
        int maximumNoOfEdgeWords = 2;
        int minimumLineLengthForSplitting = 9;
        int ln = 0;
        outerloop: for (ArrayList<Rectangle> line : resultLines) {
            ++ln;
            Collections.sort(line, new Comparator<Rectangle>() {
                @Override
                public int compare(Rectangle letter1, Rectangle letter2) {
                    return (int) (letter1.getX() - letter2.getX());
                }
            });
            logger.trace("In reallocateLinesAgain() : size of line " + ln
                    + " is " + line.size() + " which is to be compared with "
                    + (minimumLineLengthForSplitting - maximumNoOfEdgeWords));
            if (line.size() < (minimumLineLengthForSplitting - maximumNoOfEdgeWords)) {
                interimLines1.add(line);
                continue outerloop;
            }
            heights.clear();
            ArrayList<Integer> gapStats = new ArrayList<>();
            for (int i = 0; i < line.size(); ++i) {
                heights.add((int) (line.get(i).getHeight()));
                if (i > 0) {
                    gapStats.add((int) (line.get(i).getX() - line.get(i - 1).getX() - line.get(i - 1).getWidth()));
                }
            }
            Collections.sort(heights);
            double medianHeight = ((heights.size() % 2) == 0)
                    ? ((heights.get(heights.size() / 2) + heights.get((heights.size() / 2) - 1)) / 2)
                    : heights.get(heights.size() / 2);

            Collections.sort(gapStats);
            if (gapStats.size() > 1) {
                gapStats.remove(gapStats.size() - 1);
            }
            if (gapStats.size() > 1) {
                gapStats.remove(0);
            }
            if (gapStats.size() > 1) {
                gapStats.remove(gapStats.size() - 1);
            }
            if (gapStats.size() > 1) {
                gapStats.remove(0);
            }
            double medianGap = ((gapStats.size() % 2) == 0)
                    ? ((gapStats.get(gapStats.size() / 2) + gapStats.get((gapStats.size() / 2) - 1)) / 2)
                    : gapStats.get(gapStats.size() / 2);

            double acceptableGap = medianHeight * acceptableHeightMultiple;
            double acceptableEdgeGap = medianHeight * acceptableHeightMultipleForEdgeWords;

            double acceptableGapMultiple = 6;
            // empirically seen
            double acceptableGapAsMultipleOfGaps = Math.max(acceptableGapMultiple * medianGap, 2.0 * medianHeight);


            logger.trace("In reallocateLinesAgain() : Acceptable Gap for line "
                    + ln + " by gapMultiple is " + acceptableGapAsMultipleOfGaps);
            logger.trace("In reallocateLinesAgain() : Acceptable Gap for line "
                    + ln + " by heightMultiple is " + acceptableGap);

            ArrayList<Rectangle> newLine = new ArrayList<>();
            boolean firstWord = true;
            innerloop: for (int i = 0; i < line.size(); ++i) {
                if (firstWord) {
                    newLine.add(line.get(i));
                    firstWord = false;
                    continue innerloop;
                }
                int currentGap = (int) line.get(i).getX() - (int) line.get(i - 1).getX() - (int) line.get(i - 1).getWidth();
                if (((currentGap > acceptableGap) || (currentGap > acceptableGapAsMultipleOfGaps))
                        && ((line.size() - i) >= (minimumLineLengthForSplitting - 1))) {
                    logger.trace(
                            "In reallocateLinesAgain() : Splitting line " + ln);
                    interimLines1.add(newLine);
                    newLine = new ArrayList<>();
                } else {
                    if (((currentGap > acceptableEdgeGap) || (currentGap > acceptableGapAsMultipleOfGaps))
                            && (((line.size() - i) <= maximumNoOfEdgeWords) || (i < maximumNoOfEdgeWords))) {
                        logger.trace(
                                "In reallocateLinesAgain() : Splitting line " + ln);
                        interimLines1.add(newLine);
                        newLine = new ArrayList<>();
                    }
                }
                newLine.add(line.get(i));
            }
            interimLines1.add(newLine);
        }

        logger.trace(
                "In reallocateLinesAgain() : At 4 in reallocateLinesAgain()");
        logger.trace(interimLines1.toString());


        // **************************

        // do a round of cleanup to eliminate small length lines <= 2 that are unlikely
        // to have meaningful characters

        ArrayList<ArrayList<Rectangle>> resultLines1 = new ArrayList<>();

        for (ArrayList<Rectangle> line : interimLines1) {
            if (line.size() <= 2) {
                boolean markedForDeletion = false;
                double averageHeight = 0.0;
                double totalWidth = 0.0;
                for (Rectangle letter : line) {
                    averageHeight += letter.getHeight();
                    totalWidth += letter.getWidth();
                }
                averageHeight = averageHeight / line.size();
                double likelyWidthOfAWord = characterWidth;
                int likelyNumberOfWords = (int) Math.round(totalWidth / likelyWidthOfAWord);
                if (likelyNumberOfWords <= 2) {
                    markedForDeletion = true;
                }
                // }
                if (!markedForDeletion) {
                    resultLines1.add(line);
                } else {
                    logger.trace("In reallocateLinesAgain() : Dropped boxes in line - " + line);
                }
            } else {
                resultLines1.add(line);
            }
        }

        logger.trace(
                "In reallocateLinesAgain() : At 3 in reallocateLinesAgain()");
        logger.trace(resultLines1.toString());

        // do another round of cleanup to eliminate single-box lines with box of small
        // width (< mostLikelyWidth) or 0.6*mostLikelyHeight

        ArrayList<ArrayList<Rectangle>> resultLines2 = new ArrayList<>();

        for (ArrayList<Rectangle> line : resultLines1) {
            if (line.size() <= 1) {
                boolean markedForDeletion = true;
                for (Rectangle letter : line) {
                    if ((letter.getWidth() <= (characterWidth * 0.3))
                            || (letter.getHeight() <= (characterHeight * 0.45))) {
                        markedForDeletion = markedForDeletion && true;
                    } else {
                        markedForDeletion = markedForDeletion && false;
                    }
                }
                if (!markedForDeletion) {
                    resultLines2.add(line);
                } else {
                    logger.trace("In reallocateLinesAgain() : Dropped boxes in line - " + line);
                }
            } else {
                resultLines2.add(line);
            }
        }

        // **************************


        logger.debug(
                "In reallocateLinesAgain() : FINAL LINES IS - " + resultLines2);


        return resultLines2;

    }

    public static ArrayList<Rectangle> mergeAndSort(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {
        ArrayList<Rectangle> result = new ArrayList<Rectangle>();
        for (Rectangle word : line1) {
            result.add(word);
        }
        for (Rectangle word : line2) {
            int index = 0;
            innerloop: for (Rectangle existingWord : result) {
                if (word.getX() > existingWord.getX()) {
                    ++index;
                } else {
                    break innerloop;
                }
            }
            result.add(index, word);
        }
        return result;
    }

    public static final int overlapArea(Rectangle word1, Rectangle word2) {
        int xOverlap = (int) Math.max(0,Math.min(word1.getX() + word1.getWidth(), word2.getX() + word2.getWidth()) - Math.max(word1.getX(), word2.getX()));
        int yOverlap = (int) Math.max(0,Math.min(word1.getY() + word1.getHeight(), word2.getY() + word2.getHeight()) - Math.max(word1.getY(), word2.getY()));
        int areaOverlap = Math.max(0, xOverlap * yOverlap);
        return areaOverlap;
    }

    public static final ArrayList<ArrayList<Rectangle>> sortLines(ArrayList<ArrayList<Rectangle>> lines, boolean removeEmptyLines) {

        String currentMergeSortProperty = System.getProperty("java.util.Arrays.useLegacyMergeSort");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // get rid of empty lines from the input
        ArrayList<ArrayList<Rectangle>> tempLines = new ArrayList<>();
        for (ArrayList<Rectangle> line : lines) {
            if (removeEmptyLines) {
                if (line.size() != 0) {
                    tempLines.add(line);
                }
            } else {
                tempLines.add(line);
            }
        }

        // sort each line in the input lines by x-coordinate
        for (ArrayList<Rectangle> line : tempLines) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle r1, Rectangle r2) {
                    if ((r1 == null) && (r2 == null)) {
                        return 0;
                    }
                    if (r2 == null) {
                        return -1;
                    }
                    if (r1 == null) {
                        return 1;
                    }
                    return (int) (r1.getX() - r2.getX());
                }

            });
        }

        // sort the lines by y-coordinate
        Collections.sort(tempLines, new Comparator<ArrayList<Rectangle>>() {

            @Override
            public int compare(ArrayList<Rectangle> line1, ArrayList<Rectangle> line2) {

                if ((line1.size() == 0) && (line2.size() == 0)) {
                    return 0;
                }
                if ((line1.size() > 0) && (line2.size() == 0)) {
                    return 1;
                }
                if ((line1.size() == 0) && (line2.size() > 0)) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) < -5) {
                    return -1;
                }
                if ((line1.get(0).getY() - line2.get(0).getY()) > 5) {
                    return 1;
                }
                return ((int) ((line1.get(0).getX() - line2.get(0).getX()) > 5 ? 1 : ((line1.get(0).getX() - line2.get(0).getX()) < -5 ? -1 : 0)));
            }
        });

        if (currentMergeSortProperty != null) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", currentMergeSortProperty);
        }

        return tempLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> sortWord(ArrayList<ArrayList<Rectangle>> lines, boolean removeEmptyLines) {

        String currentMergeSortProperty = System.getProperty("java.util.Arrays.useLegacyMergeSort");
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        // get rid of empty lines from the input
        ArrayList<ArrayList<Rectangle>> tempLines = new ArrayList<>();
        for (ArrayList<Rectangle> line : lines) {
            if (removeEmptyLines) {
                if (line.size() != 0) {
                    tempLines.add(line);
                }
            } else {
                tempLines.add(line);
            }
        }

        // sort each line in the input lines by x-coordinate
        for (ArrayList<Rectangle> line : tempLines) {
            Collections.sort(line, new Comparator<Rectangle>() {

                @Override
                public int compare(Rectangle r1, Rectangle r2) {
                    if ((r1 == null) && (r2 == null)) {
                        return 0;
                    }
                    if (r2 == null) {
                        return -1;
                    }
                    if (r1 == null) {
                        return 1;
                    }
                    return (int) (r1.getX() - r2.getX());
                }

            });
        }

        // get rid of empty lines from the input
        ArrayList<ArrayList<Rectangle>> tempLines1 = new ArrayList<>();
        for (ArrayList<Rectangle> line : tempLines) {
            if (line.size() >= 1) {
                tempLines1.add(line);
            }
        }

        if (currentMergeSortProperty != null) {
            System.setProperty("java.util.Arrays.useLegacyMergeSort", currentMergeSortProperty);
        }

        return tempLines1;
    }

    public static final ArrayList<ArrayList<Rectangle>> mergeLinesBasedOnYOverlap(ArrayList<ArrayList<Rectangle>> lines, double minRequiredOverlap) {

        // assumes that an x-y sorted list of lines is given;
        // If a sorted list is not given, first sort using TesseractUtils.sortLine()

        // first delete all empty lines
        for (int i = lines.size()-1; i >= 0; --i) {
            if (lines.get(i).size() == 0) {
                lines.remove(i);
            }
        }

        // merge lines that are in the same-ish y-coordinate
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        ArrayList<Rectangle> newLine = new ArrayList<>();
        outerloop: for (int i = 0; i < lines.size(); ++i) {
            ArrayList<Rectangle> currentLine = lines.get(i);
            // not needed, bu kept as a matter of abundant caution
            if (currentLine.size() == 0) {
                continue outerloop;
            }
            if (newLines.size() == 0) {
                for (Rectangle word : currentLine) {
                    newLine.add(word);
                }
                // newLines.add(newLine);
                // newLine = new ArrayList<>();
                continue outerloop;
            }
            ArrayList<Rectangle> previousLine = null;
            if (newLine.size() != 0) {
                previousLine = newLine;
            } else {
                previousLine = lines.get(i-1);
            }
            Rectangle firstWordOfCurrentLine = currentLine.get(0);
            Rectangle lastWordOfPreviousLine = previousLine.get(previousLine.size() - 1);
            double absoluteOverlap = TesseractUtils.absoluteHeightOverlap(firstWordOfCurrentLine, lastWordOfPreviousLine);
            if ((TesseractUtils.absoluteHeightOverlap(firstWordOfCurrentLine, lastWordOfPreviousLine)) > (minRequiredOverlap * (firstWordOfCurrentLine.getHeight() + lastWordOfPreviousLine.getHeight()) / 2)) {
                for (Rectangle word : currentLine) {
                    newLine.add(word);
                }
                continue;
            } else {
                newLines.add(newLine);
                newLine = new ArrayList<>();
                for (Rectangle word : currentLine) {
                    newLine.add(word);
                }
            }
        }
        if (newLine.size() != 0) {
            newLines.add(newLine);
        }
        return newLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> removeLettersStartingAtWrongY(ArrayList<ArrayList<Rectangle>> lines) {

        // assumes that an x-y sorted list of lines is given;
        // If a sorted list is not given, first sort using TesseractUtils.sortLine()

        // merge lines that are in the same-ish y-coordinate
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        ArrayList<Rectangle> newLine = new ArrayList<>();
        for (int i = 0; i < lines.size(); ++i) {
            ArrayList<Rectangle> currentLine = lines.get(i);
            DescriptiveStatistics heightStats = new DescriptiveStatistics();
            for (Rectangle word : currentLine) {
                heightStats.addValue(word.getHeight());
            }
            int medianHeight = (int) heightStats.getPercentile(50);
            DescriptiveStatistics yStats = new DescriptiveStatistics();
            for (Rectangle word : currentLine) {
                if (Math.abs(word.getHeight() - medianHeight) < medianHeight / 2) {
                    yStats.addValue(word.getY());
                }
            }
            int medianY = (int) yStats.getPercentile(50);
            for (Rectangle word : currentLine) {
                if (Math.abs(word.getY() - medianY) < 2.0 * medianHeight / 5) {
                    newLine.add(word);
                }
            }
            newLines.add(newLine);
            newLine = new ArrayList<>();
        }
        return newLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> dropOverlappingBoxesInLines(ArrayList<ArrayList<Rectangle>> lines, double minRequiredOverlap) {

        // drop boxes from a line that overlaps with other boxes from the same line
        // as it is likely that overlapping boxes are spurious boxes
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); ++i) {
            logger.trace("In dropOverlappingBoxesInLines() : Line " + i);
            ArrayList<Rectangle> newLine = new ArrayList<>();
            ArrayList<Rectangle> currentLine = lines.get(i);
            int wordsInLine = currentLine.size();
            TreeSet<Integer> indicesMarkedForDeletion = new TreeSet<>();
            for (int j = 0; j < wordsInLine; ++j) {
                Rectangle currentWord = currentLine.get(j);
                for (int k = j+1; k < wordsInLine; ++k) {
                    Rectangle otherWord = currentLine.get(k);
                    if (TesseractUtils.overlapArea(currentWord, otherWord) > minRequiredOverlap) {
                        System.out.println("In dropOverlappingBoxesInLines() : Line " + i + " - For deletion, adding boxes " + j + " and " + k);
                        indicesMarkedForDeletion.add(j);
                        indicesMarkedForDeletion.add(k);
                    }
                }
            }
            for (int j = 0; j < wordsInLine; ++j) {
                if (!indicesMarkedForDeletion.contains(j)) {
                    newLine.add(currentLine.get(j));
                }
            }
            newLines.add(newLine);
        }
        return newLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> dropShortOrWideBoxes(ArrayList<ArrayList<Rectangle>> lines, double heightCutoff, double widthCutOff) {

        // drop boxes from a line that are larger than width cutoff or shorter than height cutoff
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); ++i) {
            ArrayList<Rectangle> newLine = new ArrayList<>();
            ArrayList<Rectangle> currentLine = lines.get(i);
            int wordsInLine = currentLine.size();
            TreeSet<Integer> indicesMarkedForDeletion = new TreeSet<>();
            for (int j = 0; j < wordsInLine; ++j) {
                Rectangle currentWord = currentLine.get(j);
                if (currentWord.getWidth() > widthCutOff) {
                    indicesMarkedForDeletion.add(j);
                }
                if (currentWord.getHeight() < heightCutoff) {
                    indicesMarkedForDeletion.add(j);
                }
            }
            for (int j = 0; j < wordsInLine; ++j) {
                if (!indicesMarkedForDeletion.contains(j)) {
                    newLine.add(currentLine.get(j));
                }
            }
            newLines.add(newLine);
        }
        return newLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> splitLinesBasedOnGap(ArrayList<ArrayList<Rectangle>> lines, double maxAllowedGap) {

        // assumes that an x-y sorted list of lines is given;
        // If a sorted list is not given, first sort using TesseractUtils.sortLine()

        // split lines based on gap between boxes
        logger.trace("Lines before splitting are " + lines);
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); ++i) {

            ArrayList<Rectangle> currentLine = lines.get(i);
            if (currentLine.size() <= 1) {
                newLines.add(currentLine);
                continue;
            }
            int wordsInLine = currentLine.size();
            TreeSet<Integer> indicesMarkedForSplit = new TreeSet<>();
            for (int j = 1; j < wordsInLine; ++j) {
                Rectangle currentWord = currentLine.get(j);
                Rectangle previousWord = currentLine.get(j - 1);
                if (currentWord.getX()  - (previousWord.getX() + previousWord.getWidth()) > maxAllowedGap) {
                    indicesMarkedForSplit.add(j);
                }
            }
            logger.trace("Marked words at index " + indicesMarkedForSplit + " for splitting in line " + i);
            ArrayList<Rectangle> newLine = new ArrayList<>();
            for (int j = 0; j < wordsInLine; ++j) {
                if (indicesMarkedForSplit.contains(j)) {
                    newLines.add(newLine);
                    newLine = new ArrayList<>();
                    newLine.add(currentLine.get(j));
                } else {
                    newLine.add(currentLine.get(j));
                }
            }
            if (newLine.size() > 0) {
                newLines.add(newLine);
            }
        }
        logger.trace("New Lines after splitting are " + newLines);
        return newLines;
    }

    public static final ArrayList<ArrayList<Rectangle>> dropLinesWithInsufficientBoxes(ArrayList<ArrayList<Rectangle>> lines, int minimumRequiredBoxesPerLine) {

        // drop boxes with insufficient number of boxes
        ArrayList<ArrayList<Rectangle>> newLines = new ArrayList<>();
        TreeSet<Integer> linesMarkedForDeletion = new TreeSet<>();
        for (int i = 0; i < lines.size(); ++i) {
            ArrayList<Rectangle> currentLine = lines.get(i);
            int wordsInLine = currentLine.size();
            if (wordsInLine < minimumRequiredBoxesPerLine) {
                linesMarkedForDeletion.add(i);
            }
        }
        for (int i = 0; i < lines.size(); ++i) {
            if (!linesMarkedForDeletion.contains(i)) {
                newLines.add(lines.get(i));
            }
        }
        return newLines;
    }

}
