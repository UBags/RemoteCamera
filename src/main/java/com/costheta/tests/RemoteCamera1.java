package com.costheta.tests;

import java.awt.image.BufferedImage;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import com.costheta.camera.remote.image.DefaultImages;
import com.esotericsoftware.kryonet.Client;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.WindowEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class RemoteCamera1 extends Application {

    private static boolean windowMinimised = false;

    private static int isWindows = 2;
    public static boolean server = true;
    public static int cameraID = 0;

    private static final String BASE_DIR_KEY = "baseDir";
    private static final String PROPERTIES_FILE_KEY = "propertiesFile";
    private static final String CAMERA_ID_KEY = "cameraId";

    public static int sysoutDebugLevel = 2;
    private static String baseDir = "";
    private static String initialisationFile = "camera.properties";
    private static Path initialisationFilePath = null;

    private static final String tempImageDirectory = "temp";
    private static int imageCounter = 1;
    public static Path tempImageDirPath = null;
    private static String tempImageDir = null;
    private static long timeTakenForPicture = 0;
    private static String currentImageFileName = null;

    private static final String TCP_PORT = "tcp.port";
    private static final String UDP_PORT = "udp.port";
    private static final String TIMEOUT_MILLIS = "timeout.millis";

    private static int tcpPort = 54555;
    private static int udpPort = 54777;
    private static int timeOutMillis = 5000;

    public static final String separatorLine = "*************************************************************";
    public static final String generalCommandLine = "Expecting command line values for at least 1 mandatory argument - baseDir and 2 optional arguments - cameraPropertiesFile and cameraId";
    public static final String commandLineFormat = "The base directory file has to be provided as baseDir={directory path to directory of initialisation file} cameraPropertiesFile={name of camera properties file} cameraId={number id of camera i.e. 1/2/3... }";
    public static final String configurationFileStatement = "The default configuration file is camera.properties";
    public static final String exampleCommandStatement = new StringBuilder("Example command: javaw -jar RemoteCamera.jar baseDir=/home/ubuntu/camera  cameraPropertiesFile=camera.properties cameraId=1")
            .append("\n").append("OR javaw -jar RemoteCamera.jar baseDir=C:/camera/remote  cameraPropertiesFile=camera.properties cameraId=2").toString();
    public static final String serverIdStatement = "If the camera id is not specified or is specified as 0 or a negative number, this process will start as the common client that will request and receive images from all the cameras";
    public static final String serverErrorStatement = "If the camera id is specified, it must be a number for which a UDP port has been specified in the cameraPropertiesFile file. It also has to be a positive integer.";
    public static final String exitStatement = "Exiting. Restart after making the requisite changes.";

    private static final BufferedImage EMPTY_IMAGE = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
    private static final String EMPTY_STRING = "";
    private static final String BAD_ARGUMENT = "BAD ARGUMENT";

    private static HBox topPane;
    private static HBox horizontalTop2Images;
    private static FlowPane image11;
    private static FlowPane image12;
    private static FlowPane image21;
    private static FlowPane image22;
    private static HBox horizontalBottom2Images;
    private static VBox imagePane;
    private static VBox resultPane;

    private static ImageView imageView11 = new ImageView();
    private static ImageView imageView21 = new ImageView();
    private static ImageView imageView12 = new ImageView();
    private static ImageView imageView22 = new ImageView();

    private static WebView view;
    private static WebEngine engine;

    private static BorderPane root;

    private static CameraProcessor leftCameraProcessor;
    private static CameraProcessor rightCameraProcessor;
    private static CameraProcessor topCameraProcessor;
    private static CameraProcessor bottomCameraProcessor;

    private static CameraProcessor currentProcessor;

    private static Button actionButton;

//    public static void main(String[] args) throws Exception {
//
//        int length = args.length;
//        for (int i = 0; i < length; ++i) {
//            System.out.println(args[i]);
//        }
//        if (length == 0) {
//            System.out.println(generalCommandLine);
//            System.out.println(separatorLine);
//            System.out.println(commandLineFormat);
//            System.out.println(separatorLine);
//            System.out.println(configurationFileStatement);
//            System.out.println(separatorLine);
//            System.out.println(exampleCommandStatement);
//            System.out.println(separatorLine);
//            System.out.println(serverIdStatement);
//            System.out.println(separatorLine);
//            System.out.println(serverErrorStatement);
//            System.out.println(separatorLine);
//            System.out.println(exitStatement);
//            System.exit(0);
//        } else {
//            if (length == 1) {
//                baseDir = getValueOfArguments(args, BASE_DIR_KEY);
//                System.out.println("1. baseDir = " + baseDir);
//                if (BAD_ARGUMENT.equals(baseDir)) {
//                    System.out.println(generalCommandLine);
//                    System.out.println(separatorLine);
//                    System.out.println(commandLineFormat);
//                    System.out.println(separatorLine);
//                    System.out.println(configurationFileStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(exampleCommandStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(serverIdStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(serverErrorStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(exitStatement);
//                    System.exit(0);
//                }
//            } else {
//                if (length >= 2) {
//                    baseDir = getValueOfArguments(args, BASE_DIR_KEY);
//                    System.out.println("2. baseDir = " + baseDir);
//                    if (BAD_ARGUMENT.equals(baseDir)) {
//                        System.out.println(generalCommandLine);
//                        System.out.println(separatorLine);
//                        System.out.println(commandLineFormat);
//                        System.out.println(separatorLine);
//                        System.out.println(configurationFileStatement);
//                        System.out.println(separatorLine);
//                        System.out.println(exampleCommandStatement);
//                        System.out.println(separatorLine);
//                        System.out.println(serverIdStatement);
//                        System.out.println(separatorLine);
//                        System.out.println(serverErrorStatement);
//                        System.out.println(separatorLine);
//                        System.out.println(exitStatement);
//                        System.exit(0);
//                    }
//                }
//            }
//            String initFile = getValueOfArguments(args, PROPERTIES_FILE_KEY);
//            System.out.println("initFile = " + initFile);
//            if (!EMPTY_STRING.equals(initFile)) {
//                initialisationFile = initFile;
//            }
//            initialisationFilePath = Paths.get(baseDir, initialisationFile);
//            String camId = getValueOfArguments(args, CAMERA_ID_KEY);
//            if (!EMPTY_STRING.equals(camId)) {
//                try {
//                    serverID = Integer.parseInt(camId);
//                } catch (NumberFormatException nfe) {
//                    System.out.println(serverErrorStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(exitStatement);
//                    System.exit(0);
//                }
//                if (serverID <= 0) {
//                    System.out.println(serverErrorStatement);
//                    System.out.println(separatorLine);
//                    System.out.println(exitStatement);
//                    System.exit(0);
//                }
//            } else {
//                server = false;
//            }
//        }
//        initialise();
//        sysoutDebugLevel = Integer.parseInt(System.getProperty("sdl", "11"));
//        launch(args);
//        System.out.println("Done");
//    }

    public static void main(String[] args) throws Exception {

        int length = args.length;
        for (int i = 0; i < length; ++i) {
            System.out.println(args[i]);
        }
        String baseDirInput =  getValueOfArguments(args, BASE_DIR_KEY);
        String initFileInput =  getValueOfArguments(args, PROPERTIES_FILE_KEY);
        String cameraIDInput =  getValueOfArguments(args, CAMERA_ID_KEY);
        if (!EMPTY_STRING.equals(baseDirInput)) {
            baseDir = baseDirInput;
        } else {
            baseDir = System.getProperty("user.dir");
        }
        if (!EMPTY_STRING.equals(initFileInput)) {
            initialisationFile = initFileInput;
        }
        if (!EMPTY_STRING.equals(cameraIDInput)) {
            try {
                cameraID = Integer.parseInt(cameraIDInput);
                if (cameraID <= 0) {
                    System.out.println(serverErrorStatement);
                    System.out.println(separatorLine);
                    server = false;
                }
            } catch (NumberFormatException nfe) {
                System.out.println(serverErrorStatement);
                System.out.println(separatorLine);
                server = false;
            }
        } else {
            server = false;
        }

        initialisationFilePath = Paths.get(baseDir, initialisationFile);
        initialise();
        sysoutDebugLevel = Integer.parseInt(System.getProperty("sdl", "11"));
        System.out.println(generalCommandLine);
        System.out.println(separatorLine);
        System.out.println(commandLineFormat);
        System.out.println(separatorLine);
        System.out.println(exampleCommandStatement);
        System.out.println(separatorLine);
        System.out.println(serverIdStatement);
        System.out.println(separatorLine);
        System.out.println("initialisationFilePath = " + initialisationFilePath.toAbsolutePath().toString());
        if (cameraID > 0) {
            System.out.println("Starting process as the provider of images from camera no " + cameraID);
        } else {
            System.out.println("Starting process as the central processor of images from other cameras");
        }
        CreateCameraServersAndClients.configure();
        System.out.println("Image output directory = " + tempImageDirPath.toAbsolutePath().toString());
        if (!server) {
            System.out.println("Launching Front-end");
            launch(args);
        } else {
            System.out.println("Started process as the provider of images from camera no " + cameraID);

        }
    }

    private static void initialise() {
        isWindows();
        System.out.println("Windows = " + isWindows());
        // System.out.println("Initialisation File path = " + initialisationFilePath);
        loadPropertiesFile(initialisationFilePath.toAbsolutePath().toString());
        System.out.println("Loaded properties file");
        createImageSubdirectory();
    }

    private static void createImageSubdirectory() {
        if (tempImageDirPath != null) {
            return;
        }
        imageCounter = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String tempDirName = dtf.format(now);
        tempDirName = tempDirName.replace("/","");
        tempDirName = tempDirName.replace(" ","");
        tempDirName = tempDirName.replace(":","");
        tempImageDirPath = Paths.get(baseDir, tempImageDirectory + File.separator + tempDirName);
        try {
            Files.createDirectories(tempImageDirPath);
        } catch (FileAlreadyExistsException e) {
            // do nothing
        } catch (IOException e) {
            // something else went wrong
            e.printStackTrace();
        }
        tempImageDir = tempImageDirPath.toAbsolutePath().toString();
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
        if (sysoutDebugLevel <= 2) {
            System.out.println("Returning isWindows value as " + isWindows + ", which implies "
                    + ((isWindows == 1) ? "Windows" : "Unix"));
        }
        return isWindows;
    }

    private static void loadPropertiesFile(String initFileArgument) {
        System.out.println("initFile = " + initFileArgument);
        java.util.Properties props = new java.util.Properties();
        File initFile = new File(initFileArgument);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(initFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (Exception e) {
                System.out.println("Property initialisation file " + initialisationFile + " not found in the classpath "
                        + System.getProperty("java.class.path"));
                e.printStackTrace();
            }
        } else {
            System.out.println("InputStream = null");
            System.out.println("Property initialisation file " + initialisationFile + " not found in the classpath "
                    + System.getProperty("java.class.path"));
        }

        Set<Map.Entry<Object, Object>> entries = props.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
            if (sysoutDebugLevel <= 2) {
                System.out.println(entry.getKey().toString() + "=" + entry.getValue().toString());
            }
        }
    }

    private static int clickNextPicture() {
        Instant startTime = Instant.now();
        timeTakenForPicture = 0;
        int execValue = -1;
        if (isWindows == 1) {
            if (sysoutDebugLevel <= 1) {
                System.out.println("Clicking next picture in Windows.");
            }
            return clickNextPictureInWindows();
        }
        Path newImagePath = Paths.get(tempImageDir, cameraID + "-" + imageCounter++ + ".jpg").toAbsolutePath();
        currentImageFileName = newImagePath.toString();
        Process cameraProcess = null;
        String commandString = System.getProperty("cameracommand", "sudo raspistill --width 2028 -height 1520 --nopreview -t 20 --quality 100 -e jpg -th none");
        if (sysoutDebugLevel <= 1) {
            System.out.println("Fetched canned camera command as " + commandString);
        }
        commandString = new StringBuffer(commandString).append(" -o ").append(currentImageFileName).toString();
        if (sysoutDebugLevel <= 1) {
            System.out.println("Final camera command is " + commandString);
        }
        String[] command = commandString.split(" ");

        try {
            cameraProcess = new ProcessBuilder(command).start();
            execValue = cameraProcess.waitFor();
            cameraProcess.destroy();
            timeTakenForPicture += Duration.between(startTime, Instant.now()).toMillis();
            if (sysoutDebugLevel <= 1) {
                System.out.println("Time taken to click picture = " + timeTakenForPicture);
            }
            if (sysoutDebugLevel <= 3) {
                if (execValue == 0) {
                    System.out.println("Image taken successfully");
                } else {
                    System.out.println("Image not taken. Investigate.");
                }
            }
        } catch (Exception e) {

        }
        return execValue;
    }

    private static int clickNextPictureInWindows() {
        if (sysoutDebugLevel <= 2) {
            System.out.println("Not clicked any picture in Windows.");
        }
        return -1;
    }

    public static BufferedImage getNextImage() {
        int execValue = clickNextPicture();
        if (execValue == -1) {
            return EMPTY_IMAGE;
        }
        BufferedImage imageClicked = null;
        try {
            imageClicked = ImageIO.read(new File(currentImageFileName));
        } catch (Exception e) {
            return EMPTY_IMAGE;
        }
        return imageClicked;
    }

//    private static String getValueOfArguments(String[] args, String key) {
//        if (args == null) {
//            if (BASE_DIR_KEY.equals(key)) {
//                return BAD_ARGUMENT;
//            } else {
//                return EMPTY_STRING;
//            }
//        }
//        if (args.length == 0) {
//            if (BASE_DIR_KEY.equals(key)) {
//                return BAD_ARGUMENT;
//            } else {
//                return EMPTY_STRING;
//            }
//        }
//        if (key == null) {
//            return EMPTY_STRING;
//        }
//        if (EMPTY_STRING.equals(key)) {
//            return EMPTY_STRING;
//        }
//        for (int i = 0; i < args.length; ++i) {
//            String[] keyValuePair = args[i].split("=");
//            if (keyValuePair.length != 2) {
//                continue;
//            }
//            String thisKey = keyValuePair[0];
//            if (key.equals(thisKey)) {
//                return keyValuePair[1].trim();
//            }
//        }
//        // no match found
//        if (BASE_DIR_KEY.equals(key)) {
//            return BAD_ARGUMENT;
//        }
//        return EMPTY_STRING;
//    }

    private static String getValueOfArguments(String[] args, String key) {
        if (args == null) {
            return EMPTY_STRING;
        }
        if (args.length == 0) {
            return EMPTY_STRING;
        }
        if (key == null) {
            return EMPTY_STRING;
        }
        if (EMPTY_STRING.equals(key)) {
            return EMPTY_STRING;
        }
        for (int i = 0; i < args.length; ++i) {
            String[] keyValuePair = args[i].split("=");
            if (keyValuePair.length != 2) {
                continue;
            }
            String thisKey = keyValuePair[0];
            if (key.equals(thisKey)) {
                return keyValuePair[1].trim();
            }
        }
        // no match found
        return EMPTY_STRING;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.setTitle("View from Remote Cameras");
        root = new BorderPane();

        topPane = new HBox();
        topPane.setPrefHeight(40);
        topPane.setPadding(new Insets(2));
        topPane.setAlignment(Pos.BOTTOM_CENTER);
        topPane.setStyle("-fx-background-color: #ddd");

        Label lbInfoLabel = new Label("Select Your Camera");
        lbInfoLabel.setStyle("-fx-foreground-color: #f00");
        lbInfoLabel.setPrefHeight(30);
        ObservableList<CameraProcessor> options = FXCollections.observableArrayList();
        System.out.println("About to add " + leftCameraProcessor + " to options");
        options.add(leftCameraProcessor);
        System.out.println("About to add " + rightCameraProcessor + " to options");
        options.add(rightCameraProcessor);
        System.out.println("About to add " + topCameraProcessor + " to options");
        options.add(topCameraProcessor);
        System.out.println("About to add " + bottomCameraProcessor + " to options");
        options.add(bottomCameraProcessor);

        ComboBox<CameraProcessor> cameraOptions = new ComboBox<>();
        cameraOptions.setItems(options);
        cameraOptions.setValue(leftCameraProcessor);
        cameraOptions.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<CameraProcessor>() {

            @Override
            public void changed(ObservableValue<? extends CameraProcessor> arg0, CameraProcessor arg1, CameraProcessor arg2) {
                if (arg2 != null) {
                    System.out.println(
                            "Camera Index: " + arg2);
                    // call some function for the camera to do;
                }
            }
        });
        cameraOptions.setStyle("-fx-foreground-color: #00f");
        actionButton = new Button("Inspect Part");
        actionButton.setDisable(false);
        actionButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // don't take any action if a false event is fired when the window is in
                // minimised state. (Sigh ! That happens !!)
                if (windowMinimised) {
                    return;
                }
                actionButton.setDisable(true);
                CameraProcessor currentProcessor = cameraOptions.getValue();
                boolean success = currentProcessor.requestImage();
                // sends an ImageRequest to the camera and when ImageResponse comes back,
                // it is automatically processed because a listener has been attached to the
                // processor
                if (!success) {
                    actionButton.setDisable(false);
                }
            }
        });

        topPane.getChildren().add(lbInfoLabel);
        topPane.getChildren().add(new Label("   "));
        topPane.getChildren().add(cameraOptions);
        topPane.getChildren().add(new Label("   "));
        topPane.getChildren().add(actionButton);
        cameraOptions.setVisible(true);
        lbInfoLabel.setVisible(true);
        cameraOptions.setPrefHeight(30);
        topPane.setVisible(true);

        horizontalTop2Images = new HBox();
        horizontalTop2Images.setAlignment(Pos.CENTER);
        horizontalTop2Images.setPrefSize(480, 160);
        horizontalTop2Images.setPadding(new Insets(2));
        //horizontalTop2Images.setStyle("-fx-background-color: #ccc;");

        image11 = new FlowPane(Orientation.HORIZONTAL);
        image11.setAlignment(Pos.CENTER);
        image11.setPadding(new Insets(4));
        image11.setPrefWrapLength(240); // preferred width = 240
        image11.setPrefSize(240, 160);
        //image11.setStyle("-fx-background-color: #ccc;");

        image12 = new FlowPane(Orientation.HORIZONTAL);
        image12.setAlignment(Pos.CENTER);
        image12.setPadding(new Insets(4));
        image12.setPrefWrapLength(240); // preferred width = 240
        image12.setPrefSize(240, 160);
        //image12.setStyle("-fx-background-color: #ccc;");

        image21 = new FlowPane(Orientation.HORIZONTAL);
        image21.setAlignment(Pos.CENTER);
        image21.setPadding(new Insets(4));
        image21.setPrefWrapLength(240); // preferred width = 240
        image21.setPrefSize(240, 160);
        //image21.setStyle("-fx-background-color: #ccc;");

        image22 = new FlowPane(Orientation.HORIZONTAL);
        image22.setAlignment(Pos.CENTER);
        image22.setPadding(new Insets(4));
        image22.setPrefWrapLength(240); // preferred width = 240
        image22.setPrefSize(240, 160);
        //image22.setStyle("-fx-background-color: #ccc;");

        horizontalBottom2Images = new HBox();
        horizontalBottom2Images.setAlignment(Pos.CENTER);
        horizontalBottom2Images.setPrefSize(480, 160);
        horizontalBottom2Images.setPadding(new Insets(2));
        //horizontalBottom2Images.setStyle("-fx-background-color: #ccc;");

        imagePane = new VBox();
        imagePane.setAlignment(Pos.CENTER);
        imagePane.setPadding(new Insets(2));
        imagePane.setPrefSize(480, 320);
        //imagePane.setStyle("-fx-background-color: #ccc;");

        System.out.println("Just before issuing ImageIO.read()");

        BufferedImage image1 = null;
        try {
            image1 = ImageIO.read(new File(baseDir +"/" + "Image 1.jpg"));
        } catch (IIOException exception) {
            image1 = DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        BufferedImage image2 = null;
        try {
            image2 = ImageIO.read(new File(baseDir +"/" + "Image 2.jpg"));
        } catch (IIOException exception) {
            image2 = DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        BufferedImage image3 = null;
        try {
            image3 = ImageIO.read(new File(baseDir +"/" + "Image 3.jpg"));
        } catch (IIOException exception) {
            image3 = DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        BufferedImage image4 = null;
        try {
            image4 = ImageIO.read(new File(baseDir +"/" + "Image 4.jpg"));
        } catch (IIOException exception) {
            image4 = DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        imageView11.setVisible(true);
        imageView12.setVisible(true);
        imageView21.setVisible(true);
        imageView22.setVisible(true);
        imageView11.setImage(SwingFXUtils.toFXImage(image1,null));
        imageView12.setImage(SwingFXUtils.toFXImage(image2,null));
        imageView21.setImage(SwingFXUtils.toFXImage(image3,null));
        imageView22.setImage(SwingFXUtils.toFXImage(image4,null));

        imageView11.setFitHeight(160);
        imageView11.setFitWidth(240);
        imageView11.setPreserveRatio(true);

        imageView12.setFitHeight(160);
        imageView12.setFitWidth(240);
        imageView12.setPreserveRatio(true);

        imageView21.setFitHeight(160);
        imageView21.setFitWidth(240);
        imageView21.setPreserveRatio(true);

        imageView22.setFitHeight(160);
        imageView22.setFitWidth(240);
        imageView22.setPreserveRatio(true);

        image11.getChildren().add(imageView11);
        image12.getChildren().add(imageView12);
        image21.getChildren().add(imageView21);
        image22.getChildren().add(imageView22);

        horizontalTop2Images.getChildren().add(new Separator(Orientation.VERTICAL));
        horizontalTop2Images.getChildren().add(image11);
        horizontalTop2Images.getChildren().add(new Separator(Orientation.VERTICAL));
        horizontalTop2Images.getChildren().add(image12);
        horizontalTop2Images.getChildren().add(new Separator(Orientation.VERTICAL));

        horizontalBottom2Images.getChildren().add(new Separator(Orientation.VERTICAL));
        horizontalBottom2Images.getChildren().add(image21);
        horizontalBottom2Images.getChildren().add(new Separator(Orientation.VERTICAL));
        horizontalBottom2Images.getChildren().add(image22);
        horizontalBottom2Images.getChildren().add(new Separator(Orientation.VERTICAL));

        imagePane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        imagePane.getChildren().add(horizontalTop2Images);
        imagePane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        imagePane.getChildren().add(horizontalBottom2Images);
        imagePane.getChildren().add(new Separator(Orientation.HORIZONTAL));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefSize(460, 100);
        scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        view = new WebView();
        engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                        view.requestLayout();
                        scrollPane.requestLayout();
                        scrollPane.setVvalue(1.0f);
                        //if (newValue == Worker.State.SUCCEEDED) {
                            //document finished loading
                        //}
                    }
                }
        );
        engine.loadContent("<body><div id='content'></div></body>");
        scrollPane.setContent(view);

        Label resultLabel = new Label("Results");
        resultLabel.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 15));

        FlowPane resultLabelFlowPane = new FlowPane(Orientation.HORIZONTAL);
        resultLabelFlowPane.setStyle("-fx-background-color: #eee;");
        resultLabelFlowPane.setStyle("-fx-foreground-color: #00f;");
        resultLabelFlowPane.getChildren().add(resultLabel);
        resultLabelFlowPane.setPrefWrapLength(460);
        resultLabelFlowPane.setPrefSize(460, 20);

        resultPane = new VBox();
        resultPane.setAlignment(Pos.CENTER);
        //resultPane.setVgap(4);
        //resultPane.setHgap(4);
        //resultPane.setPrefWrapLength(200);
        resultPane.setPadding(new Insets(4));
        resultPane.setPrefSize(480, 150);
        resultPane.getChildren().add(resultLabelFlowPane);
        resultPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        resultPane.getChildren().add(scrollPane);
        resultPane.setStyle("-fx-background-color: #ccc;");

        root.setTop(new VBox(new Separator(Orientation.HORIZONTAL),topPane));
        root.setCenter(imagePane);
        root.setBottom(resultPane);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(root);

        Scene scene = new Scene(stackPane, 600, 520);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                int response = RemoteCamera1.this.closeWindowEvent(primaryStage);
                if (response == 1) {
                    System.out.println("Shutting down the application");
                    try {
                        leftCameraProcessor.shutdown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        rightCameraProcessor.shutdown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        topCameraProcessor.shutdown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        bottomCameraProcessor.shutdown();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Platform.exit();
                    System.exit(0);
                }
                if (response == 2) {
                    System.out.println("Shutting down the application");
                    try {
                        leftCameraProcessor.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        rightCameraProcessor.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        topCameraProcessor.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        bottomCameraProcessor.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    Platform.exit();
                    System.exit(0);
                }
                e.consume();
            }
        });
        primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) {
                // System.out.println("minimized:" + t1.booleanValue());
                windowMinimised = t1.booleanValue();
            }
        });

//        Platform.runLater(new Runnable() {
//
//            @Override
//            public void run() {
//                primaryStage.show();
//            }
//        });
    }

    private void appendResult(String msg) {
        Document doc = engine.getDocument();
        Element el = doc.getElementById("content");
        String s = el.getTextContent();
        el.setTextContent(s + msg);
    }

    public static ImageView getLeftCameraImageView() {
        return imageView11;
    }

    public static ImageView getRightCameraImageView() {
        return imageView12;
    }

    public static ImageView getTopCameraImageView() {
        return imageView21;
    }

    public static ImageView getBottomCameraImageView() {
        return imageView22;
    }

    private int closeWindowEvent(Stage primaryStage) {
        System.out.println("Window close request ...");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getButtonTypes().remove(ButtonType.OK);
        alert.getButtonTypes().remove(ButtonType.CANCEL);
        ButtonType shutdownAll = new ButtonType("Shutdown all cameras");
        alert.getButtonTypes().add(shutdownAll);
        ButtonType closeClients = new ButtonType("Keep cameras on");
        alert.getButtonTypes().add(closeClients);
        alert.getButtonTypes().add(ButtonType.CANCEL);
        alert.setTitle("Quit application");
        alert.setContentText(String.format("Close the application and ... ?"));
        alert.initOwner(primaryStage.getOwner());
        Optional<ButtonType> res = alert.showAndWait();

        if (res.isPresent()) {
            if (res.get().equals(shutdownAll)) {
                return 1;
            }
            if (res.get().equals(closeClients)) {
                return 2;
            }
        }
        return -1;
    }

    public static final void createLeftCameraProcessor(int udpPort, Client client, boolean connected) {
        if (leftCameraProcessor == null) {
            leftCameraProcessor = CameraProcessor.createLeftCameraProcessor("Left Side Camera", 1, imageView11, udpPort, client, connected);
        }
    }

    public static final void createRightCameraProcessor(int udpPort, Client client, boolean connected) {
        if (rightCameraProcessor == null) {
            rightCameraProcessor = CameraProcessor.createRightCameraProcessor("Right Side Camera", 2, imageView12, udpPort, client, connected);
        }
    }

    public static final void createTopCameraProcessor(int udpPort, Client client, boolean connected) {
        if (topCameraProcessor == null) {
            topCameraProcessor = CameraProcessor.createTopCameraProcessor("Top Camera", 3, imageView21, udpPort, client, connected);
        }
    }

    public static final void createBottomCameraProcessor(int udpPort, Client client, boolean connected) {
        if (bottomCameraProcessor == null) {
            bottomCameraProcessor = CameraProcessor.createBottomCameraProcessor("Bottom Camera", 4, imageView22, udpPort, client, connected);
        }
    }

    public static void enableActionButton() {
        actionButton.setDisable(false);
    }

}
