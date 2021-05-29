package com.costheta.image.thresholds;

import com.costheta.image.utils.ImageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;

public class OtsuThreshold {

    private static final Logger logger = LogManager.getLogger(OtsuThreshold.class);
    private static final int DEFAULT_NUM_BINS = 256;

    public static int getOtsuThreshold(BufferedImage image) {
        int[] histogram = makeHistogram(image);
        int threshold = computeThresholdFromHistogramStandard(histogram);
        return threshold;
    }

    public static BufferedImage getOtsuImage(BufferedImage image) {
        int[] histogram = makeHistogram(image);
        int threshold = computeThresholdFromHistogramStandard(histogram);
        return ImageUtils.binarize(image, threshold);
    }

    public static BufferedImage getLocalOtsuImageExperimental(BufferedImage image) {
        int[][] grayValues = new int[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                grayValues[y][x] = image.getRGB(x,y) & 0xFF;
            }
        }
        int xDivisionLength = image.getWidth() / 2;
        int yDivisionLength = image.getHeight() / 3;

        for (int y = 0; y < image.getHeight(); y = y + yDivisionLength) {
            for (int x = 0; x < image.getWidth(); x = x + xDivisionLength) {
                int yStart = y;
                int yEnd = Math.min(image.getHeight(), y + yDivisionLength);
                int xStart = x;
                int xEnd = Math.min(image.getWidth(), x + xDivisionLength);
                int[] histogram = new int[256];
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        histogram[grayValues[yCoord][xCoord]]++;
                    }
                }
                int threshold = computeThresholdFromHistogramExperimental(histogram);
                binarizeArrayExperimental(grayValues, xStart, yStart, xDivisionLength, yDivisionLength, threshold);
            }
        }
        return ImageUtils.getImage(grayValues);
    }

    public static BufferedImage getLocalOtsuImageStandard(BufferedImage image, int xDivisionLength, int yDivisionLength) {
        int[][] grayValues = new int[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                grayValues[y][x] = image.getRGB(x,y) & 0xFF;
            }
        }

        for (int y = 0; y < image.getHeight(); y = y + yDivisionLength) {
            for (int x = 0; x < image.getWidth(); x = x + xDivisionLength) {
                int yStart = y;
                int yEnd = Math.min(image.getHeight(), y + yDivisionLength);
                int xStart = x;
                int xEnd = Math.min(image.getWidth(), x + xDivisionLength);
                int[] histogram = new int[256];
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        histogram[grayValues[yCoord][xCoord]]++;
                    }
                }
                int threshold = computeThresholdFromHistogramStandard(histogram);
                // System.out.println("Otsu Threshold returned is " + threshold);
                binarizeArrayStandard(grayValues, xStart, yStart, xDivisionLength, yDivisionLength, threshold);
            }
        }
        return ImageUtils.getImage(grayValues);
    }

    public static int[] makeHistogram(BufferedImage img) {
        return makeHistogram(img, 1);
    }

    public static int[] makeHistogram(BufferedImage img, int bucketSize) {

        int numBins = 255 / bucketSize + 1;
        final int[] histogram = new int[numBins];
        for (int y = 0; y < img.getHeight(); ++y) {
            for (int x = 0; x < img.getWidth(); ++x) {
                int pixel = img.getRGB(x,y) & 0xFF;
                histogram[pixel]++;
            }
        }
        return histogram;
    }

    public static int[] makeMinMaxHistogram(int[] histogram) {
        final int[] histData = new int[256];
        int min = -1;
        int max = histogram.length - 1;
        for (int i = histogram.length - 1; i >= 0 ; --i) {
            if (histogram[i] > 0) {
                max = i;
                break;
            }
        }
        for (int i = 0; i <= max; ++i) {
            if (min == -1) {
                if (histogram[i] > 0) {
                    min = i;
                    if (max == min) {
                        histData[128] = histogram[i];
                        return histData;
                    }
                    histData[0] = histData[0] + histogram[i];
                }
            } else {
                int scaledValue = (int)((i - min) * 255.0 / (max - min));
                histData[scaledValue] = histData[scaledValue] + histogram[i];
                // System.out.println("Converted pixel " + i + " to " + scaledValue + " between " + min + " and " + max);
            }
        }
        return histData;
    }

    /**
     * Estimate the threshold and inter-class variance for the given histogram,
     * after consolidating the histogram into groups of 4
     *
     */
    public static int computeThresholdFromHistogramStandard(int[] inputHistogram) {
        int min = 0;
        int max = 255;
        for (int i = 0; i < inputHistogram.length; ++i) {
            if (inputHistogram[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = inputHistogram.length - 1; i >=0; --i) {
            if (inputHistogram[i] > 0) {
                max = i;
                break;
            }
        }

        int[] histogram = makeMinMaxHistogram(inputHistogram);
        int total = 0;
        for (int i = 0; i < histogram.length; ++i) {
            total += histogram[i];
        }
        int binSize = 3;
        final int originalNumBins = histogram.length;
        final int numBins = ((originalNumBins % binSize) != 0) ? (originalNumBins / binSize) + 1
                : (originalNumBins / binSize);

        int[] histData = new int[numBins];
        if ((originalNumBins % binSize) != 0) {
            for (int i = 0; i < (numBins - 1); ++i) {
                int binTotal = 0;
                for (int j = 0; j < binSize; ++j) {
                    binTotal += histogram[(binSize * i) + j];
                }
                histData[i] = binTotal;
            }
            int remainingBars = originalNumBins % binSize;
            int lastBarCount = 0;
            for (int i = 0; i <= remainingBars; ++i) {
                lastBarCount += histogram[originalNumBins - 1 - i];
            }
            histData[numBins - 1] = lastBarCount;
        } else {
            for (int i = 0; i < numBins; i++) {
                int binTotal = 0;
                for (int j = 0; j < binSize; ++j) {
                    binTotal += histogram[(binSize * i) + j];
                }
                histData[i] = binTotal;
            }
        }

        double sum = 0;
        // Weighted sum of all the pixels
        for (int t = 0; t < numBins; t++) {
            sum += t * histData[t];
        }

        double sumB = 0;
        int wB = 0;
        int wF = 0;

        double varMax = 0;
        int threshold = 0;

        for (int t = 0; t < numBins; t++) {

            wB += histData[t]; // Weight Background
            if (wB == 0) {
                continue;
            }

            wF = total - wB; // Weight Foreground
            if (wF == 0) {
                break;
            }

            sumB += (t * histData[t]);
            final double mB = sumB / wB; // Mean Background
            final double mF = (sum - sumB) / wF; // Mean Foreground
            // Calculate Between Class Variance
            final double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);
            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        int currentOtsuThreshold = (binSize * threshold) + (binSize / 2);
        // System.out.println("Calculated minmax Otsu as " + currentOtsuThreshold);
        currentOtsuThreshold = min + (int) ((currentOtsuThreshold / 255.0) * (max-min));
        // System.out.print("Calculated original Otsu as " + currentOtsuThreshold + " where min = " + min + ", max = " + max + "   ;");

        // find %age of total pixels falling under the ambit of the currentOtsuThreshold.
        // If > 50%, then return OtsuThreshold as min / 2, so that all pixels are whitened
        // If between 30% to 50%, return -1, which is an indication to use relativeGammaWithPercentile(30%)
        // If between 0% - 30%, check what percentile of min-to-max it lies in. If in the first half, then return the value, else nudge downwards by 10%

        int subTotal = 0;
        for (int i = min; i <= currentOtsuThreshold ; ++i) {
            subTotal += inputHistogram[i];
        }
        double cutoff1 = 0.6;
        double actualPercentOfPixelsCovered = (subTotal * 1.0 / total);
        if (actualPercentOfPixelsCovered >= cutoff1) {
            // if more than 60% of the pixels in scope are covered by Otsu then it is no good
            // System.out.println("Actual % of pixels covered = " + actualPercentOfPixelsCovered + "; Otsu Threshold = " + currentOtsuThreshold + "; sub-total = " + subTotal + "; total = " + total);
            return min / 2;
        }
        double intervalPercentile = (currentOtsuThreshold - min) * 1.0 / (max - min);

        if (intervalPercentile < 0.1) {
            return (currentOtsuThreshold + (int) (0.15 * (max-min)));
        }

        if (intervalPercentile < 0.15) {
            return (currentOtsuThreshold + (int) (0.075 * (max-min)));
        }

        if (intervalPercentile < 0.3) {
            return currentOtsuThreshold;
        }

        // else, push it back by 5%

        // if a valid Otsu is there, decrease the threshold by 5% of (max-min)
        currentOtsuThreshold = currentOtsuThreshold - (int) (0.05 *(max-min));
        // System.out.println("Otsu Threshold returned is " + currentOtsuThreshold);
        return currentOtsuThreshold;
    }

    /**
     * Estimate the threshold and inter-class variance for the given histogram,
     * after consolidating the histogram into groups of 4
     *
     */
    public static int computeThresholdFromHistogramExperimental(int[] inputHistogram) {
        int min = 0;
        int max = 255;
        for (int i = 0; i < inputHistogram.length; ++i) {
            if (inputHistogram[i] > 0) {
                min = i;
                break;
            }
        }
        for (int i = inputHistogram.length - 1; i >=0; --i) {
            if (inputHistogram[i] > 0) {
                max = i;
                break;
            }
        }

        int[] histogram = makeMinMaxHistogram(inputHistogram);
        int total = 0;
        for (int i = 0; i < histogram.length; ++i) {
            total += histogram[i];
        }
        int binSize = 3;
        final int originalNumBins = histogram.length;
        final int numBins = ((originalNumBins % binSize) != 0) ? (originalNumBins / binSize) + 1
                : (originalNumBins / binSize);

        int[] histData = new int[numBins];
        if ((originalNumBins % binSize) != 0) {
            for (int i = 0; i < (numBins - 1); ++i) {
                int binTotal = 0;
                for (int j = 0; j < binSize; ++j) {
                    binTotal += histogram[(binSize * i) + j];
                }
                histData[i] = binTotal;
            }
            int remainingBars = originalNumBins % binSize;
            int lastBarCount = 0;
            for (int i = 0; i <= remainingBars; ++i) {
                lastBarCount += histogram[originalNumBins - 1 - i];
            }
            histData[numBins - 1] = lastBarCount;
        } else {
            for (int i = 0; i < numBins; i++) {
                int binTotal = 0;
                for (int j = 0; j < binSize; ++j) {
                    binTotal += histogram[(binSize * i) + j];
                }
                histData[i] = binTotal;
            }
        }

        double sum = 0;
        // Weighted sum of all the pixels
        for (int t = 0; t < numBins; t++) {
            sum += t * histData[t];
        }

        double sumB = 0;
        int wB = 0;
        int wF = 0;

        double varMax = 0;
        int threshold = 0;

        for (int t = 0; t < numBins; t++) {

            wB += histData[t]; // Weight Background
            if (wB == 0) {
                continue;
            }

            wF = total - wB; // Weight Foreground
            if (wF == 0) {
                break;
            }

            sumB += (t * histData[t]);
            final double mB = sumB / wB; // Mean Background
            final double mF = (sum - sumB) / wF; // Mean Foreground
            // Calculate Between Class Variance
            final double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);
            // Check if new maximum found
            if (varBetween > varMax) {
                varMax = varBetween;
                threshold = t;
            }
        }

        int currentOtsuThreshold = (binSize * threshold) + (binSize / 2);
        // System.out.println("Calculated minmax Otsu as " + currentOtsuThreshold);
        currentOtsuThreshold = min + (int) ((currentOtsuThreshold / 255.0) * (max-min));
        // System.out.println("Calculated original Otsu as " + currentOtsuThreshold + " where min = " + min + ", max = " + max);

        // find %age of total pixels falling under the ambit of the currentOtsuThreshold.
        // If > 50%, then return OtsuThreshold as min / 2, so that all pixels are whitened
        // If between 30% to 50%, return -1, which is an indication to use relativeGammaWithPercentile(30%)
        // If between 0% - 30%, check what percentile of min-to-max it lies in. If in the first half, then return the value, else nudge downwards by 10%

        int subTotal = 0;
        for (int i = min; i <= currentOtsuThreshold ; ++i) {
            subTotal += inputHistogram[i];
        }
        double cutoff1 = 0.45;
        if ((subTotal * 1.0 / total) >= cutoff1) {
            // if more than 50% of the pixels in scope are covered by Otsu then it is no good
            return min / 2;
        }
        double cutoff2 = 0.35;
        if (((subTotal * 1.0 / total) >= cutoff2) && ((subTotal * 1.0 / total) < cutoff1)) {
            // if 30-50% of the pixels in scope are covered by Otsu then it is no good
            // -1 is an indication that we should use relativeGammaEnhancement
            return -1;
        }
        double intervalPercentile = (currentOtsuThreshold - min) * 1.0 / (max - min);
        if (intervalPercentile < 0.35) {
            return currentOtsuThreshold;
        }

        // else, push it back by 5%

        // if a valid Otsu is there, decrease the threshold by 5% of (max-min)
        currentOtsuThreshold = currentOtsuThreshold - (int) (0.05 *(max-min));
        // System.out.println("Otsu Threshold returned is " + currentOtsuThreshold);
        return currentOtsuThreshold;
    }

    public static int[][] binarizeArrayStandard(int[][] grayValues, int xStart, int yStart, int width, int height, int threshold) {
        int xEnd = Math.min(xStart + width, grayValues[0].length);
        int yEnd = Math.min(yStart + height, grayValues.length);
        for (int x = xStart; x < xEnd; ++x) {
            for (int y = yStart; y < yEnd; ++y) {
                if (grayValues[y][x] <= threshold) {
                    grayValues[y][x] = 0;
                } else {
                    grayValues[y][x] = 255;
                }
            }
        }
        return grayValues;
    }

    public static int[][] binarizeArrayExperimental(int[][] grayValues, int xStart, int yStart, int width, int height, int threshold) {
        if (threshold != -1) {
            int xEnd = Math.min(xStart + width, grayValues[0].length);
            int yEnd = Math.min(yStart + height, grayValues.length);
            for (int x = xStart; x < xEnd; ++x) {
                for (int y = yStart; y < yEnd; ++y) {
                    if (grayValues[y][x] <= threshold) {
                        grayValues[y][x] = 0;
                    } else {
                        grayValues[y][x] = 255;
                    }
                }
            }
        } else {
            // grayValues = ImageUtils.relativeGammaEnhancementWithMatrix(grayValues, xStart, yStart, width, height, (int) (width * 2.0 / 3), (int) (height * 2.0 / 3),
            // 30);
            grayValues = ImageUtils.relativeBinarizationMatrix(grayValues, xStart, yStart, width, height, (int) (width * 2.0 / 3), (int) (height * 2.0 / 3),
                    30);
        }
        return grayValues;
    }


}
