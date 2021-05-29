package com.costheta.vision.datamatrix;

import static net.sourceforge.lept4j.ILeptonica.IFF_TIFF;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import org.imgscalr.Scalr;
import org.libdmtx.DMTXImage;
import org.libdmtx.DMTXTag;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.ptr.PointerByReference;
import com.costheta.vision.Detections;

import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;

public class DataMatrixDetector {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\temp\\20210314121300";

    private boolean debug = false;
    private boolean imageDebug = false;

    private static final long serialVersionUID = 1L;
    private static int isWindows = 2;

    private static ExecutorService parallelThreadPool = Executors.newFixedThreadPool(12);

    private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
            new float[] { 1.0f }, 0.0f);

    private BufferedImage troll = null;

    private String goodBeepFilePath = "GoodBeep.wav";
    private File goodBeepFile = null;
    private Clip goodBeepClip;
    private AudioInputStream goodBeepAudioInputStream;

    private String badBeepFilePath = "BadBeep.wav";
    private File badBeepFile = null;
    private Clip badBeepClip;
    private AudioInputStream badBeepAudioInputStream;

    private String readyBeepFilePath = "ReadyBeep.wav";
    private File readyBeepFile = null;
    private Clip readyBeepClip;
    private AudioInputStream readyBeepAudioInputStream;

    private String propFilePath = "config.properties";
    private File propFile = new File(propFilePath);

    private long goodBeepLength = 0L;
    private long badBeepLength = 0L;

    private static HashMap<Integer, double[]> gammaMultipliedMap = createGammaMultipliedMap(7);
    private static final double[] gammaCubedTable = new double[2000];
    private static final double[][] sigmoidFactorsTable = new double[101][766];
    private static final int[] rgbValues = new int[256];

    private boolean visibility = true;
    private long sleepTimeAfterDetection = 500L;
    private long sleepTimeWhenNothingDetected = 500L;
    private String filePrefix = "DM-Output";
    private String outputDirectory = "output";
    private String imageOutputDirectory = "images";
    private int count = 0;

    private Map<DecodeHintType, Object> hintsMap = null;

    private static final String EMPTY_STRING = "";
    private static final String NULL_STRING = "null";
    private static final ArrayList<String> SPURIOUS_VALUES = new ArrayList<String>() {
        {
            add("DataMatrix");
        }
    };
    private static final BufferedImage DEFAULT_IMAGE = new BufferedImage(10, 10, BufferedImage.TYPE_BYTE_GRAY);

    private int imageCounter = 0;

    public DataMatrixDetector() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        super();

        try {
            troll = ImageIO.read(new File("Initialisation.jpg")); // loads up libraries
        } catch (Exception e) {
            System.out.println("Couldn't read troll image");
        }

        List<BarcodeFormat> barcodeFormats = new ArrayList<>();
        barcodeFormats.add(BarcodeFormat.DATA_MATRIX);
        // barcodeFormats.add(BarcodeFormat.AZTEC);
        // barcodeFormats.add(BarcodeFormat.MAXICODE);
        // barcodeFormats.add(BarcodeFormat.QR_CODE);
        // barcodeFormats.add(BarcodeFormat.PDF_417);

        hintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        // hintsMap.put(DecodeHintType.POSSIBLE_FORMATS,
        // EnumSet.allOf(BarcodeFormat.class));
        hintsMap.put(DecodeHintType.POSSIBLE_FORMATS, barcodeFormats);
        hintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);

        System.out.println("Now starting the beep uploads");

        goodBeepFile = new File(goodBeepFilePath).getAbsoluteFile();
        badBeepFile = new File(badBeepFilePath).getAbsoluteFile();
        readyBeepFile = new File(readyBeepFilePath).getAbsoluteFile();
        goodBeepAudioInputStream = AudioSystem.getAudioInputStream(goodBeepFile);
        badBeepAudioInputStream = AudioSystem.getAudioInputStream(badBeepFile);
        readyBeepAudioInputStream = AudioSystem.getAudioInputStream(readyBeepFile);

        // create clip reference
        goodBeepClip = AudioSystem.getClip();
        badBeepClip = AudioSystem.getClip();
        readyBeepClip = AudioSystem.getClip();

        // open audioInputStream to the clip
        goodBeepClip.open(goodBeepAudioInputStream);
        badBeepClip.open(badBeepAudioInputStream);
        readyBeepClip.open(readyBeepAudioInputStream);

        goodBeepLength = goodBeepClip.getMicrosecondLength() / 1000 + 5;
        badBeepLength = badBeepClip.getMicrosecondLength() / 1000 + 5;
        long readyBeepLength = readyBeepClip.getMicrosecondLength() / 1000 + 5;
        readyBeepClip.start();
        try {
            TimeUnit.MILLISECONDS.sleep(readyBeepLength);
        } catch (Exception e) {

        }
        readyBeepClip.stop();
        readyBeepClip.close();

        // goodBeepClip.loop(Clip.LOOP_CONTINUOUSLY);
        // badBeepClip.loop(Clip.LOOP_CONTINUOUSLY);

        // create the output directory
        outputDirectory = inputDirectory + "/" + outputDirectory;
        imageOutputDirectory = inputDirectory + "/" + imageOutputDirectory;
        try {
            Files.createDirectories(Paths.get(outputDirectory).toAbsolutePath());
            Files.createDirectories(Paths.get(imageOutputDirectory).toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Couldn't create directories");
        }

        System.out.println("Ready to detect");
        process();

    }

    private void process() {

        List<String> files = filterFiles(listFiles(inputDirectory, 1), "DM2", true);
        System.out.println("The files are - " + files);
        Instant t = Instant.now();
		for (String filePath : files) {
			System.out.println("Processing - " + filePath);
            BufferedImage input = null;
			try {
				input = ImageIO.read(new File(filePath));
			} catch (Exception e) {
			}
            processInput(input, null);
//			processInput(input, filePath);
        }
        long timeTaken = Duration.between(t, Instant.now()).toMillis();
        System.out.println("Time taken per scan = " + timeTaken / (files.size() > 0 ?
        files.size() : 1));
    }

    private void processInput(BufferedImage input, String filePath) {
        BufferedImage originalImage = makeGrayBI(input);
        Instant t = Instant.now();
        final ArrayList<CompletableFuture<BufferedImage>> scalingThreads = new ArrayList<CompletableFuture<BufferedImage>>(
                2);

        scalingThreads.add(CompletableFuture.supplyAsync(() -> {
            BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC,
                    (int) (originalImage.getWidth() * 1.4), (int) (originalImage.getHeight() * 1.4),
                    Scalr.OP_ANTIALIAS);
            return scaledImage;
        }, parallelThreadPool));
        scalingThreads.add(CompletableFuture.supplyAsync(() -> {
            BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC,
                    (int) (originalImage.getWidth() * 2.0), (int) (originalImage.getHeight() * 2.0),
                    Scalr.OP_ANTIALIAS);
            return scaledImage;
        }, parallelThreadPool));

        CompletableFuture.allOf(scalingThreads.toArray(new CompletableFuture[scalingThreads.size()])).join();

        BufferedImage scaledImage1t = null;
        BufferedImage scaledImage2t = null;

        try {
            scaledImage1t = scalingThreads.get(0).get();
            scaledImage2t = scalingThreads.get(1).get();
        } catch (Exception e) {

        }

        final BufferedImage scaledImage1 = scaledImage1t;
        final BufferedImage scaledImage2 = scaledImage2t;

        final ArrayList<CompletableFuture<BufferedImage>> enhancementThreads = new ArrayList<CompletableFuture<BufferedImage>>(
                9);

        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            // final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(originalImage, 41, 41, 0.0,
            //        1.0);
            final BufferedImage imageEnhanced = copy(originalImage);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(originalImage, 33, 33, 0.0,
//                    1.0);
            final BufferedImage imageEnhanced = copy(originalImage);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(originalImage, 25, 25,
//                    0.0, 1.0);
            final BufferedImage imageEnhanced = copy(originalImage);
            return imageEnhanced;
        }, parallelThreadPool));

        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage1, 25, 25, 0.0,
//                    1.0); // 9,9
            final BufferedImage imageEnhanced = copy(scaledImage1);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage1, 19, 19,
//                    0.0, 1.0); // 11,11
            final BufferedImage imageEnhanced = copy(scaledImage1);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage1, 13, 13,
//                    0.0, 1.0); // 15, 15
            final BufferedImage imageEnhanced = copy(scaledImage1);
            return imageEnhanced;
        }, parallelThreadPool));

        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage2, 23, 23,
//                    0.0, 1.0); // 9,9
            final BufferedImage imageEnhanced = copy(scaledImage2);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage2, 17, 17,
//                    0.0, 1.0); // 11,11
            final BufferedImage imageEnhanced = copy(scaledImage2);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
//            final BufferedImage imageEnhanced = contrastEnhancementByAverageAndGammaCorrection(scaledImage2, 11, 11,
//                    0.0, 1.0); // 15, 15
            final BufferedImage imageEnhanced = copy(scaledImage2);
            return imageEnhanced;
        }, parallelThreadPool));

        CompletableFuture.allOf(enhancementThreads.toArray(new CompletableFuture[enhancementThreads.size()])).join();

        BufferedImage image1t = null;
        BufferedImage image2t = null;
        BufferedImage image3t = null;
        BufferedImage image4t = null;
        BufferedImage image5t = null;
        BufferedImage image6t = null;
        BufferedImage image7t = null;
        BufferedImage image8t = null;
        BufferedImage image9t = null;

        try {
            image1t = enhancementThreads.get(0).get();
            image2t = enhancementThreads.get(1).get();
            image3t = enhancementThreads.get(2).get();
            image4t = enhancementThreads.get(3).get();
            image5t = enhancementThreads.get(4).get();
            image6t = enhancementThreads.get(5).get();
            image7t = enhancementThreads.get(6).get();
            image8t = enhancementThreads.get(7).get();
            image9t = enhancementThreads.get(8).get();
        } catch (Exception e) {

        }

        final BufferedImage image1 = image1t;
        final BufferedImage image2 = image2t;
        final BufferedImage image3 = image3t;
        final BufferedImage image4 = image4t;
        final BufferedImage image5 = image5t;
        final BufferedImage image6 = image6t;
        final BufferedImage image7 = image7t;
        final BufferedImage image8 = image8t;
        final BufferedImage image9 = image9t;

        writeFile(image1,"jpg",imageOutputDirectory + "/image1.jpg");
        writeFile(image2,"jpg",imageOutputDirectory + "/image2.jpg");
        writeFile(image3,"jpg",imageOutputDirectory + "/image3.jpg");
        writeFile(image4,"jpg",imageOutputDirectory + "/image4.jpg");
        writeFile(image5,"jpg",imageOutputDirectory + "/image5.jpg");
        writeFile(image6,"jpg",imageOutputDirectory + "/image6.jpg");
        writeFile(image7,"jpg",imageOutputDirectory + "/image7.jpg");
        writeFile(image8,"jpg",imageOutputDirectory + "/image8.jpg");
        writeFile(image9,"jpg",imageOutputDirectory + "/image9.jpg");


        final ArrayList<CompletableFuture<Object>> processingThreads = new ArrayList<CompletableFuture<Object>>(12);
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image1);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image2);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image3);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image4);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image5);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image6);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image7);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image8);
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readDMCode(image9);
        }, parallelThreadPool));

        CompletableFuture.allOf(processingThreads.toArray(new CompletableFuture[processingThreads.size()])).join();

        Detections dmDetections1 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections2 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections3 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections4 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections5 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections6 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections7 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections8 = Detections.EMPTY_DETECTIONS;
        Detections dmDetections9 = Detections.EMPTY_DETECTIONS;

        try {

            dmDetections1 = (Detections) processingThreads.get(0).get();
            dmDetections2 = (Detections) processingThreads.get(1).get();
            dmDetections3 = (Detections) processingThreads.get(2).get();
            dmDetections4 = (Detections) processingThreads.get(3).get();
            dmDetections5 = (Detections) processingThreads.get(4).get();
            dmDetections6 = (Detections) processingThreads.get(5).get();
            dmDetections7 = (Detections) processingThreads.get(6).get();
            dmDetections8 = (Detections) processingThreads.get(7).get();
            dmDetections9 = (Detections) processingThreads.get(8).get();
        } catch (Exception e) {

        }

        String dmGoodResults1 = getResults(dmDetections1);
        dmGoodResults1 = dmGoodResults1.trim();
        System.out.println("Result 1 = " + dmGoodResults1);

        String dmGoodResults2 = getResults(dmDetections2);
        dmGoodResults2 = dmGoodResults2.trim();
        System.out.println("Result 2 = " + dmGoodResults2);

        String dmGoodResults3 = getResults(dmDetections3);
        dmGoodResults3 = dmGoodResults3.trim();
        System.out.println("Result 3 = " + dmGoodResults3);

        String dmGoodResults4 = getResults(dmDetections4);
        dmGoodResults4 = dmGoodResults4.trim();
        System.out.println("Result 4 = " + dmGoodResults4);

        String dmGoodResults5 = getResults(dmDetections5);
        dmGoodResults5 = dmGoodResults5.trim();
        System.out.println("Result 5 = " + dmGoodResults5);

        String dmGoodResults6 = getResults(dmDetections6);
        dmGoodResults6 = dmGoodResults6.trim();
        System.out.println("Result 6 = " + dmGoodResults6);

        String dmGoodResults7 = getResults(dmDetections7);
        dmGoodResults7 = dmGoodResults7.trim();
        System.out.println("Result 7 = " + dmGoodResults7);

        String dmGoodResults8 = getResults(dmDetections8);
        dmGoodResults8 = dmGoodResults8.trim();
        System.out.println("Result 8 = " + dmGoodResults8);

        String dmGoodResults9 = getResults(dmDetections9);
        dmGoodResults9 = dmGoodResults9.trim();
        System.out.println("Result 9 = " + dmGoodResults9);

        String result = concatenateResults(dmGoodResults1, dmGoodResults2, dmGoodResults3, dmGoodResults4,
                dmGoodResults5, dmGoodResults6, dmGoodResults7, dmGoodResults8, dmGoodResults9);

        boolean detected = false;

        if (!result.isEmpty()) {
            detected = true;
        }

        if (detected) {
            playGoodBeep();
            writeOutput(result);
            if (debug) {
                System.out.println("Final result = " + result);
                System.out.println("-----------");
            }
        } else {
            if (debug) {
                System.out.println("Final result = ");
                System.out.println("-----------");
            }
        }

        if (!detected) {
            playBadBeep();
        }
        if (imageDebug) {
            if (filePath != null) {
                StringBuffer outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : "bad-")
                        .append("image-").append(getJustFileName(filePath).substring(6)).append(".png");
                try {
                    writeFile(image1, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
                outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : "bad-")
                        .append("scaledImage20-").append(getJustFileName(filePath).substring(6)).append(".png");
                try {
                    writeFile(image5, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
                outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : "bad-")
                        .append("scaledImage25-").append(getJustFileName(filePath).substring(6)).append(".png");
                try {
                    writeFile(image9, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
            } else {
                StringBuffer outputFilePath = new StringBuffer(imageOutputDirectory).append("/image-")
                        .append(++imageCounter).append(".png");
                try {
                    writeFile(originalImage, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
            }
        }
        long tp = Duration.between(t, Instant.now()).toMillis();

    }

    private static int isWindows() {

        if (isWindows != 2) {
            return isWindows;
        }
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") >= 0) {
            isWindows = 1;
        } else {
            isWindows = 0;
            Process plsof = null;
            // if Unix, then umount debugfs to save time during the loop that checks if a
            // file change triggered by watchservice response has finished being in writable
            // state
            try {
                plsof = new ProcessBuilder(new String[] { "sudo", "mount", "|", "grep", "debugfs" }).start();
                plsof.destroy();
            } catch (Exception ex) {
                // TODO: handle exception ...
            }
            try {
                plsof = new ProcessBuilder(new String[] { "sudo", "umount", "$(mount", "|", "grep", "debugfs", "|",
                        "awk", "'{print", "$3}')" }).start();
                plsof.destroy();
            } catch (Exception ex) {
                // TODO: handle exception ...
            }
        }
        return isWindows;

    }

    public static void main(String[] args) throws Exception {
        Leptonica1.setLeptDebugOK(0);
        isWindows();
        populateGammaCubedTable();
        createGammaMultipliedMap(7);
        populateSigmoidFactorsTable();
        populateRGBValues();
        new DataMatrixDetector();
    }

    private static final BufferedImage makeGrayBI(BufferedImage image) {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = clone.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return clone;
    }

    private static HashMap<Integer, double[]> createGammaMultipliedMap(int maxGamma) {
        HashMap<Integer, double[]> gammaMultipliedMap = new HashMap<>();
        for (double d = 0.0; d <= maxGamma; d = d + 0.05) {
            int key = (int) Math.round(d / 0.05);
            double[] gammaMultipliedPixels = getGammaMultipliedPixels(d);
            gammaMultipliedMap.put(Integer.valueOf(key), gammaMultipliedPixels);
            // System.out.println("Created gamma multipliers for " + d);
        }
        // System.out.println("Created all the gamma multipliers");
        return gammaMultipliedMap;
    }

    private static double[] getGammaMultipliedPixels(double gamma) {
        double[] gammaMultipliedPixels = new double[256];
        for (int i = 0; i < 256; ++i) {
            gammaMultipliedPixels[i] = 255 * Math.pow((double) i / (double) 255, 1.0 / gamma);
        }
        return gammaMultipliedPixels;
    }

    private static void populateGammaCubedTable() {
        for (int x = 0; x < 2000; ++x) {
            double gamma = x * 1.0 / 1000;
            gammaCubedTable[x] = gamma * gamma * gamma;
        }
    }

    private BufferedImage relativeGammaContrastEnhancement(BufferedImage input, int kernelX, int kernelY,
                                                           int percentile) {
        if (percentile < 0) {
            percentile = 0;
        }
        if (percentile > 100) {
            percentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        int indexOfBaseComparisonPixelInKernel = (int) ((percentile * 1.0 / 100.0) * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX; x < input.getWidth() - kernelX / 2; ++x) {
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
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                int newPixel = (int) multipliers[currentPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private BufferedImage contrastEnhancementByProgressiveAverage(BufferedImage input, int kernelX, int kernelY,
                                                                  int difference) {
        // discovers the right percentile for each kernel
        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        int kernelSize = kernelX * kernelY;
        int medianIndex = kernelSize / 2;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int[] dPixel = new int[kernelSize - 1];
                for (int i = 0; i < kernelSize - 1; ++i) {
                    dPixel[i] = pixelArray[i + 1] - pixelArray[i];
                }
                int[] d2Pixel = new int[kernelSize - 2];
                int maxd2 = -255;
                int maxd2Index = 0;
                // int secondMaxd2Index = 0;
                for (int i = 0; i < kernelSize - 2; ++i) {
                    d2Pixel[i] = dPixel[i + 1] - dPixel[i];
                    if (d2Pixel[i] > maxd2) {
                        maxd2 = d2Pixel[i];
                        // secondMaxd2Index = maxd2Index;
                        maxd2Index = i;
                    }
                }
                maxd2Index += 2;
//				if ((maxd2Index >= kernelSize - 1) || (maxd2Index <= 1)) {
//					maxd2Index = secondMaxd2Index;
//				}
//				if ((maxd2Index >= kernelSize - 1) || (maxd2Index <= 1)) {
//					maxd2Index = medianIndex;
//				}
//				int average = pixelArray[0];
//				int total = pixelArray[0];
//				int currentJump = 0;
//				int pixelForComparison = pixelArray[0];
//				boolean afterMedian = true;
//				for (int i = 1; i < kernelSize; ++i) {
//					currentJump = pixelArray[i] - average;
//					total += pixelArray[i];
//					average = total / (i + 1);
//					if (currentJump > difference) {
//						if (i < medianIndex) {
//							afterMedian = false;
//						}
//						break;
//					}
//					pixelForComparison = pixelArray[i];
//				}
                int base = pixelArray[maxd2Index];
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * (base + 1))), 1999);
                gamma = Math.min(Math.max(gamma + (gamma >= 1000 ? 100 : -100), 0), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                int newPixel = (int) multipliers[currentPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private BufferedImage contrastEnhancementByAverage(BufferedImage input, int kernelX, int kernelY,
                                                       double tolerance) {
        // discovers the right percentile for each kernel
        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        if (tolerance > 0.2) {
            tolerance = 0.2;
        }
        if (tolerance < 0) {
            tolerance = 0.0;
        }
        int upperToleranceLevel = (int) (1000 * (1 + tolerance));
        int lowerToleranceLevel = (int) (1000 * (1 - tolerance));
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int kernelSize = kernelX * kernelY;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += pixel;
                    }
                }
                int base = total / kernelSize;
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * (base + 1))), 1999);

                gamma = Math.min(Math.max(
                        gamma + (gamma >= upperToleranceLevel ? 200 : (gamma < lowerToleranceLevel ? -200 : 0)), 0),
                        1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                int newPixel = (int) multipliers[currentPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private BufferedImage contrastEnhancementByAverageAndGammaCorrection(BufferedImage input1, int kernelX, int kernelY,
                                                                         double tolerance, double gammaCorrection) {

        if (input1 == null) {
            return DEFAULT_IMAGE;
        }
        BufferedImage input = bilateralTransform(input1);
        // discovers the right percentile for each kernel
        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        if (tolerance > 0.2) {
            tolerance = 0.2;
        }
        if (tolerance < 0) {
            tolerance = 0.0;
        }
        if (gammaCorrection < 0) {
            gammaCorrection = 0.0;
        }
        if (gammaCorrection > 7) {
            gammaCorrection = 7.0;
        }

        Integer gammaCorrectionInt = Integer.valueOf((int) Math.round(gammaCorrection / 0.05));
        double[] gammaCorrectionMultipliers = gammaMultipliedMap.get(gammaCorrectionInt);
        int upperToleranceLevel = (int) (1000 * (1 + tolerance));
        int lowerToleranceLevel = (int) (1000 * (1 - tolerance));
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int kernelSize = kernelX * kernelY;
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                int total = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        total += pixel;
                    }
                }
                int base = total / kernelSize;
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * (base + 1))), 1999);

                gamma = Math.min(Math.max(
                        gamma + (gamma >= upperToleranceLevel ? 200 : (gamma < lowerToleranceLevel ? -200 : 0)), 0),
                        1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                int newPixel = (int) multipliers[currentPixel];
                newPixel = (int) gammaCorrectionMultipliers[newPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private String readQRCode_ZXing(BufferedImage image) {
        // Instant t = Instant.now();
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = null;
        try {
            result = new MultiFormatReader().decode(binaryBitmap, hintsMap);
            // result = new MultiFormatReader().decode(binaryBitmap);
        } catch (Exception e) {

        }
        // long timeTaken = Duration.between(t, Instant.now()).toMillis();
        // if (debug) {
        // System.out.println("Time taken by alternate method = " + timeTaken);
        // }
        if ((debug) && (result != null)) {
            System.out.println("Output of alternate method = " + result.getText());
        }
        String finalResult = DataMatrixDetector.EMPTY_STRING;
        if (result != null) {
            finalResult = result.getText();
        }
        if (NULL_STRING.equals(finalResult)) {
            return DataMatrixDetector.EMPTY_STRING;
        }
        finalResult.replace(NULL_STRING, EMPTY_STRING);
        if (SPURIOUS_VALUES.contains(finalResult)) {
            return DataMatrixDetector.EMPTY_STRING;
        }
        return finalResult;
    }


    private Detections readDMCode(BufferedImage image) {
        DMTXImage lDImg = new DMTXImage(image);
        DMTXTag[] tags = lDImg.getTags(1, 500);
        List<String> tagValues = new ArrayList<>();

        if (tags != null) {
            for (DMTXTag tag : tags) {
                tagValues.add(tag.id);
                 System.out.println(
                 "Tag Found! Coord: " + tag.corner1.getX() + "," + tag.corner1.getY() + " Tag value: " + tag.id);
            }
        } else {
            System.out.println("No tag found");
        }
        return new Detections(tagValues);
    }

    private void playBadBeep() {
        badBeepClip.start();
        try {
            TimeUnit.MILLISECONDS.sleep(badBeepLength);
        } catch (Exception e) {

        }
        badBeepClip.setFramePosition(0);
    }

    private void playGoodBeep() {
        goodBeepClip.start();
        try {
            TimeUnit.MILLISECONDS.sleep(goodBeepLength);
        } catch (Exception e) {

        }
        goodBeepClip.setFramePosition(0);
    }

    private String getResults(Detections detections) {
        if (detections == null) {
            return DataMatrixDetector.EMPTY_STRING;
        }
        StringBuffer result = new StringBuffer(DataMatrixDetector.EMPTY_STRING);
        for (String dm : detections.detections) {
            // The message encoded in the marker
            if ((dm == null) || (DataMatrixDetector.EMPTY_STRING.equals(dm))) {
                continue;
            }
            if (SPURIOUS_VALUES.contains(dm)) {
                continue;
            }
            if (debug) {
                // System.out.println("DM message: " + dm);
            }
            result.append(dm).append(System.lineSeparator());
        }
        return result.toString();
    }

    private void writeOutput(String input) {
        Path outputFilePath = Paths.get(outputDirectory, filePrefix + "-" + ++count + ".txt").toAbsolutePath();
        try {
            Path outputFile = Files.createFile(outputFilePath);
            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.BufferedWriter(new java.io.FileWriter(outputFile.toString(), true)));
            writer.println(input);
            writer.flush();
            writer.close();
        } catch (Exception e) {

        }
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile) {
        return writeFile(bufferedImage, formatName, localOutputFile, 300);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi) {
        return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                              float compressionQuality) {

        if (bufferedImage == null) {
            return false;
        }
        RenderedImage[] input = new RenderedImage[1];
        input[0] = bufferedImage;
        return writeFile(input, formatName, localOutputFile, dpi, compressionQuality);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi)
             {
        return writeFile(images, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
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
                    TIFFDirectory dir = null;
                    try {
                        dir = TIFFDirectory.createFromMetadata(md);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

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
                    try {
                        md.setFromTree("javax_imageio_jpeg_image_1.0", root);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }

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

                        try {
                            md.mergeTree("javax_imageio_1.0", root);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }

                // Create output stream
                try {
                    output = ImageIO.createImageOutputStream(new File(localOutputFile));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                writer.setOutput(output);

                // Optionally, listen to progress, warnings, etc.

                // writeParam = writer.getDefaultWriteParam();
                if (images.length > 1) {
                    try {
                        writer.prepareWriteSequence(md);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
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
                try {
                    writer.write(null, temp, writeParam);
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                // writer.endWriteEmpty();
                if (images.length > 1) {
                    try {
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
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                // writer.endWriteSequence();
            } finally

            {
                // Close stream in finally block to avoid resource leaks
                if (output != null) {

                    try {
                        output.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        } finally

        {
            // Dispose writer in finally block to avoid memory leaks
            if (writer != null) {
                writer.dispose();
            }
        }
        return true;
    }

    public static BufferedImage relativeGammaWithSigmoidCorrection(BufferedImage input, int kernelX, int kernelY,
                                                                   int gammaPercentile, double sigmoidMultiplier, int pixelDiffWithinWhichPixelsAreRetained) {
        if (gammaPercentile < 0) {
            gammaPercentile = 0;
        }
        if (gammaPercentile > 100) {
            gammaPercentile = 100;
        }
        kernelX = Math.min(Math.max(1, kernelX), input.getWidth() / 2);
        kernelY = Math.min(Math.max(1, kernelY), input.getHeight() / 2);
        if (kernelX % 2 == 0) {
            kernelX = Math.max(1, kernelX - 1);
        }
        if (kernelY % 2 == 0) {
            kernelY = Math.max(1, kernelY - 1);
        }
        if (sigmoidMultiplier < 0) {
            sigmoidMultiplier = 0;
        }
        if (sigmoidMultiplier > 10) {
            sigmoidMultiplier = 10;
        }
        int sigmoidMultiplierIndex = (int) Math.round(sigmoidMultiplier / 0.1);

        if (pixelDiffWithinWhichPixelsAreRetained < 8) {
            pixelDiffWithinWhichPixelsAreRetained = 8;
        }
        if (pixelDiffWithinWhichPixelsAreRetained > 32) {
            pixelDiffWithinWhichPixelsAreRetained = 32;
        }

        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        int indexOfBaseComparisonPixelInKernel = (int) ((gammaPercentile * 1.0 / 100.0) * kernelX * kernelY);
//		int minIndexForSigmoidCalc = Math.min(indexOfBaseComparisonPixelInKernel,
//				(int) ((30 * 1.0 / 100.0) * kernelX * kernelY));
//		int maxIndexForSigmoidCalc = Math.max(indexOfBaseComparisonPixelInKernel,
//				(int) ((70 * 1.0 / 100.0) * kernelX * kernelY));
        int minIndexForSigmoidCalc = (int) ((40 * 1.0 / 100.0) * kernelX * kernelY);
        int maxIndexForSigmoidCalc = (int) ((60 * 1.0 / 100.0) * kernelX * kernelY);
        int[][] grayValues = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                grayValues[y][x] = currentPixel;
            }
        }
        int[] pixelArray = new int[kernelX * kernelY];
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX; x < input.getWidth() - kernelX / 2; ++x) {
                int currentPixel = grayValues[y][x];
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = grayValues[y + deltaY][x + deltaX];
                        pixelArray[(deltaY + kernelY / 2) * kernelX + (deltaX + kernelX / 2)] = pixel;
                    }
                }
                Arrays.sort(pixelArray);
                int base = Math.max(pixelArray[indexOfBaseComparisonPixelInKernel], 1);
                int minSig = Math.max(pixelArray[minIndexForSigmoidCalc], 1);
                int maxSig = Math.max(pixelArray[maxIndexForSigmoidCalc], 1);
                int sigDiff = Math.max(maxSig - minSig, pixelDiffWithinWhichPixelsAreRetained);
                int pixelDiff = Math.abs(currentPixel - base);
                int diff = pixelDiff - sigDiff;
                double sigmoidFactor = sigmoidFactorsTable[sigmoidMultiplierIndex][diff + 510];
                currentPixel = (int) (base + (currentPixel - base) * sigmoidFactor);
                int gamma = Math.min((int) Math.round(currentPixel * 1.0 / (0.001 * base)), 1999);
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                // int newPixel = (int) (255 * multipliers[currentPixel]);
                int newPixel = (int) multipliers[currentPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private static void populateSigmoidFactorsTable() {
        for (int y = 0; y < 101; ++y) {
            for (int x = -510; x <= 255; ++x) {
                sigmoidFactorsTable[y][x + 510] = 1 / (1 + Math.exp(-y * x * 1.0 / 10));
            }
        }
    }

    private List<String> listFiles(int depth) {
        return listFiles(inputDirectory, depth);
    }

    private List<String> listFiles(String inputDir, int depth) {
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

    private List<String> filterFiles(List<String> filesInDirectory, String filterString, boolean filterIn) {
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

    private static void populateRGBValues() {
        for (int i = 0; i < 256; ++i) {
            rgbValues[i] = (i << 16) | (i << 8) | i;
        }
    }

    // up == pixels above cutoff are blackened
    private BufferedImage binarizeWithCutOff(BufferedImage input, double cutOff, boolean up) {
        int blackPixel = 0x00000000;
        int whitePixel = 0x00FFFFFF;
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int currentPixel = input.getRGB(x, y) & 0xFF;
                int newPixel = up ? (currentPixel >= cutOff ? blackPixel : whitePixel)
                        : (currentPixel <= cutOff ? blackPixel : whitePixel);
                output.setRGB(x, y, newPixel);
            }
        }
        return output;
    }

    private BufferedImage blur(BufferedImage input, int kernelX, int kernelY) {
        kernelX = kernelX % 2 != 0 ? kernelX : kernelX + 1;
        kernelY = kernelY % 2 != 0 ? kernelY : kernelY + 1;
        if ((kernelX <= 0) || (kernelY <= 0)) {
            return input;
        }
        if ((kernelX <= 1) && (kernelY <= 1)) {
            return input;
        }
        int[][] pixels = new int[input.getHeight()][input.getWidth()];
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = input.getRGB(x, y) & 0xFF;
                pixels[y][x] = pixel;
            }
        }
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < kernelY / 2; ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = pixels[y][x];
                output.setRGB(x, y, rgbValues[pixel]);
            }
        }
        for (int y = input.getHeight() - kernelY / 2; y < input.getHeight(); ++y) {
            for (int x = 0; x < input.getWidth(); ++x) {
                int pixel = pixels[y][x];
                output.setRGB(x, y, rgbValues[pixel]);
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = 0; x < kernelX / 2; ++x) {
                int pixel = pixels[y][x];
                output.setRGB(x, y, rgbValues[pixel]);
            }
        }
        for (int y = 0; y < input.getHeight(); ++y) {
            for (int x = input.getWidth() - kernelX / 2; x < input.getWidth(); ++x) {
                int pixel = pixels[y][x];
                output.setRGB(x, y, rgbValues[pixel]);
            }
        }
        for (int y = kernelY / 2; y < input.getHeight() - kernelY / 2; ++y) {
            for (int x = kernelX / 2; x < input.getWidth() - kernelX / 2; ++x) {
                int total = 0;
                for (int deltaY = -kernelY / 2; deltaY <= kernelY / 2; ++deltaY) {
                    for (int deltaX = -kernelX / 2; deltaX <= kernelX / 2; ++deltaX) {
                        int pixel = pixels[y + deltaY][x + deltaX];
                        total += pixel;
                    }
                }
                int avg = total / (kernelX * kernelY);
                output.setRGB(x, y, rgbValues[avg]);
            }
        }
        return output;
    }

    private String getJustFileName(String file) {
        File aFile = new File(file);
        String lastNameWithDot = aFile.getName();
        int dotIndex = lastNameWithDot.indexOf(".");
        String lastNameWithoutDot = lastNameWithDot.substring(0, dotIndex);
        return lastNameWithoutDot;
    }

    private String concatenateResults(String... strings) {
        StringBuffer result = new StringBuffer();
        for (String string : strings) {
            String currentResult = result.toString();
            if (currentResult.indexOf(string) == -1) {
                result.append(string);
            }
        }
        return result.toString();
    }

    private BufferedImage copy(BufferedImage image) {
        BufferedImage output = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = output.createGraphics();
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return output;
    }

    private Pix bilateralTransform(Pix pix) {
        Pix output = Leptonica1.pixBilateral(pix, 50f, 80f, 4, 4);
        Pix gray = Leptonica1.pixConvertTo8(output, 0);
        LeptUtils.dispose(output);
        return gray;
    }

    private BufferedImage bilateralTransform(BufferedImage image) {
        Pix pix = getPixFromImage(image);
        Pix bilateral = Leptonica1.pixBilateral(pix, 30f, 50f, 4, 2);
        Pix gray = Leptonica1.pixConvertTo8(bilateral, 0);
        BufferedImage output = getImageFromPix(gray);
        LeptUtils.dispose(pix);
        LeptUtils.dispose(bilateral);
        LeptUtils.dispose(gray);
        return output;
    }

    public static Pix getPixFromImage(BufferedImage image) {
        if (image == null) {
            return null;
        }
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
        // System.out.println("Found writer - " + writer);
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
        ImageOutputStream ios = null;
        try {
            ios = ImageIO.createImageOutputStream(outputStream);
        } catch (IOException e) {
            return null;
        }
        writer.setOutput(ios);

        // TIFFImageWriter tiw
        // ios.seek(0);
        // ios.mark();
        try {
            writer.write(streamMetadata, new IIOImage(image, null, null), tiffWriteParam);
        } catch (IOException e) {
            return null;
        }
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
        try {
            ios.close();
        } catch (IOException e) {

        }
        writer.dispose();

        ByteBuffer buf = ByteBuffer.allocateDirect(b.length);
        buf.order(ByteOrder.nativeOrder());
        buf.put(b);
        ((Buffer) buf).flip();

        Pix pix = Leptonica.INSTANCE.pixReadMem(buf, new NativeSize(buf.capacity()));

        return pix;
    }

    public static BufferedImage getImageFromPix(Pix pix) {
        if (pix == null) {
            return DEFAULT_IMAGE;
        }
        PointerByReference pdata = new PointerByReference();
        NativeSizeByReference psize = new NativeSizeByReference();
        int format = IFF_TIFF;
        Leptonica.INSTANCE.pixWriteMem(pdata, psize, pix, format);
        byte[] b = pdata.getValue().getByteArray(0, psize.getValue().intValue());
        InputStream in = new ByteArrayInputStream(b);
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(in);
        } catch (IOException e) {
            return DEFAULT_IMAGE;
        }
        try {
            in.close();
        } catch (Exception e) {

        }
        Leptonica.INSTANCE.lept_free(pdata.getValue());
        // System.out.println("Done converting Pix to BI");
        return bi;
    }

}