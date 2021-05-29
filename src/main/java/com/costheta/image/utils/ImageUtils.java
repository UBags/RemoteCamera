package com.costheta.image.utils;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import com.costheta.image.TraceLevel;
import com.costheta.image.filters.ConvolveFilter;
import com.costheta.image.filters.GaussianFilter;
import com.costheta.image.filters.UnsharpFilter;
import com.costheta.machine.BaseImageProcessor;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.shape.Rectangle;

import static net.sourceforge.lept4j.ILeptonica.IFF_TIFF;

public class ImageUtils {

    private static final Logger logger = LogManager.getLogger(ImageUtils.class);

    private static ImageUtils imageUtils;
    public static final String BLACK = "black";
    public static final String WHITE = "white";
    public static final BufferedImage EMPTY_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);

    public static final ConvolveFilter sobelVerticalFilter = new ConvolveFilter(3, 3,
            new float[] { -1f, 0f, 1f, -2f, 0f, 2f, -1f, 0f, 1f });
    public static final ConvolveFilter sobelHorizontalFilter = new ConvolveFilter(3, 3,
            new float[] { -1f, -2f, -1f, 0f, 0f, 0f, 1f, 2f, 1f });
    public static final GaussianFilter gaussianFilter = new GaussianFilter(3);
    public static final UnsharpFilter unsharpFilter = new UnsharpFilter();

    protected ImageUtils() {

    }

    public static final void initialise() {
        imageUtils = new ImageUtils();
    }

    private static final HashMap<Integer, double[]> gammaMultipliersMap = createGammaMultipliersMap(7);
    private static final double[] gammaCubedTable = new double[2000];
    private static final double[] gammaSquaredTable = new double[2000];
    private static final double[] gammaRootTable = new double[2000];
    private static final double[][] distanceTable = new double[1001][1001];
    private static final int halfYDistanceTable = distanceTable.length / 2;
    private static final int halfXDistanceTable = distanceTable[0].length / 2;
    private static final double[][] sigmoidFactorsTable = new double[101][766];

    private static final Color GREEN = new Color(35, 175, 75);
    private static final Color AMBER = new Color(255, 200, 20);
    private static final String EMPTY_STRING = "";

    public static final int VERTICAL_DIRECTION = 1;
    public static final int HORIZONTAL_DIRECTION = 2;
    public static final int BOTH_DIRECTION = 3;

    public static final double OPTIMAL_HEIGHT_WIDTH_RATIO_FOR_CHARACTERS = 1.6;

    public static final int DILATE = -1; // introduce whites; thin the blacks
    public static final int LEAVE_UNCHANGED = 0;
    public static final int ERODE = 1; // introduce blacks; thicken the blacks

    public static final int GAMMA_ENHANCEMENT_NORMAL = 1;
    public static final int GAMMA_ENHANCEMENT_HIGH = 2;
    public static final int GAMMA_ENHANCEMENT_EXTREME = 3;
    public static final int GAMMA_ENHANCEMENT_BELOW = 4;
    public static final int GAMMA_ENHANCEMENT_ABOVE = 5;
    public static final int GAMMA_ENHANCEMENT_VARIABLE = 6;

    public static final double[] fourthPower = new double[256];
    public static final double[] cubed = new double[256];
    public static final double[] squared = new double[256];
    public static final double[] same = new double[256];
    public static final double[] zeroth = new double[256];
    public static final double[] inverse = new double[256];
    public static final double[] inverseSquared = new double[256];
    public static final double[] inverseCubed = new double[256];
    public static final double[] inverseFourth = new double[256];

    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST = 3;
    public static final int WEST = 4;

    public static final int[] ARGBPixels = new int[256];
    private static boolean argbArrayPopulated = false;

    private static Pix emptyPix;

    static {
        populateGammaCubedTable();
        populateGammaRootTable();
        populateGammaSquaredTable();
        populateDistanceTable();
        populateSigmoidFactorsTable();
        populateInverses();
        populateARGBPixels();
        unsharpFilter.setRadius(3);
        makeEmptyPix();
    }

    private static void makeEmptyPix() {
        emptyPix = Leptonica1.pixCreate(20,20, 1);
        Leptonica1.pixSetBlackOrWhite(emptyPix, ILeptonica.L_SET_WHITE);
    }

    private static final void populateARGBPixels() {
        if (argbArrayPopulated) {
            return;
        }
        for (int x = 0; x < 256; ++x) {
            ARGBPixels[x] = 0xFF000000 | (x << 16) | (x << 8) | x;
        }
        argbArrayPopulated = true;
    }

    private static final void populateInverses() {
        for (int x = 0; x < 256; ++x) {
            if (x == 0) {
                zeroth[x] = 1;
                inverse[x] = 1;
                inverseSquared[x] = 1;
                inverseCubed[x] = 1;
                inverseFourth[x] = 1;
            } else {
                double value = (1.0 / x);
                zeroth[x] = 1.0;
                inverse[x] = value;
                inverseSquared[x] = value * value;
                inverseCubed[x] = value * value * value;
                inverseFourth[x] = value * value * value * value;
            }
            same[x] = x;
            squared[x] = x * x;
            cubed[x] = x * x * x;
            fourthPower[x] = x * x * x * x;
        }
    }

    private static final void populateGammaCubedTable() {
        for (int x = 0; x < 2000; ++x) {
            double gamma = x * 1.0 / 1000;
            gammaCubedTable[x] = gamma * gamma * gamma;
        }
    }

    private static final void populateGammaRootTable() {
        for (int x = 0; x < 2000; ++x) {
            double gamma = x * 1.0 / 1000;
            gammaRootTable[x] = Math.sqrt(gamma);
        }
    }

    private static final void populateGammaSquaredTable() {
        for (int x = 0; x < 2000; ++x) {
            double gamma = x * 1.0 / 1000;
            gammaSquaredTable[x] = gamma * gamma;
        }
    }

    private static final void populateDistanceTable() {
        for (int y = -distanceTable.length / 2; y <= distanceTable.length / 2; ++y) {
            int yDist = y + distanceTable.length / 2;
            for (int x = -distanceTable[0].length / 2; x <= distanceTable[0].length / 2; ++x) {
                int xDist = x + distanceTable[0].length / 2;
                double distance = Math.sqrt(xDist * xDist + yDist * yDist);
                distanceTable[yDist][xDist] = distance;
            }
        }
    }

    private static final void populateSigmoidFactorsTable() {
        for (int y = 0; y < 101; ++y) {
            for (int x = -510; x <= 255; ++x) {
                sigmoidFactorsTable[y][x + 510] = 1 / (1 + Math.exp(-y * x * 1.0 / 10));
            }
        }
    }

    private static double[] getGammaMultipliers(double gamma) {
        double[] gammaMultipliers = new double[256];
        for (int i = 0; i < 256; ++i) {
            gammaMultipliers[i] = Math.pow((double) i / (double) 255, 1.0 / gamma);
        }
        return gammaMultipliers;
    }

    private static final HashMap<Integer, double[]> createGammaMultipliersMap(int maxGamma) {
        HashMap<Integer, double[]> gammaMultipliersMap = new HashMap<>();
        for (double d = 0.0; d <= maxGamma; d = d + 0.05) {
            int key = (int) Math.round(d / 0.05);
            double[] gammaMultipliers = getGammaMultipliers(d);
            gammaMultipliersMap.put(Integer.valueOf(key), gammaMultipliers);
            // System.out.println("Created gamma multipliers for " + d);
        }
        // System.out.println("Created all the gamma multipliers");
        return gammaMultipliersMap;
    }


    private static final BufferedImage FAULTY_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);

    public static final BufferedImage rectifyBI(BufferedImage input) {
        if (input.getType() == BufferedImage.TYPE_INT_ARGB) {
            return input;
        }
        return copyBI(input);
    }

    public static final BufferedImage rectifyBI(BufferedImage input, int kernelX, int kernelY) {
        return copyBI(input, kernelX, kernelY);
    }

    public static final BufferedImage morphGrayMinMax(BufferedImage input1, int kernelX, int kernelY, boolean erode) {

        kernelX = kernelX % 2 != 0 ? kernelX : kernelX + 1;
        kernelY = kernelY % 2 != 0 ? kernelY : kernelY + 1;
        if ((kernelX <= 0) || (kernelY <= 0)) {
            return input1;
        }
        if ((kernelX <= 1) && (kernelY <= 1)) {
            return input1;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        int[][] pixels = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y) & 0xFF;
                pixels[y][x] = pixel;
            }
        }
        BufferedImage output = copyBI(input);
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int newPixel = erode ? 255 : 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = pixels[y + deltaY][x + deltaX];
                        newPixel = erode ? Math.min(pixel, newPixel) : Math.max(pixel, newPixel);
                    }
                }
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage erodeGrayBI(BufferedImage input, int kernelX, int kernelY) {
        return morphGrayMinMax(input, kernelX, kernelY, true);
    }

    public static final BufferedImage dilateGrayBI(BufferedImage input, int kernelX, int kernelY) {
        return morphGrayMinMax(input, kernelX, kernelY, false);
    }

    public static final BufferedImage addPixelLayer(BufferedImage image, int layerSize, int cutOff, boolean addWhite) {
        // adds layers of pixels north of current pixels
        final int blackPixel = 0xFF000000;

        image = rectifyBI(image);
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        for (int y = layerSize; y < image.getHeight(); ++y) {
            for (int x = 0; x < image.getWidth(); ++x) {
                int pixel = image.getRGB(x, y) & 0xFF;
                if ((pixel < cutOff) && !addWhite) {
                    // ! addWhite == addBlack
                    int newPixel = ARGBPixels[pixel];
                    for (int deltaY = -layerSize; deltaY < 0; ++deltaY) {
                        newImage.setRGB(x, y + deltaY, newPixel);
                    }
                } else {
                    if ((pixel >= cutOff) && (addWhite)) {
                        int newPixel = ARGBPixels[pixel];
                        for (int deltaY = -layerSize; deltaY < 0; ++deltaY) {
                            newImage.setRGB(x, y + deltaY, newPixel);
                        }
                    }
                }
            }
        }
        return newImage;
    }

    public static final BufferedImage addPixelLayer(BufferedImage image, int layerSize, int direction, int cutOff, boolean addWhite) {
        // adds layers of pixels north of current pixels
        final int blackPixel = 0xFF000000;

        image = rectifyBI(image);
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        if (direction == NORTH) {
            for (int y = layerSize; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int pixel = image.getRGB(x, y) & 0xFF;
                    if ((pixel < cutOff) && !addWhite) {
                        // ! addWhite == addBlack
                        int newPixel = ARGBPixels[pixel];
                        for (int deltaY = -layerSize; deltaY < 0; ++deltaY) {
                            newImage.setRGB(x, y + deltaY, newPixel);
                        }
                    } else {
                        if ((pixel >= cutOff) && (addWhite)) {
                            int newPixel = ARGBPixels[pixel];
                            for (int deltaY = -layerSize; deltaY < 0; ++deltaY) {
                                newImage.setRGB(x, y + deltaY, newPixel);
                            }
                        }
                    }
                }
            }
        }
        if (direction == SOUTH) {
            for (int y = 0; y < image.getHeight() - layerSize; ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int pixel = image.getRGB(x, y) & 0xFF;
                    if ((pixel < cutOff) && !addWhite) {
                        // ! addWhite == addBlack
                        int newPixel = ARGBPixels[pixel];
                        for (int deltaY = 0; deltaY < layerSize; ++deltaY) {
                            newImage.setRGB(x, y + deltaY, newPixel);
                        }
                    } else {
                        if ((pixel >= cutOff) && (addWhite)) {
                            int newPixel = ARGBPixels[pixel];
                            for (int deltaY = 0; deltaY < layerSize; ++deltaY) {
                                newImage.setRGB(x, y + deltaY, newPixel);
                            }
                        }
                    }
                }
            }
        }
        if (direction == EAST) {
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = 0; x < image.getWidth() - layerSize; ++x) {
                    int pixel = image.getRGB(x, y) & 0xFF;
                    if ((pixel < cutOff) && !addWhite) {
                        // ! addWhite == addBlack
                        int newPixel = ARGBPixels[pixel];
                        for (int deltaX = 0; deltaX < layerSize; ++deltaX) {
                            newImage.setRGB(x + deltaX, y, newPixel);
                        }
                    } else {
                        if ((pixel >= cutOff) && (addWhite)) {
                            int newPixel = ARGBPixels[pixel];
                            for (int deltaX = 0; deltaX < layerSize; ++deltaX) {
                                newImage.setRGB(x + deltaX, y, newPixel);
                            }
                        }
                    }
                }
            }
        }
        if (direction == WEST) {
            for (int y = 0; y < image.getHeight(); ++y) {
                for (int x = layerSize; x < image.getWidth(); ++x) {
                    int pixel = image.getRGB(x, y) & 0xFF;
                    if ((pixel < cutOff) && !addWhite) {
                        // ! addWhite == addBlack
                        int newPixel = ARGBPixels[pixel];
                        for (int deltaX = -layerSize; deltaX < 0; ++deltaX) {
                            newImage.setRGB(x + deltaX, y, newPixel);
                        }
                    } else {
                        if ((pixel >= cutOff) && (addWhite)) {
                            int newPixel = ARGBPixels[pixel];
                            for (int deltaX = -layerSize; deltaX < 0; ++deltaX) {
                                newImage.setRGB(x + deltaX, y, newPixel);
                            }
                        }
                    }
                }
            }
        }

        return newImage;
    }

    public static final BufferedImage[] addPixelLayer(BufferedImage[] images, int layerSize, int cutOff, boolean addWhite) {
        BufferedImage[] newImages = new BufferedImage[images.length];
        for (int i = 0; i < newImages.length; ++i) {
            newImages[i] = addPixelLayer(images[i], layerSize, cutOff, addWhite);
        }
        return newImages;
    }

    public static final Pix addPixelLayer(Pix pix, int layerSize, int cutOff, boolean addWhite) {
        final int blackPixel = 0xFF000000;

        // first, just copy the image as is
        BufferedImage image = convertPixToImage(pix);

        BufferedImage newImage = addPixelLayer(image, layerSize, cutOff, addWhite);
        Pix newPix = convertImageToPix(newImage);
        Pix newPix8 = Leptonica.INSTANCE.pixConvertTo8(newPix, 0);
        Pix newPix1 = Leptonica.INSTANCE.pixConvertTo1(newPix8, 192);
        LeptUtils.dispose(newPix);
        LeptUtils.dispose(newPix8);
        return newPix1;
    }

    public static final BufferedImage convertPixToImage(Pix pix) {
        return convertPixToImage(pix, false, 1000);
    }


    /**
     * Converts Leptonica <code>Pix</code> to <code>BufferedImage</code>.
     *
     * @param pix source pix
     * @return BufferedImage output image
     * @throws IOException
     */
    public static final BufferedImage convertPixToImage(Pix pix, boolean debug, int debugLevel) {
        PointerByReference pdata = new PointerByReference();
        NativeSizeByReference psize = new NativeSizeByReference();
        int format = IFF_TIFF;
        Leptonica1.pixWriteMem(pdata, psize, pix, format);
        if (debug && (debugLevel <= 2)) {
            System.out.println("pix = " + pix);
            System.out.println("pix size = " + pix.size());
            System.out.println("pdata in PixToImage = " + pdata);
            System.out.println("psize in PixToImage = " + psize);
            System.out.println("psize.getValue() in PixToImage = " + psize.getValue());
            System.out.println("psize.getValue().intValue() in PixToImage = " + psize.getValue().intValue());
        }
        byte[] b = pdata.getValue().getByteArray(0, psize.getValue().intValue());
        InputStream in = new ByteArrayInputStream(b);
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(in);
            in.close();
        } catch (Exception e) {
            bi = FAULTY_IMAGE;
        }

        Leptonica1.lept_free(pdata.getValue());
        // System.out.println("Done converting Pix to BI");
        return rectifyBI(bi);
    }

    public static final Pix convertImageToPix(BufferedImage image) {
        return getPixFromBufferedImage(image);
    }


    public static final Pix convertImageToPix(BufferedImage image, boolean debug, int debugLevel) {
        return getPixFromBufferedImage(image);
    }

    public static final Pix getPixFromBufferedImage(BufferedImage image) {
        ByteBuffer buf = null;
        try {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
            ImageWriter writer = null;
            boolean twelveMonkeysWriterFound = false;
            while (writers.hasNext()) {
                writer = writers.next();
                if (writer.getClass().toString().indexOf("twelvemonkeys") != -1) {
                    twelveMonkeysWriterFound = true;
                    break;
                }
            }

            if (!twelveMonkeysWriterFound) {
                // throw new RuntimeException(
                // "Need to install JAI Image I/O
                // package.\nhttps://github.com/jai-imageio/jai-imageio-core");
                throw new RuntimeException("Need to install TwelveMonkeys Image I/O package");
            }

            // Set up the writeParam
            ImageWriteParam tiffWriteParam = writer.getDefaultWriteParam();
            tiffWriteParam.setCompressionMode(ImageWriteParam.MODE_DISABLED);

            // Get the stream metadata
            IIOMetadata streamMetadata = writer.getDefaultStreamMetadata(tiffWriteParam);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
            writer.setOutput(ios);

            // TIFFImageWriter tiw
            // ios.seek(0);
            // ios.mark();
            writer.write(streamMetadata, new IIOImage(image, null, null), tiffWriteParam);
            // writer.write(image);
            // writer.dispose();
            // ios.reset();
            // System.out.println("Stream Position = " + ios.getStreamPosition());
            // System.out.println("Flushed Position = " + ios.getFlushedPosition());
            byte[] b = null;
            try {
                ios.seek(0);
                b = new byte[(int) ios.length()];
                ios.read(b, 0, b.length);
            } catch (Exception e) {
                b = outputStream.toByteArray();
            }
            // System.out.println(Arrays.toString(b));
            ios.close();
            writer.dispose();

            buf = ByteBuffer.allocateDirect(b.length);
            buf.order(ByteOrder.nativeOrder());
            buf.put(b);
            ((Buffer) buf).flip();
        } catch(Exception e) {
            return null;
        }

        Pix pix = Leptonica1.pixReadMem(buf, new NativeSize(buf.capacity()));
        return pix;
    }

    public static final Pix rotatePix(Pix input, float angle, boolean introduceWhite) {

        BufferedImage inputBI = convertPixToImage(input);
        BufferedImage rotatedBI = rotateImage(inputBI, angle, introduceWhite);
        Pix rotatedPix = getDepth1Pix(rotatedBI, 128);
        return rotatedPix;
    }

    public static final BufferedImage rotateImage(BufferedImage originalBI, float angle, boolean introduceWhite) {

        final double rads1 = Math.toRadians(angle);
        final double sin1 = Math.abs(Math.sin(rads1));
        final double cos1 = Math.abs(Math.cos(rads1));
        final int w1 = (int) Math.floor((originalBI.getWidth() * cos1) + (originalBI.getHeight() * sin1));
        final int h1 = (int) Math.floor((originalBI.getHeight() * cos1) + (originalBI.getWidth() * sin1));
        BufferedImage rotatedBI = new BufferedImage(w1, h1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = rotatedBI.createGraphics();
        if (!introduceWhite) {
            g1.setColor(java.awt.Color.BLACK);
        } else {
            g1.setColor(java.awt.Color.WHITE);
        }
        g1.fillRect(0, 0, w1, h1);
        RenderingHints rh = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g1.setRenderingHints(rh);
        RenderingHints rh1 = new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g1.setRenderingHints(rh1);
        RenderingHints rh2 = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g1.setRenderingHints(rh2);
        final AffineTransform at1 = new AffineTransform();
        at1.rotate(rads1, w1 / 2.0, h1 / 2.0);
        at1.translate((w1 - originalBI.getWidth()) / 2, (h1 - originalBI.getHeight()) / 2);
        g1.drawImage(originalBI, at1, null);
        g1.dispose();

        return rotatedBI;

    }

    public static final Pix getGammaEnhancedPix(Pix inputPix, boolean debug, int debugLevel) {
        int[] clippedPixelData = get8BitPixelDataCopyForBI(inputPix, debug, debugLevel);
        float gammaEnhancement = findGammaEnhancement(clippedPixelData, debug, debugLevel);
        Pix gammaEnhancedPix = Leptonica1.pixGammaTRC(null, inputPix, gammaEnhancement, 0, 255);
        return gammaEnhancedPix;
    }

    public static final int[] getPixelDataCopy(Pix inputPix, boolean debug, int debugLevel) {
        IntByReference pixelDataRef = Leptonica.INSTANCE.pixGetData(inputPix);
        Pointer pixelDataPointer = pixelDataRef.getPointer();
        int[] pixelData = pixelDataPointer.getIntArray(0, inputPix.h * inputPix.w);

        int[] pixelDataCopy = new int[inputPix.h * inputPix.w];
        System.arraycopy(pixelData, 0, pixelDataCopy, 0, inputPix.h * inputPix.w);
        return pixelDataCopy;
    }

    public static final int[] get32BitPixelDataCopyForBI(Pix inputPix, boolean debug, int debugLevel) {
        IntByReference pixelDataRef = Leptonica.INSTANCE.pixGetData(inputPix);
        Pointer pixelDataPointer = pixelDataRef.getPointer();
        int[] pixelData = pixelDataPointer.getIntArray(0, inputPix.h * inputPix.w);
        int[] pixelDataCopy = new int[pixelData.length];

        // change bits from RGBA (Pix representation) to ARGB
        // (BufferedImage.TYPE_INT_ARGB)
        for (int i = 0; i < pixelDataCopy.length; ++i) {
            int red = (pixelData[i] >> 24) & 0xFF;
            int green = (pixelData[i] >> 16) & 0xFF;
            int blue = (pixelData[i] >> 8) & 0xFF;

            pixelDataCopy[i] = (0xFF << 24) | (red << 16) | (green << 8) | blue;
        }
        return pixelDataCopy;
    }

    public static final int[] get8BitPixelDataCopyForBI(Pix inputPix, boolean debug, int debugLevel) {
        // takes an input pix of depth 8
        if (inputPix == null) {
            int[] returnArray = new int[1];
            returnArray[0] = 0;
            return returnArray;
        }

        Pix tempPix = Leptonica.INSTANCE.pixConvertTo32(inputPix);
        IntByReference pixelDataRef = Leptonica.INSTANCE.pixGetData(tempPix);
        Pointer pixelDataPointer = pixelDataRef.getPointer();
        int[] pixelData = pixelDataPointer.getIntArray(0, inputPix.h * inputPix.w);
        int[] pixelDataCopy = new int[pixelData.length];

        for (int i = 0; i < pixelData.length; ++i) {
            pixelDataCopy[i] = (pixelData[i] >> 8) & 0xFF0000FF;
        }

//        if (debug && (debugLevel <= 1)) {
//            BufferedImage testImage = new BufferedImage(inputPix.w, inputPix.h, BufferedImage.TYPE_INT_ARGB);
//            int[] pixelRGB = new int[pixelDataCopy.length];
//            for (int i = 0; i < pixelDataCopy.length; ++i) {
//                pixelRGB[i] = 0xFF000000 | (pixelDataCopy[i] << 16) | (pixelDataCopy[i] << 8) | pixelDataCopy[i];
//                System.out.println("Original = " + String.format("0x%08X", pixelData[i]) + ": Original Copy = "
//                        + String.format("0x%08X", pixelDataCopy[i]) + ": RGB Pixel = "
//                        + String.format("0x%08X", pixelRGB[i]));
//                testImage.setRGB(i % inputPix.w, i / inputPix.w, pixelRGB[i]);
//            }
//        }
        LeptUtils.dispose(tempPix);
        return pixelDataCopy;
    }

    public static final int[] get8BitPixelDataCopyForBI(BufferedImage image8, boolean debug, int debugLevel) {
        // takes an input BI of depth 8
        if (image8.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            int[] returnArray = new int[1];
            returnArray[0] = 0;
            return returnArray;
        }

        byte[] pixels = ((java.awt.image.DataBufferByte) image8.getRaster().getDataBuffer()).getData();
        int[] pixelDataCopy = new int[pixels.length];

        for (int i = 0; i < pixels.length; ++i) {
            pixelDataCopy[i] = pixels[i] & 0xFF0000FF;
        }

        if (debug && (debugLevel <= 1)) {
            BufferedImage testImage = new BufferedImage(image8.getWidth(), image8.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            int[] pixelRGB = new int[pixelDataCopy.length];
            for (int i = 0; i < pixelDataCopy.length; ++i) {
                pixelRGB[i] = ARGBPixels[pixelDataCopy[i]];
                System.out.println("Original = " + String.format("0x%08X", pixels[i]) + ": Original Copy = "
                        + String.format("0x%08X", pixelDataCopy[i]) + ": RGB Pixel = "
                        + String.format("0x%08X", pixelRGB[i]));
                testImage.setRGB(i % image8.getWidth(), i / image8.getWidth(), pixelRGB[i]);
            }
        }
        return pixelDataCopy;
    }

    public static final Pix createPixCopyWithNewData(Pix inputPix, int[] pixelDataForBI, boolean debug, int debugLevel) {
        BufferedImage copy = new BufferedImage(inputPix.w, inputPix.h, BufferedImage.TYPE_INT_ARGB);
        int[] rgbRaster = ((DataBufferInt) copy.getRaster().getDataBuffer()).getData();
        System.arraycopy(pixelDataForBI, 0, rgbRaster, 0, inputPix.w * inputPix.h);
        copy.setRGB(0, 0, 0);
        Pix newPix = convertImageToPix(copy);
        // Leptonica.INSTANCE.pixFreeData(copy);

        // IntBuffer relevantData = IntBuffer.wrap(pixelData);
        // relevantData.position(0);
        // Leptonica.INSTANCE.pixSetData(copy, relevantData);
        // relevantData.compact();
        // relevantData.clear();
        // System.out.println("IntBuffer is " + (relevantData.isDirect() ? "Direct" :
        // "Indirect"));
        return newPix;
    }

    public static final Pix createWhitePixCopyWithNewData(Pix inputPix, int[] pixelDataForBI, boolean debug, int debugLevel) {
        BufferedImage copy = new BufferedImage(inputPix.w, inputPix.h, BufferedImage.TYPE_BYTE_GRAY);
        for (int j = 0; j < inputPix.h; ++j) {
            for (int i = 0; i < inputPix.w; ++i) {
                byte pixelValue = (byte) (pixelDataForBI[j * inputPix.w + i]);
                copy.setRGB(i, j, pixelValue);
            }
        }

        Pix newPix = convertImageToPix(copy);
        return newPix;
    }

    public static final float findGammaEnhancement(int[] pixelData, boolean debug, int debugLevel) {
        int totalValueOfPixels = 0;

        for (int j = 0; j < pixelData.length; ++j) {
            int pixel = pixelData[j] & 0xFF;
            totalValueOfPixels += pixel;
        }
        int averageOfPixels = (int) (totalValueOfPixels * 1.0f / (pixelData.length > 0 ? pixelData.length : 1));

        // System.out.println("Pixel average = " + averageOfPixels);

        if (averageOfPixels < 60) {
            return 4.0f;
        } else {
            if (averageOfPixels < 80) {
                return 3.75f;
            } else {
                if (averageOfPixels < 100) {
                    return 3.5f;
                } else {
                    if (averageOfPixels < 120) {
                        return 3.25f;
                    } else {
                        if (averageOfPixels < 140) {
                            return 3.00f;
                        } else {
                            if (averageOfPixels < 160) {
                                return 2.75f;
                            } else {
                                if (averageOfPixels < 180) {
                                    return 2.5f;
                                } else {
                                    if (averageOfPixels < 190) {
                                        return 2.25f;
                                    } else {
                                        if (averageOfPixels < 200) {
                                            return 2.0f;
                                        } else {
                                            if (averageOfPixels < 210) {
                                                return 1.75f;
                                            } else {
                                                if (averageOfPixels < 220) {
                                                    return 1.5f;
                                                } else {
                                                    if (averageOfPixels < 227) {
                                                        return 1.375f;
                                                    } else {
                                                        if (averageOfPixels < 235) {
                                                            return 1.25f;
                                                        } else {
                                                            if (averageOfPixels < 245) {
                                                                return 1.125f;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return 1.0f;
    }

    public static final BufferedImage copyBI(BufferedImage image, int kernelX, int kernelY) {
        BufferedImage clone = new BufferedImage(image.getWidth() + kernelX, image.getHeight() + kernelY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = clone.createGraphics();
        g2d.setPaint ( new Color ( 255, 255, 255 ) );
        g2d.fillRect ( 0, 0, clone.getWidth(), clone.getHeight() );
        g2d.drawImage(image, kernelX / 2, kernelY / 2, null);
        g2d.dispose();
        return clone;
    }

    public static final BufferedImage copyBI(BufferedImage image) {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clone;
    }

    public static final BufferedImage copy(BufferedImage image, int kernelX, int kernelY) {
        return copyBI(image, kernelX, kernelY);
    }

    public static final BufferedImage copy(BufferedImage image) {
        return copyBI(image);
    }

    public static final BufferedImage contrastNormalisation(BufferedImage input1, int kernelX, int kernelY,
                                                      int smoothingKernelX, int smoothingKernelY) {
        smoothingKernelX = Math.min(smoothingKernelX, 5);
        smoothingKernelY = Math.min(smoothingKernelY, 5);
        BufferedImage smoothed = blur(input1, smoothingKernelX, smoothingKernelY);
        kernelX = kernelX % 2 != 0 ? kernelX : kernelX + 1;
        kernelY = kernelY % 2 != 0 ? kernelY : kernelY + 1;
        if ((kernelX <= 0) || (kernelY <= 0)) {
            return smoothed;
        }
        if ((kernelX <= 1) && (kernelY <= 1)) {
            return smoothed;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
//        for (int y = 0; y < kernelY / 2; ++y) {
//            for (int x = 0; x < smoothed.getWidth(); ++x) {
//                int pixel = smoothed.getRGB(x, y);
//                output.setRGB(x, y, pixel);
//            }
//        }
//        for (int y = smoothed.getHeight() - kernelY / 2; y < smoothed.getHeight(); ++y) {
//            for (int x = 0; x < smoothed.getWidth(); ++x) {
//                int pixel = smoothed.getRGB(x, y);
//                output.setRGB(x, y, pixel);
//            }
//        }
//        for (int y = 0; y < smoothed.getHeight(); ++y) {
//            for (int x = 0; x < kernelX / 2; ++x) {
//                int pixel = smoothed.getRGB(x, y);
//                output.setRGB(x, y, pixel);
//            }
//        }
//        for (int y = 0; y < smoothed.getHeight(); ++y) {
//            for (int x = smoothed.getWidth() - kernelX / 2; x < smoothed.getWidth(); ++x) {
//                int pixel = smoothed.getRGB(x, y);
//                output.setRGB(x, y, pixel);
//            }
//        }
        for (int y = kernelY / 2; y < smoothed.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < smoothed.getWidth() - kernelX / 2 - 1; ++x) {
                int min = 255;
                int max = 0;
                int currentPixel = smoothed.getRGB(x, y) & 0xFF;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = smoothed.getRGB(x + deltaX, y + deltaY) & 0xFF;
                        min = Math.min(min, pixel);
                        max = Math.max(max, pixel);
                    }
                }
                double percentile = (currentPixel - min) * 1.0 / Math.max((max - min), 1);
                int newPixel = (int) (255 * percentile);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage binarizeWithCutOff(BufferedImage input, double cutOff) {
        return binarizeWithCutOff(input, cutOff, false);
    }

    // up == pixels above cutoff are blackened
    public static final BufferedImage binarizeWithCutOff(BufferedImage input, double cutOff, boolean up) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                int newPixel = up ? (currentPixel > cutOff ? 0 : 255) : (currentPixel < cutOff ? 0 : 255);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage gammaContrastEnhancement(BufferedImage input) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);

        int total = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                total += currentPixel;
            }
        }
        double average = total / (input.getWidth() * input.getHeight());

        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                double gamma = Math.min(currentPixel * 1.0 / average, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage gammaContrastEnhancementSkipWhites(BufferedImage input, int whiteCutoff, int gammaEnhancementType) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int total = 0;
        int totalPixels = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                if (currentPixel < whiteCutoff) {
                    total+=currentPixel;
                    ++totalPixels;
                }
            }
        }
        if (totalPixels < 100) {
            return output;
        }
        double average = (total * 1.0) / totalPixels;

        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel >= whiteCutoff) {
                    continue;
                }
                double gamma = currentPixel * 1.0 / average;
                if (gammaEnhancementType == GAMMA_ENHANCEMENT_EXTREME) {
                    if (gamma > 1) {
                        gamma = gamma * gamma;
                    } else {
                        gamma = gamma * gamma * gamma;
                    }
                } else {
                    if (gammaEnhancementType == GAMMA_ENHANCEMENT_HIGH) {
                        gamma = gamma * gamma;
                    }
                }
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage gammaContrastEnhancementSkipWhites(BufferedImage input, int gammaEnhancementType) {

        // Assume that the darkest spots are what interest us, so
        // look for a minima in the histogram after the first maxima is reached
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int total = 0;
        int[] histogram = new int[256];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                histogram[currentPixel]++;
                total+=currentPixel;
            }
        }
        int average = total / (input.getWidth() * input.getHeight());
        int[] smoothedHistogram = new int[256];
        int smootheningPeriod = 5;
        double emaFactor = 2 * 1.0 / (smootheningPeriod + 1);
        smoothedHistogram[0] = histogram[0];
        for (int i = 1; i < histogram.length; ++i) {
            smoothedHistogram[i] = (int) ((1 - emaFactor) * smoothedHistogram[i-1] + emaFactor * histogram[i]);
        }
        // double smooth it
        int[] doubleSmoothedHistogram = new int[256];
        doubleSmoothedHistogram[0] = smoothedHistogram[0];
        for (int i = 1; i < smoothedHistogram.length; ++i) {
            doubleSmoothedHistogram[i] = (int) ((1 - emaFactor) * doubleSmoothedHistogram[i-1] + emaFactor * smoothedHistogram[i]);
        }
        // find the max below 225
        int maxFreq = Integer.MIN_VALUE;
        int maxFreqIndex = 0;
        int secondMaxFreq = 0;
        int secondMaxIndex = 0;
        int thirdMaxFreq = 0;
        int thirdMaxIndex = 0;
        int fourthMaxFreq = 0;
        int fourthMaxIndex = 0;
        for (int i = 0; i < 225; ++i) {
            if (doubleSmoothedHistogram[i] > maxFreq) {
                fourthMaxFreq = thirdMaxFreq;
                fourthMaxIndex = thirdMaxIndex;
                thirdMaxFreq = secondMaxFreq;
                thirdMaxIndex = secondMaxIndex;
                secondMaxFreq = maxFreq;
                secondMaxIndex = maxFreqIndex;
                maxFreq = doubleSmoothedHistogram[i];
                maxFreqIndex = i;
            } else {
                if (doubleSmoothedHistogram[i] > secondMaxFreq) {
                    fourthMaxFreq = thirdMaxFreq;
                    fourthMaxIndex = thirdMaxIndex;
                    thirdMaxFreq = secondMaxFreq;
                    thirdMaxIndex = secondMaxIndex;
                    secondMaxFreq = doubleSmoothedHistogram[i];
                    secondMaxIndex = i;
                } else {
                    if (doubleSmoothedHistogram[i] > thirdMaxFreq) {
                        fourthMaxFreq = thirdMaxFreq;
                        fourthMaxIndex = thirdMaxIndex;
                        thirdMaxFreq = doubleSmoothedHistogram[i];
                        thirdMaxIndex = i;
                    } else {
                        if (doubleSmoothedHistogram[i] > fourthMaxFreq) {
                            fourthMaxFreq = thirdMaxFreq;
                            fourthMaxIndex = thirdMaxIndex;
                        }
                    }
                }
            }
        }
        // find the first and second max index of these maxima
        int[] indices = new int[4];
        indices[0] = maxFreqIndex;
        indices[1] = secondMaxIndex;
        indices[2] = thirdMaxIndex;
        indices[3] = fourthMaxIndex;
        Arrays.sort(indices);
        int firstMaxIndex = indices[0];
        secondMaxIndex = indices[1];
        thirdMaxIndex = indices[2];

        // usually a good point for binarization is the minima between 1st and second max
        // sometimes though, it is between the second and third

        // The rule of thumb we'll use is that if the average pixel score is high,
        // then the minima that we are looking for is between the second and third, else
        // it is between the first and second

        // find the first minima after this maxima
        int minima = Integer.MAX_VALUE;
        int binarizationIndex = 0;
        if ((average > 200) && (thirdMaxIndex > 0)) {
            for (int i = secondMaxIndex + 1; i < thirdMaxIndex; ++i) {
                if (doubleSmoothedHistogram[i] < minima) {
                    minima = doubleSmoothedHistogram[i];
                    binarizationIndex = i;
                }
            }
        } else {
            for (int i = firstMaxIndex + 1; i < secondMaxIndex; ++i) {
                if (doubleSmoothedHistogram[i] < minima) {
                    minima = doubleSmoothedHistogram[i];
                    binarizationIndex = i;
                }
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                double gamma = currentPixel * 1.0 / binarizationIndex;
                if (gammaEnhancementType == GAMMA_ENHANCEMENT_EXTREME) {
                    if (gamma > 1) {
                        gamma = gamma * gamma;
                    } else {
                        gamma = gamma * gamma * gamma;
                    }
                } else {
                    if (gammaEnhancementType == GAMMA_ENHANCEMENT_HIGH) {
                        gamma = gamma * gamma;
                    }
                }
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage relativeGammaAverageEnhancementSkipWhites(BufferedImage input1, int kernelX, int kernelY, int whiteCutoff, int gammaEnhancementType) {

        kernelX = Math.min(Math.max(3, kernelX), input1.getWidth() / 4);
        kernelY = Math.min(Math.max(3, kernelY), input1.getHeight() / 4);
        if (kernelX % 2 == 0) {
            kernelX = kernelX + 1;
        }
        if (kernelY % 2 == 0) {
            kernelY = kernelY + 1;
        }

        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }

        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX/ 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel >= whiteCutoff) {
                    continue;
                }
                int total = 0;
                int totalPixels = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < whiteCutoff) {
                            total += pixel;
                            ++totalPixels;
                        }
                    }
                }
                if (totalPixels < 5) {
                    continue;
                }
                double average = total * 1.0 / totalPixels;
                double gamma = currentPixel * 1.0 / average;
                if ((gamma > 1) && (gammaEnhancementType == GAMMA_ENHANCEMENT_BELOW)) {
                    continue;
                }
                if ((gamma < 1) && (gammaEnhancementType == GAMMA_ENHANCEMENT_ABOVE)) {
                    continue;
                }
                if ((gammaEnhancementType == GAMMA_ENHANCEMENT_EXTREME)
                        || (gammaEnhancementType == GAMMA_ENHANCEMENT_ABOVE)
                        || (gammaEnhancementType == GAMMA_ENHANCEMENT_BELOW)){
                    gamma = gamma * gamma * gamma;
                } else {
                    if (gammaEnhancementType == GAMMA_ENHANCEMENT_HIGH) {
                        gamma = gamma * gamma;
                    }
                }
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }

        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage selectiveBlackening(BufferedImage input1, int kernelX, int kernelY, int whiteCutoff, double gammaCutoff) {

        kernelX = Math.min(Math.max(3, kernelX), input1.getWidth() / 4);
        kernelY = Math.min(Math.max(3, kernelY), input1.getHeight() / 4);
        if (kernelX % 2 == 0) {
            kernelX = kernelX + 1;
        }
        if (kernelY % 2 == 0) {
            kernelY = kernelY + 1;
        }

        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }

        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX/ 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel >= whiteCutoff) {
                    continue;
                }
                int total = 0;
                int totalPixels = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < whiteCutoff) {
                            total += pixel;
                            ++totalPixels;
                        }
                    }
                }
                if (totalPixels < 5) {
                    continue;
                }
                double average = total * 1.0 / totalPixels;
                double gamma = currentPixel * 1.0 / average;
                if (gamma > gammaCutoff) {
                    continue;
                }
                gamma = gamma * gamma * gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage fullImageGammaEnhancementWithBias(BufferedImage input) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int bucketSize = 15;
        int[] histogram = new int[(255 / bucketSize) + 1];
        int total = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                total += currentPixel;
            }
        }
        int average = total / (input.getWidth() * input.getHeight());
        int cutoff = (average + 255) / 2;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                if (grayValues[y][x] <= cutoff) {
                    histogram[grayValues[y][x] / bucketSize]++;
                }
            }
        }

        int maxBucketValue = Integer.MIN_VALUE;
        int maxBucketIndex = 2;
        for (int i = maxBucketIndex; i < histogram.length; ++i) {
            if (histogram[i] > maxBucketValue) {
                maxBucketValue = histogram[i];
                maxBucketIndex = i;
            }
        }
        int maxPixel = maxBucketIndex * bucketSize + bucketSize /2;
        if (maxPixel > 180) {
            return input;
        }
        double gamma = Math.min((2.0 * 200) / maxPixel, 7);
        Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
        double[] multipliers = gammaMultipliersMap.get(gammaInt);
        int[] newPixels = new int[256];
        for (int i = 0; i < newPixels.length; ++i) {
            int newPixel = (int) (255 * multipliers[i]);
            newPixels[i] = ARGBPixels[newPixel];
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > 240) {
                    continue;
                }
                output.setRGB(x, y, newPixels[currentPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage fullImageGammaEnhancement(BufferedImage input, int gammaEnhancementType) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int total = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                total += currentPixel;
            }
        }
        int average = total / (input.getWidth() * input.getHeight());
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                double gamma = Math.min((currentPixel * 1.0) / average, 7);
                if (gammaEnhancementType == GAMMA_ENHANCEMENT_HIGH) {
                    gamma = gamma * gamma;
                } else {
                    if (gammaEnhancementType == GAMMA_ENHANCEMENT_EXTREME) {
                        gamma = gamma * gamma * gamma;
                    }
                }
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage imageGammaEnhancement(BufferedImage input, Rectangle box, int gammaEnhancementType) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int total = 0;
        for (int y = (int) box.getY(); y < Math.min(box.getY() + box.getHeight(), input.getHeight()); ++y) {
            for (int x = (int) box.getX(); x < Math.min(box.getX() + box.getWidth(), input.getWidth()); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                total += currentPixel;
            }
        }
        int average = (int) (total / (box.getWidth() * box.getHeight()));
        for (int y = (int) box.getY(); y < Math.min(box.getY() + box.getHeight(), input.getHeight()); ++y) {
            for (int x = (int) box.getX(); x < Math.min(box.getX() + box.getWidth(), input.getWidth()); ++x) {
                int currentPixel = grayValues[y][x];
                double gamma = Math.min((currentPixel * 1.0) / average, 7);
                if (gammaEnhancementType == GAMMA_ENHANCEMENT_HIGH) {
                    gamma = gamma * gamma;
                } else {
                    if (gammaEnhancementType == GAMMA_ENHANCEMENT_EXTREME) {
                        gamma = gamma * gamma * gamma;
                    }
                }
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage fullImageGammaEnhancementSkipWhites(BufferedImage input, int whiteCutoff) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int bucketSize = 10;
        int[] histogram = new int[(255 / bucketSize) + 1];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                if (currentPixel > whiteCutoff) {
                    continue;
                }
                histogram[currentPixel / bucketSize]++;
            }
        }
        int maxBucketValue = Integer.MIN_VALUE;
        int maxBucketIndex = 0;
        for (int i = 0; i < histogram.length; ++i) {
            if (histogram[i] > maxBucketValue) {
                maxBucketValue = histogram[i];
                maxBucketIndex = i;
            }
        }
        int maxPixel = maxBucketIndex * bucketSize + bucketSize / 2;
        if (maxPixel > 180) {
            return input;
        }
        double gamma = Math.min((2.0 * 200) / maxPixel, 7);
        Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
        double[] multipliers = gammaMultipliersMap.get(gammaInt);
        int[] newPixels = new int[256];
        for (int i = 0; i < newPixels.length; ++i) {
            int newPixel = (int) (255 * multipliers[i]);
            newPixels[i] = ARGBPixels[newPixel];
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > 240) {
                    continue;
                }
                output.setRGB(x, y, newPixels[currentPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage simpleGammaContrastEnhancement(BufferedImage input1, int kernelX, int kernelY) {
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int total = 0;
                int currentPixel = input.getRGB(x, y) & 0xFF;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = input.getRGB(x + deltaX, y + deltaY) & 0xFF;
                        total += pixel;
                    }
                }
                double average = total * 1.0 / (kernelX * kernelY);
                double gamma = (currentPixel * 1.0 / average);
                // gamma = gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeGammaContrastEnhancement(BufferedImage input1, int kernelX, int kernelY) {
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int total = 0;
                int currentPixel = input.getRGB(x, y) & 0xFF;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = input.getRGB(x + deltaX, y + deltaY) & 0xFF;
                        total += pixel;
                    }
                }
                double average = total * 1.0 / (kernelX * kernelY);
                double gamma = (currentPixel * 1.0 / average);
                gamma = gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeGammaContrastEnhancement(BufferedImage input, Rectangle box, int kernelX, int kernelY) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        for (int y = (int) box.getY(); y < Math.min((int) (box.getY() + box.getHeight()), input.getHeight() - kernelY / 2); ++y) {
            for (int x = (int) box.getX(); x < Math.min((int) (box.getX() + box.getWidth()), input.getWidth() - kernelX / 2); ++x) {
                int total = 0;
                int currentPixel = input.getRGB(x, y) & 0xFF;
                for (int deltaY = Math.max( -((int)box.getY()), -kernelY / 2); deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = Math.max( -((int)box.getX()), -kernelX / 2); deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = input.getRGB(x + deltaX, y + deltaY) & 0xFF;
                        total += pixel;
                    }
                }
                double average = total * 1.0 / (kernelX * kernelY);
                double gamma = (currentPixel * 1.0 / average);
                gamma = gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
            }
        }
        return output;
    }

    public static final BufferedImage relativeGammaContrastEnhancementWithPercentTolerance(BufferedImage input1, int kernelX, int kernelY, double tolerance) {
        // System.out.println("Entered relativeGammaContrastEnhancementWithPercentTolerance()");
        if (tolerance > 0.3) {
            tolerance = 0.3;
        }
        if (tolerance < 0) {
            tolerance = 0.0;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if ((currentPixel > 242) || (currentPixel < 16)) {
                    continue;
                }
                // System.out.println("Processing pixel value " + currentPixel);
                int total = 0;
                int pixelCount = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if ((pixel > 242) || (pixel < 16)) {
                            continue;
                        }
                        total += pixel;
                        ++pixelCount;
                    }
                }
                if (pixelCount <= 3) {
                    continue;
                }
                double average = total * 1.0 / (kernelX * kernelY);
                // System.out.println("Processing pixel value : " + currentPixel + " average : " + average + " against limits " + average * (1 - tolerance) + " and " + average * (1 + tolerance));
                if ((currentPixel > average * (1 - tolerance)) && (currentPixel < average * (1 + tolerance))) {
                    continue;
                }
                double gamma = (currentPixel * 1.0 / average);
                gamma = gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                // System.out.println("Changed pixel from " + currentPixel + " to " + newPixel);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeGammaContrastEnhancementWithAbsoluteTolerance(BufferedImage input1, int kernelX, int kernelY, int tolerance) {
        if (tolerance > 40) {
            tolerance = 40;
        }
        if (tolerance < 0) {
            tolerance = 0;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if ((currentPixel > 242) || (currentPixel < 16)) {
                    continue;
                }
                int total = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y][x];
                        total += pixel;
                    }
                }
                double average = total * 1.0 / (kernelX * kernelY);
                if ((currentPixel > average - tolerance) && (currentPixel < average + tolerance)) {
                    continue;
                }
                double gamma = (currentPixel * 1.0 / average);
                gamma = gamma * gamma * gamma;
                gamma = Math.min(gamma, 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeScaledGammaContrastEnhancement(BufferedImage input1, int kernelX, int kernelY,
                                                                 int percentile) {

        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        int size = kernelX * kernelY;
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[size - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeScaledGammaContrastEnhancement(BufferedImage input, Rectangle box, int kernelX, int kernelY,
                                                                             int percentile) {

        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), (int) box.getWidth());
        kernelY = Math.min(Math.max(1, kernelY), (int) box.getHeight());
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        int size = kernelX * kernelY;
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = (int) box.getY(); y < Math.min(box.getY() + box.getHeight(), input.getHeight() - kernelY / 2); ++y) {
            for (int x = (int) box.getX(); x < Math.min((int) (box.getX() + box.getWidth()), input.getWidth() - kernelX / 2); ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = Math.max( -((int)box.getY()), -kernelY / 2); deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = Math.max( -((int)box.getX()), -kernelX / 2); deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[size - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage relativeScaledGammaContrastEnhancementSkipWhites(BufferedImage input1, int kernelX, int kernelY,
                                                                             int percentile, int whiteCutOff) {
        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or above the white cutoff

        int whitePixel = 0xFFFFFFFF;
        int whiteGrayValue = 255;
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel == whiteGrayValue) {
                    continue;
                }
                if (currentPixel >= whiteCutOff) {
                    continue;
                }
                ArrayList<Integer> pixelValues = new ArrayList<>();
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2) {
                            continue;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < whiteGrayValue) {
                            pixelValues.add(pixel);
                        }
                    }
                }
                if (pixelValues.size() <= 2) {
                    output.setRGB(x, y, whitePixel);
                    continue;
                }
                int[] pixelArray = pixelValues.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[pixelArray.length - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeScaledGammaContrastEnhancementSkipWhites(BufferedImage input, Rectangle box, int kernelX, int kernelY,
                                                                                       int percentile, int whiteCutOff) {
        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or above the white cutoff

        int whitePixel = 0xFFFFFFFF;
        int whiteGrayValue = 255;
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 90;
        }
        kernelX = Math.min(Math.max(1, kernelX), (int) box.getWidth());
        kernelY = Math.min(Math.max(1, kernelY), (int) box.getHeight());
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = (int) box.getY(); y < Math.min((int) (box.getY() + box.getHeight()), input.getHeight() - kernelY / 2); ++y) {
            for (int x = (int) box.getX(); x < Math.min((int) (box.getX() + box.getWidth()), input.getWidth() - kernelX / 2); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel == whiteGrayValue) {
                    continue;
                }
                if (currentPixel >= whiteCutOff) {
                    continue;
                }
                ArrayList<Integer> pixelValues = new ArrayList<>();
                for (int deltaY = Math.max( -((int)box.getY()), -kernelY / 2); deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = Math.max( -((int)box.getX()), -kernelX / 2); deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < whiteGrayValue) {
                            pixelValues.add(pixel);
                        }
                    }
                }
                if (pixelValues.size() <= 2) {
                    output.setRGB(x, y, whitePixel);
                    continue;
                }
                int[] pixelArray = pixelValues.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[pixelArray.length - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage gammaEnhancementBelowGrayPixel(BufferedImage input1, int kernelX, int kernelY,
                                                                                       int percentile, int grayPixelCutOff) {
        // What is the Cutoff ? : don't change the pixel value if the pixel is equal to or above the grayPixel cutoff

        // Similarity with relativeScaledGammaContrastEnhancementSkipWhites() is that
        // this skips the central pixel if it is >= grayPixelCutoff

        // Difference with relativeScaledGammaContrastEnhancementSkipWhites() is that
        // while tabulating the percentile, it only skips the whitePixels (actually, >= 254), while the
        // relativeScaledGammaContrastEnhancementSkipWhites() skips pixels > grayPixelCutoff while
        // tabulating percentile

        // Also, this routine skips forward if the central pixel is <= 8

        int whitePixel = 0xFFFFFFFF;
        int whiteGrayValue = 255;
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage  input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel >= grayPixelCutOff) {
                    continue;
                }
                if (currentPixel <= 8) {
                    continue;
                }
                ArrayList<Integer> pixelValues = new ArrayList<>();
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < 254) {
                            pixelValues.add(pixel);
                        }
                    }
                }
                if (pixelValues.size() <= 2) {
                    output.setRGB(x, y, whitePixel);
                    continue;
                }
                int[] pixelArray = pixelValues.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[pixelArray.length - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage darkenThinLightHorizontalLines(BufferedImage input1, int kernelX, int kernelY, int percentile) {
        // for thin characters, there is sometimes a problem of extracting the thin lines in
        // gamma enhancement

        // this routine overcomes that problem so that one can continue with a low percentile for
        // gamma enhancement and then augment the result with this routine
        int whitePixel = 0xFFFFFFFF;
        int whiteGrayValue = 255;
        kernelX = Math.min(Math.max(5, kernelX), 7);
        kernelY = Math.min(Math.max(3, kernelY), 7);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        int bucketSize = 10;
        int[] histogram = new int[(255 / bucketSize) + 1];
        // int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                // grayValues[y][x] = currentPixel;
                if (currentPixel < 235) {
                    // int index = currentPixel / bucketSize;
                    // int population = histogram[index];
                    // histogram[index] = ++population;
                    histogram[currentPixel / bucketSize]++;
                }
            }
        }
        // System.out.println(Arrays.deepToString(histogram));
        int highestBucketValue = Integer.MIN_VALUE;
        int highestBucket = 0;
        for (int i = 0; i < histogram.length; ++i) {
            if (histogram[i] > highestBucketValue) {
                highestBucketValue = histogram[i];
                highestBucket = i;
            }
        }
        whiteGrayValue = (highestBucket * bucketSize) + bucketSize / 2;
        int grayValueCutoff = Math.max(100, whiteGrayValue - 10);
        System.out.println("Highest bucket is " + highestBucket + " with population " + highestBucketValue);
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        BufferedImage output1 = clipBI(output, clipRect);
        return gammaEnhancementBelowGrayPixel(output1, kernelX, kernelY, percentile, grayValueCutoff);
    }

//    public static final BufferedImage relativeScaledGammaContrastEnhancementSkipBlacks(BufferedImage input, int kernelX, int kernelY,
//                                                                                       int percentile, int blackCutOff, int whiteCutOff) {
//        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or less than the black cutoff
//        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or more than the white cutoff
//        int whitePixel = 0xFF000000 | 255 << 16 | 255 << 8 | 255;
//        int blackPixel = 0x00000000;
//        int whiteGrayValue = 255;
//        if (percentile < 0) {
//            percentile = 0;
//        }
//        if (percentile > 100) {
//            percentile = 100;
//        }
//        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
//        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
//        if (kernelX % 2 == 0) {
//            kernelX = Math.max(1, kernelX - 1);
//        }
//        if (kernelY % 2 == 0) {
//            kernelY = Math.max(1, kernelY - 1);
//        }
//        BufferedImage output = copyBI(input);
//        double baseComparisonPercent = (percentile * 1.0 / 100.0);
//        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
//        for (int y = 0; y < input.getHeight(); ++y) {
//            for (int x = 0; x < input.getWidth(); ++x) {
//                int currentPixel = input.getRGB(x, y) & 0xFF;
//                grayValues[y][x] = currentPixel;
//            }
//        }
//        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
//            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
//                int currentPixel = grayValues[y][x];
//                if (currentPixel >= whiteCutOff) {
//                    continue;
//                }
//                if (currentPixel <= blackCutOff) {
//                    continue;
//                }
//                ArrayList<Integer> pixelValues = new ArrayList<>();
//                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
//                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
//                        int pixel = grayValues[y + deltaY][x + deltaX];
//                        if ((pixel < whiteGrayValue) && (pixel > blackCutOff)){
//                            pixelValues.add(pixel);
//                        }
//                    }
//                }
//                if (pixelValues.size() <= 2) {
//                    output.setRGB(x, y, whitePixel);
//                    continue;
//                }
//                int[] pixelArray = pixelValues.stream().mapToInt(i -> i).toArray();
//                Arrays.sort(pixelArray);
//                int min = pixelArray[0];
//                int max = pixelArray[pixelArray.length - 1];
//                int base = (int) ((max - min) * baseComparisonPercent) + min;
//                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
//                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
//                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
//                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                int newPixel = (int) (255 * multipliers[currentPixel]);
//                output.setRGB(x, y, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
//            }
//        }
//        return output;
//    }

    public static final BufferedImage relativeScaledGammaContrastEnhancementSkipWhitesAndBlacks(BufferedImage input1, int kernelX, int kernelY,
                                                                                       int percentile, int whiteCutOff, int blackCutOff) {
        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or less than the black cutoff
        // Whats is the Cutoff ? : don't change the pixel value if the pixel is equal to or more than the white cutoff

        int whiteGrayValue = 255;
        int whitePixel = 0xFFFFFFFF;
        int blackPixel = 0xFF000000;
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        double baseComparisonPercent = (percentile * 1.0 / 100.0);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel >= whiteCutOff) {
                    continue;
                }
                if (currentPixel <= blackCutOff) {
                    // output.setRGB(x, y, 0xFF000000 | currentPixel << 16 | currentPixel << 8 | currentPixel);
                    continue;
                }
                ArrayList<Integer> pixelValues = new ArrayList<>();
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        // if ((pixel < whiteGrayValue) && (pixel > blackCutOff)){
                        if (pixel < whiteGrayValue) {
                            pixelValues.add(pixel);
                        }
                    }
                }
                if (pixelValues.size() <= 2) {
                    output.setRGB(x, y, ARGBPixels[currentPixel]);
                    continue;
                }
                int[] pixelArray = pixelValues.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                int min = pixelArray[0];
                int max = pixelArray[pixelArray.length - 1];
                int base = (int) ((max - min) * baseComparisonPercent) + min;
                int gamma = Math.min((int) Math.round((currentPixel - min) * 1.0 / (0.001 * Math.max(base - min, 1))), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage relativeGammaContrastEnhancement(BufferedImage input1, int kernelX, int kernelY,
                                                                       int percentile) {

        // top percentile % cells are blackened, rest are whitened
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        // int indexOfBaseComparisonPixelInKernel = (int) ((percentile * 1.0 / 100.0) * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        // int[] pixelArray = new int[kernelX * kernelY];
        ArrayList<Integer> pixelArrayList = new ArrayList<>();
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                pixelArrayList = new ArrayList<>();
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    inner: for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArrayList.add(pixel);
                        // pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                if (pixelArrayList.size() > 1) {
                    int[] pixelArray = pixelArrayList.stream().mapToInt(i -> i).toArray();
                    Arrays.sort(pixelArray);
                    int indexOfBaseComparisonPixelInKernel = (int) (pixelArrayList.size() * percentile / 100);
                    int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                    int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * base)), 1999);
                    double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                    Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                    double[] multipliers = gammaMultipliersMap.get(gammaInt);
                    int newPixel = (int) (255 * multipliers[currentPixel]);
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage equaliseOrEnhanceContrast(BufferedImage input1, int kernelX, int kernelY,
                                                          double lowerCutoffRatio, double upperCutoffRatio, boolean useMedian) {

        if (lowerCutoffRatio < 0.01) {
            lowerCutoffRatio = 0.01;
        }
        if (lowerCutoffRatio > 0.99) {
            lowerCutoffRatio = 0.99;
        }
        if (upperCutoffRatio < 0.01) {
            upperCutoffRatio = 0.01;
        }
        if (upperCutoffRatio > 0.99) {
            upperCutoffRatio = 0.99;
        }
        if (upperCutoffRatio <= lowerCutoffRatio) {
            upperCutoffRatio = lowerCutoffRatio + 0.01;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        int medianIndex = (int) (0.5 * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int kernelSize = kernelX * kernelY;
        int[] pixelArray = new int[kernelSize];
        int total = 0;
        int average = 1;
        double ratio = 0.0;
        int base = 0;
        int newPixel = 0;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                total = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    inner: for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += pixel;
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                average = Math.max(total / kernelSize, 1);
                base = Math.max(pixelArray[medianIndex], 1);
                ratio = currentPixel * 1.0 / average;
                if ((ratio <= lowerCutoffRatio) || (ratio >= upperCutoffRatio)) {
                    int gamma = Math.min((int) Math.round(ratio / 0.001), 1999);
                    double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                    Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                    double[] multipliers = gammaMultipliersMap.get(gammaInt);
                    newPixel = (int) (255 * multipliers[currentPixel]);
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                } else {
                    if (!useMedian) {
                        newPixel = (currentPixel + average) / 2;
                        output.setRGB(x, y, ARGBPixels[newPixel]);
                    } else {
                        output.setRGB(x, y, ARGBPixels[base]);
                    }
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage medianFilter(BufferedImage input, int kernelX, int kernelY) {
        return rankFilter(input, kernelX, kernelY, 50);
    }

    public static final BufferedImage rankFilter(BufferedImage input1, int kernelX, int kernelY, int percentile) {

        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input1.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input1.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);
        // int indexOfBaseComparisonPixelInKernel = (int) ((percentile * 1.0 / 100.0) * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        // int[] pixelArray = new int[kernelX * kernelY];
        ArrayList<Integer> pixelArrayList = new ArrayList<>();
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                pixelArrayList = new ArrayList<>();
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        // pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                        pixelArrayList.add(pixel);
                    }
                }
                int[] pixelArray = pixelArrayList.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                int indexOfBaseComparisonPixelInKernel = (pixelArrayList.size() * percentile) / 100;
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                output.setRGB(x, y, ARGBPixels[base]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage copyBorder(BufferedImage input1, int kernelX, int kernelY) {
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < kernelY / 2; ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y);
                output.setRGB(x, y, pixel);
            }
        }
        for (int y = input.getHeight() - kernelY / 2; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y);
                output.setRGB(x, y, pixel);
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < kernelX / 2; ++x) {
                int pixel = input.getRGB(x, y);
                output.setRGB(x, y, pixel);
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = input.getWidth() - kernelX / 2; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y);
                output.setRGB(x, y, pixel);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage createBlackCopy(BufferedImage template) {
        BufferedImage copy = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setPaint(new Color(0, 0, 0));
        graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        graphics.dispose();
        return copy;
    }

    public static final BufferedImage createWhiteCopy(BufferedImage template) {
        BufferedImage copy = new BufferedImage(template.getWidth(), template.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.setPaint(new Color(255, 255, 255));
        graphics.fillRect(0, 0, copy.getWidth(), copy.getHeight());
        graphics.dispose();
        return copy;
    }

    public static final BufferedImage createMaskFromTemplateAndMaskRectangle(BufferedImage image, Rectangle mask) {
        BufferedImage white = createWhiteCopy(image);
        if (mask == null || mask.getWidth() <= 1 || mask.getHeight() <= 1) {
            return white;
        }
        Graphics2D graphics = white.createGraphics();
        graphics.setPaint(new Color(0, 0, 0));
        graphics.fillRect((int) mask.getX(), (int) mask.getY(), (int) mask.getWidth(), (int) mask.getHeight());
        graphics.dispose();
        return white;

    }

    public static final BufferedImage getThinConnectedBI(BufferedImage input) {
        Pix pix = convertImageToPix(input);
        Pix pix8 = Leptonica.INSTANCE.pixConvertTo8(pix, 0);
        Pix pix1 = Leptonica.INSTANCE.pixConvertTo1(pix8, 128);
        Pix pixThin = Leptonica.INSTANCE.pixThinConnected(pix1, ILeptonica.L_THIN_FG, 8, 0);

        BufferedImage output = convertPixToImage(pixThin);
        LeptUtils.dispose(pix);
        LeptUtils.dispose(pix8);
        LeptUtils.dispose(pix1);
        LeptUtils.dispose(pixThin);

        return rectifyBI(output);
    }

    public static final String getJustFileName(String file) {
        File aFile = new File(file);
        String lastNameWithDot = aFile.getName();
        int dotIndex = lastNameWithDot.indexOf(".");
        String lastNameWithoutDot = lastNameWithDot.substring(0, dotIndex);
        return lastNameWithoutDot;
    }

    public static Pix bilateralTransform(Pix pix) {
        Pix output = Leptonica1.pixBilateral(pix, 50f, 80f, 4, 4);
        Pix gray = Leptonica1.pixConvertTo8(output, 0);
        LeptUtils.dispose(output);
        return gray;
    }

    public static final BufferedImage bilateralTransform(BufferedImage image) {
        Pix pix = convertImageToPix(image);
        Pix bilateral = Leptonica1.pixBilateral(pix, 30f, 50f, 4, 2);
        Pix gray = Leptonica1.pixConvertTo8(bilateral, 0);
        BufferedImage output = convertPixToImage(gray);
        LeptUtils.dispose(pix);
        LeptUtils.dispose(bilateral);
        LeptUtils.dispose(gray);
        return rectifyBI(output);
    }

    public static final boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile) {
        return writeFile(bufferedImage, formatName, localOutputFile, 300);
    }

    public static final boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi) {
        return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f);
    }

    public static final boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                                    float compressionQuality) {

        if (bufferedImage == null) {
            return false;
        }
        RenderedImage[] input = new RenderedImage[1];
        input[0] = bufferedImage;
        return writeFile(input, formatName, localOutputFile, dpi, compressionQuality);
    }

    public static final boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi) {
        return writeFile(images, formatName, localOutputFile, dpi, 0.5f);
    }

    public static final boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
                                    float compressionQuality) {

        if (images == null) {
            throw new IllegalArgumentException("No images available for writing to : " + formatName + " file");
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);

        if (!writers.hasNext()) {
            throw new IllegalArgumentException("No writer available for: " + formatName + " files");
        }

        IIOImage temp = null;
        ImageTypeSpecifier its = null;
        IIOMetadata md = null;
        ImageWriter writer = null;
        ImageWriteParam writeParam = null;
        ImageOutputStream output = null;
        its = ImageTypeSpecifier.createFromRenderedImage(images[0]);
        boolean writerFound = false;

        try {
            // Loop until we get the best driver, i.e. one that supports
            // setting dpi in the standard metadata format; however we'd also
            // accept a driver that can't, if a better one can't be found
            for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
                if (writer != null) {
                    writer.dispose();
                }
                writer = iw.next();
                if (writer == null) {
                    continue;
                }
                writeParam = writer.getDefaultWriteParam();
                md = writer.getDefaultImageMetadata(its, writeParam);
                if (md == null) {
                    continue;
                }
                if (md.isReadOnly() || !md.isStandardMetadataFormatSupported()) {
                    writerFound = false;
                } else {
                    writerFound = true;
                    break;
                }
            }

            if (!writerFound) {
                StringBuilder sb = new StringBuilder();
                String[] writerFormatNames = ImageIO.getWriterFormatNames();
                for (String fmt : writerFormatNames) {
                    sb.append(fmt);
                    sb.append(' ');
                }
                throw new IllegalArgumentException("No suitable writer found. Metadata of all writers for : "
                        + formatName
                        + " files are either Read-Only or don't support standard metadata format. Supported formats are : "
                        + sb);
            }

            try {

                // compression
                if ((writeParam != null) && writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    if (formatName.toLowerCase().startsWith("tif")) {
                        writeParam.setCompressionType("LZW"); // org.apache.pdfbox.filter.TIFFExtension.COMPRESSION_LZW
                        writeParam.setCompressionQuality(compressionQuality);
                    }
                }

                if (formatName.toLowerCase().startsWith("tif")) {
                    // TIFF metadata
                    // Convert default metadata to TIFF metadata
                    TIFFDirectory dir = TIFFDirectory.createFromMetadata(md);

                    // Get {X,Y} resolution tags
                    BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
                    TIFFTag tagXRes = base.getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION);
                    TIFFTag tagYRes = base.getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION);

                    // Create {X,Y} resolution fields
                    TIFFField resolution = new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
                            BaselineTIFFTagSet.RESOLUTION_UNIT_INCH);
                    TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });
                    TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });

                    // Add {X,Y} resolution fields to TIFFDirectory
                    dir.addTIFFField(resolution);
                    dir.addTIFFField(fieldXRes);
                    dir.addTIFFField(fieldYRes);

                    // Add unit field to TIFFDirectory (change to RESOLUTION_UNIT_CENTIMETER if
                    // necessary)
                    dir.addTIFFField(new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
                            BaselineTIFFTagSet.RESOLUTION_UNIT_INCH));

                    // assign the new dir as the new IIOImageMetadata
                    md = dir.getAsMetadata();
                } else if ("jpeg".equals(formatName.toLowerCase()) || "jpg".equals(formatName.toLowerCase())) {
                    // This segment must be run before other meta operations,
                    // or else "IIOInvalidTreeException: Invalid node: app0JFIF"
                    // The other (general) "meta" methods may not be used, because
                    // this will break the reading of the meta data in tests
                    Element root = (Element) md.getAsTree("javax_imageio_jpeg_image_1.0");
                    NodeList jvarNodeList = root.getElementsByTagName("JPEGvariety");
                    Element jvarChild;
                    if (jvarNodeList.getLength() == 0) {
                        jvarChild = new IIOMetadataNode("JPEGvariety");
                        root.appendChild(jvarChild);
                    } else {
                        jvarChild = (Element) jvarNodeList.item(0);
                    }

                    NodeList jfifNodeList = jvarChild.getElementsByTagName("app0JFIF");
                    Element jfifChild;
                    if (jfifNodeList.getLength() == 0) {
                        jfifChild = new IIOMetadataNode("app0JFIF");
                        jvarChild.appendChild(jfifChild);
                    } else {
                        jfifChild = (Element) jfifNodeList.item(0);
                    }
                    if (jfifChild.getAttribute("majorVersion").isEmpty()) {
                        jfifChild.setAttribute("majorVersion", "1");
                    }
                    if (jfifChild.getAttribute("minorVersion").isEmpty()) {
                        jfifChild.setAttribute("minorVersion", "2");
                    }
                    jfifChild.setAttribute("resUnits", "1"); // inch
                    jfifChild.setAttribute("Xdensity", Integer.toString(dpi));
                    jfifChild.setAttribute("Ydensity", Integer.toString(dpi));
                    if (jfifChild.getAttribute("thumbWidth").isEmpty()) {
                        jfifChild.setAttribute("thumbWidth", "0");
                    }
                    if (jfifChild.getAttribute("thumbHeight").isEmpty()) {
                        jfifChild.setAttribute("thumbHeight", "0");
                    }

                    // mergeTree doesn't work for ARGB
                    md.setFromTree("javax_imageio_jpeg_image_1.0", root);

                } else {
                    // write metadata is possible
                    if ((md != null) && !md.isReadOnly() && md.isStandardMetadataFormatSupported()) {
                        IIOMetadataNode root = (IIOMetadataNode) md.getAsTree("javax_imageio_1.0");

                        IIOMetadataNode dimension = null;
                        NodeList nodeList = root.getElementsByTagName("Dimension");
                        if (nodeList.getLength() > 0) {
                            dimension = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            dimension = new IIOMetadataNode("Dimension");
                            root.appendChild(dimension);
                        }

                        // PNG writer doesn't conform to the spec which is
                        // "The width of a pixel, in millimeters"
                        // but instead counts the pixels per millimeter
                        float res = "PNG".equals(formatName.toUpperCase()) ? dpi / 25.4f : 25.4f / dpi;

                        IIOMetadataNode hps = null;

                        nodeList = dimension.getElementsByTagName("HorizontalPixelSize");
                        if (nodeList.getLength() > 0) {
                            hps = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            hps = new IIOMetadataNode("HorizontalPixelSize");
                            dimension.appendChild(hps);
                        }

                        hps.setAttribute("value", Double.toString(res));

                        IIOMetadataNode vps = null;

                        nodeList = dimension.getElementsByTagName("VerticalPixelSize");
                        if (nodeList.getLength() > 0) {
                            vps = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            vps = new IIOMetadataNode("VerticalPixelSize");
                            dimension.appendChild(vps);
                        }

                        vps.setAttribute("value", Double.toString(res));

                        md.mergeTree("javax_imageio_1.0", root);
                    }
                }

                // Create output stream
                output = ImageIO.createImageOutputStream(new File(localOutputFile));

                writer.setOutput(output);

                // Optionally, listen to progress, warnings, etc.

                // writeParam = writer.getDefaultWriteParam();
                if (images.length > 1) {
                    writer.prepareWriteSequence(md);
                }

                // Optionally, control format specific settings of param (requires casting), or
                // control generic write settings like sub sampling, source region, output type
                // etc.

                // Optionally, provide thumbnails and image/stream metadata

                /*
                 * final String pngMetadataFormatName = "javax_imageio_1.0";
                 *
                 * // Convert dpi (dots per inch) to dots per meter final double metersToInches
                 * = 39.3701; int dotsPerMeter = (int) Math.round(dpi * metersToInches);
                 *
                 * IIOMetadataNode pHYs_node = new IIOMetadataNode("pHYs");
                 * pHYs_node.setAttribute("pixelsPerUnitXAxis", Integer.toString(dotsPerMeter));
                 * pHYs_node.setAttribute("pixelsPerUnitYAxis", Integer.toString(dotsPerMeter));
                 * pHYs_node.setAttribute("unitSpecifier", "meter");
                 *
                 * IIOMetadataNode root = new IIOMetadataNode(pngMetadataFormatName);
                 * root.appendChild(pHYs_node);
                 *
                 * md.mergeTree(pngMetadataFormatName, root);
                 */

                /*
                 * double dotsPerMilli = ((1.0 * dpi) / 10) / 2.54; IIOMetadataNode horiz = new
                 * IIOMetadataNode("HorizontalPixelSize"); horiz.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode vert = new
                 * IIOMetadataNode("VerticalPixelSize"); vert.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode dim = new
                 * IIOMetadataNode("Dimension"); dim.appendChild(horiz); dim.appendChild(vert);
                 * IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
                 * root.appendChild(dim); md.mergeTree("javax_imageio_1.0", root);
                 */

                // writer.prepareWriteEmpty(md, its, images[0].getWidth(),
                // images[0].getHeight(), null, null, writeParam);
                temp = new IIOImage(images[0], null, md);
                writer.write(null, temp, writeParam);
                // writer.endWriteEmpty();
                if (images.length > 1) {
                    if (!writer.canInsertImage(1)) {
                        throw new IllegalArgumentException("The writer for " + formatName
                                + " files is not able to add more than one image to the file : " + localOutputFile);
                    } else {
                        for (int i = 1; i < images.length; i++) {
                            // writer.prepareWriteEmpty(md, its, images[i].getWidth(),
                            // images[i].getHeight(), null, null,
                            // writeParam);
                            temp = new IIOImage(images[i], null, md);
                            writer.writeInsert(i, temp, writeParam);
                            // writer.endWriteEmpty();
                        }
                    }
                }
                // writer.endWriteSequence();
            } finally {
                // Close stream in finally block to avoid resource leaks
                if (output != null) {
                    output.close();
                }
            }
        } catch (Exception e) {

        } finally {
            // Dispose writer in finally block to avoid memory leaks
            if (writer != null) {
                writer.dispose();
            }
        }
        return true;
    }

    public static final BufferedImage clipBI(BufferedImage input, Rectangle box) {
        BufferedImage destination = new BufferedImage((int) box.getWidth(), (int) box.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destination.createGraphics();
        g.drawImage(input, 0, 0, (int) box.getWidth(), (int) box.getHeight(), (int) box.getX(), (int) box.getY(), (int) (box.getX() + box.getWidth()), (int) (box.getY() + box.getHeight()), null);
        g.dispose();
        return destination;
    }

    public static final Pix clipBI2Pix(BufferedImage input, Rectangle box) {
        BufferedImage destination = new BufferedImage((int) box.getWidth(), (int) box.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destination.createGraphics();
        g.drawImage(input, 0, 0, (int) box.getWidth(), (int) box.getHeight(), (int) box.getX(), (int) box.getY(), (int) box.getX() + (int) box.getWidth(), (int) box.getY() + (int) box.getHeight(), null);
        g.dispose();
        Pix clippedImage = getPixFromBufferedImage(destination);
        return clippedImage;
    }

    public static Pix clipPix(Pix input, Rectangle box) {
        BufferedImage inputCopy = convertPixToImage(input);
        BufferedImage destination = new BufferedImage((int) box.getWidth(), (int) box.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destination.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, (int) box.getWidth(), (int) box.getHeight());
        g.drawImage(inputCopy, 0, 0, (int) box.getWidth(), (int) box.getHeight(), (int) box.getX(), (int) box.getY(), (int) box.getX() + (int) box.getWidth(), (int) box.getY() + (int) box.getHeight(), null);
        g.dispose();
        Pix clippedImage = getPixFromBufferedImage(destination);
        return clippedImage;
    }

    public static final BufferedImage clipPix2BI(Pix input, Rectangle box) {
        BufferedImage inputCopy = convertPixToImage(input);
        BufferedImage destination = new BufferedImage((int) box.getWidth(), (int) box.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = destination.createGraphics();
        g.drawImage(inputCopy, 0, 0, (int) box.getWidth(), (int) box.getHeight(), (int) box.getX(), (int) box.getY(), (int) (box.getX() + box.getWidth()), (int) (box.getY() + box.getHeight()), null);
        g.dispose();
        return destination;
    }

    public static final BufferedImage blur(BufferedImage input1, int kernelX, int kernelY) {

        kernelX = kernelX % 2 != 0 ? kernelX : kernelX + 1;
        kernelY = kernelY % 2 != 0 ? kernelY : kernelY + 1;
        if ((kernelX <= 0) || (kernelY <= 0)) {
            return input1;
        }
        if ((kernelX <= 1) && (kernelY <= 1)) {
            return input1;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        int[][] pixels = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y) & 0xFF;
                pixels[y][x] = pixel;
            }
        }
        BufferedImage output = copyBI(input);
//        for (int y = 0; y < kernelY / 2; ++y) {
//            for (int x = 0; x < input.getWidth(); ++x) {
//                int pixel = pixels[y][x];
//                output.setRGB(x, y, 0xFF000000 | pixel << 16 | pixel << 8 | pixel);
//            }
//        }
//        for (int y = input.getHeight() - kernelY / 2; y < input.getHeight(); ++y) {
//            for (int x = 0; x < input.getWidth(); ++x) {
//                int pixel = pixels[y][x];
//                output.setRGB(x, y, 0xFF000000 | pixel << 16 | pixel << 8 | pixel);
//            }
//        }
//        for (int y = 0; y < input.getHeight(); ++y) {
//            for (int x = 0; x < kernelX / 2; ++x) {
//                int pixel = pixels[y][x];
//                output.setRGB(x, y, 0xFF000000 | pixel << 16 | pixel << 8 | pixel);
//            }
//        }
//        for (int y = 0; y < input.getHeight(); ++y) {
//            for (int x = input.getWidth() - kernelX / 2; x < input.getWidth(); ++x) {
//                int pixel = pixels[y][x];
//                output.setRGB(x, y, 0xFF000000 | pixel << 16 | pixel << 8 | pixel);
//            }
//        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int total = 0;
                int totalPixels = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    inner: for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue inner;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            break outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue inner;
                        }
                        int pixel = pixels[y + deltaY][x + deltaX];
                        total += pixel;
                        ++totalPixels;
                    }
                }
                int avg = total / totalPixels;
                output.setRGB(x, y, ARGBPixels[avg]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input1.getWidth(), input1.getHeight());
        return clipBI(output, clipRect);
    }

    public static BufferedImage averageCoalesce(BufferedImage red, BufferedImage green, BufferedImage blue) {
        BufferedImage newImage = new BufferedImage(red.getWidth(), red.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < red.getHeight(); ++y) {
            for (int x = 0; x < red.getWidth(); ++x) {
                int redPixel = red.getRGB(x, y) & 0xFF;
                int greenPixel = green.getRGB(x, y) & 0xFF;
                int bluePixel = blue.getRGB(x, y) & 0xFF;
                int average = (int) (greenPixel * 0.59 + redPixel * 0.3 + bluePixel * 0.11);
                newImage.setRGB(x, y, ARGBPixels[average]);
            }
        }
        return newImage;
    }

    public static final BufferedImage[] averageCoalesce(BufferedImage[] red, BufferedImage[] green, BufferedImage[] blue) {
        BufferedImage[] newImages = new BufferedImage[red.length];
        for (int i = 0; i < red.length; ++i) {
            newImages[i] = averageCoalesce(red[i], green[i], blue[i]);
        }
        return newImages;
    }

    public static final BufferedImage[] eliminateBlackBridges(BufferedImage[] images, int gapSeparation, int cutOff, int direction) {
        BufferedImage[] newImages = new BufferedImage[images.length];
        for (int i = 0; i < newImages.length; ++i) {
            newImages[i] = eliminateBlackBridges(images[i], gapSeparation, cutOff, direction);
        }
        return newImages;
    }

    public static final BufferedImage[] eliminateBlackBridgesIteratively(BufferedImage[] images, int gapSeparation, int cutOff,
                                                           int direction) {
        BufferedImage[] newImages = new BufferedImage[images.length];
        for (int i = 0; i < newImages.length; ++i) {
            newImages[i] = eliminateBlackBridgesIteratively(images[i], gapSeparation, cutOff, direction);
        }
        return newImages;
    }

    public static final BufferedImage eliminateBlackBridges(BufferedImage image, int gapSeparation, int cutOff, int directionOfBridge) {
        image = rectifyBI(image);
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        if (directionOfBridge == HORIZONTAL_DIRECTION) {
            for (int y = 0; y < image.getHeight() - (gapSeparation + 1); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int topPixel = image.getRGB(x, y) & 0xFF;
                    int bottomPixel = image.getRGB(x, y + gapSeparation + 1) & 0xFF;
                    if ((topPixel > cutOff) && (bottomPixel > cutOff)) {
                        int mid = (topPixel + bottomPixel) / 2;
                        int midPixel = ARGBPixels[mid];
                        for (int yCoord = y + 1; yCoord < y + gapSeparation + 1; ++yCoord) {
                            newImage.setRGB(x, yCoord, midPixel);
                        }
                    }
                }
            }
        } else {
            if (directionOfBridge == VERTICAL_DIRECTION) {
                for (int x = 0; x < image.getWidth() - (gapSeparation + 1); ++x) {
                    for (int y = 0; y < image.getHeight(); ++y) {
                        int leftPixel = image.getRGB(x, y) & 0xFF;
                        int rightPixel = image.getRGB(x + gapSeparation + 1, y) & 0xFF;
                        if ((leftPixel > cutOff) && (rightPixel > cutOff)) {
                            int mid = (leftPixel + rightPixel) / 2;
                            int midPixel = ARGBPixels[mid];
                            for (int xCoord = x + 1; xCoord < x + gapSeparation + 1; ++xCoord) {
                                newImage.setRGB(xCoord, y, midPixel);
                            }
                        }
                    }
                }
            } else {
                if (directionOfBridge == BOTH_DIRECTION) {
                    for (int y = 0; y < image.getHeight() - (gapSeparation + 1); ++y) {
                        for (int x = 0; x < image.getWidth(); ++x) {
                            int topPixel = image.getRGB(x, y) & 0xFF;
                            int bottomPixel = image.getRGB(x, y + gapSeparation + 1) & 0xFF;
                            if ((topPixel > cutOff) && (bottomPixel > cutOff)) {
                                int mid = (topPixel + bottomPixel) / 2;
                                int midPixel = ARGBPixels[mid];
                                for (int yCoord = y + 1; yCoord < y + gapSeparation + 1; ++yCoord) {
                                    newImage.setRGB(x, yCoord, midPixel);
                                }
                            }
                        }
                    }
                    for (int x = 0; x < image.getWidth() - (gapSeparation + 1); ++x) {
                        for (int y = 0; y < image.getHeight(); ++y) {
                            int leftPixel = image.getRGB(x, y) & 0xFF;
                            int rightPixel = image.getRGB(x + gapSeparation + 1, y) & 0xFF;
                            if ((leftPixel > cutOff) && (rightPixel > cutOff)) {
                                int mid = (leftPixel + rightPixel) / 2;
                                int midPixel = ARGBPixels[mid];
                                for (int xCoord = x + 1; xCoord < x + gapSeparation + 1; ++xCoord) {
                                    newImage.setRGB(xCoord, y, midPixel);
                                }
                            }
                        }
                    }
                }
            }
        }
        return newImage;
    }

    public static final Pix eliminateBlackBridges(Pix image, int gapSeparation, int cutOff, int directionOfBridge) {
        BufferedImage input = convertPixToImage(image);
        input = rectifyBI(input);
        BufferedImage output = eliminateBlackBridges(input, gapSeparation, cutOff, directionOfBridge);
        Pix outputPix = null;
        if (image.d == 1) {
            outputPix = getDepth1Pix(output, 128);
        } else {
            if (image.d == 8) {
                outputPix = getDepth8Pix(output);
            } else {
                outputPix = convertImageToPix(output);
            }
        }
        return outputPix;
    }

    public static final BufferedImage eliminateBlackBridgesIteratively(BufferedImage image, int gapSeparation, int cutOff,
                                                         int direction) {
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        for (int i = gapSeparation; i > 0; --i) {
            newImage = eliminateBlackBridges(newImage, i, cutOff, direction);
        }
        return newImage;
    }

    public static final Pix eliminateBlackBridgesIteratively(Pix image, int gapSeparation, int cutOff,
                                                                       int direction) {
        BufferedImage input = convertPixToImage(image);
        input = rectifyBI(input);
        BufferedImage output = eliminateBlackBridgesIteratively(input, gapSeparation, cutOff, direction);
        Pix outputPix = null;
        if (image.d == 1) {
            outputPix = getDepth1Pix(output, 128);
        } else {
            if (image.d == 8) {
                outputPix = getDepth8Pix(output);
            } else {
                outputPix = convertImageToPix(output);
            }
        }
        return outputPix;
    }

    public static final BufferedImage[] eliminateWhiteBridges(BufferedImage[] images, int gapSeparation, int cutOff, int direction) {
        BufferedImage[] newImages = new BufferedImage[images.length];
        for (int i = 0; i < newImages.length; ++i) {
            newImages[i] = eliminateWhiteBridges(images[i], gapSeparation, cutOff, direction);
        }
        return newImages;
    }

    public static final BufferedImage[] eliminateWhiteBridgesIteratively(BufferedImage[] images, int gapSeparation, int cutOff,
                                                           int direction) {
        BufferedImage[] newImages = new BufferedImage[images.length];
        for (int i = 0; i < newImages.length; ++i) {
            newImages[i] = eliminateWhiteBridgesIteratively(images[i], gapSeparation, cutOff, direction);
        }
        return newImages;
    }

    // removes black spots / lines based on the cleaning kernel
    public static final BufferedImage eliminateWhiteBridges(BufferedImage image, int gapSeparation, int cutOff, int directionOfBridge) {
        image = rectifyBI(image);
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        if (directionOfBridge == HORIZONTAL_DIRECTION) {
            for (int y = 0; y < image.getHeight() - (gapSeparation + 1); ++y) {
                for (int x = 0; x < image.getWidth(); ++x) {
                    int topPixel = image.getRGB(x, y) & 0xFF;
                    int bottomPixel = image.getRGB(x, y + gapSeparation + 1) & 0xFF;
                    if ((topPixel < cutOff) && (bottomPixel < cutOff)) {
                        int mid = (topPixel + bottomPixel) / 2;
                        int midPixel = ARGBPixels[mid];
                        for (int yCoord = y + 1; yCoord < y + gapSeparation + 1; ++yCoord) {
                            newImage.setRGB(x, yCoord, midPixel);
                        }
                    }
                }
            }
        } else {
            if (directionOfBridge == VERTICAL_DIRECTION) {
                for (int x = 0; x < image.getWidth() - (gapSeparation + 1); ++x) {
                    for (int y = 0; y < image.getHeight(); ++y) {
                        int leftPixel = image.getRGB(x, y) & 0xFF;
                        int rightPixel = image.getRGB(x + gapSeparation + 1, y) & 0xFF;
                        if ((leftPixel < cutOff) && (rightPixel < cutOff)) {
                            int mid = (leftPixel + rightPixel) / 2;
                            int midPixel = ARGBPixels[mid];
                            for (int xCoord = x + 1; xCoord < x + gapSeparation + 1; ++xCoord) {
                                newImage.setRGB(xCoord, y, midPixel);
                            }
                        }
                    }
                }
            }
        }
        return newImage;
    }

    public static final BufferedImage eliminateWhiteBridgesIteratively(BufferedImage image, int gapSeparation, int cutOff,
                                                         int direction) {
        image = rectifyBI(image);
        // first, just copy the image as is
        BufferedImage newImage = copyBI(image);
        for (int i = gapSeparation; i > 0; --i) {
            newImage = eliminateWhiteBridges(newImage, i, cutOff, direction);
        }
        return newImage;
    }

    public static BufferedImage imageBinaryAnd(BufferedImage image1, BufferedImage image2) {
        // white and black = white; white and white = white; black and black = black
        if ((image1 == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        image1 = rectifyBI(image1);
        image2 = rectifyBI(image2);
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), image1.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = Math.max(pixel1,pixel2);
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    // Gray images work differently compared to binarized images
    public static BufferedImage imageGrayAnd(BufferedImage image1, BufferedImage image2) {
        // white and black = white; white and white = white; black and black = black
        if ((image1 == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        image1 = rectifyBI(image1);
        image2 = rectifyBI(image2);
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), image1.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = Math.min(pixel1,pixel2);
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    public static BufferedImage imageBinaryOr(BufferedImage image1, BufferedImage image2) {
        // white or black = black; white or white = white; black or black = black
        if ((image1 == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        image1 = rectifyBI(image1);
        image2 = rectifyBI(image2);
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), image1.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = Math.min(pixel1,pixel2);
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    // Gray images work differently from binarized images
    public static BufferedImage imageGrayOr(BufferedImage image1, BufferedImage image2) {
        // white or black = black; white or white = white; black or black = black
        if ((image1 == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        image1 = rectifyBI(image1);
        image2 = rectifyBI(image2);
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), image1.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = Math.max(pixel1,pixel2);
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    public static BufferedImage imageAverage(BufferedImage image1, BufferedImage image2) {
        if ((image1 == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        image1 = rectifyBI(image1);
        image2 = rectifyBI(image2);
        int width = Math.min(image1.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), image1.getHeight());
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y) & 0xFF;
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = (pixel1 + pixel2) / 2;
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    public static BufferedImage imageAverageSkipWhites(BufferedImage baseImage, BufferedImage image2) {
        // If the baseImage has a white pixel, keep the cell as white in the output.
        // Rest are averaged
        if ((baseImage == null) || (image2 == null)) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        baseImage = rectifyBI(baseImage);
        image2 = rectifyBI(image2);
        int width = Math.min(baseImage.getWidth(), image2.getWidth());
        int height = Math.min(image2.getHeight(), baseImage.getHeight());
        BufferedImage output = copyBI(baseImage);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = baseImage.getRGB(x, y) & 0xFF;
                if (pixel1 >= 248) {
                    continue;
                }
                int pixel2 = image2.getRGB(x, y) & 0xFF;
                int pixel3 = (pixel1 + pixel2) / 2;
                int outputPixel = ARGBPixels[pixel3];
                output.setRGB(x, y, outputPixel);
            }
        }
        return output;
    }

    public static BufferedImage imageMask(BufferedImage image1, BufferedImage mask) {

        if (image1 == null) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        if (mask == null) {
            return image1;
        }
        int whitePixel = 0xFFFFFFFF;
        image1 = rectifyBI(image1);
        mask = rectifyBI(mask);
        int width = image1.getWidth();
        int height = image1.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y);
                int maskPixel = mask.getRGB(x, y) & 0xFF;
                if (maskPixel < 128) {
                    output.setRGB(x, y, pixel1);
                } else {
                    output.setRGB(x, y, whitePixel);
                }
            }
        }
        return output;
    }

    public static BufferedImage invertedImageMask(BufferedImage image1, BufferedImage mask) {

        if (image1 == null) {
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        }
        if (mask == null) {
            return image1;
        }
        int blackPixel = 0xFF000000;
        image1 = rectifyBI(image1);
        mask = rectifyBI(mask);
        int width = image1.getWidth();
        int height = image1.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int pixel1 = image1.getRGB(x, y);
                int maskPixel = mask.getRGB(x, y) & 0xFF;
                if (maskPixel < 128) {
                    output.setRGB(x, y, pixel1);
                } else {
                    output.setRGB(x, y, blackPixel);
                }
            }
        }
        return output;
    }

    public static List<String> listFiles(String inputDir, int depth) {
        if (inputDir != null) {
            Path path0 = Paths.get(inputDir);
            // System.out.println("Input Directory is given as = " + path0);
            Path inputDirectoryPath = null;
            try {
                inputDirectoryPath = Files.createDirectories(path0);
            } catch (FileAlreadyExistsException e) {
                // the directory already exists.
                inputDirectoryPath = path0;
            } catch (IOException e) {
                // something else went wrong
                // e.printStackTrace();
            }

            try (Stream<Path> stream = Files.walk(inputDirectoryPath, depth)) {
                return stream.filter(file -> !Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
                        .collect(Collectors.toList());
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return new ArrayList<String>();
    }

    public static List<String> filterFiles(List<String> filesInDirectory, String filterString, boolean filterIn) {
        List<String> filteredFiles = new ArrayList<>();
        for (String file : filesInDirectory) {
            int index = file.indexOf(filterString);
            if (index != -1) {
                if (filterIn) {
                    filteredFiles.add(file);
                }
            } else {
                if (!filterIn) {
                    filteredFiles.add(file);
                }
            }
        }
        return filteredFiles;
    }

    public static BufferedImage removeSmallTrails(BufferedImage input, ArrayList<CleaningKernel> kernels,
                                                  boolean whiteBorder_BlackTrail) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int whitePixel = 0xFFFFFFFF;
        int blackPixel = 0xFF000000;
        int whiteColour = 255;
        int blackColour = 0;
        int offendingColour = whiteBorder_BlackTrail ? blackColour : whiteColour;
        int floodFillColour = whiteBorder_BlackTrail ? whitePixel : blackPixel;
        for (CleaningKernel kernel : kernels) {
            int kHeight = kernel.height;
            int kWidth = kernel.width;
            if (kHeight < 3) {
                kHeight = 3;
            }
            if (kWidth < 3) {
                kWidth = 3;
            }
            if (kHeight > input.getHeight()) {
                kHeight = input.getHeight();
            }
            if (kWidth > input.getWidth()) {
                kWidth = input.getWidth();
            }
            for (int y = 0; y <= input.getHeight() - kHeight; ++y) {
                innerloop: for (int x = 0; x <= input.getWidth() - kWidth; ++x) {
                    int yCheck = y;
                    for (int deltaX = 0; deltaX < kWidth; ++deltaX) {
                        int pixel = grayValues[yCheck][x + deltaX];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    yCheck = y + kHeight - 1;
                    for (int deltaX = 0; deltaX < kWidth; ++deltaX) {
                        int pixel = grayValues[yCheck][x + deltaX];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    int xCheck = x;
                    for (int deltaY = 0; deltaY < kHeight; ++deltaY) {
                        int pixel = grayValues[y + deltaY][xCheck];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    xCheck = x + kWidth - 1;
                    for (int deltaY = 0; deltaY < kHeight; ++deltaY) {
                        int pixel = grayValues[y + deltaY][xCheck];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    // if the code reaches here, it means that a matching (all white or all black)
                    // border has been found
                    for (int deltaY = 1; deltaY < kHeight - 1; ++deltaY) {
                        for (int deltaX = 1; deltaX < kWidth - 1; ++deltaX) {
                            output.setRGB(x + deltaX, y + deltaY, floodFillColour);
                        }
                    }
                }
            }
        }
        return output;
    }

    // if whiteBorderBlackTrail is false, it implies blackBorderWhiteTrail
    public static BufferedImage removeSmallTrailsInRectangle(BufferedImage input, Rectangle focusArea, ArrayList<CleaningKernel> kernels,
                                                  boolean whiteBorder_BlackTrail) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[(int) focusArea.getHeight()][(int) focusArea.getWidth()];
        for (int y = (int) focusArea.getY(); y < (int) (focusArea.getY() + focusArea.getHeight()); ++y) {
            for (int x = (int) focusArea.getX(); x < (int) (focusArea.getX() + focusArea.getWidth()); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y - (int) focusArea.getY()][x - (int) focusArea.getX()] = currentPixel;
            }
        }
        int whitePixel = 0xFFFFFFFF;
        int blackPixel = 0xFF000000;
        int whiteColour = 255;
        int blackColour = 0;
        int offendingColour = whiteBorder_BlackTrail ? blackColour : whiteColour;
        int floodFillColour = whiteBorder_BlackTrail ? whitePixel : blackPixel;
        for (CleaningKernel kernel : kernels) {
            int kHeight = kernel.height;
            int kWidth = kernel.width;
            if (kHeight < 3) {
                kHeight = 3;
            }
            if (kWidth < 3) {
                kWidth = 3;
            }
            for (int y = 0; y < (int) focusArea.getHeight() - kHeight; ++y) {
                innerloop: for (int x = 0; x < (int) focusArea.getWidth() - kWidth; ++x) {
                    int yCheck = y;
                    for (int deltaX = 0; deltaX <= kWidth; ++deltaX) {
                        int pixel = grayValues[yCheck][x + deltaX];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    yCheck = y + kHeight;
                    for (int deltaX = 0; deltaX <= kWidth; ++deltaX) {
                        int pixel = grayValues[yCheck][x + deltaX];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    int xCheck = x;
                    for (int deltaY = 0; deltaY <= kHeight; ++deltaY) {
                        int pixel = grayValues[y + deltaY][xCheck];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    xCheck = x + kWidth;
                    for (int deltaY = 0; deltaY <= kHeight; ++deltaY) {
                        int pixel = grayValues[y + deltaY][xCheck];
                        if (pixel == offendingColour) {
                            // offending pixel found at border
                            continue innerloop;
                        }
                    }
                    // if the code reaches here, it means that a matching (all white or all black)
                    // border has been found
                    for (int deltaY = 1; deltaY < kHeight; ++deltaY) {
                        for (int deltaX = 1; deltaX < kWidth; ++deltaX) {
                            output.setRGB(x + deltaX + (int) focusArea.getX(), y + deltaY + (int) focusArea.getY(), floodFillColour);
                        }
                    }
                }
            }
        }
        return output;
    }

    public static final Pix removeLines(Pix input, int direction, int minLineLengthToBeRetained) {
        // takes a depth=1 as input Pix
        if (input.d != 1) {
            return input;
        }
        if (minLineLengthToBeRetained <= 10) {
            return input;
        }
        int kernelLength = (minLineLengthToBeRetained + 2);

        if (direction == VERTICAL_DIRECTION) {
            Pix pix1 = Leptonica1.pixOpenBrick(null, input, 1, kernelLength);
            Pix seed = Leptonica1.pixDilateBrick(null, pix1, 1, 3);
            Pix seedFilled = Leptonica1.pixSeedfillBinary(null, seed, input, 4);
            Pix verticalLinesEliminatedPix = Leptonica1.pixSubtract(null, input, seedFilled);
            LeptUtils.dispose(pix1);
            LeptUtils.dispose(seed);
            LeptUtils.dispose(seedFilled);
            return getDepth1Pix(verticalLinesEliminatedPix, 128);
        }
        if (direction == HORIZONTAL_DIRECTION) {
            Pix pix1 = Leptonica1.pixOpenBrick(null, input, kernelLength, 1);
            Pix seed = Leptonica1.pixDilateBrick(null, pix1, 3, 1);
            Pix seedFilled = Leptonica1.pixSeedfillBinary(null, seed, input, 4);
            Pix horizontalLinesEliminatedPix = Leptonica1.pixSubtract(null, input, seedFilled);
            LeptUtils.dispose(pix1);
            LeptUtils.dispose(seed);
            LeptUtils.dispose(seedFilled);
            return getDepth1Pix(horizontalLinesEliminatedPix);
        }
        if (direction == BOTH_DIRECTION) {
            Pix pix1 = Leptonica1.pixOpenBrick(null, input, kernelLength, 1);
            Pix seed = Leptonica1.pixDilateBrick(null, pix1, 3, 1);
            Pix seedFilled = Leptonica1.pixSeedfillBinary(null, seed, input, 4);
            Pix horizontalLinesEliminatedPix = Leptonica1.pixSubtract(null, input, seedFilled);

            Pix pix2 = Leptonica1.pixOpenBrick(null, horizontalLinesEliminatedPix, 1, kernelLength);
            Pix seed1 = Leptonica1.pixDilateBrick(null, pix2, 1, 3);
            Pix seedFilled1 = Leptonica1.pixSeedfillBinary(null, seed1, horizontalLinesEliminatedPix, 4);
            Pix verticalLinesEliminatedPix = Leptonica1.pixSubtract(null, horizontalLinesEliminatedPix, seedFilled1);

            LeptUtils.dispose(pix1);
            LeptUtils.dispose(seed);
            LeptUtils.dispose(seedFilled);
            LeptUtils.dispose(horizontalLinesEliminatedPix);
            LeptUtils.dispose(pix2);
            LeptUtils.dispose(seed1);
            LeptUtils.dispose(seedFilled1);
            return getDepth1Pix(verticalLinesEliminatedPix);
        }
        return getDepth1Pix(input, 144);
    }

    public static final BufferedImage removeLines(BufferedImage inputBI, int direction, int minLineLengthToBeRetained) {
        if (minLineLengthToBeRetained <= 10) {
            return inputBI;
        }

        Pix inputPix = convertImageToPix(inputBI);
        Pix inputPix1 = getDepth1Pix(inputPix, 128);
        Pix cleanedPix = removeLines(inputPix1, direction, minLineLengthToBeRetained);
        BufferedImage output = convertPixToImage(cleanedPix);
        LeptUtils.dispose(inputPix);
        LeptUtils.dispose(inputPix1);
        LeptUtils.dispose(cleanedPix);
        return output;
    }

    public static final float findSkewAngle(BufferedImage image, int cutOff) {
        FloatBuffer angleFB = FloatBuffer.allocate(1);
        FloatBuffer confFB = FloatBuffer.allocate(1);
        Pix pix = getDepth1Pix(image, cutOff);
        Leptonica1.pixFindSkewOrthogonalRange(pix, angleFB, confFB, 4, 4, 47.0f, 0.30f,
                0.30f, 5.0f);
        float angle = angleFB.get(0);
        LeptUtils.dispose(pix);
        return angle;
    }

    public static final float findSkewAngle(Pix pix, int cutOff) {
        if (pix.d == 1) {
            FloatBuffer angleFB = FloatBuffer.allocate(1);
            FloatBuffer confFB = FloatBuffer.allocate(1);
            Leptonica1.pixFindSkewOrthogonalRange(pix, angleFB, confFB, 4, 4, 47.0f, 0.30f,
                    0.30f, 5.0f);
            float angle = angleFB.get(0);
            return angle;
        }
        Pix pix8 = null;
        Pix pix1 = null;
        if (pix.d != 8) {
            pix8 = Leptonica1.pixConvertTo8(pix, 0);
            pix1 = Leptonica1.pixConvertTo1(pix8, cutOff);
        } else {
            pix1 = Leptonica1.pixConvertTo1(pix, cutOff);
        }
        FloatBuffer angleFB = FloatBuffer.allocate(1);
        FloatBuffer confFB = FloatBuffer.allocate(1);
        Leptonica1.pixFindSkewOrthogonalRange(pix1, angleFB, confFB, 4, 4, 47.0f, 0.30f,
                0.30f, 5.0f);
        float angle = angleFB.get(0);
        LeptUtils.dispose(pix8);
        LeptUtils.dispose(pix1);
        return angle;
    }

    public static final Pix getDepth1Pix(BufferedImage image) {
        return getDepth1Pix(image, 128);
    }

    public static final Pix getDepth1Pix(BufferedImage image, int cutOff) {
        Pix pix32 = convertImageToPix(image);
        Pix pix8 = Leptonica1.pixConvertTo8(pix32, 0);
        Pix pix1 = Leptonica1.pixConvertTo1(pix8, cutOff);
        LeptUtils.dispose(pix32);
        LeptUtils.dispose(pix8);
        return pix1;
    }

    public static final Pix getDepth1Pix(Pix pix) {
        return getDepth1Pix(pix, 128);
    }

    public static final Pix getDepth1Pix(Pix pix, int cutOff) {
        if (pix.d == 1) {
            // cannot return the same pix as it maybe disposed by the user method. Hence, return a copy
            return Leptonica1.pixCopy(null, pix);
        }
        if (pix.d == 8) {
            Pix pix1 = Leptonica1.pixConvertTo1(pix, cutOff);
            return pix1;
        }
        Pix pix8 = Leptonica1.pixConvertTo8(pix, 0);
        Pix pix1 = Leptonica1.pixConvertTo1(pix8, cutOff);
        LeptUtils.dispose(pix8);
        return pix1;
    }

    public static final Pix getDepth8Pix(BufferedImage image) {
        Pix pix32 = convertImageToPix(image);
        Pix pix8 = Leptonica1.pixConvertTo8(pix32, 0);
        LeptUtils.dispose(pix32);
        return pix8;
    }

    public static final Pix getDepth8Pix(Pix pix) {

        if (pix.d == 8) {
            return Leptonica1.pixCopy(null,pix);
        }
        Pix pix8 = Leptonica1.pixConvertTo8(pix, 0);
        return pix8;
    }

    public static BufferedImage closeGrayBI(BufferedImage input, int kernelX, int kernelY) {
        BufferedImage first = morphGrayMinMax(input, kernelX, kernelY, false);
        return morphGrayMinMax(first, kernelX, kernelY, true);
    }

    public static BufferedImage openGrayBI(BufferedImage input, int kernelX, int kernelY) {
        BufferedImage first = morphGrayMinMax(input, kernelX, kernelY, true);
        return morphGrayMinMax(first, kernelX, kernelY, false);
    }

    public static final BufferedImage morphGrayMinMaxWithFilter(BufferedImage input, int kernelX, int kernelY, int cutoff, boolean erode, String avoidBlacksWhites) {

        // System.out.println("Type of image in morphGrayMinMaxWithFilter() is " + input.getType());
        // uses a filter to leave some pixels unchanged
        // if avoidBlacksWhites is "black", leaves blacks unchanged below cutoff
        // if avoidBlacksWhites is "white", leaves whites unchanged above cutoff
        boolean avoidBlacks = false;
        if (BLACK.equals(avoidBlacksWhites)) {
            avoidBlacks = true;
            // System.out.println("Avoiding Blacks");
        }
        kernelX = kernelX % 2 != 0 ? kernelX : kernelX + 1;
        kernelY = kernelY % 2 != 0 ? kernelY : kernelY + 1;
        if ((kernelX <= 0) || (kernelY <= 0)) {
            return input;
        }
        if ((kernelX <= 1) && (kernelY <= 1)) {
            return input;
        }
        input = rectifyBI(input, kernelX, kernelY);
        int[][] pixels = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y) & 0xFF0000FF;
                pixels[y][x] = pixel;
//                if (debug && pixel < 250) {
//                    System.out.print("(y,x)["+y +","+x+"]="+pixel+" ");
//                }
            }
//            if (debug) {
//                System.out.println("");
//            }
        }
        BufferedImage output = copyBI(input);
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            innerloop: for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                if (avoidBlacks) {
                    if (pixels[y][x] <= cutoff) {
                        continue innerloop;
                    }
                } else {
                    if (pixels[y][x] >= cutoff) {
                        continue innerloop;
                    }
                }
                if (erode) {
                    // if the desire is to thicken blacks
                    if (pixels[y][x] <= 32) {
                        // there is no need to further erode (blacken) what is already black !!
                        continue innerloop;
                    }
                } else {
                    // if the desire is to thin blacks
                    if (pixels[y][x] >= 250) {
                        // there is no need to further dilate (whiten) what is already white !!
                        continue innerloop;
                    }
                }
//                if (debug) {
//                    System.out.println("Processing (y,x) [" + y + "," + x + "] as pixel value is " + pixels[y][x]);
//                }
                int newPixel = erode ? 255 : 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = pixels[y + deltaY][x + deltaX];
                        newPixel = erode ? Math.min(pixel, newPixel) : Math.max(pixel, newPixel);
                    }
                }
                newPixel = newPixel & 0xFF;
                int newPixelValue = ARGBPixels[newPixel];
                output.setRGB(x, y, newPixelValue);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage erodeGrayBIWithFilter(BufferedImage input, int kernelX, int kernelY, int cutoff, String avoidBlacksWhites) {
        return morphGrayMinMaxWithFilter(input, kernelX, kernelY, cutoff, true, avoidBlacksWhites);
    }

    public static final BufferedImage dilateGrayBIWithFilter(BufferedImage input, int kernelX, int kernelY, int cutoff, String avoidBlacksWhites) {
        return morphGrayMinMaxWithFilter(input, kernelX, kernelY, cutoff, false, avoidBlacksWhites);
    }

    public static BufferedImage closeGrayBIWithFilter(BufferedImage input, int kernelX, int kernelY, int filter, int cutoff, String avoidBlacksWhites) {
        BufferedImage first = morphGrayMinMaxWithFilter(input, kernelX, kernelY, cutoff, false, avoidBlacksWhites);
        return morphGrayMinMaxWithFilter(first, kernelX, kernelY, 255-cutoff, true, BLACK.equals(avoidBlacksWhites) ? WHITE : BLACK);
    }

    public static BufferedImage openGrayBIWithFilter(BufferedImage input, int kernelX, int kernelY, int cutoff, String avoidBlacksWhites) {
        BufferedImage first = morphGrayMinMaxWithFilter(input, kernelX, kernelY, cutoff, true, avoidBlacksWhites);
        return morphGrayMinMaxWithFilter(first, kernelX, kernelY, 255 - cutoff,false, BLACK.equals(avoidBlacksWhites) ? WHITE : BLACK);
    }

    public static final Pix getDerivativeImage(Pix input, ArrayList<ArrayList<Rectangle>> lines) {
        Pix cleanedPix = Leptonica1.pixCreate(Leptonica1.pixGetWidth(input),
                Leptonica1.pixGetHeight(input), input.d);
        Leptonica1.pixSetBlackOrWhite(cleanedPix, ILeptonica.L_SET_WHITE);

        int numberOfLines = lines.size();
        for (int i = 0; i < numberOfLines; ++i) {
            ArrayList<Rectangle> line = lines.get(i);
            int numberOfWords = line.size();
            for (int j = 0; j < numberOfWords; ++j) {
                Rectangle currentLetter = line.get(j);
                Box aBox = Leptonica1.boxCreate((int) currentLetter.getX(), (int) currentLetter.getY(), (int) currentLetter.getWidth(), (int) currentLetter.getHeight());
                Pix pixOriginalX = Leptonica1.pixClipRectangle(input, aBox, null);
                Leptonica1.pixRasterop(cleanedPix, aBox.x, aBox.y,
                        aBox.w, aBox.h,
                        ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
                LeptUtils.dispose(aBox);
                LeptUtils.dispose(pixOriginalX);
            }
        }
        return cleanedPix;
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines) {
        return getDerivativeImages(input, lines, null, LEAVE_UNCHANGED, OPTIMAL_HEIGHT_WIDTH_RATIO_FOR_CHARACTERS);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, int erodeOrDilateImages) {
        return getDerivativeImages(input, lines, null, erodeOrDilateImages, OPTIMAL_HEIGHT_WIDTH_RATIO_FOR_CHARACTERS);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, double[] desiredScalingFactors) {
        return getDerivativeImages(input, lines, desiredScalingFactors, LEAVE_UNCHANGED, OPTIMAL_HEIGHT_WIDTH_RATIO_FOR_CHARACTERS);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, int erodeOrDilateImages, double desiredHeightWidthRatio) {
        return getDerivativeImages(input, lines, null, erodeOrDilateImages, desiredHeightWidthRatio);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, double[] desiredScalingFactors, int erodeOrDilateImages) {
        return getDerivativeImages(input, lines, desiredScalingFactors, erodeOrDilateImages, OPTIMAL_HEIGHT_WIDTH_RATIO_FOR_CHARACTERS);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, double[] desiredScalingFactors, double desiredHeightWidthRatio) {
        return getDerivativeImages(input, lines, desiredScalingFactors, LEAVE_UNCHANGED, desiredHeightWidthRatio);
    }

    public static final Pixa getDerivativeImages(Pix input, ArrayList<ArrayList<Rectangle>> lines, double[] desiredScalingFactors, int erodeOrDilateImages, double desiredHeightWidthRatio) {

        // desiredScalingFactors : the scaling factors for the various image variants
        // length of desiredScalingFactors = length of erodeOrDilateImages

        // erodeOrDilateImages : -1 to dilate, 0 to leave unchanged, 1 to erode
        // dilate = blacken i.e thicken black lines ;
        // erode = whiten i.e. thin black lines

        int bestHeight = 28; // in pixels
        int numberOfLines = lines.size();
        Pixa pixArray = Leptonica1.pixaCreate(numberOfLines);
        final int boundary = 10;
        if (desiredScalingFactors == null) {
            desiredScalingFactors = new double[] {1.0};
        }
        if ((desiredScalingFactors != null) && (desiredScalingFactors.length == 0)) {
            return pixArray;
        }
        // System.out.println("Scaling Factors = " + Arrays.toString(desiredScalingFactors));
        // System.out.println("Erode / Dilate Factors = " + erodeOrDilateImages);

        for (int i = 0; i < numberOfLines; ++i) {
            ArrayList<Rectangle> line = lines.get(i);
            int numberOfWords = line.size();
            if (numberOfWords == 0) {
                for (int j = 0; j < desiredScalingFactors.length; ++j) {
                    Leptonica1.pixaAddPix(pixArray,emptyPix,ILeptonica.L_COPY);
                }
                continue;
            }
            ArrayList<Integer> hStats = new ArrayList<>(numberOfWords);
            ArrayList<Integer> wStats = new ArrayList<>(numberOfWords);
            int startX = Integer.MAX_VALUE;
            int startY = Integer.MAX_VALUE;
            int endX = Integer.MIN_VALUE;
            int endY = Integer.MIN_VALUE;
            for (int j = 0;j < numberOfWords; ++j) {
                Rectangle word = line.get(j);
                hStats.add((int) word.getHeight());
                wStats.add((int) word.getWidth());
                startX = Math.min(startX, (int) word.getX());
                startY = Math.min(startY, (int) word.getY());
                endX = Math.max(endX, (int) word.getX() + (int) word.getWidth());
                endY = Math.max(endY, (int) word.getY() + (int) word.getHeight());
            }
            int[] heights = hStats.stream().mapToInt(k -> k).toArray();
            int[] widths = wStats.stream().mapToInt(k -> k).toArray();
            Arrays.sort(heights);
            Arrays.sort(widths);
            int maxHeight = heights[heights.length - 1];
            int maxWidth = widths[widths.length - 1];
            double medianHeight = heights.length % 2 == 1 ? heights[heights.length / 2] : (heights[heights.length / 2] + heights[heights.length / 2 -1]) * 1.0 / 2;
            double medianWidth = widths.length % 2 == 1 ? widths[widths.length / 2] : (widths[widths.length / 2] + widths[widths.length / 2 -1]) * 1.0 / 2;
            if ((maxHeight < 7) || (maxWidth < 7)) {
                for (int j = 0; j < desiredScalingFactors.length; ++j) {
                    Leptonica1.pixaAddPix(pixArray,emptyPix,ILeptonica.L_COPY);
                }
                continue;
            }
            double heightScalingFactor = bestHeight * 1.0 / medianHeight;
            logger.debug("heightScalingFactor = " + heightScalingFactor);
            // double widthScalingFactor = ((medianHeight / medianWidth) / desiredHeightWidthRatio) * heightScalingFactor;
            double widthScalingFactor = (bestHeight * 1.0) / (medianWidth * desiredHeightWidthRatio);
            logger.debug("widthScalingFactor = " + widthScalingFactor);
            Pix cleanPix = Leptonica1.pixCreate(endX - startX + (2 * boundary),
                    endY - startY + (2 * boundary), input.d);
            Leptonica1.pixSetBlackOrWhite(cleanPix, ILeptonica.L_SET_WHITE);
            Box aBox = Leptonica1.boxCreate(startX, startY, endX - startX, endY - startY);
            Pix pixOriginalX = Leptonica1.pixClipRectangle(input, aBox, null);
            Leptonica1.pixRasterop(cleanPix, boundary, boundary,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            Pix pixOriginalX_Scaled = Leptonica1.pixScaleGeneral(cleanPix, (float) widthScalingFactor, (float) heightScalingFactor,
                    0.0f, 1);
            // Leptonica1.pixWrite(Processor.debugDirectory + "/" + "pix-" + i + ".png", pixOriginalX_Scaled, ILeptonica.IFF_PNG);
            for (int j = 0; j < desiredScalingFactors.length; ++j) {
                int index = j + i*desiredScalingFactors.length;
                float scalingFactor = (float) desiredScalingFactors[j];
                Pix scaledPix = Leptonica1.pixScaleGeneral(pixOriginalX_Scaled, scalingFactor, scalingFactor,
                        0.0f, 1);
                // Leptonica1.pixWrite(Processor.debugDirectory + "/" + "scaled-" + index + ".png", scaledPix, ILeptonica.IFF_PNG);
                Pix morphologicalPix = null;
                if (erodeOrDilateImages == LEAVE_UNCHANGED) {
                    morphologicalPix = Leptonica1.pixCopy(null, scaledPix);
                } else {
                    if (erodeOrDilateImages <= DILATE) {
                        if (scaledPix.d == 1) {
                            morphologicalPix = Leptonica1.pixErodeBrick(null,scaledPix,3,3);
                        } else {
                            Pix mPix8 = Leptonica1.pixConvertTo8(scaledPix,0);
                            morphologicalPix = Leptonica1.pixDilateGray(mPix8,3,3);
                            LeptUtils.dispose(mPix8);
                        }
                    } else {
                        if (scaledPix.d == 1) {
                            morphologicalPix = Leptonica1.pixDilateBrick(null,scaledPix,3,3);
                        } else {
                            Pix mPix8 = Leptonica1.pixConvertTo8(scaledPix,0);
                            morphologicalPix = Leptonica1.pixErodeGray(mPix8,3,3);
                            LeptUtils.dispose(mPix8);
                        }
                    }
                }
                // Leptonica1.pixWrite(Processor.debugDirectory + "/" + "morphology-" + index + ".png", morphologicalPix, ILeptonica.IFF_PNG);
                Leptonica1.pixaAddPix(pixArray,morphologicalPix,ILeptonica.L_COPY);
                // System.out.println("Added a pix");
                LeptUtils.dispose(scaledPix);
                LeptUtils.dispose(morphologicalPix);
            }

            LeptUtils.dispose(pixOriginalX_Scaled);
            LeptUtils.dispose(pixOriginalX);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(cleanPix);
        }
        return pixArray;
    }

    public static final void printPixArray(Pixa pixArray) {
        printPixArray(pixArray, null, null, TraceLevel.FATAL);
    }

    public static final void printPixArray(Pixa pixArray, String directory, int runCounter, TraceLevel traceLevel) {
        printPixArray(pixArray, directory, runCounter, 0, traceLevel);
    }

    public static final void printPixArray(Pixa pixArray, String directory, int runCounter, int threadNumber, TraceLevel traceLevel) {
        printPixArray(pixArray, directory, runCounter + " - " + threadNumber + " - ", traceLevel);
    }

    public static final void printPixArray(Pixa pixArray, String directory, String filePrefix, TraceLevel traceLevel) {
        if (pixArray == null) {
            return;
        }
        if (traceLevel.getValue() < BaseImageProcessor.getImageDebugLevel().getValue()) {
            return;
        }
        int numberOfImages = Leptonica1.pixaGetCount(pixArray);
        if (numberOfImages == 0) {
            return;
        }
        // System.out.println("Number of Images = " + numberOfImages);
        if ((directory == null) || (EMPTY_STRING.equals(directory))) {
            directory = System.getProperty("user.dir");
            Path dir = Paths.get(directory);
            directory = dir.toAbsolutePath().toString();
        }
        if ((filePrefix == null) || (EMPTY_STRING.equals(filePrefix))) {
            filePrefix = "temp";
        }
        for (int i = 0; i < numberOfImages; ++i) {
            Pix pix = Leptonica1.pixaGetPix(pixArray, i, ILeptonica.L_COPY);
            Leptonica1.pixWrite(directory + "/" + filePrefix + " - " + i + ".png", pix, ILeptonica.IFF_PNG);
            LeptUtils.dispose(pix);
        }
    }

    public static final BufferedImage getGrayBI(BufferedImage image) {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clone;
    }

    public static final BufferedImage getBinaryBI(BufferedImage image) {
        BufferedImage clone1 = getGrayBI(image);

        BufferedImage clone2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = clone2.createGraphics();
        g2d.drawImage(clone1, 0, 0, null);
        g2d.dispose();
        return clone2;
    }

    public static final BufferedImage findAndHighlightSmallCircles(BufferedImage input, int kernelSide, int targetPixelValue, double targetRatio) {

        BufferedImage input1 = copyBI(input);
        if (kernelSide < 11) {
            kernelSide = 11;
        }
        if (kernelSide % 2 == 0) {
            kernelSide = kernelSide + 1;
        }
        if (targetPixelValue > 8) {
            targetPixelValue = 8;
        }

        // Ideally :
        // divide the kernel into 3-4 parts, starting from the inside and calculate the
        // average of the pixels in each part

        // The outermost layer is 2 pixels thick
        // The one inside is also 2 pixels thick
        // The inner ones are 3/5 pixels thick (1 single box, if there are no more than 3 or 5 pixels left) or
        // if there are 7 or more pixels (say, x), then a 2 pixel thick layer, followed by a square of side (x - 2) pixels

        // In this case, assuming there are only 3 layers.
        // Maybe, it will work out fine in most cases

        boolean fourLayers = (kernelSide >= 15); // kept, but not needed in current implementation

        int target = 0xFF000000 | (targetPixelValue << 16) | (targetPixelValue << 8) | targetPixelValue;
        int whitePixel = 0xFFFFFFFF;
        BufferedImage gray = getGrayBI(input1);
        BufferedImage output = copy(input1);
        int[][] pixels = new int[gray.getHeight()][gray.getWidth()];
        for (int y = kernelSide / 2; y < input.getHeight() - kernelSide / 2; ++y) {
            for (int x = kernelSide / 2; x < input.getWidth() - kernelSide / 2; ++x) {
                int pixel = gray.getRGB(x,y) & 0xFF;
                pixels[y][x] = pixel;
            }
        }
        int circlesFound = 0;
        for (int y = kernelSide / 2; y < input.getHeight() - kernelSide / 2; ++y) {
            innerloop: for (int x = kernelSide / 2; x < input.getWidth() - kernelSide / 2; ++x) {

                if (pixels[y][x] == targetPixelValue) {
                    continue innerloop;
                }
                if (pixels[y][x] == 255) {
                    continue innerloop;
                }
                int cPixel = input1.getRGB(x,y);
                int red = (cPixel >> 16) & 0xFF;
                int green = (cPixel >> 8) & 0xFF;
                int blue = cPixel & 0xFF;
                // if green pixel, skip this pixel
                if ((blue <= (green + 20)) && ((green - red) >= 30)) {
                    continue innerloop;
                }

                // if there are 3 layers
                // calculate average of outermost layer of 2 pixels
                int layer1Total = 0;
                int layer1NoOfPixels = 0;
                for (int dy = -kernelSide/2 ; dy < -kernelSide / 2 + 2; ++dy) {
                    for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer1Total += pixels[y+dy][x+dx];
                        ++layer1NoOfPixels;
                    }
                }
                for (int dy = kernelSide/2 ; dy > kernelSide / 2 - 2; --dy) {
                    for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer1Total += pixels[y+dy][x+dx];
                        ++layer1NoOfPixels;
                    }
                }
                for (int dy = -kernelSide/2 + 2 ; dy <= kernelSide / 2 - 2; ++dy) {
                    for (int dx = -kernelSide / 2; dx < - kernelSide / 2 + 2; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer1Total += pixels[y+dy][x+dx];
                        ++layer1NoOfPixels;
                    }
                }
                for (int dy = -kernelSide/2 + 2 ; dy <= kernelSide / 2 - 2; ++dy) {
                    for (int dx = kernelSide / 2; dx > kernelSide / 2 - 2; --dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer1Total += pixels[y+dy][x+dx];
                        ++layer1NoOfPixels;
                    }
                }
                double layer1Average = (layer1Total * 1.0) / layer1NoOfPixels;

                // calculate average for the next layer
                int layer2Total = 0;
                int layer2NoOfPixels = 0;
                for (int dy = -kernelSide/2 + 2 ; dy < -kernelSide / 2 + 4; ++dy) {
                    for (int dx = -kernelSide / 2 + 2; dx <= kernelSide / 2 - 2; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer2Total += pixels[y+dy][x+dx];
                        ++layer2NoOfPixels;
                    }
                }
                for (int dy = kernelSide/2 - 2 ; dy > kernelSide / 2 - 4; --dy) {
                    for (int dx = -kernelSide / 2 + 2; dx <= kernelSide / 2 - 2; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer2Total += pixels[y+dy][x+dx];
                        ++layer2NoOfPixels;
                    }
                }
                for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                    for (int dx = -kernelSide / 2 + 2; dx < -kernelSide / 2 + 4; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer2Total += pixels[y+dy][x+dx];
                        ++layer2NoOfPixels;
                    }
                }
                for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                    for (int dx = kernelSide / 2 - 2; dx > kernelSide / 2 - 4; --dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer2Total += pixels[y+dy][x+dx];
                        ++layer2NoOfPixels;
                    }
                }
                double layer2Average = (layer2Total * 1.0) / layer2NoOfPixels;

                // calculate average for the 3rd and last layer
                int layer3Total = 0;
                int layer3NoOfPixels = 0;
                for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                    for (int dx = -kernelSide / 2 + 4; dx <= kernelSide / 2 - 4; ++dx) {
                        if (pixels[y+dy][x+dx] == targetPixelValue) {
                            continue innerloop;
                        }
                        if (pixels[y+dy][x+dx] == 255) {
                            continue innerloop;
                        }
                        layer3Total += pixels[y+dy][x+dx];
                        ++layer3NoOfPixels;
                    }
                }
                double layer3Average = (layer3Total * 1.0) / layer3NoOfPixels;
                double layer1And2Average = (layer1Total + layer2Total) * 1.0 /(layer1NoOfPixels + layer2NoOfPixels);

                double ratio1 = layer1Average / layer3Average;
                double ratio2 = layer1And2Average / layer3Average;
                double ratio3 = layer2Average / layer3Average;
                double ratio4 = layer2Average / layer3Average;

                double cutoff = targetRatio;

                boolean circleFound = false;
                // boolean darkCircle = false;
                if ((ratio1 > cutoff) || (ratio2 > cutoff) || (ratio3 > cutoff)) {
                    circleFound = true;
                    // darkCircle = true;
                }
//                if ((ratio1 < 1/cutoff) || (ratio2 < 1 / cutoff) || (ratio3 < 1 / cutoff)) {
//                    circleFound = true;
//                    darkCircle = false;
//                }

                if (circleFound) {
                    ++circlesFound;
                    System.out.println("Circle found at [" + x + "," + y + "]");
                    // change the circle pixels to the target pixel and
                    // useGammaEnhancement to change the values of all other cell
                    for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                        for (int dx = -kernelSide / 2 + 4; dx <= kernelSide / 2 - 4; ++dx) {
                            output.setRGB(x + dx,y + dy,target);
                            pixels[y+dy][x+dx] = targetPixelValue;
                        }
                    }

/*

                    if (ratio4 > cutoff) {
                        // lighten all other cells
                        double ratio = Math.max(ratio1, ratio4);
                        for (int dy = -kernelSide/2 ; dy < -kernelSide / 2 + 4; ++dy) {
                            for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
//                                double gamma = Math.min(ratio, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = kernelSide/2 ; dy > kernelSide / 2 - 4; --dy) {
                            for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
//                                double gamma = Math.min(ratio, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                            for (int dx = -kernelSide / 2; dx < -kernelSide / 2 + 4; ++dx) {
//                                double gamma = Math.min(ratio, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                            for (int dx = kernelSide / 2; dx > kernelSide / 2 - 4; --dx) {
//                                double gamma = Math.min(ratio, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                    } else {
                        // lighten cells of outermost layer
                        for (int dy = -kernelSide/2 ; dy < -kernelSide / 2 + 2; ++dy) {
                            for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
//                                double gamma = Math.min(ratio1, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = kernelSide/2 ; dy > kernelSide / 2 - 2; --dy) {
                            for (int dx = -kernelSide / 2; dx <= kernelSide / 2; ++dx) {
//                                double gamma = Math.min(ratio1, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = -kernelSide/2 + 2 ; dy <= kernelSide / 2 - 2; ++dy) {
                            for (int dx = -kernelSide / 2; dx < - kernelSide / 2 + 2; ++dx) {
//                                double gamma = Math.min(ratio1, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        for (int dy = -kernelSide/2 + 2 ; dy <= kernelSide / 2 - 2; ++dy) {
                            for (int dx = kernelSide / 2; dx > kernelSide / 2 - 2; --dx) {
//                                double gamma = Math.min(ratio1, 7.0);
//                                Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
//                                double[] multipliers = gammaMultipliersMap.get(gammaInt);
//                                int newPixel = (int) (255 * multipliers[pixels[y+dy][x+dx]]);
//                                output.setRGB(x + dx, y + dy, 0xFF000000 | (newPixel << 16) | (newPixel << 8) | newPixel);
                                output.setRGB(x+dx,y+dy,whitePixel);
                                pixels[y+dy][x+dx] = 255;
                            }
                        }
                        // darken cells of layer 2
                        for (int dy = -kernelSide/2 + 2 ; dy < -kernelSide / 2 + 4; ++dy) {
                            for (int dx = -kernelSide / 2 + 2; dx <= kernelSide / 2 - 2; ++dx) {
                                output.setRGB(x + dx,y + dy,target);
                                pixels[y+dy][x+dx] = targetPixelValue;
                            }
                        }
                        for (int dy = kernelSide/2 - 2 ; dy > kernelSide / 2 - 4; --dy) {
                            for (int dx = -kernelSide / 2 + 2; dx <= kernelSide / 2 - 2; ++dx) {
                                output.setRGB(x + dx,y + dy,target);
                                pixels[y+dy][x+dx] = targetPixelValue;
                            }
                        }
                        for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                            for (int dx = -kernelSide / 2 + 2; dx < -kernelSide / 2 + 4; ++dx) {
                                output.setRGB(x + dx,y + dy,target);
                                pixels[y+dy][x+dx] = targetPixelValue;
                            }
                        }
                        for (int dy = -kernelSide/2 + 4 ; dy <= kernelSide / 2 - 4; ++dy) {
                            for (int dx = kernelSide / 2 - 2; dx > kernelSide / 2 - 4; --dx) {
                                output.setRGB(x + dx,y + dy,target);
                                pixels[y+dy][x+dx] = targetPixelValue;
                            }
                        }
                    }
*/
                }
            }
        }
        System.out.println("Number of circles found = " + circlesFound);
        return output;
    }

    public static BufferedImage getBufferedImage(byte[] imageBytes) {
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage image = null;
        try {
            image = ImageIO.read(is);
        } catch (Exception e) {
            return EMPTY_IMAGE;
        }
        return image;
    }

    // doesn't work ; need to figure out what to change
    public static final BufferedImage normaliseBackground(BufferedImage input1, int kernelX, int kernelY,
                                                                int targetBackgroundValue, int tolerance) {
        int targetPixelValue = 0xFF000000 | (targetBackgroundValue << 16) | (targetBackgroundValue << 8) | (targetBackgroundValue);
        // tolerance = Math.min(tolerance, 0.3);
        tolerance = Math.min(tolerance, 50);
        tolerance = Math.max(0, tolerance);
        kernelX = Math.min(Math.max(1, kernelX), 31);
        kernelY = Math.min(Math.max(1, kernelY), 31);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        kernelX = Math.max(11, kernelX);
        kernelY = Math.max(11, kernelY);
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);

        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] pixelValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
                pixelValues[y][x] = pix;
            }
        }
        int kernelSize = kernelX * kernelY;
        int medianIndex = kernelSize / 2;
        int[] pixelArray = new int[kernelSize];
        int total = 0;
        int aboveTargetCount = 0;
        int average = 1;
        double ratio = 0.0;
        int median = 0;
        int newPixel = 0;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > targetBackgroundValue) {
                    continue;
                }
                total = 0;
                aboveTargetCount = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel > targetBackgroundValue) {
                            ++aboveTargetCount;
                        }
                        total += pixel;
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                if (aboveTargetCount < kernelSize * 2.0 / kernelY) {
                    continue;
                }
                average = Math.max(total / kernelSize, 1);
                if (average > targetBackgroundValue) {
                    continue;
                }
                Arrays.sort(pixelArray);
                median = Math.max(pixelArray[medianIndex], 1);
                if (median > targetBackgroundValue) {
                    continue;
                }
                if (median > average) {
                    continue;
                }
                // System.out.print("Current : " + currentPixel + ", Average : " + average + ", Median : " + median);
                if (average > median + 5) {
                    // System.out.println("");
                    continue;
                }
                // System.out.println(" - > Changed");
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel < average) {
                            pixelValues[y + deltaY][x + deltaX] = targetPixelValue;
                        }
                    }
                }
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                output.setRGB(x, y, pixelValues[y][x]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage eliminateNonCharactersInNearStraightLineImages(BufferedImage input, int kernelX) {
        input = rectifyBI(input);
        int targetWhite = 0xFFFFFFFF;
        kernelX = Math.min(Math.max(1, kernelX), 21);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        kernelX = Math.max(13, kernelX);

        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] pixelValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
                pixelValues[y][x] = pix;
            }
        }
        int total = 0;
        int aboveTargetCount = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > 200) {
                    continue;
                }
                aboveTargetCount = 0;
                for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                    int pixel = grayValues[y][x + deltaX];
                    if (pixel > 200) {
                        ++aboveTargetCount;
                    }
                    total += pixel;
                }
                if (aboveTargetCount > kernelX * 0.1) {
                    continue;
                }
                // System.out.print("Current : " + currentPixel + ", Average : " + average + ", Median : " + median);
                // System.out.println(" - > Changed");
                for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                    pixelValues[y][x + deltaX] = targetWhite;
                }
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                output.setRGB(x, y, pixelValues[y][x]);
            }
        }
        return output;
    }

    public static final BufferedImage lightenCentreCellsYAxis(BufferedImage input1, int kernelX, int kernelY) {

        int targetWhite = 0xFFFFFFFF;
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        kernelX = Math.min(Math.max(1, kernelX), 7);

        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        kernelY = Math.min(Math.max(3, kernelY), 9);
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);

        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int totalAbove = 0;
        int totalBelow = 0;
        final int cellsAbove = (kernelY / 2) * kernelX;
        final int cellsBelow = (kernelY / 2) * kernelX;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                totalAbove = 0;
                totalBelow = 0;
                for (int deltaY = -kernelY / 2; deltaY < 0; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {

                        int pixel = grayValues[y + deltaY][x + deltaX];
                        totalAbove += pixel;
                    }
                }
                for (int deltaY = 1; deltaY <= kernelY / 2 ; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        totalBelow += pixel;
                    }
                }
                int averageAbove = totalAbove / cellsAbove;
                int averageBelow = totalBelow / cellsBelow;
                if ((currentPixel > averageAbove) && (currentPixel > averageBelow)) {
                    // lighten cell
                    double gamma = (currentPixel * 1.0 / Math.min(averageAbove, averageBelow));
                    gamma = gamma * gamma * gamma;
                    gamma = Math.min(gamma, 7.0);
                    Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                    double[] multipliers = gammaMultipliersMap.get(gammaInt);
                    int newPixel = (int) (255 * multipliers[currentPixel]);
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);

    }

    public static final BufferedImage lightenCentreCellsXAxis(BufferedImage input1, int kernelX, int kernelY) {

        int targetWhite = 0xFFFFFFFF;
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX + 1);
        }
        kernelX = Math.min(Math.max(3, kernelX), 9);

        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY + 1);
        }
        kernelY = Math.min(Math.max(1, kernelY), 7);
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);

        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int totalLeft = 0;
        int totalRight = 0;
        final int cellsLeft = (kernelX / 2) * kernelY;
        final int cellsRight = (kernelX / 2) * kernelY;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                totalLeft = 0;
                totalRight = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX < 0; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        totalLeft += pixel;
                    }
                }
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2 ; ++deltaY) {
                    for (int deltaX = 1; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        totalRight += pixel;
                    }
                }
                int averageLeft = totalLeft / cellsLeft;
                int averageRight = totalRight / cellsRight;
                if ((currentPixel > averageLeft) && (currentPixel > averageRight)) {
                    // lighten cell
                    double gamma = (currentPixel * 1.0 / Math.min(averageLeft, averageRight));
                    gamma = gamma * gamma * gamma;
                    gamma = Math.min(gamma, 7.0);
                    Integer gammaInt = Integer.valueOf((int) Math.round(gamma / 0.05));
                    double[] multipliers = gammaMultipliersMap.get(gammaInt);
                    int newPixel = (int) (255 * multipliers[currentPixel]);
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);

    }

    public static final BufferedImage harmonicFilter(BufferedImage input1, int kernelX, int kernelY, int harmonicOrder, int weightPower, int whiteCutoff) {
        // harmonic order can be from -4 to 4
        // int weightPower can be 1, 2, or 4
        if (harmonicOrder < -4) {
            harmonicOrder = -4;
        }
        if (harmonicOrder > 4) {
            harmonicOrder = 4;
        }
        if (harmonicOrder == 0) {
            return rectifyBI(input1);
        }

        if (weightPower < 1) {
            weightPower = 1;
        }
        if (weightPower > 3) {
            weightPower = 3;
        }

        int targetWhite = 0xFFFFFFFF;
        if (kernelX % 2 == 0) {
            kernelX = kernelX + 1;
        }
        kernelX = Math.min(Math.max(3, kernelX), input1.getWidth() / 4);

        if (kernelY % 2 == 0) {
            kernelY = kernelY + 1;
        }
        kernelY = Math.min(Math.max(3, kernelY), input1.getHeight() / 4);
        int radius = Math.max(kernelX / 2, kernelY / 2) + 1;

        BufferedImage input = rectifyBI(input1, kernelX, kernelY);

        double[] denominator = inverse;
        double[] numerator = zeroth;

        if (harmonicOrder == -4) {
            denominator = inverseFourth;
            numerator = inverseCubed;
        }
        if (harmonicOrder == -3) {
            denominator = inverseCubed;
            numerator = inverseSquared;
        }
        if (harmonicOrder == -2) {
            denominator = inverseSquared;
            numerator = inverse;
        }
        if (harmonicOrder == -1) {
            denominator = inverse;
            numerator = zeroth;
        }
        if (harmonicOrder == 1) {
            denominator = same;
            numerator = zeroth;
        }
        if (harmonicOrder == 2) {
            denominator = squared;
            numerator = same;
        }
        if (harmonicOrder == 3) {
            denominator = cubed;
            numerator = squared;
        }
        if (harmonicOrder == 4) {
            denominator = fourthPower;
            numerator = cubed;
        }

        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                double denominatorTotal = 0.0;
                double numeratorTotal = 0.0;
                int totalPixels = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        if (pixel >= whiteCutoff) {
                            continue;
                        }
                        int where = Math.max(Math.abs(deltaX), Math.abs(deltaY));
                        int weight = radius - where;
                        if (weightPower == 2) {
                            weight = weight * weight;
                        }
                        if (weightPower == 3) {
                            weight = weight * weight * weight;
                        }
                        numeratorTotal += weight * numerator[pixel];
                        denominatorTotal += weight * denominator[pixel];
                        ++totalPixels;
                    }
                }
                if (totalPixels < 5) {
                    continue;
                }
                int newPixel = Math.max(0,Math.min((int) ((numeratorTotal * 1.0)/ denominatorTotal), 255));
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage binarize(BufferedImage input, double cutOff) {
        input = rectifyBI(input);
        int targetWhite = 0xFFFFFFFF;
        int targetBlack = 0xFF000000;
        BufferedImage output = copyBI(input);
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                if ((currentPixel == 255) || (currentPixel == 0)) {
                    continue;
                }
                if (currentPixel <= cutOff) {
                    output.setRGB(x, y, targetBlack);
                } else {
                    output.setRGB(x, y, targetWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage invert(BufferedImage input) {
        input = rectifyBI(input);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                currentPixel = 255 - currentPixel;
                output.setRGB(x, y, ARGBPixels[currentPixel]);
            }
        }
        return output;
    }

    public static Pix removeSaltPepper(Pix pixs) {
        Pix pix1 = null, pix2 = null, pix3 = null, pix4 = null, pix5 = null; // , pix6 = null, pix7 = null, pix8 = null,
        Sel sel1 = null, sel2 = null, sel3 = null, sel4 = null, sel5 = null; // , sel6 = null, sel7 = null, sel8 = null;

        String selString1 = "ooooCoooo";
        String selString2 = "ooooo Coo  ooooo";
        String selString3 = "oooooo   oo C oo   oooooo";
        String selString4 = "oooooooo     oo     oo  C  oo     oo     oooooooo";

        if (Leptonica.INSTANCE.pixGetDepth(pixs) != 1) {
            // 232 = SBImageUtils.GREY_REPLACEMENT_PIXEL
            pix1 = Leptonica1.pixConvertTo1(pixs, 232 - 80);
        } else {
            pix1 = Leptonica1.pixCopy(null, pixs);
        }

        sel1 = Leptonica1.selCreateFromString(selString1, 3, 3, "saltAndPepper1");
        sel2 = Leptonica1.selCreateFromString(selString2, 4, 4, "saltAndPepper2");
        sel3 = Leptonica1.selCreateFromString(selString3, 5, 5, "saltAndPepper3");
        sel4 = Leptonica1.selCreateBrick(2, 2, 0, 0, ILeptonica.SEL_HIT);
        sel5 = Leptonica1.selCreateFromString(selString4, 7, 7, "saltAndPepper4");

        pix2 = Leptonica1.pixHMT(null, pix1, sel1.getPointer());
        Pix pix21 = Leptonica1.pixDilate(null, pix2, sel4.getPointer());
        Pix pix22 = Leptonica1.pixSubtract(null, pix1, pix21);
        LeptUtils.dispose(pix1);
        LeptUtils.dispose(pix2);
        LeptUtils.dispose(pix21);

        pix3 = Leptonica1.pixHMT(null, pix22, sel2.getPointer());
        Pix pix31 = Leptonica1.pixDilate(null, pix3, sel4.getPointer());
        Pix pix32 = Leptonica1.pixSubtract(null, pix22, pix31);
        LeptUtils.dispose(pix22);
        LeptUtils.dispose(pix3);
        LeptUtils.dispose(pix31);

        pix4 = Leptonica1.pixHMT(null, pix32, sel3.getPointer());
        Pix pix41 = Leptonica1.pixDilate(null, pix4, sel4.getPointer());
        Pix pix42 = Leptonica1.pixSubtract(null, pix32, pix41);
        LeptUtils.dispose(pix32);
        LeptUtils.dispose(pix4);
        LeptUtils.dispose(pix41);

        pix5 = Leptonica1.pixHMT(null, pix42, sel5.getPointer());
        Pix pix51 = Leptonica1.pixDilate(null, pix5, sel4.getPointer());
        Pix pix52 = Leptonica1.pixSubtract(null, pix42, pix51);
        LeptUtils.dispose(pix42);
        LeptUtils.dispose(pix5);
        LeptUtils.dispose(pix51);

        LeptUtils.dispose(sel1);
        LeptUtils.dispose(sel2);
        LeptUtils.dispose(sel3);
        LeptUtils.dispose(sel4);
        LeptUtils.dispose(sel5);

        return pix52;
    }


    public static void runAndWait(final Runnable run) {
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(new Runnable() {
            public void run() {
                try {
                    run.run();
                } finally {
                    doneLatch.countDown();
                }
            }
        });
        try {
            doneLatch.await();
        } catch (InterruptedException ie) {

        }
    }

    public static final BufferedImage drawRectangle(BufferedImage input, Rectangle aRectangle, Color color) {
        BufferedImage copy = copyBI(input);
        Graphics2D g = copy.createGraphics();
        g.setColor(color);
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect((int) aRectangle.getX(), (int) aRectangle.getY(), (int) aRectangle.getWidth(), (int) aRectangle.getWidth(), 3,3);
        g.dispose();
        return copy;
    }

    public static final BufferedImage eliminateDiagonalBlacks(BufferedImage input, int kernelSize) {
        input = rectifyBI(input);
        int targetWhite = 0xFFFFFFFF;
        if (kernelSize % 2 == 0) {
            kernelSize = Math.max(1, kernelSize + 1);
        }
        kernelSize = Math.min(Math.max(3, kernelSize), 5);

        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelSize / 2; y < input.getHeight() - kernelSize / 2; ++y) {
            innerloop : for (int x = kernelSize / 2; x < input.getWidth() - kernelSize / 2; ++x) {
                int currentPixel = grayValues[y][x];
                if (grayValues[y][x] > 128) {
                    continue innerloop;
                }
                boolean topDownDiagonal = true;
                for (int delta = -kernelSize / 2; delta < 0; ++delta) {
                    topDownDiagonal = topDownDiagonal && (grayValues[y+delta][x+delta] > 128);
                }
                for (int delta = 1; delta <= kernelSize / 2; ++delta) {
                    topDownDiagonal = topDownDiagonal && (grayValues[y+delta][x+delta] > 128);
                }
                boolean leftRightDiagonal = true;
                for (int delta = -kernelSize / 2; delta < 0; ++delta) {
                    leftRightDiagonal = leftRightDiagonal && (grayValues[y+delta][x-delta] > 128);
                }
                for (int delta = 1; delta <= kernelSize / 2; ++delta) {
                    leftRightDiagonal = leftRightDiagonal && (grayValues[y-delta][x+delta] > 128);
                }
                if (topDownDiagonal || leftRightDiagonal) {
                    output.setRGB(x, y, targetWhite);
                }
            }
        }
        return output;
    }

    public static final BufferedImage cleanThenJoinDisjointedBridges(BufferedImage input) {
        // Used to ensure that pixConnComp works properly on
        // characters with thin diagonals

        // First, clean with a few cleaning Kernels

        // Takes a 2 x 2 kernel
        // If NE and SW corners are black, while NW and SE corners are white,
        // it makes the SE corner black
        // Likewise, if NW and SE corners are black while NE and SW corners are white,
        // it makes the NE corner black

        ArrayList<CleaningKernel> cleaningKernels = new ArrayList<>();
        cleaningKernels.add(new CleaningKernel(3,3));
        cleaningKernels.add(new CleaningKernel(3,5));
        cleaningKernels.add(new CleaningKernel(5,3));
        cleaningKernels.add(new CleaningKernel(7,3));
        cleaningKernels.add(new CleaningKernel(3,7));
        cleaningKernels.add(new CleaningKernel(9,5));
        cleaningKernels.add(new CleaningKernel(9,7));
        cleaningKernels.add(new CleaningKernel(11,5));
        cleaningKernels.add(new CleaningKernel(11,7));
        cleaningKernels.add(new CleaningKernel(13,5));
//        cleaningKernels.add(new CleaningKernel(13,7));
//        cleaningKernels.add(new CleaningKernel(15,5));
//        cleaningKernels.add(new CleaningKernel(15,7));
//        cleaningKernels.add(new CleaningKernel(17,5));
//        cleaningKernels.add(new CleaningKernel(17,7));
//        cleaningKernels.add(new CleaningKernel(19,5));
//        cleaningKernels.add(new CleaningKernel(19,7));
//        cleaningKernels.add(new CleaningKernel(19,9));
        input = removeSmallTrails(input, cleaningKernels, true);
        BufferedImage output = joinDisjointedBridges(input);
        return output;
    }

    public static final BufferedImage joinDisjointedBridges(BufferedImage input) {
        // Used to ensure that pixConnComp works properly on
        // characters with thin diagonals

        // Takes a 2 x 2 kernel
        // If NE and SW corners are black, while NW and SE corners are white,
        // it makes the SE corner black
        // Likewise, if NW and SE corners are black while NE and SW corners are white,
        // it makes the NE corner black
        input = rectifyBI(input);
        int targetBlack = 0xFF000000;
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = 0; y < input.getHeight() - 1; ++y) {
            for (int x = 0; x < input.getWidth() - 1; ++x) {
                int nwCorner = grayValues[y][x];
                int neCorner = grayValues[y][x+1];
                int swCorner = grayValues[y+1][x];
                int seCorner = grayValues[y+1][x+1];
                if ((neCorner < 128) && (swCorner < 128) && (seCorner > 128) && (nwCorner > 128)) {
                    output.setRGB(x + 1, y + 1, targetBlack);
                }
                if ((nwCorner < 128) && (seCorner < 128) && (neCorner > 128) && (swCorner > 128)) {
                    output.setRGB(x + 1, y, targetBlack);
                }
            }
        }
        return output;
    }

    public static final Pix joinDisjointedBridges(Pix inputPix) {
        // Used to ensure that pixConnComp works properly on
        // characters with thin diagonals

        // Takes a 2 x 2 kernel
        // If NE and SW corners are black, while NW and SE corners are white,
        // it makes the SE corner black
        // Likewise, if NW and SE corners are black while NE and SW corners are white,
        // it makes the NE corner black
        int inputDepth = inputPix.d;
        BufferedImage input = ImageUtils.convertPixToImage(inputPix);
        input = rectifyBI(input);
        int targetBlack = 0xFF000000;
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = 0; y < input.getHeight() - 1; ++y) {
            for (int x = 0; x < input.getWidth() - 1; ++x) {
                int nwCorner = grayValues[y][x];
                int neCorner = grayValues[y][x+1];
                int swCorner = grayValues[y+1][x];
                int seCorner = grayValues[y+1][x+1];
                if ((neCorner < 128) && (swCorner < 128) && (seCorner > 128) && (nwCorner > 128)) {
                    output.setRGB(x + 1, y + 1, targetBlack);
                }
                if ((nwCorner < 128) && (seCorner < 128) && (neCorner > 128) && (swCorner > 128)) {
                    output.setRGB(x + 1, y, targetBlack);
                }
            }
        }
        if (inputDepth == 1) {
            return getDepth1Pix(output, 128);
        }
        if (inputDepth == 8) {
            return getDepth8Pix(output);
        }
        return convertImageToPix(output);
    }

    public static final Pix cleanThenJoinDisjointedBridges(Pix inputPix) {
        // Used to ensure that pixConnComp works properly on
        // characters with thin diagonals

        // First, clean small black trails

        // Then, takes a 2 x 2 kernel
        // If NE and SW corners are black, while NW and SE corners are white,
        // it makes the SE corner black
        // Likewise, if NW and SE corners are black while NE and SW corners are white,
        // it makes the NE corner black
        int inputDepth = inputPix.d;
        BufferedImage input = ImageUtils.convertPixToImage(inputPix);
        ArrayList<CleaningKernel> cleaningKernels = new ArrayList<>();
        cleaningKernels.add(new CleaningKernel(3,3));
        cleaningKernels.add(new CleaningKernel(3,5));
        cleaningKernels.add(new CleaningKernel(5,3));
        cleaningKernels.add(new CleaningKernel(7,3));
        cleaningKernels.add(new CleaningKernel(3,7));
        cleaningKernels.add(new CleaningKernel(9,5));
        cleaningKernels.add(new CleaningKernel(9,7));
        cleaningKernels.add(new CleaningKernel(11,5));
        cleaningKernels.add(new CleaningKernel(11,7));
        cleaningKernels.add(new CleaningKernel(13,5));
        cleaningKernels.add(new CleaningKernel(13,7));
//        cleaningKernels.add(new CleaningKernel(15,5));
//        cleaningKernels.add(new CleaningKernel(15,7));
//        cleaningKernels.add(new CleaningKernel(17,5));
//        cleaningKernels.add(new CleaningKernel(17,7));
//        cleaningKernels.add(new CleaningKernel(19,5));
//        cleaningKernels.add(new CleaningKernel(19,7));
//        cleaningKernels.add(new CleaningKernel(19,9));
        input = removeSmallTrails(input, cleaningKernels, true);
        int targetBlack = 0xFF000000;
        BufferedImage output = copyBI(input);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pix = input.getRGB(x, y);
                int currentPixel =  pix & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = 0; y < input.getHeight() - 1; ++y) {
            for (int x = 0; x < input.getWidth() - 1; ++x) {
                int nwCorner = grayValues[y][x];
                int neCorner = grayValues[y][x+1];
                int swCorner = grayValues[y+1][x];
                int seCorner = grayValues[y+1][x+1];
                if ((neCorner < 128) && (swCorner < 128) && (seCorner > 128) && (nwCorner > 128)) {
                    output.setRGB(x + 1, y + 1, targetBlack);
                }
                if ((nwCorner < 128) && (seCorner < 128) && (neCorner > 128) && (swCorner > 128)) {
                    output.setRGB(x + 1, y, targetBlack);
                }
            }
        }
        if (inputDepth == 1) {
            return getDepth1Pix(output, 128);
        }
        if (inputDepth == 8) {
            return getDepth8Pix(output);
        }
        return convertImageToPix(output);
    }


    public static final Pix drawBoundingBoxesOnPix(Pix pix, ArrayList<ArrayList<Rectangle>> words) {
        L_Bmf bbNumberingFont = Leptonica1.bmfCreate(null, 4);
        Pix pix1 = Leptonica1.pixConvertTo32(pix);
        int[] colors = {0xFF000000,0x00FF0000,0x0000FF00};
        byte[] rval = {(byte) 0xFF,0x00,0x00};
        byte[] gval = {0x00,(byte)0xFF,0x00};
        byte[] bval = {0x00,0x00,(byte)0xFF};
        int lineNo = 1;
        for (ArrayList<Rectangle> word : words) {
            int wordNo = 1;
            for (Rectangle letterBox : word) {
                Box box = Leptonica1.boxCreate((int) letterBox.getX(), (int) letterBox.getY(), (int) letterBox.getWidth(), (int) letterBox.getHeight());
                Leptonica1.pixRenderBox(pix1, box, 1, ILeptonica.L_FLIP_PIXELS);
                Leptonica1.pixRenderBoxArb(pix1, box, 1, rval[lineNo % rval.length], gval[lineNo % gval.length], bval[lineNo % bval.length]);
                Leptonica1.pixSetTextline(pix1, bbNumberingFont, lineNo + "." + wordNo, colors[lineNo % colors.length], (int) letterBox.getX(),
                        (int) letterBox.getY() - 1, null, null);
                LeptUtils.dispose(box);
                ++wordNo;
            }
            ++lineNo;
        }
        // Leptonica1.pixSetTextline(pix1, bbNumberingFont, identifier, 0xFF000000, 3, 10, null, null);
        LeptUtils.dispose(bbNumberingFont);
        return pix1;
    }

    public static final BufferedImage gammaEnhancementWithMask(BufferedImage input1, int kernelX, int kernelY,
                                                                       int characterWidth, int characterHeight, int percentile) {

        // top percentile % cells are blackened, rest are whitened

        // first, do a gamma enhancement with 3*width, 3*height, and 8 percentile
        // then, binarize with 32 cutoff
        // then, erode with 2*width and 1*height
        // this is the mask for the original
        // get the masked original
        // now do a gamma enhancement with given kernels (best = 1.5* width and 1.5*height) with given (best = 30-35) percentile
        // while ignoring all white cells

        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage GE_1 = copyBI(input);
        int kernelX1 = Math.min(characterWidth * 3, input.getWidth() / 4);
        int kernelY1 = Math.min(characterHeight * 3, input.getHeight() / 4);
        if (kernelX1 % 2 == 0) {
            ++kernelX1;
        }
        if (kernelY1 % 2 == 0) {
            ++kernelY1;
        }
        int percentile1 = 8;
        int indexOfBaseComparisonPixelInKernel = 0;
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        // int[] pixelArray = new int[kernelX1 * kernelY1];
        ArrayList<Integer> pixelArrayList = new ArrayList<>();
        for (int y = kernelY1 / 2; y < input.getHeight() - kernelY1 / 2 - 1; ++y) {
            for (int x = kernelX1; x < input.getWidth() - kernelX1 / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                pixelArrayList = new ArrayList<>();
                outer: for (int deltaY = -kernelY1 / 2; deltaY <= kernelY1 / 2; ++deltaY) {
                    for (int deltaX = -kernelX1 / 2; deltaX <= kernelX1 / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArrayList.add(pixel);
                        // pixelArray[(deltaY + kernelY1 / 2) * kernelX1 + (deltaX + kernelX1 / 2)] = pixel;
                    }
                }
                int[] pixelArray = pixelArrayList.stream().mapToInt(i -> i).toArray();
                Arrays.sort(pixelArray);
                indexOfBaseComparisonPixelInKernel = (pixelArrayList.size() * percentile) / 100;
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * base)), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                GE_1.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        BufferedImage inputToMask = ImageUtils.binarize(GE_1, 32);
        BufferedImage mask = ImageUtils.erodeGrayBI(inputToMask, characterWidth * 2, characterHeight);
        BufferedImage maskedOriginal = ImageUtils.imageMask(input, mask);
        BufferedImage gammaEnhanced = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(maskedOriginal, kernelX, kernelY, percentile, 242);
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(gammaEnhanced, clipRect);
    }

    public static final BufferedImage gammaEnhancementWithMask(BufferedImage input, int characterWidth, int characterHeight) {

        // top percentile % cells are blackened, rest are whitened

        // first, do a gamma enhancement with 3*width, 3*height, and 8 percentile
        // then, binarize with 32 cutoff
        // then, erode with 2*width and 1*height
        // this is the mask for the original
        // get the masked original
        // now do a gamma enhancement with 1.5* width and 1.5*height with 30-35 percentile
        // while ignoring all white cells

        input = rectifyBI(input);
        BufferedImage GE_1 = copyBI(input);
        int kernelX = Math.min(characterWidth * 3, input.getWidth() / 4);
        int kernelY = Math.min(characterHeight * 3, input.getHeight() / 4);
        if (kernelX % 2 == 0) {
            ++kernelX;
        }
        if (kernelY % 2 == 0) {
            ++kernelY;
        }
        int percentile1 = 8;
        int indexOfBaseComparisonPixelInKernel = (int) ((percentile1 * 1.0 / 100.0) * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * base)), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                GE_1.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        BufferedImage inputToMask = ImageUtils.binarize(GE_1, 32);
        BufferedImage mask = ImageUtils.erodeGrayBI(inputToMask, characterWidth * 2, characterHeight);
        BufferedImage maskedOriginal = ImageUtils.imageMask(input, mask);
        BufferedImage gammaEnhanced = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(maskedOriginal, (int) (characterWidth * 1.5), (int) (characterHeight * 1.5), 35, 242);
        return gammaEnhanced;
    }

    public static final BufferedImage sharpen(BufferedImage input, int kernelSize) {
        unsharpFilter.setRadius(kernelSize);
        return rectifyBI(unsharpFilter.filter(input, null));
    }

    public static final BufferedImage normaliseBackground(BufferedImage input) {

        input = rectifyBI(input);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int widthDivisions = 6;
        int heightDivisions = 12;   // have more of these because on a metallic surface, light is likely to
                                    // to create more horizontal patterns
        // int widthDivisions = input.getWidth() / 80;
        // int heightDivisions = input.getHeight() / 80;
        int gridWidth = input.getWidth() / widthDivisions;
        int gridHeight = input.getHeight() / heightDivisions;
        int[][] gridMode = new int[heightDivisions][widthDivisions];
        int[][] gridAverage = new int[heightDivisions][widthDivisions];
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] blurredGrayValues = new int[input.getHeight()][input.getWidth()];
        BufferedImage blurredImage = ImageUtils.blur(ImageUtils.blur(input,5, 5), 5, 5);
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                blurredGrayValues[y][x] = blurredImage.getRGB(x,y) & 0xFF;
                grayValues[y][x] = input.getRGB(x,y) & 0xFF;
            }
        }
        int bucketSize = 10;
        int skipLastFewBuckets = 3;
        ArrayList<Integer> modes = new ArrayList<>();
        int overallTotal = 0;
        for (int gridY = 0; gridY < heightDivisions; ++gridY) {
            for (int gridX = 0; gridX < widthDivisions; ++gridX) {
                // find the mode and average for each grid
                int[] histogram = new int[256 / bucketSize + 1];
                int total = 0;
                int startX = gridX * gridWidth;
                int startY = gridY * gridHeight;
                int endX = Math.min(startX + gridWidth, input.getWidth());
                int endY = Math.min(startY + gridHeight, input.getHeight());
                for (int y = startY; y < endY; ++y) {
                    for (int x = startX; x < endX; ++x) {
                        // int pixel = blurredGrayValues[y][x];
                        int pixel = grayValues[y][x];
                        total += pixel;
                        overallTotal += pixel;
                        int bucket = pixel / bucketSize;
                        int value = histogram[bucket];
                        histogram[bucket] = ++value;
                    }
                }
                int totalPixels = (endX - startX) * ( endY - startY);
                double average = (total * 1.0) / totalPixels;
                int maxFreq = Integer.MIN_VALUE;
                int mode = 0;
                for (int i = 0; i < histogram.length - skipLastFewBuckets; ++i) {
                    // avoid the last 3 buckets
                    if (histogram[i] >= maxFreq) {
                        maxFreq = histogram[i];
                        mode = i;
                    }
                }
                mode = (mode * bucketSize) + bucketSize / 2;
                // if (mode > 32) {
                    // trying to avoid creating pictures that are overly black
                    // modes.add(mode);
                // }
                gridMode[gridY][gridX] = mode;
                gridAverage[gridY][gridX] = (int) average;
            }
        }
        int[] modeArray = modes.stream()
                .map(i -> (i == null ? 0 : i))
                .mapToInt(Integer::intValue)
                .toArray();
        Arrays.sort(modeArray);
        // System.out.println("Grid modes are : " + Arrays.deepToString(gridMode));
        // System.out.println("Modes array is : " + Arrays.toString(modeArray));
        // int finalMode = modeArray[(modeArray.length + 1) / 2];
        int overallAverage = overallTotal / (input.getHeight() * input.getWidth());
        int finalMode = 160;
        // System.out.println("Final background mode is : " + finalMode);
        for (int gridY = 0; gridY < heightDivisions; ++gridY) {
            for (int gridX = 0; gridX < widthDivisions; ++gridX) {
                // ensures that over-whitening and over-blackening of cells doesn't happen
                int currentGridMode = Math.min(Math.max(25, gridMode[gridY][gridX]), 225);
                if (currentGridMode > finalMode) {
                    System.out.println("currentGridMode value for grid [" + gridX + "," + gridY + "] is " + currentGridMode);
                }
                int avgDifference = overallAverage - gridAverage[gridY][gridX];
                double scaler = (finalMode * 1.0) / currentGridMode;
                // System.out.println("Scaler value for grid [" + gridX + "," + gridY + "] is " + scaler);
                int startX = gridX * gridWidth;
                int startY = gridY * gridHeight;
                int endX = Math.min(startX + gridWidth, input.getWidth());
                int endY = Math.min(startY + gridHeight, input.getHeight());
                for (int y = startY; y < endY; ++y) {
                    for (int x = startX; x < endX; ++x) {
                        // int newPixel = (int) Math.max(0,Math.min(grayValues[y][x] + difference, 255));
                        // int newPixel = (int) Math.min(255, grayValues[y][x] * scaler);
                        int newPixel = Math.max(0, Math.min(grayValues[y][x] + avgDifference, 255));
                        output.setRGB(x,y,ARGBPixels[newPixel]);
                    }

                }
            }
        }
        return output;
    }

    public static final BufferedImage findEdgesModerateAggression(BufferedImage input1, boolean darkenEdgesAggresively) {
        int kernelX = 3;
        int kernelY = 3;
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage sobel1_0 = ImageUtils.sobelVerticalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel2_0 = ImageUtils.sobelHorizontalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel3_0 = ImageUtils.sobelNESWDiagonalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel4_0 = ImageUtils.sobelNWSEDiagonalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel0 = ImageUtils.imageGrayOr(sobel1_0, sobel2_0);
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel3_0);
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel4_0);
        // BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, Math.max(kernelX, kernelY) + 1);
        BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, 0);
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, sobel0.getWidth() - kernelX, sobel0.getHeight() - kernelY);
        finalSobel = clipBI(finalSobel, clipRect);
        finalSobel = ImageUtils.stretchHistogram(finalSobel);
        return finalSobel;
    }

    public static final BufferedImage findEdgesConservatively(BufferedImage input1, boolean darkenEdgesAggresively) {
        int kernelX = 3;
        int kernelY = 3;
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage sobel1_0 = ImageUtils.sobelVerticalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel2_0 = ImageUtils.sobelHorizontalFilter(input, kernelX, kernelY, darkenEdgesAggresively);
        BufferedImage sobel0 = ImageUtils.imageGrayOr(sobel1_0, sobel2_0);
        // BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, Math.max(kernelX, kernelY) + 1);
        BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, 0);
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, finalSobel.getWidth() - kernelX, finalSobel.getHeight() - kernelY);
        finalSobel = clipBI(finalSobel, clipRect);
        finalSobel = ImageUtils.stretchHistogram(finalSobel);
        return finalSobel;
    }

    public static final BufferedImage findCannyEdges(BufferedImage input1) {
        GrayU8 gray = ConvertBufferedImage.convertFrom(input1, (GrayU8) null);
        GrayU8 edgeGray = gray.createSameShape();
        // Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
        // It has also been configured to save the trace as a graph.  This is the graph created while performing
        // hysteresis thresholding.
        CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(1, true, true, GrayU8.class, GrayS16.class);
        // The edge image is actually an optional parameter.  If you don't need it just pass in null
        canny.process(gray, 0.1f, 0.3f, edgeGray);
        BufferedImage edgeImage = new BufferedImage(edgeGray.width, edgeGray.height, BufferedImage.TYPE_INT_ARGB);
        edgeImage = ConvertBufferedImage.convertTo(edgeGray, null);
        // CannyEdge comes as a binary number 0 or 1.
        // This needs to be converted to 0-255 range, which is done by fullImageGammaEnhancementWithBias
        edgeImage = ImageUtils.fullImageGammaEnhancementWithBias(edgeImage);
        edgeImage = ImageUtils.invert(edgeImage);
        // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "edgeImage_GE.png");
        int average = ImageUtils.getAverage(edgeImage);
        edgeImage = ImageUtils.binarize(edgeImage, average);
        return edgeImage;
    }

    public static final BufferedImage findEdgesAggressively(BufferedImage input1, boolean aggressiveEdgeDarkening) {
        int kernelX = 3;
        int kernelY = 3;
        // BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage input = rectifyBI(input1);
        BufferedImage sobel1_0 = ImageUtils.sobelVerticalFilter(input, kernelX, kernelY, aggressiveEdgeDarkening);
        BufferedImage sobel2_0 = ImageUtils.sobelHorizontalFilter(input, kernelX, kernelY, aggressiveEdgeDarkening);
        BufferedImage sobel3_0 = ImageUtils.sobelNESWDiagonalFilter(input, kernelX, kernelY, aggressiveEdgeDarkening);
        BufferedImage sobel4_0 = ImageUtils.sobelNWSEDiagonalFilter(input, kernelX, kernelY, aggressiveEdgeDarkening);
        BufferedImage edge11 = ImageUtils.edgeFilter(input, kernelX, kernelY);
        BufferedImage sobel0 = ImageUtils.imageGrayOr(sobel1_0, sobel2_0);
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel3_0);
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel4_0);
        sobel0 = ImageUtils.imageGrayOr(sobel0, edge11);
        // BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, Math.max(kernelX, kernelY) + 1);
        BufferedImage finalSobel = ImageUtils.makeEdgeImageBorderWhite(sobel0, 0);
        // Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, finalSobel.getWidth() - kernelX, finalSobel.getHeight() - kernelY);
        // finalSobel = clipBI(finalSobel, clipRect);
        finalSobel = ImageUtils.stretchHistogram(finalSobel);
        return finalSobel;
    }

    public static final BufferedImage makeEdgeImageBorderWhite(BufferedImage input1, int borderWidth) {
        if (input1 == null) {
            throw new IllegalArgumentException("Input image cannot be null");
        }
        BufferedImage output = rectifyBI(input1);
        borderWidth = Math.max(borderWidth, 0);
        borderWidth = Math.min(Math.min(input1.getHeight() / 2, input1.getWidth() / 2), borderWidth);
        if (borderWidth == 0) {
            return input1;
        }
        // In the original edge image, the edges are white and most of the image is black
        // Hence, we invert the image to see the edges as black.
        // So, the border pixels have to set to black to eventually get a white border
        int whitePixel = ARGBPixels[0];

        for (int y = 0; y < borderWidth; ++y) {
            // System.out.println("Entered top border");
            for (int x = 0; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = output.getHeight() - borderWidth; y < output.getHeight(); ++y) {
            // System.out.println("Entered bottom border");
            for (int x = 0; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            // System.out.println("Entered left border");
            for (int x = 0; x < borderWidth; ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            // System.out.println("Entered right border");
            for (int x = output.getWidth() - borderWidth; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        return output;
    }

    public static final BufferedImage makeBorderWhite(BufferedImage input1, int borderWidth) {
        BufferedImage output = rectifyBI(input1);
        int whitePixel = ARGBPixels[255];
        for (int y = 0; y < borderWidth; ++y) {
            // System.out.println("Entered top border");
            for (int x = 0; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = output.getHeight() - borderWidth; y < output.getHeight(); ++y) {
            // System.out.println("Entered bottom border");
            for (int x = 0; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            // System.out.println("Entered left border");
            for (int x = 0; x < borderWidth; ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            // System.out.println("Entered right border");
            for (int x = output.getWidth() - borderWidth; x < output.getWidth(); ++x) {
                output.setRGB(x,y,whitePixel);
            }
        }
        return output;
    }

    public static final BufferedImage sobelVerticalFilter(BufferedImage input1, int kernelX, int kernelY, boolean aggressiveEdgeDarkening) {
        if (kernelX > 5) {
            kernelX = 5;
        }
        if (kernelY > 5) {
            kernelY = 5;
        }
        if (kernelX < 3) {
            kernelX = 3;
        }
        if (kernelY < 3) {
            kernelX = 3;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues1 = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues2 = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaX == 0) {
                            continue;
                        }
                        if (deltaY == 0) {
                            weight = 2;
                        } else {
                            weight = 1;
                        }
                        weight = weight * (deltaX) / Math.abs(deltaX);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues1[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaX == 0) {
                            continue;
                        }
                        if (deltaY == 0) {
                            weight = 2;
                        } else {
                            weight = 1;
                        }
                        weight = -weight * (deltaX) / Math.abs(deltaX);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues2[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                if (aggressiveEdgeDarkening) {
                    output.setRGB(x, y, outputValues1[y][x] | outputValues2[y][x]);
                } else {
                    output.setRGB(x,y,Math.min(outputValues1[y][x], outputValues2[y][x]));
                }
            }
        }

        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage sobelHorizontalFilter(BufferedImage input1, int kernelX, int kernelY, boolean aggressiveEdgeDarkening) {
        if (kernelX > 5) {
            kernelX = 5;
        }
        if (kernelY > 5) {
            kernelY = 5;
        }
        if (kernelX < 3) {
            kernelX = 3;
        }
        if (kernelY < 3) {
            kernelX = 3;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues1 = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues2 = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY == 0) {
                            continue;
                        }
                        if (deltaX == 0) {
                            weight = 2;
                        } else {
                            weight = 1;
                        }
                        weight = weight * (deltaY) / Math.abs(deltaY);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues1[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY == 0) {
                            continue;
                        }
                        if (deltaX == 0) {
                            weight = 2;
                        } else {
                            weight = 1;
                        }
                        weight = -weight * (deltaY) / Math.abs(deltaY);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues2[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                if (aggressiveEdgeDarkening) {
                    output.setRGB(x, y, outputValues1[y][x] | outputValues2[y][x]);
                } else {
                    output.setRGB(x,y,Math.min(outputValues1[y][x], outputValues2[y][x]));
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage sobelNESWDiagonalFilter(BufferedImage input1, int kernelX, int kernelY, boolean aggressiveEdgeDarkening) {
        if (kernelX > 5) {
            kernelX = 5;
        }
        if (kernelY > 5) {
            kernelY = 5;
        }
        if (kernelX < 3) {
            kernelX = 3;
        }
        if (kernelY < 3) {
            kernelX = 3;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        int maxValue = Math.max(kernelX / 2, kernelY /2) + 1;
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues1 = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues2 = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY + deltaX == 0) {
                            continue;
                        }
                        weight = maxValue - (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        // weight = (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        int sum = deltaX + deltaY;
                        weight = weight * (sum) / Math.abs(sum);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues1[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY + deltaX == 0) {
                            continue;
                        }
                        weight = maxValue - (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        // weight = (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        int sum = deltaX + deltaY;
                        weight = -weight * (sum) / Math.abs(sum);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues2[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                if (aggressiveEdgeDarkening) {
                    output.setRGB(x, y, outputValues1[y][x] | outputValues2[y][x]);
                } else {
                    output.setRGB(x,y,Math.min(outputValues1[y][x], outputValues2[y][x]));
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage sobelNWSEDiagonalFilter(BufferedImage input1, int kernelX, int kernelY, boolean aggressiveEdgeDarkening) {
        if (kernelX > 5) {
            kernelX = 5;
        }
        if (kernelY > 5) {
            kernelY = 5;
        }
        if (kernelX < 3) {
            kernelX = 3;
        }
        if (kernelY < 3) {
            kernelX = 3;
        }
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        int maxValue = Math.max(kernelX / 2, kernelY / 2) + 1;
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues1 = new int[input.getHeight()][input.getWidth()];
        int[][] outputValues2 = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY == deltaX) {
                            continue;
                        }
                        weight = maxValue - (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        // weight = (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        int difference = deltaX - deltaY;
                        weight = weight * (difference) / Math.abs(difference);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues1[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if (deltaY == deltaX) {
                            continue;
                        }
                        weight = maxValue - (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        // weight = (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        int difference = deltaX - deltaY;
                        weight = -weight * (difference) / Math.abs(difference);
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                outputValues2[y][x] = ARGBPixels[total];
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                if (aggressiveEdgeDarkening) {
                    output.setRGB(x, y, outputValues1[y][x] | outputValues2[y][x]);
                } else {
                    output.setRGB(x,y,Math.min(outputValues1[y][x], outputValues2[y][x]));
                }
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage edgeFilter(BufferedImage input1, int kernelX, int kernelY) {
        if (kernelX > 5) {
            kernelX = 5;
        }
        if (kernelY > 5) {
            kernelY = 5;
        }
        if (kernelX < 3) {
            kernelX = 3;
        }
        if (kernelY < 3) {
            kernelX = 3;
        }
        int kernelSize = kernelX * kernelY;
        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2 - 1; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                int weight = 0;
                outer: for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        if ((y + deltaY) < kernelY / 2) {
                            continue outer;
                        }
                        if ((x + deltaX) < kernelX / 2) {
                            continue;
                        }
                        if ((y + deltaY) >= input.getHeight() - kernelY / 2 - 2) {
                            continue outer;
                        }
                        if ((x + deltaX) >= input.getWidth() - kernelX / 2 - 2) {
                            continue;
                        }
                        if ((deltaY == 0) && (deltaX == 0)) {
                            weight = kernelSize - 1;
                        } else {
                            weight = -1;
                        }
                        // weight = 3 - (Math.max(Math.abs(deltaX), Math.abs(deltaY)));
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += weight * pixel;
                    }
                }
                total = Math.max(0, Math.min(total, 255));
                output.setRGB(x, y, ARGBPixels[total]);
            }
        }
        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage lightenIntermediates(BufferedImage input) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        int targetPixel = 0xFF000000 | (180 << 16) | (180 << 8) | 180;
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth() - 4; ++x) {
                int leftPixel = grayValues[y][x];
                int rightPixel = grayValues[y][x + 3];
                int midPixel1 = grayValues[y][x + 1];
                int midPixel2 = grayValues[y][x + 2];
                if ((leftPixel < 8) && (rightPixel < 8) && (midPixel1 > 16) && (midPixel2 > 16)) {
                    output.setRGB(x + 1, y, targetPixel);
                    output.setRGB(x + 2, y, targetPixel);
                    continue;
                }
                rightPixel = grayValues[y][x + 2];
                midPixel1 = grayValues[y][x + 1];
                if ((leftPixel < 8) && (rightPixel < 8) && (midPixel1 > 16)) {
                    output.setRGB(x + 1, y, targetPixel);
                    continue;
                }
            }
        }
        for (int y = 0; y < input.getHeight() - 4; ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int topPixel = grayValues[y][x];
                int bottomPixel = grayValues[y + 3][x];
                int midPixel1 = grayValues[y + 1][x];
                int midPixel2 = grayValues[y + 2][x];
                if ((topPixel < 8) && (bottomPixel < 8) && (midPixel1 > 16) && (midPixel2 > 16)) {
                    output.setRGB(x, y + 1, targetPixel);
                    output.setRGB(x, y + 2, targetPixel);
                    continue;
                }
                bottomPixel = grayValues[y + 2][x];
                midPixel1 = grayValues[y + 1][x];
                if ((topPixel < 8) && (bottomPixel < 8) && (midPixel1 > 16)) {
                    output.setRGB(x, y + 1, targetPixel);
                    continue;
                }
            }
        }
        return output;
    }

    public static int findBinarizationPixelInThinStrip(int[] histogram) {
        int histogramStart = 0;
        int histogramEnd = 255;
        for (int i = 0; i < histogram.length; ++i) {
            if (histogram[i] > 0) {
                histogramStart = i;
                break;
            }
        }
        for (int i = 255; i >= 0; --i) {
            if (histogram[i] > 0) {
                histogramEnd = i;
                break;
            }
        }
        int minGapNeeded = 14;
        if ((histogramStart > 245) || ((histogramEnd - histogramStart) < minGapNeeded)) {
            // System.out.println("Histogram Start = " + histogramStart + "; binarizationIndex = " + 10);
            return 10;
        }
        if ((histogramEnd - histogramStart) < 30) {
            // most likely, it does not contain any point that matters
            // but, do a double check by checking if the maxima is somewhere in the middle
            int maximaIndex = 0;
            int maxima = Integer.MIN_VALUE;
            for (int i = histogramStart; i <= histogramEnd; ++i) {
                if (histogram[i] > maxima) {
                    maxima = histogram[i];
                    maximaIndex = i;
                }
            }
            // if maximaIndex is close to the edges, then reconsider
            // else, lighten all cells
            if ((Math.abs(maximaIndex - histogramStart) < minGapNeeded / 2) || (Math.abs(histogramEnd - maximaIndex) < minGapNeeded / 2)) {
            } else {
                // return a value equal to histogramStart / 2
                // System.out.println("Maxima Index is not close to the edges. Hence, binarization pixel is " + (histogramStart / 2));
                return (histogramStart / 2);
            }
        }
        // else find the minima in the middle
        int[] smoothedHistogram = new int[256];
        int smootheningPeriod = 3;
        double emaFactor = 2 * 1.0 / (smootheningPeriod + 1);
        smoothedHistogram[0] = histogram[0];
        for (int i = 1; i < histogram.length; ++i) {
            smoothedHistogram[i] = (int) ((1 - emaFactor) * smoothedHistogram[i-1] + emaFactor * histogram[i]);
        }
        // double smooth it
        int[] doubleSmoothedHistogram = new int[256];
        doubleSmoothedHistogram[0] = smoothedHistogram[0];
        for (int i = 1; i < smoothedHistogram.length; ++i) {
            doubleSmoothedHistogram[i] = (int) ((1 - emaFactor) * doubleSmoothedHistogram[i-1] + emaFactor * smoothedHistogram[i]);
        }

        // find the first minima within the range
        int minima = Integer.MAX_VALUE;
        int binarizationIndex = 0;
        for (int i = histogramStart + minGapNeeded / 2; i < histogramEnd - minGapNeeded / 2; ++i) {
            if (doubleSmoothedHistogram[i] < minima) {
                minima = doubleSmoothedHistogram[i];
                binarizationIndex = i;
            }
        }
        if (binarizationIndex == 0) {
            // System.out.println("histogramStart = " + histogramStart + "; histogramEnd = " + histogramEnd + "; minima = " + minima);
        }
        binarizationIndex = Math.max(0,binarizationIndex - smootheningPeriod); // compensating for the lag shift
        // check if the minima is close to the edges
        if ((Math.abs(binarizationIndex - histogramStart) < 5) || (Math.abs(histogramEnd - binarizationIndex) < 5)) {
            // System.out.println("After double smoothing, binarization Index is close to the edges. Hence, binarization pixel is " + (histogramStart / 2));
            return histogramStart / 2;
        }
        System.out.println("After double smoothing, returning proper binarization pixel as " + (binarizationIndex));
        return binarizationIndex;
    }

    public static final BufferedImage thinStripEnhancement(BufferedImage input) {

        // Assume that the darkest spots are what interest us, so
        // look for a minima in the histogram after the first maxima is reached
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int[] histogram = new int[256];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                histogram[currentPixel]++;
            }
        }
        int width = 29;
        int height = 7;
        for (int y = 0; y < input.getHeight(); y = y + height) {
            for (int x = 0; x < input.getWidth(); x = x + width) {
                int yStart = y;
                int yEnd = Math.min(input.getHeight(), yStart + height);
                int xStart = x;
                int xEnd = Math.min(input.getWidth(), xStart + width);
                int[] kernelHistogram = new int[256];
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        int pixel = grayValues[yCoord][xCoord];
                        kernelHistogram[pixel]++;
                    }
                }
                // System.out.println("About to find binarizationPixel");
                int binarizationPixel = findBinarizationPixelInThinStrip(kernelHistogram);
                // System.out.println("Found binarizationPixel as " + binarizationPixel);
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        int currentPixel = grayValues[yCoord][xCoord];
                        double gamma = currentPixel * 1.0 / binarizationPixel;
                        double gammaCubed = gamma * gamma * gamma;
                        gammaCubed = Math.max(0.0, Math.min(gammaCubed, 6.95));
                        Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05) - 1);
                        double[] multipliers = gammaMultipliersMap.get(gammaInt);
                        if (multipliers == null) {
                            System.out.println("yCoord = " + yCoord + "; xCoord = " + xCoord + "; currentPixel = " + currentPixel + "; binarizationPixel = " + binarizationPixel);
                            System.out.println("gamma = " + gamma + "; gammaCubed = " + gammaCubed);
                            System.out.println("gammaInt = " + gammaInt);
                        }
                        int newPixel = (int) (255 * multipliers[currentPixel]);
                        output.setRGB(xCoord, yCoord, ARGBPixels[newPixel]);
                    }
                }
            }
        }
        return output;
    }

    public static BufferedImage getImage(int[][] grayValues) {
        int width = grayValues[0].length;
        int height = grayValues.length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                image.setRGB(x, y, ARGBPixels[grayValues[y][x]]);
            }
        }
        return image;
    }

    public static final int[][] relativeGammaEnhancementWithMatrix(int[][] grayValues, int xStart, int yStart, int width, int height, int kernelX, int kernelY,
                                                                       int percentile) {

        // top percentile % cells are blackened, rest are whitened
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        int yEnd = Math.min(yStart + height, grayValues.length);
        height = yEnd - yStart;
        int xEnd = Math.min(xStart + width, grayValues[0].length);
        width = xEnd-xStart;
        kernelX = Math.min(Math.max(1, kernelX), width / 2);
        kernelY = Math.min(Math.max(1, kernelY), height / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        int indexOfBaseComparisonPixelInKernel = (int) ((percentile * 1.0 / 100.0) * kernelX * kernelY);
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = yStart + kernelY / 2; y < yEnd - kernelY / 2; ++y) {
            for (int x = xStart + kernelX / 2; x < xEnd - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * base)), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliersMap.get(gammaInt);
                int newPixel = (int) (255 * multipliers[currentPixel]);
                grayValues[y][x] = newPixel;
            }
        }
        return grayValues;
    }

    public static final int[][] relativeBinarizationMatrix(int[][] grayValues, int xStart, int yStart, int width, int height, int kernelX, int kernelY,
                                                                   int percentile) {

        // top percentile % cells are blackened, rest are whitened
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        int yEnd = Math.min(yStart + height, grayValues.length);
        height = yEnd - yStart;
        int xEnd = Math.min(xStart + width, grayValues[0].length);
        width = xEnd-xStart;
        kernelX = Math.min(Math.max(1, kernelX), width / 2);
        kernelY = Math.min(Math.max(1, kernelY), height / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        int indexOfBaseComparisonPixelInKernel = (int) ((percentile * 1.0 / 100.0) * kernelX * kernelY);
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = yStart + kernelY / 2; y < yEnd - kernelY / 2; ++y) {
            for (int x = xStart + kernelX / 2; x < xEnd - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                int newPixel = (currentPixel >= base) ? 255 : 0;
                grayValues[y][x] = newPixel;
            }
        }
        return grayValues;
    }

    public static int getAverage(BufferedImage input) {
        int total = 0;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                total += (input.getRGB(x, y) & 0xFF);
            }
        }
        return total / (input.getWidth() * input.getHeight());
    }

    public static Rectangle getBoundingBoxEnvelope(BufferedImage input, int expectedCharacterHeight, int expectedCharacterWidth) {
        if (input == null) {
            return new Rectangle (0,0,10,10);
        }
        int pixelLayerToBeAdded = 1;
        BufferedImage edgeImage = ImageUtils.addPixelLayer(input, pixelLayerToBeAdded, 128, false);
        int height = edgeImage.getHeight();
        int width = edgeImage.getWidth();
        double maxHeightRatio = expectedCharacterHeight * 1.0 / input.getHeight()  * 1.4;
        double maxWidthRatio = expectedCharacterWidth * 1.0 / input.getWidth()  * 5;
        Pix connCompReadyPix = ImageUtils.getDepth1Pix(edgeImage);
        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, 4);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);

        int borderGutterWidth = input.getWidth() / 15; // 6% on width
        int borderGutterHeight = input.getHeight() / 15; // 6% on height

        // get rid of the bounding boxes at the 4 corners
        // and, bounding boxes that have greater dimensions than the width and height cutoffs
        ArrayList<Rectangle> boxes = new ArrayList<Rectangle>(0);
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);
            int xS = aBox.x;
            int xE = aBox.x + aBox.w;
            int yS = aBox.y;
            int yE = aBox.y + aBox.h;
            int xGE = input.getWidth() - borderGutterWidth;
            int yGE = input.getHeight() - borderGutterHeight;
            if ((xS <= borderGutterWidth) || (xS >= xGE) || (xE > xGE) || (yS <= borderGutterHeight) || (yS >= yGE) || (yE > yGE)) {
                // System.out.println("Dropping box [" + aBox.x + "," + aBox.y + "," + aBox.h + "," + aBox.w + "] due to border gutter infringement");
                LeptUtils.dispose(aBox);
                continue;
            }
            if (aBox.w >= maxWidthRatio * width) {
                // System.out.println("Dropping box [" + aBox.x + "," + aBox.y + "," + aBox.h + "," + aBox.w + "] due to excess width");
                LeptUtils.dispose(aBox);
                continue;
            }
            if (aBox.h >= maxHeightRatio * height) {
                // System.out.println("Dropping box [" + aBox.x + "," + aBox.y + "," + aBox.h + "," + aBox.w + "] due to excess height");
                LeptUtils.dispose(aBox);
                continue;
            }

            boxes.add(new Rectangle(aBox.x, aBox.y, aBox.w, aBox.h));
            LeptUtils.dispose(aBox);
        }
        // arrange the boxes into lines

        // first, sort them based on Y of their base
        Collections.sort(boxes,  new Comparator<Rectangle>() {

            @Override
            public int compare(Rectangle r1, Rectangle r2) {
                return (int) (r1.getY() + r1.getHeight() - r2.getY() - r2.getHeight());
            }

        });

        // get a sense of the likely character height (go for the 90th percentile)
        DescriptiveStatistics hStats = new DescriptiveStatistics();
        for (Rectangle aBox : boxes) {
            hStats.addValue(aBox.getHeight());
        }

        int likelyHeight = (int) hStats.getPercentile(90) ;
        // System.out.println("Actual likely Height = " + (likelyHeight - pixelLayerToBeAdded));

        // next put into lines
        ArrayList<ArrayList<Rectangle>> lines = new ArrayList<>();

        ArrayList<Rectangle> newLine = new ArrayList<>();
        ArrayList<Integer> startYs = new ArrayList<>();
        ArrayList<Integer> endYs = new ArrayList<>();
        ArrayList<Integer> boxHeights = new ArrayList<>();
        for (Rectangle aBox : boxes) {
            if (lines.size() == 0) {
                newLine.add(aBox);
                lines.add(newLine);
                startYs.add((int)aBox.getY());
                endYs.add((int)(aBox.getY() + aBox.getHeight()));
                boxHeights.add((int)aBox.getHeight());
                continue;
            }
            int currentYStart = (int) aBox.getY();
            int currentYEnd = (int) (aBox.getY() + aBox.getHeight());
            int index = -1;
            // find which line number the current box fits into
            for (int lineNumber = 0; lineNumber < startYs.size(); ++lineNumber) {
                // System.out.println("Line Number = " + lineNumber + "; startYs = " + startYs + "; endYs = " + endYs + "; boxHeights = " + boxHeights);
                int sY = startYs.get(lineNumber);
                int eY = endYs.get(lineNumber);
                // determine tolerance based on height of tallest box in the line
                int tolerance = 0;
                double actualToLikelyRatio = boxHeights.get(lineNumber) * 1.0 / likelyHeight;
                if (actualToLikelyRatio > 0.75) {
                    tolerance = likelyHeight / 3;
                } else {
                    if (actualToLikelyRatio > 0.5) {
                        tolerance = likelyHeight / 3;
                    } else {
                        if (actualToLikelyRatio > 0.33) {
                            tolerance = likelyHeight / 2;
                        } else {
                            tolerance = (int) (likelyHeight * 1.0 / 1.5);
                        }
                    }
                }
                if ((currentYStart >= (sY - tolerance)) && (currentYEnd <= (eY + tolerance))) {
                    // System.out.println("Determined that box " + aBox + " fits in line number " + lineNumber);
                    index = lineNumber;
                    break;
                }
            }

            if (index != -1) {
                // if a line is found, fit it there
                lines.get(index).add(aBox);
                int currentValue = startYs.remove(index);
                startYs.add(index, (int) Math.min(currentValue, aBox.getY()));
                currentValue = endYs.remove(index);
                endYs.add(index, (int) Math.max(currentValue, aBox.getY() + aBox.getHeight()));
                currentValue = boxHeights.remove(index);
                boxHeights.add(index, (int) Math.max(currentValue, aBox.getHeight()));
                // System.out.println("After fitment of box " + aBox + " in line " + index + ", startYs = " + startYs + ", endYs " + endYs + ", and boxHeights = " + boxHeights);
            } else {
                // make a new line
                newLine = new ArrayList<>();
                newLine.add(aBox);
                lines.add(newLine);
                startYs.add((int)aBox.getY());
                endYs.add((int)(aBox.getY() + aBox.getHeight()));
                boxHeights.add((int) aBox.getHeight());
                // System.out.println("After fitment of box " + aBox + " in a new line, startYs = " + startYs + ", endYs = " + endYs + ", and boxHeights = " + boxHeights);
            }
        }

        // All boxes have been allocated to lines by now

        // check the lines to see if the boxes in each line constitute a proper line
        // currently, checking for the height and length of each line seems sufficient

        ArrayList<Integer> heights = new ArrayList<>();
        ArrayList<Integer> lengths = new ArrayList<>();

        for (ArrayList<Rectangle> line : lines) {
            int startX = Integer.MAX_VALUE;
            int startY = Integer.MAX_VALUE;
            int endX = Integer.MIN_VALUE;
            int endY = Integer.MIN_VALUE;
            for (Rectangle box : line) {
                startX = Math.min((int) box.getX(), startX);
                startY = Math.min((int) box.getY(), startY);
                endX = Math.max(endX, (int) box.getX() + (int) box.getWidth());
                endY = Math.max(endY, (int) box.getY() + (int) box.getHeight());
            }
            heights.add(endY - startY);
            lengths.add(endX - startX);
        }

        // remove lines that are smaller than a certain height
        // remove lines that are smaller than a certain width
        int numberOfLines = heights.size();
        for (int lineNumber = numberOfLines - 1; lineNumber >= 0; --lineNumber) {
            if (heights.get(lineNumber) <= likelyHeight * 0.75) {
                lines.remove(lineNumber);
                continue;
            }
            if (lengths.get(lineNumber) <= likelyHeight * 0.55) { // 0.67
                lines.remove(lineNumber);
                continue;
            }
        }

        // System.out.println("After dropping lines of small height and width, the number of lines remaining are " + lines.size());

        // if the remaining lines are at similar startY and endY, merge them
        ArrayList<ArrayList<Rectangle>> mergedLines = new ArrayList<>();
        newLine = new ArrayList<>();
        int index = 0;
        int lastAddedYStart = 0;
        int lastAddedYEnd = 0;
        for (ArrayList<Rectangle> line : lines) {
            if (mergedLines.size() == 0) {
                for (Rectangle word : line) {
                    newLine.add(word);
                }
                lastAddedYStart = startYs.get(index);
                lastAddedYEnd = endYs.get(index);
                ++index;
                continue;
            }
            int currentYStart = startYs.get(index);
            int currentYEnd = endYs.get(index);
            if ((Math.abs(currentYStart - lastAddedYStart) < likelyHeight / 4) && (Math.abs(currentYEnd - lastAddedYEnd) < likelyHeight / 4)){
                for (Rectangle word : line) {
                    newLine.add(word);
                }
                ++index;
            } else {
                mergedLines.add(newLine);
                newLine = new ArrayList<>();
                for (Rectangle word : line) {
                    newLine.add(word);
                }
                lastAddedYStart = startYs.get(index);
                lastAddedYEnd = endYs.get(index);
                ++index;
            }
        }

        if (newLine.size() > 0) {
            mergedLines.add(newLine);
        }

        // if there are more than 1 line, retain the line with most number of boxes
        if (mergedLines.size() > 1) {
            // chop off lines with less number of boxes
            int maxBoxes = Integer.MIN_VALUE;
            for (ArrayList<Rectangle> line : mergedLines) {
                maxBoxes = Math.max(line.size(), maxBoxes);
            }
            for (int i = mergedLines.size() - 1; i >= 0; --i) {
                if (mergedLines.get(i).size() != maxBoxes) {
                    mergedLines.remove(i);
                }
            }
        }

        ArrayList<Rectangle> remainingLine = new ArrayList<>();
        if (mergedLines.size() > 0) {
            remainingLine = mergedLines.get(0);
        }

        if (remainingLine.size() == 0) {
            return new Rectangle(0,0,10,10);
        }

        int startX = Integer.MAX_VALUE;
        int startY = Integer.MAX_VALUE;
        int endX = Integer.MIN_VALUE;
        int endY = Integer.MIN_VALUE;
        for (Rectangle box : remainingLine) {
            startX = Math.min((int) box.getX(), startX);
            startY = Math.min((int) box.getY(), startY);
            endX = Math.max(endX, (int) box.getX() + (int) box.getWidth());
            endY = Math.max(endY, (int) box.getY() + (int) box.getHeight());
        }

        LeptUtils.dispose(connCompReadyPix);
        LeptUtils.dispose(boxes1);
        return new Rectangle(startX, startY, endX- startX, endY - startY);
    }

    public static ArrayList<Rectangle> getBoundingBoxesInLine(BufferedImage input, int characterWidth, int characterHeight) {
        if (input == null) {
            return new ArrayList<Rectangle>();
        }
        BufferedImage input1 = ImageUtils.addPixelLayer(input,1, NORTH, 240, false);
        input1 = ImageUtils.addPixelLayer(input1,1, SOUTH, 240, false);
        input1 = ImageUtils.addPixelLayer(input1,1, EAST, 240, false);
        input1 = ImageUtils.addPixelLayer(input1,1, WEST, 240, false);
        ArrayList<Rectangle> boundingBoxes = new ArrayList<>();
        Pix connCompReadyPix = ImageUtils.getDepth1Pix(input1);
        Boxa boxes1 = Leptonica1.pixConnCompBB(connCompReadyPix, 4);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes1);

        // get rid of the bounding boxes that have small height or small width
        ArrayList<Rectangle> boxes = new ArrayList<Rectangle>(0);
        int minimumHeightNeeded = characterHeight / 2;
        int minimumWidthNeeded = characterWidth / 4;
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes1, i, ILeptonica.L_CLONE);
            if (aBox.w <= minimumWidthNeeded) {
                // System.out.println("Dropping box [" + aBox.x + "," + aBox.y + "," + aBox.h + "," + aBox.w + "] due to excess width");
                LeptUtils.dispose(aBox);
                continue;
            }
            if (aBox.h <= minimumHeightNeeded) {
                // System.out.println("Dropping box [" + aBox.x + "," + aBox.y + "," + aBox.h + "," + aBox.w + "] due to excess height");
                LeptUtils.dispose(aBox);
                continue;
            }
            boxes.add(new Rectangle(aBox.x, aBox.y, aBox.w, aBox.h));
            LeptUtils.dispose(aBox);
        }
        // arrange the boxes into lines

        // first, sort them based on X-End of their base
        Collections.sort(boxes,  new Comparator<Rectangle>() {

            @Override
            public int compare(Rectangle r1, Rectangle r2) {
                return (int) (r1.getX() + r1.getWidth() - r2.getX() - r2.getWidth());
            }

        });

        return boxes;
    }

    public static final BufferedImage extractClips(BufferedImage input, ArrayList<Rectangle> clipRectangles) {
        BufferedImage whiteCopy = ImageUtils.createWhiteCopy(input);
        Graphics2D g2d = whiteCopy.createGraphics();
        for (Rectangle clip : clipRectangles) {
            BufferedImage clippedImage = ImageUtils.clipBI(input, clip);
            g2d.drawImage(clippedImage, (int) clip.getX(), (int) clip.getY(), null);
        }
        g2d.dispose();
        return whiteCopy;
    }

    public static final BufferedImage extractBackgroundNormalisedClips(BufferedImage input, ArrayList<Rectangle> clipRectangles) {
        BufferedImage whiteCopy = ImageUtils.createWhiteCopy(input);
        Graphics2D g2d = whiteCopy.createGraphics();
        for (Rectangle clip : clipRectangles) {
            BufferedImage clippedImage = ImageUtils.clipBI(input, clip);
            Pix clippedPix = getDepth8Pix(clippedImage);
            Pix bnPix = Leptonica1.pixBackgroundNormFlex(clippedPix, 4, 4, 1, 1, 0);
            if (bnPix != null) {
                clippedImage = convertPixToImage(bnPix);
            } else {
                System.out.println("Skipping background normalisation");
            }
            LeptUtils.dispose(clippedPix);
            LeptUtils.dispose(bnPix);
            g2d.drawImage(clippedImage, (int) clip.getX(), (int) clip.getY(), null);
        }
        g2d.dispose();
        return whiteCopy;
    }

    public static final BufferedImage retainClip(BufferedImage image, Rectangle rectangle) {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = clone.createGraphics();
        g2d.setPaint ( new Color ( 255, 255, 255 ) );
        g2d.fillRect ( 0, 0, clone.getWidth(), clone.getHeight() );
        BufferedImage clippedImage = clipBI(image, rectangle);
        g2d.drawImage(clippedImage, (int) rectangle.getX(), (int) rectangle.getY(), null);
        g2d.dispose();
        return clone;
    }

    public static final BufferedImage retainRectangle(BufferedImage image, Rectangle rectangle) {
        return retainClip(image, rectangle);
    }

    public static final BufferedImage stretchHistogram(BufferedImage input1) {
        if (input1 == null) {
            throw new IllegalArgumentException("Histogram cannot be stretched for a null image");
        }
        BufferedImage output = rectifyBI(input1);
        int[][] grayValues = new int[output.getHeight()][output.getWidth()];
        int maxPixel = Integer.MIN_VALUE;
        int minPixel = Integer.MAX_VALUE;
        for (int y = 0; y < output.getHeight(); ++y) {
            for (int x = 0; x < output.getWidth(); ++x) {
                int currentPixel = output.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                maxPixel = Math.max(maxPixel, currentPixel);
                minPixel = Math.min(minPixel, currentPixel);
            }
        }
        if ((minPixel == 0) && (maxPixel == 255)) {
            return output;
        }
        int difference = maxPixel - minPixel;
        if (difference == 0) {
            return output;
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            for (int x = 0; x < output.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * 255);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage stretchHistogram(BufferedImage input1, Rectangle box) {
        if (input1 == null) {
            throw new IllegalArgumentException("Histogram cannot be stretched for a null image");
        }
        BufferedImage output = rectifyBI(input1);
        int[][] grayValues = new int[output.getHeight()][output.getWidth()];
        int maxPixel = Integer.MIN_VALUE;
        int minPixel = Integer.MAX_VALUE;
        for (int y = (int) box.getY(); y < Math.min(box.getY() + box.getHeight(), output.getHeight()); ++y) {
            for (int x = (int) box.getX(); x < Math.min(box.getX() + box.getWidth(), output.getWidth()); ++x) {
                int currentPixel = output.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                maxPixel = Math.max(maxPixel, currentPixel);
                minPixel = Math.min(minPixel, currentPixel);
            }
        }
        if ((minPixel == 0) && (maxPixel == 255)) {
            return output;
        }
        int difference = maxPixel - minPixel;
        if (difference == 0) {
            return output;
        }
        for (int y = (int) box.getY(); y < Math.min(box.getY() + box.getHeight(), output.getHeight()); ++y) {
            for (int x = (int) box.getX(); x < Math.min(box.getX() + box.getWidth(), output.getWidth()); ++x) {
                int currentPixel = grayValues[y][x];
                int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * 255);
                output.setRGB(x, y, ARGBPixels[newPixel]);
            }
        }
        return output;
    }

    public static final BufferedImage stretchLocalHistogram(BufferedImage input1, int xDivisionLength, int yDivisionLength) {
        if (input1 == null) {
            throw new IllegalArgumentException("Histogram cannot be stretched for a null image");
        }
        BufferedImage output = rectifyBI(input1);
        int[][] grayValues = new int[output.getHeight()][output.getWidth()];
        for (int y = 0; y < output.getHeight(); ++y) {
            for (int x = 0; x < output.getWidth(); ++x) {
                int currentPixel = output.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        for (int y = 0; y < output.getHeight(); y = y + yDivisionLength) {
            innerloop: for (int x = 0; x < output.getWidth(); x = x + xDivisionLength) {
                int yStart = y;
                int yEnd = Math.min(output.getHeight(), y + yDivisionLength);
                int xStart = x;
                int xEnd = Math.min(output.getWidth(), x + xDivisionLength);
                int maxPixel = Integer.MIN_VALUE;
                int minPixel = Integer.MAX_VALUE;
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        int currentPixel = grayValues[yCoord][xCoord];
                        maxPixel = Math.max(maxPixel, currentPixel);
                        minPixel = Math.min(minPixel, currentPixel);
                    }
                }
                if ((minPixel == 0) && (maxPixel == 255)) {
                    continue innerloop;
                }
                int difference = maxPixel - minPixel;
                if (difference == 0) {
                    continue innerloop;
                }
                for (int yCoord = yStart; yCoord < yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord < xEnd; ++xCoord) {
                        int currentPixel = grayValues[yCoord][xCoord];
                        int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * 255);
                        output.setRGB(xCoord, yCoord, ARGBPixels[newPixel]);
                    }
                }
            }
        }
        return output;
    }

    public static final BufferedImage stretchHistogramWithWhiteBias(BufferedImage input1, int whiteCutoff) {
        // ignore original pixels above whiteCutoff
        // at the end, stretch the rest of the histogram, and also convert all cells that are more than whiteCutoff to 255
        if (input1 == null) {
            throw new IllegalArgumentException("Histogram cannot be stretched for a null image");
        }
        BufferedImage output = rectifyBI(input1);
        int[][] grayValues = new int[output.getHeight()][output.getWidth()];
        int maxPixel = Integer.MIN_VALUE;
        int minPixel = Integer.MAX_VALUE;
        for (int y = 0; y < output.getHeight(); ++y) {
            for (int x = 0; x < output.getWidth(); ++x) {
                int currentPixel = output.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                if (currentPixel > whiteCutoff) {
                    continue;
                }
                maxPixel = Math.max(maxPixel, currentPixel);
                minPixel = Math.min(minPixel, currentPixel);
            }
        }
        if ((minPixel == 0) && (maxPixel == 255)) {
            return output;
        }
        int difference = maxPixel - minPixel;
        if (difference == 0) {
            return output;
        }
        for (int y = 0; y < output.getHeight(); ++y) {
            for (int x = 0; x < output.getWidth(); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > whiteCutoff) {
                    output.setRGB(x, y, ARGBPixels[255]);
                } else {
                    int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * 255);
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                }
            }
        }
        return output;
    }

    public static final BufferedImage enhanceContrastLocally(BufferedImage input1, int kernelX, int kernelY, int whiteCutoff, boolean onlyLighten) {
        if (input1 == null) {
            throw new IllegalArgumentException("Contrast enhancement cannot be done for a null image");
        }
        kernelX = Math.min(Math.max(7, kernelX), input1.getWidth());
        kernelY = Math.min(Math.max(7, kernelY), input1.getHeight());
        if (kernelX % 2 == 0) {
            kernelX = kernelX + 1;
        }
        if (kernelY % 2 == 0) {
            kernelY = kernelY + 1;
        }
        whiteCutoff = Math.min(whiteCutoff, 255);

        BufferedImage input = rectifyBI(input1, kernelX, kernelY);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int overallMin = Integer.MAX_VALUE;
        int overallMax = Integer.MIN_VALUE;
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                if ((y >= kernelY / 2) && (x >= kernelX / 2) && (y < input.getHeight() - kernelY / 2 - 1) && (x < input.getWidth() - kernelX / 2 - 1)) {
                    overallMin = Math.min(overallMin, currentPixel);
                    overallMax = Math.max(overallMax, currentPixel);
                }
            }
        }
        int overallDifference = overallMax - overallMin;
        for (int y = kernelY / 2; y < output.getHeight() - kernelY / 2 - 1; ++y) {
            innerloop: for (int x = kernelX / 2; x < output.getWidth() - kernelX / 2 - 1; ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > whiteCutoff) {
                    continue innerloop;
                }
                int yStart = Math.max(kernelY / 2, y - kernelY / 2);
                int yEnd = Math.min(output.getHeight() - kernelY / 2 - 2, y + kernelY / 2);
                int xStart = Math.max(kernelX / 2, x - kernelX / 2);
                int xEnd = Math.min(output.getWidth() - kernelX / 2 - 2, x + kernelX / 2);
                int maxPixel = Integer.MIN_VALUE;
                int minPixel = Integer.MAX_VALUE;
                int total = 0;
                int totalPixels = 0;
                outer : for (int yCoord = yStart; yCoord <= yEnd; ++yCoord) {
                    inner: for (int xCoord = xStart; xCoord <= xEnd; ++xCoord) {
                        // if (yCoord < kernelY / 2) {
                        //     continue outer;
                        // }
                        // if (xCoord < kernelX / 2) {
                        //     continue inner;
                        // }
                        // if (yCoord >= input.getHeight() - kernelY / 2) {
                        //    continue outer;
                        // }
                        // if (xCoord >= input.getWidth() - kernelX / 2) {
                        //    continue inner;
                        // }
                        int currentPixelInside = grayValues[yCoord][xCoord];
                        if (currentPixelInside > whiteCutoff) {
                            continue;
                        }
                        total += currentPixelInside;
                        ++totalPixels;
                        maxPixel = Math.max(maxPixel, currentPixelInside);
                        minPixel = Math.min(minPixel, currentPixelInside);
                    }
                }
                if ((minPixel > 255) || (maxPixel < 0)) {
                    continue innerloop;
                }
                if ((minPixel == 0) && (maxPixel == 255)) {
                    continue innerloop;
                }
                int difference = maxPixel - minPixel;
//                if (difference <= overallDifference / 5) {
//                    int average = (totalPixels != 0) ? total / totalPixels : (overallMin + overallMax) / 2;
//                    if (currentPixel < (average + minPixel) / 2) {
//                        output.setRGB(x, y, ARGBPixels[minPixel]);
//                    } else {
//                        output.setRGB(x, y, ARGBPixels[maxPixel]);
//                    }
//                    continue innerloop;
//                }
//                System.out.println("At [" + x +"," + y +"], maxPixel = " + maxPixel + ", minPixel = " + minPixel + ", currentPixel = " + currentPixel);
                int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * whiteCutoff);
                if (!onlyLighten) {
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                } else {
                    if (newPixel > currentPixel) {
                        output.setRGB(x, y, ARGBPixels[newPixel]);
                    }
                }
            }
        }

        Rectangle clipRect = new Rectangle(kernelX / 2, kernelY / 2, input.getWidth() - kernelX, input.getHeight() - kernelY);
        return clipBI(output, clipRect);
    }

    public static final BufferedImage enhanceContrastLocally(BufferedImage input1, Rectangle box, int kernelX, int kernelY, int whiteCutoff, boolean onlyLighten) {
        if (input1 == null) {
            throw new IllegalArgumentException("Contrast enhancement cannot be done for a null image");
        }
        kernelX = Math.min(Math.max(7, kernelX), (int) box.getWidth());
        kernelY = Math.min(Math.max(7, kernelY), (int) box.getHeight());
        if (kernelX % 2 == 0) {
            kernelX = kernelX + 1;
        }
        if (kernelY % 2 == 0) {
            kernelY = kernelY + 1;
        }

        BufferedImage input = rectifyBI(input1);
        BufferedImage output = copyBI(input);

        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        int overallMin = Integer.MAX_VALUE;
        int overallMax = Integer.MIN_VALUE;
        for (int y = Math.max(0,(int) (box.getY() - kernelY / 2)); y < Math.min(box.getY() + box.getHeight() + kernelY / 2, input.getHeight()); ++y) {
            for (int x = Math.max(0,(int) (box.getX() - kernelX / 2)); x < Math.min(box.getX() + box.getWidth() + kernelX / 2,input.getWidth()); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
                overallMin = Math.min(overallMin, currentPixel);
                overallMax = Math.max(overallMax, currentPixel);
            }
        }
        int overallDifference = overallMax - overallMin;

        for (int y = (int) box.getY(); y < Math.min(output.getHeight(), box.getY() + box.getHeight()); ++y) {
            innerloop: for (int x = (int) box.getX(); x < Math.min(output.getWidth(), box.getX() + box.getWidth()); ++x) {
                int currentPixel = grayValues[y][x];
                if (currentPixel > whiteCutoff) {
                    continue innerloop;
                }
                int yStart = Math.max((int) box.getY(),y - kernelY / 2);
                int yEnd = Math.min(output.getHeight() - 1, y + kernelY / 2);
                int xStart = Math.max((int) box.getX(),x - kernelX / 2);
                int xEnd = Math.min(output.getWidth() - 1, x + kernelX / 2);
                int maxPixel = Integer.MIN_VALUE;
                int minPixel = Integer.MAX_VALUE;
                int total = 0;
                int totalPixels = 0;
                for (int yCoord = yStart; yCoord <= yEnd; ++yCoord) {
                    for (int xCoord = xStart; xCoord <= xEnd; ++xCoord) {
                        int currentPixelInside = grayValues[yCoord][xCoord];
                        if (currentPixelInside > whiteCutoff) {
                            continue;
                        }
                        total += currentPixelInside;
                        ++totalPixels;
                        maxPixel = Math.max(maxPixel, currentPixelInside);
                        minPixel = Math.min(minPixel, currentPixelInside);
                    }
                }
                if ((minPixel > 255) || (maxPixel < 0)) {
                    continue innerloop;
                }
                if ((minPixel == 0) && (maxPixel == 255)) {
                    continue innerloop;
                }
                int difference = maxPixel - minPixel;
//                if (difference <= overallDifference / 5) {
//                    int average = (totalPixels != 0) ? total / totalPixels : (overallMin + overallMax) / 2;
//                    if (currentPixel < (average + minPixel) / 2) {
//                        output.setRGB(x, y, ARGBPixels[minPixel]);
//                    } else {
//                        output.setRGB(x, y, ARGBPixels[maxPixel]);
//                    }
//                    continue innerloop;
//                }
                int newPixel = (int) (((currentPixel - minPixel) * 1.0 / difference) * whiteCutoff);
                if (!onlyLighten) {
                    output.setRGB(x, y, ARGBPixels[newPixel]);
                } else {
                    if (newPixel > currentPixel) {
                        output.setRGB(x, y, ARGBPixels[newPixel]);
                    }
                }
            }
        }
        return output;
    }

    public static final double getGamma(BufferedImage input) {
        int average = getAverage(input);
        if (average > 245) {
            return 0.05;
        }
        if (average > 240) {
            return 0.1;
        }
        if (average > 230) {
            return 0.15;
        }
        if (average > 220) {
            return 0.25;
        }
        if (average > 210) {
            return 0.4;
        }
        if (average > 200) {
            return 0.6;
        }
        if (average > 190) {
            return 0.8;
        }
        if (average > 175) {
            return 0.9;
        }
        if (average > 150) {
            return 1.0;
        }
        if (average > 125) {
            return 1.2;
        }
        if (average > 100) {
            return 1.35;
        }
        if (average > 75) {
            return 1.5;
        }
        if (average > 50) {
            return 1.8;
        }
        return 2.0;
    }

    public static final double getGamma(double imageAverage) {
        if (imageAverage > 247) {
            return 0.05;
        }
        if (imageAverage > 244) {
            return 0.01;
        }
        if (imageAverage > 240) {
            return 0.15;
        }
        if (imageAverage > 230) {
            return 0.20;
        }
        if (imageAverage > 220) {
            return 0.30;
        }
        if (imageAverage > 210) {
            return 0.4;
        }
        if (imageAverage > 200) {
            return 0.6;
        }
        if (imageAverage > 190) {
            return 0.8;
        }
        if (imageAverage > 175) {
            return 0.9;
        }
        if (imageAverage > 150) {
            return 1.0;
        }
        if (imageAverage > 125) {
            return 1.2;
        }
        if (imageAverage > 100) {
            return 1.35;
        }
        if (imageAverage > 75) {
            return 1.5;
        }
        if (imageAverage > 50) {
            return 1.8;
        }
        return 2.0;
    }

    public static final BufferedImage constantGammaEnhancement(BufferedImage input, double gammaFactor) {
        input = rectifyBI(input);
        BufferedImage output = copyBI(input);
        if (gammaFactor > 7) {
            gammaFactor = 6.99;
        }
        if (gammaFactor < 0) {
            gammaFactor = 0.01;
        }
        Integer gammaInt = Integer.valueOf((int) Math.round(gammaFactor / 0.05));
        double[] multipliers = gammaMultipliersMap.get(gammaInt);
        int[] newPixels = new int[256];
        for (int i = 0; i < newPixels.length; ++i) {
            int newPixel = (int) (255 * multipliers[i]);
            newPixels[i] = ARGBPixels[newPixel];
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                output.setRGB(x, y, newPixels[currentPixel]);
            }
        }
        return output;
    }

    public static void applyQualityRenderingHints(Graphics2D g2d) {

        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    }

    public static final Rectangle getBoundingRectangle(javafx.scene.shape.Polygon polygon) {
        ObservableList<Double> points = polygon.getPoints();
        Double minX = Double.MAX_VALUE;
        Double minY = Double.MAX_VALUE;
        Double maxX = Double.MIN_VALUE;
        Double maxY = Double.MIN_VALUE;
        for (int i = 0; i < points.size(); i = i + 2) {
            Double pt1 = points.get(i);
            Double pt2 = points.get(i+1);
            minX = Math.min(minX, pt1);
            maxX = Math.max(maxX, pt1);
            minY = Math.min(minY, pt2);
            maxY = Math.max(maxY, pt2);
        }
        Rectangle boundingRectangle = new Rectangle(minX, minY, maxX - minX, maxY - minY);
        return boundingRectangle;
    }
}
