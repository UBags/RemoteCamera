package com.costheta.electronicboards;

import com.costheta.image.utils.ImageUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;

public class ElectronicBoardUtils extends ImageUtils {

    private static Logger logger = LogManager.getLogger(ElectronicBoardUtils.class);

    public static final BufferedImage replaceGreensWithWhite (BufferedImage input) {
        // greens are generally found by ensuring that
        // abs(G-R) is a large number and B <= G + 20
        int whitePixel = (255 << 16) | (255 << 8) | 255;
        int alternateWhite = 0xFFFFFFFF;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input1);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if ((pixel == whitePixel) || (pixel == alternateWhite)){
                    continue;
                }
                if (isGreen(pixel)) {
                    output.setRGB(x,y,alternateWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage replaceLightColouredWithWhite (BufferedImage input) {
        int whitePixel = (255 << 16) | (255 << 8) | 255;
        int alternateWhite = 0xFFFFFFFF;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input1);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if ((pixel == whitePixel) || (pixel == alternateWhite)){
                    continue;
                }
                if (isLightColoured(pixel)) {
                    output.setRGB(x,y,alternateWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage replaceCopperColoursWithWhite (BufferedImage input) {
        int whitePixel = (255 << 16) | (255 << 8) | 255;
        int alternateWhite = 0xFFFFFFFF;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input1);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if ((pixel == whitePixel) || (pixel == alternateWhite)){
                    continue;
                }
                if (isCopperColoured(pixel)) {
                    output.setRGB(x,y,alternateWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage replaceNearWhitesWithWhite (BufferedImage input) {
        int whitePixel = (255 << 16) | (255 << 8) | 255;
        int alternateWhite = 0xFFFFFFFF;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input1);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if ((pixel == whitePixel) || (pixel == alternateWhite)){
                    continue;
                }
                if (isNearWhite(pixel)) {
                    output.setRGB(x,y,alternateWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage replaceNearGraysWithWhite (BufferedImage input) {
        int whitePixel = (255 << 16) | (255 << 8) | 255;
        int alternateWhite = 0xFFFFFFFF;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if ((pixel == whitePixel) || (pixel == alternateWhite)){
                    continue;
                }
                // logger.trace("[" + x + "," + y + "] = ");
                if (isNearGray(pixel)) {
                    output.setRGB(x,y,alternateWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage enhanceWhitesSuppressOthers(BufferedImage input) {
        int whitePixel = 0xFFFFFFFF;
        int blackPixel = 0xFF000000;
        BufferedImage input1 = rectifyBI(input);
        BufferedImage output = copy(input);
        for (int y = 0; y < input1.getHeight(); ++y) {
            for (int x = 0; x < input1.getWidth(); ++x) {
                int pixel = input1.getRGB(x,y);
                if (isAlmostWhite(pixel)) {
                    output.setRGB(x,y,whitePixel);
                } else {
                    output.setRGB(x,y,blackPixel);
                }
            }
        }
        return output;
    }

    public static final BufferedImage enhanceMaxMinus10Percent(BufferedImage input, int widthDivisions, int heightDivisions) {
        return enhanceMaxMinusXPercent(input, 10, widthDivisions, heightDivisions);
    }

    public static final BufferedImage enhanceMaxMinusXPercent(BufferedImage input, int X, int widthDivisions, int heightDivisions) {
        // in a given kernel, find the max red, max green, max blue
        // then, mark those cells as black that are > (maxRed * (1 - X)) && > (maxGreen * (1 - X))
        // and > (maxBlue * (1 - X))
        int whitePixel = 0xFFFFFFFF;
        int blackPixel = 0xFF000000;
        BufferedImage input1 = rectifyBI(input);
        int heightDivision = input1.getHeight() / heightDivisions;
        int widthDivision = input1.getWidth() / widthDivisions;
        double factor = (1 - X*1.0/100);
        BufferedImage output = new BufferedImage(input1.getWidth(), input1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < heightDivisions; ++j) {
            for (int i = 0; i < widthDivisions; ++i) {
                int maxRed = 0;
                int maxGreen = 0;
                int maxBlue = 0;
                for (int y = j * heightDivision; y < Math.min((j + 1) * heightDivision, input1.getHeight()); ++y) {
                    for (int x = i * widthDivision; x < Math.min((i + 1) * widthDivision, input1.getWidth()); ++x) {
                        int pixel = input1.getRGB(x,y);
                        maxRed = Math.max((pixel >> 16) & 0xFF, maxRed);
                        maxGreen = Math.max((pixel >> 8) & 0xFF, maxGreen);
                        maxBlue = Math.max(pixel & 0xFF, maxBlue);
                    }
                }
                for (int y = j * heightDivision; y < Math.min((j + 1) * heightDivision, input1.getHeight()); ++y) {
                    for (int x = i * widthDivision; x < Math.min((i + 1) * widthDivision, input1.getWidth()); ++x) {
                        int pixel = input1.getRGB(x,y);
                        int red = (pixel >> 16) & 0xFF;
                        int green = (pixel >> 8) & 0xFF;
                        int blue = pixel & 0xFF;
                        if ((red >= factor * maxRed) && (green >= factor * maxGreen) && (blue >= maxBlue * factor)) {
                            output.setRGB(x, y, blackPixel);
                        } else {
                            output.setRGB(x, y, whitePixel);
                        }
                    }
                }
            }
        }
        return output;
    }

    public static final boolean isGreen(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return (blue <= (green + 20)) && ((green - red) >= 30);
    }

    public static final boolean isLightColoured(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        boolean isLight = (red > 200) && (green > 172) && (blue > 172);
        isLight = isLight || ((red > 224) && (green > 160) && (blue > 160));
        isLight = isLight || ((red > 160) && (red > 144) && (blue > 144));
        return isLight;
    }

    public static final boolean isCopperColoured(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return ((red > 160) && (green > 128) && (blue > 128) && (green < 216) && (blue < 216));
    }

    public static final boolean isNearWhite(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        boolean nearWhite = ((red > 208) && (green > 208) && (blue > 208));
        // logger.trace("[" + red + "," + green + "," + blue + "] = " + nearWhite);
        return nearWhite;
    }

    public static final boolean isNearGray(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        int max = Math.max(Math.max(red, green), blue);
        int min = Math.min(Math.min(red, green), blue);
        boolean nearGray = (max > 144) && (((max - min) <= 12) ||
                (((max - min) <= 50) && (red < blue + 10) && (red < green + 10)));
        nearGray = nearGray || ((red < green - 10) & (red < blue - 10));
        // logger.trace("[" + red + "," + green + "," + blue + "] = " + nearGray);
        return nearGray;
    }

    public static final boolean isAlmostWhite(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        return ((red > 216) && (green > 216) && (blue > 216));
    }

}
