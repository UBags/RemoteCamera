// for > Java 8,
// --module-path "E:/TechWerx/Distribution Packaging/javafx-sdk-13.0.1/lib" 
// --add-modules=javafx.controls,javafx.media,javafx.fxml

package com.costheta.vision.qrcode;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import javax.swing.SwingUtilities;

import com.costheta.vision.Detections;
import org.imgscalr.Scalr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamDiscoveryService;
import com.github.sarxos.webcam.WebcamDriver;
import com.github.sarxos.webcam.WebcamLockException;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.openimaj.OpenImajDriver;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

//public class QRDetector3 extends Application {
public class QRDetector3 {
/*
    private String inputDirectory = "E:\\TechWerx\\CosTheta\\QR Code Images";

    private boolean debug = false;
    private boolean imageDebug = false;

    private static final long serialVersionUID = 1L;
    private static int isWindows = 2;

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
    private static ExecutorService parallelThreadPool = Executors.newFixedThreadPool(12);

    private static final Stroke STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f,
            new float[] { 1.0f }, 0.0f);

    private Webcam webcam = null;
    private WebcamPanel.Painter painter = null;
    private WebcamPanel webcamPanel = null;
    private BufferedImage troll = null;
    private ArrayList<String> lastQRCodes = new ArrayList<>();

    private String goodBeepFilePath = "GoodBeep.wav";
    private File goodBeepFile = null;
    private Clip goodBeepClip;
    private AudioInputStream goodBeepAudioInputStream;

    private final Button startButton = new Button("Process Image");
    private final TextFlow textFlow = new TextFlow();
    private final APainter panelPainter = new APainter();
    private Dimension preferredDimension = WebcamResolution.VGA.getSize();
    private AnimationTimer timer;
    private boolean startButtonAbled = true;
    private boolean windowMinimised = false;

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
    private String webcamKeyWord = "USB";
    private String filePrefix = "QRCode-Output";
    private String outputDirectory = "output";
    private String imageOutputDirectory = "images";
    private int count = 0;

    private Map<DecodeHintType, Object> hintsMap = null;

    private static final String EMPTY_STRING = "";
    private static final String NULL_STRING = "null";
    private static final ArrayList<String> SPURIOUS_VALUES = new ArrayList<String>() {
        {
            add("DataMatrix");
            add("QRCode");
        }
    };

    private int imageCounter = 0;

    public QRDetector3() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        super();

        troll = ImageIO.read(new File("Initialisation.jpg")); // loads up libraries

        // List<BarcodeFormat> barcodeFormats = new ArrayList<>();
        // barcodeFormats.add(BarcodeFormat.AZTEC);
        // barcodeFormats.add(BarcodeFormat.DATA_MATRIX);
        // barcodeFormats.add(BarcodeFormat.MAXICODE);
        // barcodeFormats.add(BarcodeFormat.QR_CODE);
        // barcodeFormats.add(BarcodeFormat.PDF_417);

        // hintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        // hintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        // hintsMap.put(DecodeHintType.POSSIBLE_FORMATS,
        // EnumSet.allOf(BarcodeFormat.class));
        // hintsMap.put(DecodeHintType.POSSIBLE_FORMATS, barcodeFormats);
        // hintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);

        java.util.Properties props = new java.util.Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(propFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (Exception e) {
                System.out.println("Property initialisation file " + propFilePath + " not found in the classpath "
                        + System.getProperty("java.class.path"));
                e.printStackTrace();
            }
        } else {
            System.out.println("InputStream = null");
            System.out.println("Property initialisation file " + propFilePath + " not found in the classpath "
                    + System.getProperty("java.class.path"));
        }
        visibility = Boolean.parseBoolean(props.getProperty("visibility", "true"));
        sleepTimeAfterDetection = Long.parseLong(props.getProperty("sleepTimeAfterDetection", "3000"));
        sleepTimeWhenNothingDetected = Long.parseLong(props.getProperty("sleepTimeWhenNothingDetected", "500"));
        webcamKeyWord = props.getProperty("webcamKeyword", "USB");
        outputDirectory = props.getProperty("outputDirectory", "output");
        debug = Boolean.parseBoolean(props.getProperty("debug", "false"));
        imageDebug = Boolean.parseBoolean(props.getProperty("imageDebug", "false"));

        if (debug) {
            System.out
                    .println("visibility = " + visibility + "; inactivity after detection = " + sleepTimeAfterDetection
                            + "; general looping time for detection  = " + sleepTimeWhenNothingDetected);
            System.out.println("Looking for webcams with the keyword - " + webcamKeyWord);
            System.out.println("Debugging = " + debug + "; imageDebugging = " + imageDebug);
        }

        WebcamDriver ourDriver = null;
        if (isWindows != 1) {
            // Webcam.setDriver(new V4l4jDriver());
            ourDriver = new OpenImajDriver();
            Webcam.setDriver(ourDriver);
        } else {
            ourDriver = Webcam.getDriver();
        }

        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams.isEmpty()) {
            if (debug) {
                System.out.println("No webcams found");
            }
            System.exit(0);
        } else {
            for (Webcam cam : webcams) {
                if (debug) {
                    System.out.println("Current view settings for " + cam.getName() + " are : " + cam.getViewSize());
                }
            }
        }

        for (Webcam cam : webcams) {
            if ((cam.getName()).contains(webcamKeyWord)) {
                webcam = cam;
                System.out.println("Found webcam " + webcam + " with keyWord " + webcamKeyWord);
                break;
            }
        }

        if (webcam == null) {
            System.out.println("Finding default webcam as current webcam = " + webcam);
            try {
                webcam = Webcam.getDefault(2000);
            } catch (Exception e) {

            }
        }

        if (webcam == null) {
            System.out.println("No viable webcam found");
            System.exit(0);
        }

        if (ourDriver == null) {
            ourDriver = Webcam.getDriver();
        }

        List<WebcamDevice> devices = ourDriver.getDevices();
        // if (devices.size() < 2) {
        // webcam = Webcam.getDefault();
        // }
        Dimension preferredDimension = WebcamResolution.VGA.getSize();
        for (WebcamDevice device : devices) {
            Dimension[] dimensions = device.getResolutions();
            int currentHeight = 0;
            for (Dimension dimension : dimensions) {
                if (debug) {
                    System.out.println("Dimensions of device " + device.getName() + " are : w = " + dimension.width
                            + "; h = " + dimension.height);
                }
                if (dimension.height > currentHeight) {
                    preferredDimension = dimension;
                    currentHeight = dimension.height;
                }
            }
            device.setResolution(preferredDimension);
            if (debug) {
                System.out.println("Set resolution of device " + device.getName() + " to : width = "
                        + preferredDimension.width + "; height = " + preferredDimension.height);
            }
        }

        // webcam.setViewSize(preferredDimension);
        try {
            if (webcam.isOpen()) {
                webcam.close();
            }
            webcam.setViewSize(preferredDimension);
            webcam.open(true);
        } catch (WebcamLockException e) {
            System.out.println("Webcam is already in use and locked by another process");
            System.exit(0);
        }

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

        WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
        discovery.stop();

        // create the output directory
        try {
            Files.createDirectories(Paths.get(outputDirectory).toAbsolutePath());
            Files.createDirectories(Paths.get(imageOutputDirectory).toAbsolutePath());
        } catch (Exception e) {

        }

        System.out.println("Ready to detect");

        // EXECUTOR.execute(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        webcamPanel = new WebcamPanel(webcam, true);
        // panel.setPreferredSize(WebcamResolution.HD.getSize());
        webcamPanel.setPreferredSize(preferredDimension);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setFPSLimited(true);
        webcamPanel.setFPSLimit(25);
        webcamPanel.setPainter(panelPainter);
        webcamPanel.setVisible(visibility);

        painter = webcamPanel.getDefaultPainter();
        System.out.println("painter = " + painter);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) ((screenSize.getWidth() * 2) / 3);
        int height = (int) ((screenSize.getHeight() * 2) / 3);
        double screenDimensionsRatio = width * 1.0 / height;
        int scrollPaneHeight = 80;

        // System.out.println("Entered start");
        final ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().add("Left side image");
        comboBox.getItems().add("Right side image");
        comboBox.getItems().add("Top image");
        HBox hbox = new HBox(comboBox, startButton);
        Separator separator = new Separator(Orientation.HORIZONTAL);
        VBox top = new VBox(hbox, separator);

        final SwingNode swingNode = new SwingNode();
        swingNode.setContent(webcamPanel);

        // createSwingContent(swingNode);

        BorderPane borderPane = new BorderPane();
        borderPane.getChildren().add(swingNode);
        VBox center = new VBox(borderPane);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setId("Results");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(scrollPaneHeight);
        scrollPane.setMaxHeight(scrollPaneHeight);
        scrollPane.setContent(textFlow);
        Separator separator1 = new Separator(Orientation.HORIZONTAL);
        VBox bottom = new VBox(separator1, scrollPane);
        // VBox.setVgrow(scrollPane, Priority.ALWAYS);
        textFlow.getChildren().addListener((ListChangeListener<Node>) ((change) -> {
            textFlow.requestLayout();
            scrollPane.requestLayout();
            scrollPane.setVvalue(1.0f);
        }));

        BorderPane overallPane = new BorderPane();
        overallPane.setStyle("-fx-padding: 10;" + "-fx-border-style: solid inside;" + "-fx-border-width: 2;"
                + "-fx-border-insets: 5;" + "-fx-border-radius: 5;" + "-fx-border-color: blue;");
        overallPane.setTop(top);
        overallPane.setCenter(center);
        overallPane.setBottom(bottom);

        StackPane root = new StackPane();
        root.getChildren().add(overallPane);
        Scene scene = new Scene(root, width, height);
        primaryStage.setTitle("Process Connecting Rod");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();
        webcamPanel.start();
        comboBox.setValue("Left side image");

        startButtonAbled = true;

        startButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // don't take any action if a false event is fired when the window is in
                // minimised state. (Sigh ! That happens !!)
                if (windowMinimised) {
                    return;
                }
                startButtonAbled = false;
                processInput(comboBox);
            }
        });

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                int response = QRDetector3.this.closeWindowEvent(primaryStage);
                if (response != -1) {
                    System.out.println("Shutting down the application");
                    timer.stop();
                    parallelThreadPool.shutdown();
                    Platform.exit();
                    System.exit(0);
                } else {
                    e.consume();
                }
            }
        });
        primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                // System.out.println("minimized:" + t1.booleanValue());
                windowMinimised = t1.booleanValue();
            }
        });
        timer = new AnimationTimer() {
            private long lastTime = System.currentTimeMillis();
            @Override
            public void handle(long now) {
                if (now > lastTime + 250_000_000L) {
                    startButton.setDisable(!startButtonAbled);
                    startButton.setVisible(startButtonAbled);
                    lastTime = now;
                    // webcamPanel.repaint();
                }
//				if ((now / 1000000) % 2000 < 50) {
//					System.out.println("drawType = " + drawType);
//				}
            }
        };
        timer.start();

        // startButton.fire();
        // setVisible(visibility);

    }

//	@Override
//	public void run() {
//		while (true) {
//			if (!webcam.isOpen()) {
//				return;
//			}
//			webcam.getCustomViewSizes();
//			Instant t = Instant.now();
//			BufferedImage originalImage = makeGrayBI(webcam.getImage());
//			if (imageDebug) {
//				StringBuffer outputFilePath = new StringBuffer(imageOutputDirectory).append("/image-")
//						.append(++imageCounter).append(".png");
//				try {
//					writeFile(originalImage, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
//				} catch (Exception e) {
//
//				}
//			}
//			// BufferedImage image = webcam.getImage();
//			final BufferedImage image = relativeGammaWithSigmoidCorrection(originalImage, 5, 13, 25, 0.2, 24);
//			long tp = Duration.between(t, Instant.now()).toMillis();
//			// System.out.print(tp);
//			// System.out.print(" ");
//			// BufferedImage processedImage = Scalr.resize(image, Scalr.Method.SPEED,
//			// Scalr.Mode.AUTOMATIC,
//			// (int) (image.getWidth() * 2.0), (int) (image.getHeight() * 2.0),
//			// Scalr.OP_ANTIALIAS);
//			// BufferedImage processedImage = relativeGammaContrastEnhancement(image, 3, 11,
//			// 30);
//			// BufferedImage processedImage = makeGrayBI(image);
//			tp = Duration.between(t, Instant.now()).toMillis();
//			// System.out.print(tp);
//			// System.out.print(" ");
//			
//			final ArrayList<CompletableFuture<Object>> threads = new ArrayList<CompletableFuture<Object>>(2);
//			threads.add(CompletableFuture.supplyAsync(() -> {
//				return readQRCode_dmCV(image);
//			}, parallelThreadPool));
//			threads.add(CompletableFuture.supplyAsync(() -> {
//				return readQRCode_ZXing(image);
//			}, parallelThreadPool));
//
//			CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
//
//			Detections dmDetections = Detections.EMPTY_DETECTIONS;
//			String zxingDetection = QRDetector3.EMPTY_STRING;
//			try {
//			
//				dmDetections = (Detections) threads.get(0).get();
//				zxingDetection = (String) threads.get(1).get();
//			} catch (Exception e) {
//
//			}
//
//			String dmGoodResults = getResults(dmDetections.detections);
//			String dmBadResults = getResults(dmDetections.failures);
//			
//			zxingDetection = zxingDetection.trim();
//			dmGoodResults = dmGoodResults.trim();
//			dmBadResults = dmBadResults.trim();
//
//			// if (dmGoodResults.isBlank()) {
//			if (dmGoodResults.isEmpty()) {
//				// if (!zxingDetection.isBlank()) {
//				if (!zxingDetection.isEmpty()) {
//					dmGoodResults = (new StringBuffer(dmGoodResults).append(zxingDetection)).toString();
//				}
//			} else {
//				if (dmGoodResults.indexOf(zxingDetection) == -1) {
//					// if (!dmBadResults.isBlank()) {
//					if (!dmBadResults.isEmpty()) {
//						dmGoodResults = (new StringBuffer(dmGoodResults).append(System.lineSeparator())
//								.append(zxingDetection))
//								.toString();
//					}
//				}
//			}
//
//			boolean detected = false;
//
//			// if (!dmGoodResults.isBlank()) {
//			if (!dmGoodResults.isEmpty()) {
//				detected = true;
//			}
//
//			if (detected) {
//				playGoodBeep();
//				writeOutput(dmGoodResults);
//				if (debug) {
//					System.out.println("Final result = " + dmGoodResults);
//					System.out.println("-----------");
//				}
//			}
//			
//			if ((dmDetections.failures.size() > 0) && !detected) {
//				playBadBeep();
//			}
//
//			try {
//				if (detected) {
//					TimeUnit.MILLISECONDS.sleep(Math.max(sleepTimeAfterDetection - tp, 1));
//					if (sleepTimeAfterDetection > 750) {
//						lastQRCodes.clear(); // added this, as lastQRCodes are not required when sleep time is large
//					}
//				} else {
//					TimeUnit.MILLISECONDS.sleep(Math.max(sleepTimeWhenNothingDetected - tp, 1));
//				}
//			} catch (InterruptedException ie) {
//
//			}
//		}
//	}

    public void run() {

        // List<String> files = filterFiles(listFiles(inputDirectory, 1), "image",
        // true);
        // System.out.println("The files are - " + files);
        // Instant t = Instant.now();
        // for (String filePath : files) {
        // System.out.println("Processing - " + filePath);
        while (true) {
            if (!webcam.isOpen()) {
                return;
            }
            webcam.getCustomViewSizes();
            BufferedImage input = null;
            // try {
            // input = ImageIO.read(new File(filePath));
            // } catch (Exception e) {
            // }
            input = webcam.getImage();
            processInput(input, null);
        }
        // long timeTaken = Duration.between(t, Instant.now()).toMillis();
        // System.out.println("Time taken per scan = " + timeTaken / (files.size() > 0 ?
        // files.size() : 1));
    }

    private void processInput(ComboBox<String> comboBox) {

    }

    private void processInput(BufferedImage input, String filePath) {
        BufferedImage originalImage = makeGrayBI(input);
        Instant t = Instant.now();
        final ArrayList<CompletableFuture<BufferedImage>> scalingThreads = new ArrayList<CompletableFuture<BufferedImage>>(
                2);

        scalingThreads.add(CompletableFuture.supplyAsync(() -> {
            BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC,
                    (int) (originalImage.getWidth() * 2.0), (int) (originalImage.getHeight() * 2.0),
                    Scalr.OP_ANTIALIAS);
            return scaledImage;
        }, parallelThreadPool));
        scalingThreads.add(CompletableFuture.supplyAsync(() -> {
            BufferedImage scaledImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC,
                    (int) (originalImage.getWidth() * 2.5), (int) (originalImage.getHeight() * 2.5),
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
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(originalImage, 7, 7, 0.0);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(originalImage, 9, 9, 0.0);
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(originalImage, 13, 13, 0.0);
            return imageEnhanced;
        }, parallelThreadPool));

        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage1, 9, 9, 0.0); // 9,9
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage1, 11, 11, 0.0); // 11,11
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage1, 15, 15, 0.0); // 15, 15
            return imageEnhanced;
        }, parallelThreadPool));

        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage2, 11, 11, 0.0); // 9,9
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage2, 15, 15, 0.0); // 11,11
            return imageEnhanced;
        }, parallelThreadPool));
        enhancementThreads.add(CompletableFuture.supplyAsync(() -> {
            final BufferedImage imageEnhanced = contrastEnhancementByAverage(scaledImage2, 19, 19, 0.0); // 15, 15
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

        final ArrayList<CompletableFuture<Object>> processingThreads = new ArrayList<CompletableFuture<Object>>(12);
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image1));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image1));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image2));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image2));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image3));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image3));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image4));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image4));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image5));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image5));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image6));
        }, parallelThreadPool));
//		processingThreads.add(CompletableFuture.supplyAsync(() -> {
//			return readQRCode_ZXing(copy(image6));
//		}, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image7));
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image8));
        }, parallelThreadPool));
        processingThreads.add(CompletableFuture.supplyAsync(() -> {
            return readQRCode_dmCV(copy(image9));
        }, parallelThreadPool));

        *//*************
         * final BufferedImage image1 = relativeGammaContrastEnhancement(input, 9, 9,
         * 80); final BufferedImage image = relativeGammaContrastEnhancement(image1, 9,
         * 9, 20); // These 2 get 21 readings from 127
         ****************//*

        *//*************
         * final BufferedImage image = contrastEnhancementByAverage(input, 7, 7, 0.0);
         * // This gives 44 readings from 127
         ****************//*

        // BufferedImage image = webcam.getImage();
        // final BufferedImage image = relativeGammaWithSigmoidCorrection(originalImage,
        // 5, 5, 60, 0.8, 8);
        // final BufferedImage image1 = blur(originalImage, 3, 3);
        // final BufferedImage image1 = relativeGammaContrastEnhancement(input, 9, 9,
        // 70); // 13 x 13 x 60
        // final BufferedImage image = relativeGammaContrastEnhancement(image1, 9, 9,
        // 30);

        // final BufferedImage image = contrastEnhancementByProgressiveAverage(input, 7,
        // 7, 12);

        // final BufferedImage image = relativeGammaContrastEnhancement(image1, 9, 9,
        // 50);

        // final BufferedImage image2 = relativeGammaContrastEnhancement(image1, 7, 7,
        // 50); // 13 x 13 x 60

        // works, 9 x 9 x 70 is OK as well
        // final BufferedImage image3 = relativeGammaContrastEnhancement(image2, 7, 7,
        // 50);

        // GrayU8 grayU8 = ConvertBufferedImage.convertFrom(image3, (GrayU8) null);
        // GrayU8 sauvola = grayU8.createSameShape();
        // GThresholdImageOps.localSauvola(grayU8, sauvola, ConfigLength.fixed(11),
        // 0.1f, false);
        // final BufferedImage image4 = ConvertBufferedImage.convertTo(sauvola,
        // (BufferedImage) null);
        // final BufferedImage image5 = binarizeWithCutOff(image4, 0, false);

        // final BufferedImage image = makeGrayBI(image5);

        // long tp = Duration.between(t, Instant.now()).toMillis();
        // System.out.print(tp);
        // System.out.print(" ");
        // BufferedImage processedImage = Scalr.resize(image, Scalr.Method.SPEED,
        // Scalr.Mode.AUTOMATIC,
        // (int) (image.getWidth() * 2.0), (int) (image.getHeight() * 2.0),
        // Scalr.OP_ANTIALIAS);
        // BufferedImage processedImage = relativeGammaContrastEnhancement(image, 3, 11,
        // 30);
        // BufferedImage processedImage = makeGrayBI(image);
        // tp = Duration.between(t, Instant.now()).toMillis();
        // System.out.print(tp);
        // System.out.print(" ");

        CompletableFuture.allOf(processingThreads.toArray(new CompletableFuture[processingThreads.size()])).join();

        Detections dmDetections1 = Detections.EMPTY_DETECTIONS;
        String zxingDetection1 = QRDetector3.EMPTY_STRING;

        Detections dmDetections2 = Detections.EMPTY_DETECTIONS;
        String zxingDetection2 = QRDetector3.EMPTY_STRING;

        Detections dmDetections3 = Detections.EMPTY_DETECTIONS;
        String zxingDetection3 = QRDetector3.EMPTY_STRING;

        Detections dmDetections4 = Detections.EMPTY_DETECTIONS;
        String zxingDetection4 = QRDetector3.EMPTY_STRING;

        Detections dmDetections5 = Detections.EMPTY_DETECTIONS;
        String zxingDetection5 = QRDetector3.EMPTY_STRING;

        Detections dmDetections6 = Detections.EMPTY_DETECTIONS;
        String zxingDetection6 = QRDetector3.EMPTY_STRING;

        Detections dmDetections7 = Detections.EMPTY_DETECTIONS;
        String zxingDetection7 = QRDetector3.EMPTY_STRING;

        Detections dmDetections8 = Detections.EMPTY_DETECTIONS;
        String zxingDetection8 = QRDetector3.EMPTY_STRING;

        Detections dmDetections9 = Detections.EMPTY_DETECTIONS;
        String zxingDetection9 = QRDetector3.EMPTY_STRING;

        try {

            dmDetections1 = (Detections) processingThreads.get(0).get();
            // zxingDetection1 = (String) processingThreads.get(1).get();

            dmDetections2 = (Detections) processingThreads.get(1).get();
            // zxingDetection2 = (String) processingThreads.get(3).get();

            dmDetections3 = (Detections) processingThreads.get(2).get();
            // zxingDetection3 = (String) processingThreads.get(5).get();

            dmDetections4 = (Detections) processingThreads.get(3).get();
            // zxingDetection4 = (String) processingThreads.get(7).get();

            dmDetections5 = (Detections) processingThreads.get(4).get();
            // zxingDetection5 = (String) processingThreads.get(9).get();

            dmDetections6 = (Detections) processingThreads.get(5).get();
            // zxingDetection6 = (String) processingThreads.get(11).get();

            dmDetections7 = (Detections) processingThreads.get(6).get();
            dmDetections8 = (Detections) processingThreads.get(7).get();
            dmDetections9 = (Detections) processingThreads.get(8).get();
        } catch (Exception e) {

        }

        String dmGoodResults1 = getResults(dmDetections1.detections);
        zxingDetection1 = zxingDetection1.trim();
        dmGoodResults1 = dmGoodResults1.trim();

        String dmGoodResults2 = getResults(dmDetections2.detections);
        zxingDetection2 = zxingDetection2.trim();
        dmGoodResults2 = dmGoodResults2.trim();

        String dmGoodResults3 = getResults(dmDetections3.detections);
        zxingDetection3 = zxingDetection3.trim();
        dmGoodResults3 = dmGoodResults3.trim();

        String dmGoodResults4 = getResults(dmDetections4.detections);
        zxingDetection4 = zxingDetection4.trim();
        dmGoodResults4 = dmGoodResults4.trim();

        String dmGoodResults5 = getResults(dmDetections5.detections);
        zxingDetection5 = zxingDetection5.trim();
        dmGoodResults5 = dmGoodResults5.trim();

        String dmGoodResults6 = getResults(dmDetections6.detections);
        zxingDetection6 = zxingDetection6.trim();
        dmGoodResults6 = dmGoodResults6.trim();

        String dmGoodResults7 = getResults(dmDetections7.detections);
        zxingDetection7 = zxingDetection7.trim();
        dmGoodResults7 = dmGoodResults7.trim();

        String dmGoodResults8 = getResults(dmDetections8.detections);
        zxingDetection8 = zxingDetection8.trim();
        dmGoodResults8 = dmGoodResults8.trim();

        String dmGoodResults9 = getResults(dmDetections9.detections);
        zxingDetection9 = zxingDetection9.trim();
        dmGoodResults9 = dmGoodResults9.trim();

        // if (dmGoodResults.isBlank()) {
        if (dmGoodResults1.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection1.isEmpty()) {
                dmGoodResults1 = (new StringBuffer(dmGoodResults1).append(zxingDetection1)).toString();
            }
        } else {
            if (dmGoodResults1.indexOf(zxingDetection1) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults1.isEmpty()) {
                    dmGoodResults1 = (new StringBuffer(dmGoodResults1).append(System.lineSeparator())
                            .append(zxingDetection1)).toString();
                }
            }
        }

        if (dmGoodResults2.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection2.isEmpty()) {
                dmGoodResults2 = (new StringBuffer(dmGoodResults2).append(zxingDetection2)).toString();
            }
        } else {
            if (dmGoodResults2.indexOf(zxingDetection2) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults2.isEmpty()) {
                    dmGoodResults2 = (new StringBuffer(dmGoodResults2).append(System.lineSeparator())
                            .append(zxingDetection2)).toString();
                }
            }
        }

        if (dmGoodResults3.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection3.isEmpty()) {
                dmGoodResults3 = (new StringBuffer(dmGoodResults3).append(zxingDetection3)).toString();
            }
        } else {
            if (dmGoodResults3.indexOf(zxingDetection3) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults3.isEmpty()) {
                    dmGoodResults3 = (new StringBuffer(dmGoodResults3).append(System.lineSeparator())
                            .append(zxingDetection3)).toString();
                }
            }
        }

        if (dmGoodResults4.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection4.isEmpty()) {
                dmGoodResults4 = (new StringBuffer(dmGoodResults4).append(zxingDetection4)).toString();
            }
        } else {
            if (dmGoodResults4.indexOf(zxingDetection4) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults4.isEmpty()) {
                    dmGoodResults4 = (new StringBuffer(dmGoodResults4).append(System.lineSeparator())
                            .append(zxingDetection4)).toString();
                }
            }
        }

        if (dmGoodResults5.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection5.isEmpty()) {
                dmGoodResults5 = (new StringBuffer(dmGoodResults5).append(zxingDetection5)).toString();
            }
        } else {
            if (dmGoodResults5.indexOf(zxingDetection5) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults5.isEmpty()) {
                    dmGoodResults5 = (new StringBuffer(dmGoodResults5).append(System.lineSeparator())
                            .append(zxingDetection5)).toString();
                }
            }
        }

        if (dmGoodResults6.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection6.isEmpty()) {
                dmGoodResults6 = (new StringBuffer(dmGoodResults6).append(zxingDetection6)).toString();
            }
        } else {
            if (dmGoodResults6.indexOf(zxingDetection6) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults6.isEmpty()) {
                    dmGoodResults6 = (new StringBuffer(dmGoodResults6).append(System.lineSeparator())
                            .append(zxingDetection6)).toString();
                }
            }
        }

        if (dmGoodResults7.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection7.isEmpty()) {
                dmGoodResults7 = (new StringBuffer(dmGoodResults7).append(zxingDetection7)).toString();
            }
        } else {
            if (dmGoodResults7.indexOf(zxingDetection7) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults7.isEmpty()) {
                    dmGoodResults7 = (new StringBuffer(dmGoodResults7).append(System.lineSeparator())
                            .append(zxingDetection7)).toString();
                }
            }
        }

        if (dmGoodResults8.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection8.isEmpty()) {
                dmGoodResults8 = (new StringBuffer(dmGoodResults8).append(zxingDetection8)).toString();
            }
        } else {
            if (dmGoodResults8.indexOf(zxingDetection8) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults8.isEmpty()) {
                    dmGoodResults8 = (new StringBuffer(dmGoodResults8).append(System.lineSeparator())
                            .append(zxingDetection8)).toString();
                }
            }
        }

        if (dmGoodResults9.isEmpty()) {
            // if (!zxingDetection.isBlank()) {
            if (!zxingDetection9.isEmpty()) {
                dmGoodResults9 = (new StringBuffer(dmGoodResults9).append(zxingDetection9)).toString();
            }
        } else {
            if (dmGoodResults9.indexOf(zxingDetection9) == -1) {
                // if (!dmBadResults.isBlank()) {
                if (!dmBadResults9.isEmpty()) {
                    dmGoodResults9 = (new StringBuffer(dmGoodResults9).append(System.lineSeparator())
                            .append(zxingDetection9)).toString();
                }
            }
        }

        String result = concatenateResults(dmGoodResults1, dmGoodResults2, dmGoodResults3, dmGoodResults4,
                dmGoodResults5, dmGoodResults6, dmGoodResults7, dmGoodResults8, dmGoodResults9);

        boolean detected = false;

        // if (!dmGoodResults.isBlank()) {
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
        }

        if (!detected && ((dmDetections1.failures.size() > 0) || (dmDetections2.failures.size() > 0)
                || (dmDetections3.failures.size() > 0) || (dmDetections4.failures.size() > 0)
                || (dmDetections5.failures.size() > 0) || (dmDetections6.failures.size() > 0)
                || (dmDetections7.failures.size() > 0) || (dmDetections8.failures.size() > 0)
                || (dmDetections9.failures.size() > 0))) {
            playBadBeep();
        }
        if (imageDebug) {
            if (filePath != null) {
                StringBuffer outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : (dmDetections1.failures.size() > 0 ? "qrcodebad-" : "bad-"))
                        .append("image-").append(getJustFileName(filePath).substring(6)).append(".png");
                try {
                    writeFile(image1, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
                outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : (dmDetections1.failures.size() > 0 ? "qrcodebad-" : "bad-"))
                        .append("scaledImage20-").append(getJustFileName(filePath).substring(6)).append(".png");
                try {
                    writeFile(image5, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
                } catch (Exception e) {

                }
                outputFilePath = new StringBuffer(imageOutputDirectory).append("/")
                        .append(detected ? "good-" : (dmDetections1.failures.size() > 0 ? "qrcodebad-" : "bad-"))
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
        try {
            if (detected) {
                TimeUnit.MILLISECONDS.sleep(Math.max(sleepTimeAfterDetection - tp, 1));
                if (sleepTimeAfterDetection > 750) {
                    lastQRCodes.clear(); // added this, as lastQRCodes are not required when sleep time is large
                }
            } else {
                TimeUnit.MILLISECONDS.sleep(Math.max(sleepTimeWhenNothingDetected - tp, 1));
            }
        } catch (InterruptedException ie) {

        }

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
        isWindows();
        populateGammaCubedTable();
        createGammaMultipliedMap(7);
        populateSigmoidFactorsTable();
        populateRGBValues();
        QRDetector3 detector = new QRDetector3();
        detector.launch(args);
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
                // this gives 44 good readings out of 127

                *//*
                 * gamma = Math.min(Math.max( gamma + (gamma >= upperToleranceLevel ? 600 :
                 * (gamma < lowerToleranceLevel ? -200 : 0)), 0), 1999);
                 *//*
                double gammaCubed = Math.min(gammaCubedTable[gamma], 7.0);
                Integer gammaInt = Integer.valueOf((int) Math.round(gammaCubed / 0.05));
                double[] multipliers = gammaMultipliedMap.get(gammaInt);
                int newPixel = (int) multipliers[currentPixel];
                output.setRGB(x, y, rgbValues[newPixel]);
            }
        }
        return output;
    }

    private static GrayU8 sharpen(BufferedImage input) {
        GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8) null);
        GrayU8 sharpened = gray.createSameShape();
        EnhanceImageOps.sharpen8(gray, sharpened);
        return sharpened;
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
        String finalResult = QRDetector3.EMPTY_STRING;
        if (result != null) {
            finalResult = result.getText();
        }
        if (NULL_STRING.equals(finalResult)) {
            return QRDetector3.EMPTY_STRING;
        }
        finalResult.replace(NULL_STRING, EMPTY_STRING);
        if (SPURIOUS_VALUES.contains(finalResult)) {
            return QRDetector3.EMPTY_STRING;
        }
        return finalResult;
    }

    private Detections readQRCode_dmCV(BufferedImage image) {
        GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8) null);
        // GrayU8 gray = sharpen(image);
        // System.out.print(tp);
        // System.out.print(" ");

        QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);
        detector.process(gray);
        // Get's a list of all the qr codes it could successfully detect and decode
        List<QrCode> detections = detector.getDetections();
        List<QrCode> failures = detector.getFailures();

//		if (detections.size() == 0) {
//			lastQRCodes.clear();
//		} else {
//			if (detections.size() != lastQRCodes.size()) {
//				lastQRCodes.clear();
//			}
//		}
        return new Detections(detections, failures);

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

    private String getResults(List<QrCode> detections) {
        lastQRCodes.clear();
        if (detections.size() == 0) {
            return QRDetector3.EMPTY_STRING;
        }
        StringBuffer result = new StringBuffer(QRDetector3.EMPTY_STRING);
        for (QrCode qr : detections) {
            // The message encoded in the marker
            String message = qr.message;
            if (message == null) {
                continue;
            }
            if (SPURIOUS_VALUES.contains(message)) {
                continue;
            }
            if (debug) {
                System.out.println("BF message: " + message);
            }
            lastQRCodes.add(message);
            result.append(message).append(System.lineSeparator());
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

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile) throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, 300);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi)
            throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                              float compressionQuality) throws Exception {

        if (bufferedImage == null) {
            return false;
        }
        RenderedImage[] input = new RenderedImage[1];
        input[0] = bufferedImage;
        return writeFile(input, formatName, localOutputFile, dpi, compressionQuality);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi)
            throws Exception {
        return writeFile(images, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
                              float compressionQuality) throws Exception {

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

                *//*
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
                 *//*

                *//*
                 * double dotsPerMilli = ((1.0 * dpi) / 10) / 2.54; IIOMetadataNode horiz = new
                 * IIOMetadataNode("HorizontalPixelSize"); horiz.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode vert = new
                 * IIOMetadataNode("VerticalPixelSize"); vert.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode dim = new
                 * IIOMetadataNode("Dimension"); dim.appendChild(horiz); dim.appendChild(vert);
                 * IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
                 * root.appendChild(dim); md.mergeTree("javax_imageio_1.0", root);
                 *//*

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
            } finally

            {
                // Close stream in finally block to avoid resource leaks
                if (output != null) {
                    output.close();
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

    private void createSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // swingNode.setContent(webcamPanel);
                // webcamPanel.start();
            }
        });
    }

    private int closeWindowEvent(Stage primaryStage) {
        System.out.println("Window close request ...");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.getButtonTypes().remove(ButtonType.OK);
        alert.getButtonTypes().add(ButtonType.CANCEL);
        alert.getButtonTypes().add(ButtonType.YES);
        alert.setTitle("Quit application");
        alert.setContentText(String.format("Close the application and Exit ?"));
        alert.initOwner(primaryStage.getOwner());
        Optional<ButtonType> res = alert.showAndWait();

        if (res.isPresent()) {
            if (res.get().equals(ButtonType.CANCEL)) {
                return -1;
            }
        }
        return 1;
    }

    public class APainter implements WebcamPanel.Painter {
        @Override
        public void paintPanel(WebcamPanel panel, Graphics2D g2) {
            if (painter != null) {
                painter.paintPanel(panel, g2);
            }
        }

        @Override
        public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {

            if (painter != null) {
                painter.paintImage(panel, image, g2);
            }

        }

    }*/
}