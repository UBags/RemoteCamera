package com.costheta.tests;

import com.costheta.camera.remote.client.NetworkClientCamera;
import com.costheta.image.BasePaneConstants;
import com.costheta.image.utils.ImageUtils;
import com.costheta.kems.connectingrods.ConnectingRod_AshokLeyland;
import com.costheta.kems.connectingrods.LeftSideTextProcessor;
import com.costheta.kems.connectingrods.RightSideTextProcessor;
import com.costheta.machine.*;
import com.costheta.machine.clip.ClipCoordinatesPolygonData;
import com.costheta.machine.clip.ClippingPolygon;
import com.costheta.machine.clip.utils.ClippingUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class GetPolygonCoordinates extends BasePopupWindow implements BasePaneConstants {

//    private static final String FX_EXPERIENCE_LOGO_URL = "http://fxexperience.com/wp-content/uploads/2010/06/logo.png";
//    final ObjectProperty<Image> poster = new SimpleObjectProperty<Image>(new Image(FX_EXPERIENCE_LOGO_URL));


    private static final Logger logger = LogManager.getLogger(GetPolygonCoordinates.class);
    private static final String comma = ",";
    private static final String newLine = "\n";
    private static final Color[] drawingColors =
            new Color[] {LIGHT_GREEN, LIGHT_GRAY, LIGHT_PINK, LIGHT_BLUE, LIGHT_AQUA, LIGHT_ORANGE, BRIGHT_PINK,
            LIGHT_RED, LIGHT_VIOLET};
    private static final Polygon dummyPolygon = new Polygon();
    static {
        dummyPolygon.getPoints().addAll(new Double[]{0.0,0.0,1.0,1.0});
    }

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\KEMS\\Pictures";
    private static String fileName = "6-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    private ImagePane imagePane;
    private double screenScaleFactor = 1.8/3;

    private int strokeWidth = 3;
    private int state = 1;
    private int xStart = -1;
    private int yStart = -1;
    private int xEnd = -1;
    int yEnd = -1;
    boolean windowMinimised = false;

    double widthConversion = 1.0;
    double heightConversion = 1.0;

    private LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> clippingHashMap;
    private ArrayList<ArrayList<ClippingPolygon>> clippingRegions;

    // these are the objects that we will work with, to ensure that the original is maintained
    private LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> wipHashMap;
    private ArrayList<ArrayList<ClippingPolygon>> wipClippingRegions;
    FlowPane[] clipCoordPane;
    private ArrayList<ArrayList<Text>> coordText;

    private ClipCoordinatesPolygonData clipCoordinatesData;
    private String partName;
    private String inspectionPointName;
    private BufferedImage inputImage;

    ObservableList<BaseImageProcessor> imageProcessorsList;
    ComboBox<BaseImageProcessor> imageProcessorsComboBox;
    StringConverter<BaseImageProcessor> imageProcessorsNameConverter;
    ChangeListener<BaseImageProcessor> imageProcessorsChangeListener;

    private ArrayList<BaseImageProcessor> currentImageProcessors;
    private BaseImageProcessor currentImageProcessor;
    private int currentProcessorIndex = 0;

    ObservableList<ClippingPolygon> clippingRegionsList;
    ComboBox<ClippingPolygon> clippingRegionsComboBox;
    StringConverter<ClippingPolygon> clippingRegionsNameConverter;
    ChangeListener<ClippingPolygon> clippingRegionsChangeListener;

    ArrayList<ClippingPolygon> currentClippingPolygons;
    int currentClippingPolygonIndex = 1;
    ClippingPolygon currentClippingPolygon;
    private Polygon currentPolygon;
    private Polygon oldPolygon;
    private Double xNext;
    private Double yNext;
    private boolean newPolygonNeeded = true;
    private int escKeyCounter = 0;
    private boolean mouseMoved = false;
    private boolean lastActionWasEscKeyPressed = false;
    // Paint whitePaint = Paint.valueOf("whitesmoke");
    // Paint bluePaint = Paint.valueOf("blue");
    // Paint yellowPaint = Paint.valueOf("yellow");

    private boolean needsToBeSaved = false;

    private ArrayList<Shape> identifiers = new ArrayList<>();

    public GetPolygonCoordinates(ClipCoordinatesPolygonData clipCoordinatesData, Stage owner) {
        this.clipCoordinatesData = clipCoordinatesData;
        this.partName = clipCoordinatesData.getPartName();
        this.inspectionPointName = clipCoordinatesData.getInspectionPointName();
        this.inputImage = clipCoordinatesData.getInputImage();
        this.currentImageProcessors = clipCoordinatesData.getProcessors();
        clippingHashMap = ClippingUtils.getClipPolygonsForInspectionPoint(clipCoordinatesData.getInspectionPoint());
        clippingRegions = ClippingUtils.getClippingPolygonsArray(clipCoordinatesData.getInspectionPoint());
        wipHashMap = ClippingUtils.copyPolygonHashMap(clippingHashMap);
        this.owner = owner;
    }

    public static void main(String[] args) {

        System.out.println("Testing ChamferAndNotch.processImage()");

        debugDirectory = debugDirectory + "/";
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {
        }

        BufferedImage original = null;
        try {
            original = ImageIO.read(new File(inputDirectory + "/" + fileName));
        } catch (Exception e) {

        }

        ClippingUtils.loadClippingProperties();
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new JFXPanel(); // initializes JavaFX environment
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException ie) {

        }

        NetworkClientCamera leftCamera = new NetworkClientCamera("Left Side Camera", 1);
        Part part1 = new ConnectingRod_AshokLeyland("AL1", "100001", 2, "Ashok Leyland Connecting Rod - Single Notch");
        InspectionPoint leftCameraInspectionPoint1 = new InspectionPoint("Left IP", leftCamera);

        ArrayList<String> leftSideStringPatterns1 = new ArrayList<>();
        leftSideStringPatterns1.add("DDA");
        LeftSideTextProcessor leftSideTextProcessor1 = new LeftSideTextProcessor("Text Inspection 1", leftSideStringPatterns1, 3);

        ArrayList<String> rightSideStringPatterns1 = new ArrayList<>();
        rightSideStringPatterns1.add("ADDDA");
        RightSideTextProcessor rightSideTextProcessor1 = new RightSideTextProcessor("Text Inspection 2", rightSideStringPatterns1, 4);

        // Add the inspection points to the part
        part1.addInspectionPoint(leftCameraInspectionPoint1);
        // Add the processors to the Inspection Points
        leftCameraInspectionPoint1.addImageProcessor(leftSideTextProcessor1);
        leftCameraInspectionPoint1.addImageProcessor(rightSideTextProcessor1);
        logger.trace("Created Part 1 and added inspection points and image processors");

        ClipCoordinatesPolygonData ccd = new ClipCoordinatesPolygonData(leftCameraInspectionPoint1);
        GetPolygonCoordinates gcc = new GetPolygonCoordinates(ccd, null);
        gcc.inputImage = original;
        gcc.process();
    }

    protected Object processImage() {

        //System.out.println("Entered processImage()");
        if ((partName == null) || ("".equals(partName))) {
            logger.debug("Unable to process as there is no Part Name");
            //System.out.println("Unable to process as there is no Part Name");
            return Boolean.FALSE;
        }
        if ((inspectionPointName == null) || ("".equals(inspectionPointName))) {
            logger.debug("Unable to process as there is no Inspection Point Name");
            //System.out.println("Unable to process as there is no Inspection Point Name");
            return Boolean.FALSE;
        }
        if ((currentImageProcessors == null) || (currentImageProcessors.size() == 0)) {
            logger.debug("Unable to process as there is no Processor Name");
            //System.out.println("Unable to process as there is no Processor Name");
            return Boolean.FALSE;
        }
        if (inputImage == null) {
            logger.debug("Unable to process as no input image is provided");
            //System.out.println("Unable to process as no input image is provided");
            return Boolean.FALSE;
        }

        logger.trace("Past the checks in processImage()");
        //System.out.println("Past the checks in processImage()");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new JFXPanel(); // initializes JavaFX environment
            }
        });

        Toolkit defaultToolKit = Toolkit.getDefaultToolkit();
        Dimension screenSize = defaultToolKit.getScreenSize();

        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        double screenRatio = screenHeight * 1.0 / screenWidth;

        int w = inputImage.getWidth();
        int h = inputImage.getHeight();
        double currentRatio = h * 1.0 / w;

        boolean fixHeightFirst = false;
        if (screenRatio < currentRatio) {
            fixHeightFirst = true;
        }

        int targetWidth = 0;
        int targetHeight = 0;

        if (fixHeightFirst) {
            targetHeight = (int) (screenHeight * screenScaleFactor);
            targetWidth = (int) (targetHeight / currentRatio);
        } else {
            targetWidth = (int) (screenWidth * screenScaleFactor);
            targetHeight = (int) (targetWidth * currentRatio);
        }

        // after getting the widthConversion and heightConversion,
        // immediately calculate the clippinRegions with the display coordinates
        widthConversion = targetWidth * 1.0 / w;
        heightConversion = targetHeight * 1.0 / h;
        wipClippingRegions = convertToDisplayCoordinates(clippingRegions);

        BufferedImage bufferedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(widthConversion, heightConversion);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        bufferedImage = scaleOp.filter(inputImage, bufferedImage);
        imagePane = new ImagePane(bufferedImage);

        imagePane.setFocusTraversable(true);
        imagePane.requestFocus();

        final int imageWidth = targetWidth;
        final int imageHeight = targetHeight;
        System.out.println("imageWidth = " + imageWidth);
        System.out.println("imageHeight = " + imageHeight);

        final int partPaneHeight = 40;
        final int topPaneHeight = 30;
        final int bottomPaneHeight = 60;
        final int spacerWidth = 5;
        final int topPaneInset = 2;
        final int bottomPaneInset = 2;
        final int saveButtonWidth = 80;
        final int partLabelWidth = 125;
        final int ipLabelWidth = 250;
        final int processorLabelWidth = 80;
        final int processorComboBoxWidth = 180;
        final int regionLabelWidth = 50;
        final int assessmentRegionComboBoxWidth = 30;
        final int labelHeight = 15;
        final int coordTextHeight = 10;

        HBox partPane1 = new HBox();
        partPane1.setPrefWidth(targetWidth);
        partPane1.setPrefHeight(topPaneHeight + 2 * topPaneInset);
        partPane1.setPadding(new Insets(topPaneInset));
        partPane1.setAlignment(Pos.CENTER_LEFT);
        partPane1.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        Region partPaneSpacer1 = new Region();
        partPaneSpacer1.setPrefWidth(spacerWidth);
        Label partNameLabel = new Label("Part : " + partName);
        partNameLabel.setFont(TREBUCHET_BOLD_12);
        partNameLabel.setTextFill(WHITE);
        partNameLabel.setAlignment(Pos.CENTER_LEFT);
        partNameLabel.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        partNameLabel.setPrefHeight(partPaneHeight - 7);
        partNameLabel.setPrefWidth(partLabelWidth);
        Region partPaneSpacer2 = new Region();
        partPaneSpacer2.setPrefWidth(spacerWidth);
        Label ipNameLabel = new Label("Inspection Point : " + inspectionPointName);
        ipNameLabel.setFont(TREBUCHET_BOLD_12);
        ipNameLabel.setTextFill(WHITE);
        ipNameLabel.setAlignment(Pos.CENTER_LEFT);
        ipNameLabel.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        ipNameLabel.setPrefHeight(topPaneHeight - 7);
        ipNameLabel.setPrefWidth(ipLabelWidth);
        Region partPaneSpacer3 = new Region();
        partPaneSpacer3.setPrefWidth(spacerWidth);
        HBox.setHgrow(partPaneSpacer1, Priority.NEVER);
        HBox.setHgrow(partNameLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer2, Priority.NEVER);
        HBox.setHgrow(ipNameLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer3, Priority.ALWAYS);
        partPane1.getChildren().add(partPaneSpacer1);
        partPane1.getChildren().add(partNameLabel);
        partPane1.getChildren().add(partPaneSpacer2);
        partPane1.getChildren().add(ipNameLabel);
        partPane1.getChildren().add(partPaneSpacer3);

        final HBox partPane2 = new HBox();
        partPane2.setPrefWidth(targetWidth);
        partPane2.setPrefHeight(topPaneHeight + 2 * topPaneInset);
        partPane2.setPadding(new Insets(topPaneInset));
        partPane2.setAlignment(Pos.CENTER_LEFT);
        partPane2.setBackground(DARKGRAY_BACKGROUND);
        Region partPaneSpacer4 = new Region();
        partPaneSpacer4.setPrefWidth(spacerWidth);
        Label processorLabel = new Label("Processor  ");
        processorLabel.setFont(TREBUCHET_BOLD_12);
        processorLabel.setTextFill(WHITE);
        processorLabel.setAlignment(Pos.CENTER_LEFT);
        processorLabel.setBackground(DARKGRAY_BACKGROUND);
        processorLabel.setPrefHeight(topPaneHeight - 7);
        processorLabel.setPrefWidth(processorLabelWidth);
        Region partPaneSpacer5 = new Region();
        partPaneSpacer5.setPrefWidth(spacerWidth);

        clippingRegionsNameConverter = new StringConverter<ClippingPolygon>() {
            @Override
            public String toString(ClippingPolygon assessmentRegion) {
                return String.valueOf(assessmentRegion.id);
            }

            @Override
            public ClippingPolygon fromString(String id) {
                return clippingRegionsList.stream()
                        .filter(item -> String.valueOf(item.id).equals(id))
                        .collect(Collectors.toList()).get(0);
            }
        };
        clippingRegionsChangeListener = new ChangeListener<ClippingPolygon>() {
            @Override
            public void changed(ObservableValue observable, ClippingPolygon oldAssessmentRegion, ClippingPolygon newAssessmentRegion) {
                // update the polygon stored in the previous assessment region with the coordinates of the current polygon
                logger.trace("Entering clippingRegionsChangeListener");
                logger.debug(echoStatus());
                updateClippingRegions(currentPolygon, currentClippingPolygonIndex);
                currentClippingPolygon = newAssessmentRegion;
                currentPolygon = null;
                escKeyCounter = 0;
                oldPolygon = currentClippingPolygon.clippingPolygon;
                // updateClippingRegions(dummyPolygon, currentClippingPolygonIndex);
                for (int i = 0; i < currentClippingPolygons.size(); ++i) {
                    if (currentClippingPolygon == currentClippingPolygons.get(i)) {
                        currentClippingPolygonIndex = i + 1;
                        break;
                    }
                }
                drawAllPolygons(currentClippingPolygonIndex);
                // updateFlowPaneBackground(clipCoordPane, currentProcessorIndex);
                updateLabels(coordText, currentProcessorIndex, currentClippingPolygonIndex);
                logger.trace("Leaving clippingRegionsChangeListener");
                logger.debug(echoStatus());
            }
        };
        imageProcessorsNameConverter = new StringConverter<BaseImageProcessor>() {
            @Override
            public String toString(BaseImageProcessor processor) {
                return processor.getName();
            }

            @Override
            public BaseImageProcessor fromString(String id) {
                return imageProcessorsList.stream()
                        .filter(item -> item.getName().equals(id))
                        .collect(Collectors.toList()).get(0);
            }
        };

        imageProcessorsList = FXCollections.observableArrayList(currentImageProcessors);
        imageProcessorsComboBox = new ComboBox(imageProcessorsList);
        imageProcessorsComboBox.setConverter(imageProcessorsNameConverter);
        imageProcessorsComboBox.getSelectionModel().select(0);
        currentImageProcessor = currentImageProcessors.get(0);
        imageProcessorsComboBox.setEditable(false);
        imageProcessorsComboBox.setPrefWidth(targetWidth - 5 * spacerWidth - processorLabelWidth - regionLabelWidth - assessmentRegionComboBoxWidth - 20);
        // processorComboBox.setPrefWidth(processorComboBoxWidth);

        Region partPaneSpacer6 = new Region();
        partPaneSpacer6.setPrefWidth(spacerWidth);
        Label regionLabel = new Label("Region ");
        regionLabel.setFont(TREBUCHET_BOLD_12);
        regionLabel.setTextFill(WHITE);
        regionLabel.setAlignment(Pos.CENTER_LEFT);
        regionLabel.setBackground(DARKGRAY_BACKGROUND);
        regionLabel.setPrefHeight(topPaneHeight - 7);
        regionLabel.setPrefWidth(regionLabelWidth);
        Region partPaneSpacer7 = new Region();
        partPaneSpacer7.setPrefWidth(spacerWidth);

        currentClippingPolygons = wipClippingRegions.get(currentProcessorIndex);
        clippingRegionsList = FXCollections.observableArrayList(currentClippingPolygons);
        clippingRegionsComboBox = new ComboBox(clippingRegionsList);
        clippingRegionsComboBox.setConverter(clippingRegionsNameConverter);
        clippingRegionsComboBox.getSelectionModel().select(0);
        currentClippingPolygon = currentClippingPolygons.get(0);
        currentPolygon = null;
        escKeyCounter = 0;
        oldPolygon = currentClippingPolygon.clippingPolygon;
        clippingRegionsComboBox.setEditable(false);
        clippingRegionsComboBox.valueProperty().addListener(clippingRegionsChangeListener);
        clippingRegionsComboBox.setPrefWidth(assessmentRegionComboBoxWidth);

        final HBox assessmentRegionHolder = new HBox();
        assessmentRegionHolder.setAlignment(Pos.CENTER);
        assessmentRegionHolder.setBackground(DARKGRAY_BACKGROUND);
        assessmentRegionHolder.setPrefHeight(topPaneHeight - 7);
        assessmentRegionHolder.setPrefWidth(assessmentRegionComboBoxWidth);
        assessmentRegionHolder.getChildren().add(clippingRegionsComboBox);

        Region partPaneSpacer8 = new Region();
        partPaneSpacer8.setPrefWidth(spacerWidth);
        HBox.setHgrow(partPaneSpacer4, Priority.NEVER);
        HBox.setHgrow(processorLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer5, Priority.NEVER);
        HBox.setHgrow(imageProcessorsComboBox, Priority.ALWAYS);
        HBox.setHgrow(partPaneSpacer6, Priority.NEVER);
        HBox.setHgrow(regionLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer7, Priority.NEVER);
        HBox.setHgrow(assessmentRegionHolder, Priority.NEVER);
        HBox.setHgrow(clippingRegionsComboBox, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer8, Priority.NEVER);

        partPane2.getChildren().add(partPaneSpacer4);
        partPane2.getChildren().add(processorLabel);
        partPane2.getChildren().add(partPaneSpacer5);
        partPane2.getChildren().add(imageProcessorsComboBox);
        partPane2.getChildren().add(partPaneSpacer6);
        partPane2.getChildren().add(regionLabel);
        partPane2.getChildren().add(partPaneSpacer7);
        partPane2.getChildren().add(assessmentRegionHolder);
        partPane2.getChildren().add(partPaneSpacer8);
        partPane2.setVisible(true);

        int noOfProcessors = currentImageProcessors.size();
        clipCoordPane = new FlowPane[noOfProcessors];

        imageProcessorsChangeListener = new ChangeListener<BaseImageProcessor>() {
            @Override
            public void changed(ObservableValue observable, BaseImageProcessor oldValue, BaseImageProcessor newValue) {
                logger.trace("Entering imageProcessorsChangeListener");
                logger.debug(echoStatus());
                currentImageProcessor = newValue;
                // System.out.println("Processor changed to " + currentProcessor.getName());
                for (int i = 0; i < currentImageProcessors.size(); ++i) {
                    if (currentImageProcessor == currentImageProcessors.get(i)) {
                        currentProcessorIndex = i;
                        break;
                    }
                }
                removeAllPolygons();
                assessmentRegionHolder.getChildren().remove(clippingRegionsComboBox);
                currentClippingPolygons = wipClippingRegions.get(currentProcessorIndex);
                // System.out.println("Current assessment regions changed to " + currentAssessmentRegionsInProcessor);
                clippingRegionsList = FXCollections.observableArrayList(currentClippingPolygons);
                clippingRegionsComboBox = new ComboBox(clippingRegionsList);
                clippingRegionsComboBox.setConverter(clippingRegionsNameConverter);
                clippingRegionsComboBox.getSelectionModel().select(0);
                currentClippingPolygon = currentClippingPolygons.get(0);
                currentPolygon = null;
                escKeyCounter = 0;
                oldPolygon = currentClippingPolygon.clippingPolygon;
                for (int i = 0; i < currentClippingPolygons.size(); ++i) {
                    if (currentClippingPolygon == currentClippingPolygons.get(i)) {
                        currentClippingPolygonIndex = i + 1;
                        break;
                    }
                }
                clippingRegionsComboBox.setEditable(false);
                clippingRegionsComboBox.valueProperty().addListener(clippingRegionsChangeListener);
                clippingRegionsComboBox.setPrefWidth(assessmentRegionComboBoxWidth);
                assessmentRegionHolder.getChildren().add(clippingRegionsComboBox);
                assessmentRegionHolder.setVisible(true);
                drawAllPolygons(currentClippingPolygonIndex);
                updateFlowPaneBackground(clipCoordPane, currentProcessorIndex);
                updateLabels(coordText, currentProcessorIndex, currentClippingPolygonIndex);
                logger.trace("Leaving imageProcessorsChangeListener");
                logger.debug(echoStatus());
            }
        };
        imageProcessorsComboBox.valueProperty().addListener(imageProcessorsChangeListener);

        HBox topPaneMessage = new HBox();
        topPaneMessage.setPrefWidth(targetWidth);
        topPaneMessage.setPrefHeight(topPaneHeight + 2 * topPaneInset);
        topPaneMessage.setPadding(new Insets(topPaneInset));
        topPaneMessage.setAlignment(Pos.CENTER_LEFT);
        topPaneMessage.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        Label lbInfoLabel = new Label("Draw a polygon. Left-click to start; left-click to define next point; right-click to finish defining the polygon.\nPress <Esc> once to remove last point. Press <Esc> twice to go back to the previous polygon");
        lbInfoLabel.setFont(TREBUCHET_BOLD_8);
        lbInfoLabel.setTextFill(WHITE);
        lbInfoLabel.setAlignment(Pos.CENTER);
        lbInfoLabel.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        lbInfoLabel.setPrefHeight(topPaneHeight - 7);
        lbInfoLabel.setPrefWidth(targetWidth - 2 * spacerWidth - 5);
        lbInfoLabel.setWrapText(true);
        Region spacer1 = new Region();
        spacer1.setPrefWidth(spacerWidth);
        Region spacer2 = new Region();
        spacer2.setPrefWidth(spacerWidth);
        HBox.setHgrow(spacer1, Priority.NEVER);
        HBox.setHgrow(lbInfoLabel, Priority.NEVER);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        topPaneMessage.getChildren().add(spacer1);
        topPaneMessage.getChildren().add(lbInfoLabel);
        topPaneMessage.getChildren().add(spacer2);
        topPaneMessage.setVisible(true);

        VBox topVBox = new VBox(partPane1, new Separator(Orientation.HORIZONTAL), partPane2, new Separator(Orientation.HORIZONTAL), topPaneMessage);

        int bPaneHeight = Math.max(bottomPaneHeight + 2 * bottomPaneInset, currentImageProcessors.size() * labelHeight);
        HBox bottomPane = new HBox();
        bottomPane.setPrefWidth(targetWidth);
        // bottomPane.setPrefHeight(bottomPaneHeight + 2 * bottomPaneInset);
        bottomPane.setPrefHeight(bPaneHeight);
        bottomPane.setPadding(new Insets(bottomPaneInset));
        bottomPane.setAlignment(Pos.CENTER_LEFT);
        bottomPane.setBackground(VERYLIGHTPINK_BACKGROUND);
        Button saveButton = new Button("Save the Clip Coordinates");
        saveButton.wrapTextProperty().setValue(true);
        saveButton.setPrefWidth(saveButtonWidth);
        saveButton.setPrefHeight(bottomPaneHeight - 7);
        saveButton.setFont(TREBUCHET_BOLD_12);
        saveButton.setTextFill(BLACK);
        saveButton.setDisable(false);
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // don't take any action if a false event is fired when the window is in
                // minimised state. (Sigh ! That happens !!)
                if (windowMinimised) {
                    return;
                }
                ClippingUtils.saveClippingPolygonProperties(wipHashMap, convertToImageCoordinates(wipClippingRegions));
                // logger.trace("Saving following coordinate " + selection);
                needsToBeSaved = false;
            }
        });
        int coordPaneWidth = targetWidth - 3 * spacerWidth - saveButtonWidth - 5;

        for (int i = 0; i < noOfProcessors; ++i) {
            clipCoordPane[i] = new FlowPane();
            clipCoordPane[i].setAlignment(Pos.CENTER_LEFT);
            if (i != 0) {
                clipCoordPane[i].setBackground(VERYLIGHTPINK_BACKGROUND);
            } else {
                clipCoordPane[i].setBackground(BLUE_BACKGROUND);
            }
            clipCoordPane[i].setPrefHeight(bottomPaneHeight - 7);
            clipCoordPane[i].setPrefWidth(coordPaneWidth);
            HBox.setHgrow(clipCoordPane[i], Priority.ALWAYS);
        }
        Region spacer3 = new Region();
        spacer3.setPrefWidth(spacerWidth);
        Region spacer4 = new Region();
        spacer4.setPrefWidth(spacerWidth);

        coordText = new ArrayList<>();
        for (int i = 0; i < wipClippingRegions.size(); ++i) {
            Region aSpacer = new Region();
            aSpacer.setPrefWidth(spacerWidth);
            clipCoordPane[i].getChildren().add(aSpacer);
            ArrayList<Text> labelLine = new ArrayList<>();
            if (i == currentProcessorIndex) {
                for (int j = 0; j < wipClippingRegions.get(i).size(); ++j) {
                    Text aText = new Text();
                    aText.setFont(TREBUCHET_BOLD_7);
                    if (j == currentClippingPolygonIndex - 1) {
                        aText.setFill(Color.YELLOW);
                    } else {
                        aText.setFill(WHITE);
                    }
                    aText.setTextAlignment(TextAlignment.LEFT);
                    aText.setWrappingWidth(coordPaneWidth * 1.0 / (wipClippingRegions.get(i).size() + 0.15));
                    labelLine.add(aText);
                    clipCoordPane[i].getChildren().add(aText);
                }
                coordText.add(labelLine);
            } else {
                for (int j = 0; j < wipClippingRegions.get(i).size(); ++j) {
                    Text aText = new Text();
                    //String bgColor = "-rtfx-background-color: white";
                    //aText.setStyle(bgColor);
                    aText.setFont(TREBUCHET_BOLD_7);
                    aText.setFill(BLACK);
                    aText.setTextAlignment(TextAlignment.LEFT);
                    aText.setWrappingWidth(coordPaneWidth * 1.0 / (wipClippingRegions.get(i).size() + 0.15));
                    labelLine.add(aText);
                    clipCoordPane[i].getChildren().add(aText);
                }
                coordText.add(labelLine);
            }
        }

        HBox.setHgrow(spacer3, Priority.NEVER);
        HBox.setHgrow(saveButton, Priority.NEVER);
        HBox.setHgrow(spacer4, Priority.NEVER);

        VBox coordinates = new VBox();
        for (int i = 0; i < clipCoordPane.length; ++i) {
            coordinates.getChildren().add(clipCoordPane[i]);
        }
        bottomPane.getChildren().add(spacer3);
        bottomPane.getChildren().add(saveButton);
        bottomPane.getChildren().add(spacer4);
        bottomPane.getChildren().add(coordinates);
        bottomPane.setVisible(true);

        BorderPane constrainingPane = new BorderPane();

        constrainingPane.setTop(topVBox);
        constrainingPane.setCenter(imagePane);
        constrainingPane.setBottom(bottomPane);
        constrainingPane.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-insets: -2;");

        // layout the scene.
        StackPane layout = new StackPane();
        layout.getChildren().add(constrainingPane);
        layout.setStyle("-fx-background-color: whitesmoke;");
        Scene scene = new Scene(layout, targetWidth, targetHeight + 2 * partPaneHeight + topPaneHeight + bottomPaneHeight); // extra 3 * 10 for the Separators

        // clamp the pane to the scene size.
        constrainingPane.maxWidthProperty().bind(scene.widthProperty().divide(1));
        constrainingPane.minWidthProperty().bind(scene.widthProperty().divide(1));
        constrainingPane.maxHeightProperty().bind(scene.heightProperty().divide(1));
        constrainingPane.minHeightProperty().bind(scene.heightProperty().divide(1));

        imagePane.setOnMouseEntered(new EventHandler() {
            @Override
            public void handle(Event event) {
                scene.setCursor(Cursor.HAND); //Change cursor to hand
            }
        });

        imagePane.setOnMouseExited(new EventHandler() {
            public void handle(Event me) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });

        imagePane.setOnMouseClicked(new EventHandler() {
            @Override
            public void handle(Event event) {
                mouseMoved = false;
                lastActionWasEscKeyPressed = false;
                escKeyCounter = 0;
                MouseEvent me = (MouseEvent) event;
                MouseButton button = me.getButton();
                if (button != MouseButton.PRIMARY) {
                    logger.trace("Detected Right Click");
                    logger.debug("Before updating to new polygon - " + echoStatus());
                    scene.setCursor(Cursor.HAND);
                    if (!newPolygonNeeded) {
                        Rectangle bounds = ImageUtils.getBoundingRectangle(currentPolygon);
                        int actualX = (int) (bounds.getX() / widthConversion);
                        int actualY = (int) (bounds.getY() / heightConversion);
                        int actualWidth = (int) (bounds.getWidth() / widthConversion);
                        int actualHeight = (int) (bounds.getHeight() / heightConversion);
                        String coords = new StringBuilder(currentImageProcessors.get(currentProcessorIndex).getName()).append(": (startX :").append(actualX)
                                .append(", startY :").append(actualY).append(", width :").append(actualWidth)
                                .append(", height :").append(actualHeight).append(")").toString();
                        logger.info("The final coords of the bounding Rectangle for this Polygon are " + coords);
                        removeAllPolygons();
                        updateClippingRegions(currentPolygon, currentClippingPolygonIndex);
                        updateLabels(coordText, currentProcessorIndex, currentClippingPolygonIndex);
                        drawAllPolygons(currentClippingPolygonIndex);
                        coordinates.requestLayout();
                        logger.debug("After updating to new polygon - " + echoStatus());
                        needsToBeSaved = true;
                    }
                    newPolygonNeeded = true;
                } else {
                    xNext = me.getX();
                    yNext = me.getY();
                    removeAllPolygons();
                    if (newPolygonNeeded) {
                        currentPolygon = new Polygon();
                        newPolygonNeeded = false;
                    }
                    currentPolygon.getPoints().add(xNext);
                    currentPolygon.getPoints().add(yNext);
                    setCurrentPolygon(currentPolygon);
                    scene.setCursor(Cursor.CROSSHAIR);
                    drawAllPolygonsExceptCurrent(currentClippingPolygonIndex);
                    drawYellowPolygon(currentPolygon);
                    // System.out.println("Polygon = " + currentPolygon);
                }
            }
        });

        imagePane.setOnMouseMoved(new EventHandler() {
            @Override
            public void handle(Event event) {
                // System.out.println("Entered Mouse dragged");
                if (newPolygonNeeded) {
                    return;
                }
                removePolygon(currentPolygon);
                mouseMoved = true;
                if (!lastActionWasEscKeyPressed) {
                    int currentNumberOfPoints = currentPolygon.getPoints().size();
                    if (currentNumberOfPoints > 2) {
                        currentPolygon.getPoints().remove(currentNumberOfPoints - 2, currentNumberOfPoints);
                    }
                }
                lastActionWasEscKeyPressed = false;
                MouseEvent me = (MouseEvent) event;
                xNext = me.getX();
                yNext = me.getY();
                currentPolygon.getPoints().add(xNext);
                currentPolygon.getPoints().add(yNext);
                drawYellowPolygon(currentPolygon);
                scene.setCursor(Cursor.CROSSHAIR);
                // System.out.println("Polygon = " + currentShape);
            }
        });

        scene.setOnKeyPressed(new EventHandler() {
            @Override
            public void handle(Event event) {
                KeyEvent me = (KeyEvent) event;
                if (newPolygonNeeded) {
                    return;
                }
                if (KeyCode.ESCAPE == me.getCode()) {
                    System.out.println("Detected Escape Key Press. mouseMoved = " + mouseMoved);
                    ++escKeyCounter;
                    removePolygon(currentPolygon);
                    if (escKeyCounter == 1) {
                        int currentNumberOfPoints = currentPolygon.getPoints().size();
                        System.out.println("Starting no of points = " + currentNumberOfPoints);
                        if (currentNumberOfPoints > 2) {
                            currentPolygon.getPoints().remove(currentNumberOfPoints - 2, currentNumberOfPoints);
                        }
                        if (mouseMoved) {
                            currentNumberOfPoints = currentPolygon.getPoints().size();
                            System.out.println("No of points = " + currentNumberOfPoints);
                            if (currentNumberOfPoints > 2) {
                                currentPolygon.getPoints().remove(currentNumberOfPoints - 2, currentNumberOfPoints);
                            }
                        }
                        currentNumberOfPoints = currentPolygon.getPoints().size();
                        System.out.println("Points remaining = " + currentNumberOfPoints);
                        drawYellowPolygon(currentPolygon);
                    } else {
                        if (escKeyCounter > 1) {
                            currentPolygon = oldPolygon;
                            escKeyCounter = 0;
                            newPolygonNeeded = true;
                            mouseMoved = false;
                            drawYellowPolygon(currentPolygon);
                        }
                    }
                    lastActionWasEscKeyPressed = true;
                }
            }
        });

//        imagePane.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
//            if (KeyCode.ESCAPE == event.getCode()) {
//                System.out.println("Detected Escape Key Press");
//                ++escKeyCounter;
//                removePolygon(currentPolygon);
//                if (escKeyCounter == 1) {
//                    int currentNumberOfPoints = currentPolygon.getPoints().size();
//                    if (currentNumberOfPoints > 2) {
//                        currentPolygon.getPoints().remove(currentNumberOfPoints - 2, currentNumberOfPoints);
//                    }
//                    drawYellowPolygon(currentPolygon);
//                } else {
//                    if (escKeyCounter > 1) {
//                        currentPolygon = oldPolygon;
//                        drawYellowPolygon(currentPolygon);
//                        escKeyCounter = 0;
//                    }
//                }
//            }
//        });

//        imagePane.setOnMouseReleased(new EventHandler() {
//            @Override
//            public void handle(Event event) {
//                MouseEvent me = (MouseEvent) event;
//                if (mouseClickedState && mouseDragged) {
//                    // if (selection.getParent() != null) {
//                    //    imagePane.getChildren().remove(selection);
//                    // }
//                    selection.setX(xStart);
//                    selection.setY(yStart);
//                    xEnd = (int) me.getX();
//                    yEnd = (int) me.getY();
//                    if (xEnd >= xStart) {
//                        xEnd = Math.min(imageWidth - strokeWidth, xEnd);
//                        selection.setWidth(xEnd - xStart);
//                    } else {
//                        xEnd = Math.max(0,xEnd);
//                        selection.setX(xEnd);
//                        selection.setWidth(xStart - xEnd);
//                    }
//                    if (yEnd >= yStart) {
//                        yEnd = Math.min(imageHeight - 1 * strokeWidth, yEnd);
//                        selection.setHeight(yEnd - yStart);
//                    } else {
//                        yEnd = Math.max(yEnd, 0);
//                        selection.setY(yEnd);
//                        selection.setHeight(yStart - yEnd);
//                    }
//                    // System.out.println("Drawing rectangle from [" + xStart + "," + yStart + "] till [" + xEnd + "," + yEnd + "]");
//                    scene.setCursor(Cursor.HAND);
//                    int actualX = (int) (selection.getX() / widthConversion);
//                    int actualY = (int) (selection.getY() / heightConversion);
//                    int actualWidth =  (int) (selection.getWidth() / widthConversion);
//                    int actualHeight =  (int) (selection.getHeight() / heightConversion);
//                    // if (mouseDragged) {
//                    String coords = new StringBuilder(currentProcessors.get(currentProcessorIndex).getName()).append(": (startX :").append(actualX)
//                            .append(", startY :").append(actualY).append(", width :").append(actualWidth)
//                            .append(", height :").append(actualHeight).append(")").toString();
//                    logger.info("The final coords are " + coords);
//                    updateAssessmentRegion(selection, currentAssessmentRegionIndex);
//                    updateLabels(coordText, currentProcessorIndex, currentAssessmentRegionIndex);
//                    drawRectangles(currentAssessmentRegionIndex);
//                    coordinates.requestLayout();
//                    needsToBeSaved = true;
//                    // }
//                }
//                mouseDragged = false;
//                mouseClickedState = false;
//            }
//        });
        // System.out.println("CoordText = " + coordText);
        updateLabels(coordText, currentProcessorIndex, currentClippingPolygonIndex);

        Stage thisStage = new Stage();
        thisStage.setTitle("Configuration");
//        scene.addEventFilter(KeyEvent.KEY_PRESSED,
//                event -> System.out.println("Pressed: " + event.getCode()));
//        imagePane.setFocusTraversable(true);
//        imagePane.requestFocus();
        thisStage.setScene(scene);
        thisStage.setResizable(false);
        thisStage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            thisStage.initOwner(owner);
        }
        thisStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                if (needsToBeSaved) {
                    int response = GetPolygonCoordinates.this.closeWindowEvent(thisStage);
                    // 1 = exit; 2 = save and exit; 3 = cancel
                    if (response == 1) {
                        logger.trace("Closing down the clipping coordinates window");
                        thisStage.close();
                    }
                    if (response == 2) {
                        logger.trace("Saving and then closing down the clipping coordinates window");
                        ClippingUtils.saveClippingPolygonProperties(wipHashMap, convertToImageCoordinates(wipClippingRegions));
                        // logger.trace("Saving following coordinate " + selection);
                        thisStage.close();
                    }
                } else {
                    thisStage.close();
                }
                e.consume();
            }
        });
        thisStage.show();
        drawAllPolygons(currentClippingPolygonIndex);
        return Boolean.TRUE;
    }

    private void updateFlowPaneBackground(FlowPane[] labels, int index) {
        for (int i = 0; i < labels.length; ++i) {
            if (i == index) {
                labels[i].setBackground(BLUE_BACKGROUND);
            } else {
                labels[i].setBackground(VERYLIGHTPINK_BACKGROUND);
            }
        }
    }

    private String getCoords(ClippingPolygon clippingPolygon) {
        Polygon polygon = clippingPolygon.clippingPolygon;
        polygon = convertPolygonToImageCoordinates(polygon);
        Rectangle bounds = ImageUtils.getBoundingRectangle(polygon);
        String coords = (int) Math.round(bounds.getX()) +"," + (int) Math.round(bounds.getY()) + "," + (int) Math.round(bounds.getWidth()) + "," + (int) Math.round(bounds.getHeight());
        return coords;
    }

    private void updateLabels(ArrayList<ArrayList<Text>> lines, int processorIndex, int regionIndex) {
        for (int i = 0; i < lines.size(); ++i) {
            ArrayList<Text> line = lines.get(i);
            ArrayList<ClippingPolygon> clippingPolygons = wipClippingRegions.get(i);
            for (int j = 0; j < line.size(); ++j) {
                Text word = line.get(j);
                ClippingPolygon clippingPolygon = clippingPolygons.get(j);
                if (i != processorIndex) {
                    //String bgColor = "-rtfx-background-color: white";
                    //word.setStyle(bgColor);
                    word.setFill(BLACK);
                    word.setText(getCoords(clippingPolygon));
                } else {
                    if (j != regionIndex - 1) {
                        //String bgColor = "-rtfx-background-color: white";
                        //word.setStyle(bgColor);
                        word.setFill(WHITE);
                        word.setText(getCoords(clippingPolygon));
                    } else {
                        //String bgColor = "-rtfx-background-color: yellow";
                        //word.setStyle(bgColor);
                        word.setFill(Color.YELLOW);
                        word.setText(getCoords(clippingPolygon));
                    }
                }
            }
        }
    }

    private void drawAllPolygons(int currentRegionIndex) {
        // draw the specific rectangle with yellow border
        // draw the remaining rectangles with blue border

        // if mouseDragged is false, then draw all rectangles
        // else draw only the yellow rectangle
        removeAllPolygons();
        for (int i = 0; i < currentClippingPolygons.size(); ++i) {
            Polygon aPolygon = currentClippingPolygons.get(i).clippingPolygon;
            if (currentRegionIndex == i + 1) {
                drawYellowPolygon(aPolygon);
            } else {
                drawPolygon(aPolygon, i);
            }
        }
    }

    private void drawAllPolygonsExceptCurrent(int currentRegionIndex) {
        // draw the specific rectangle with yellow border
        // draw the remaining rectangles with blue border

        // if mouseDragged is false, then draw all rectangles
        // else draw only the yellow rectangle
        removeAllPolygons();
        for (int i = 0; i < currentClippingPolygons.size(); ++i) {
            Polygon aPolygon = currentClippingPolygons.get(i).clippingPolygon;
            if (currentRegionIndex != i + 1) {
                drawPolygon(aPolygon, i);
                Rectangle bounds = ImageUtils.getBoundingRectangle(aPolygon);

            }
        }
    }

    private void setCurrentPolygon(Polygon polygon) {
        // removePolygon(currentPolygon);
        currentPolygon = polygon;
        // drawPolygon(polygon, LIGHT_YELLOW);
    }

    private void drawPolygon(Polygon polygon, int i) {
        drawPolygon(polygon, drawingColors[i % drawingColors.length]);
    }

    private void drawPolygon(Polygon polygon, Color color) {
        polygon.setFill(null); // transparent
        polygon.setStroke(color); // border
        polygon.setStrokeWidth(strokeWidth);
        polygon.getStrokeDashArray().add(10.0);
        // System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
        imagePane.getChildren().add(polygon);
    }

    private void drawYellowPolygon(Polygon polygon) {
        drawPolygon(polygon, LIGHT_YELLOW);
    }

    private void removeAllPolygons() {
        // remove all rectangles from the wipClippingRegions
        // remove the selection
        for (int i = 0; i < currentClippingPolygons.size(); ++i) {
            Polygon aPolygon = currentClippingPolygons.get(i).clippingPolygon;
            removePolygon(aPolygon);
        }
        removePolygon(currentPolygon);
    }

    private void removePolygon(Polygon aPolygon) {
        imagePane.getChildren().remove(aPolygon);
    }

    private Polygon updateClippingRegions(Polygon clippingPolygon, int assessmentRegionIndex) {

        if (clippingPolygon != null) {
            currentClippingPolygons.get(assessmentRegionIndex - 1).clippingPolygon = clippingPolygon;
        }
        return oldPolygon;
    }

    private Polygon convertPolygonToDisplayCoordinates(Polygon input) {
        ObservableList<Double> points = input.getPoints();
        double[] pointsArray = points.stream().mapToDouble(d -> (d.doubleValue() * widthConversion)).toArray();
        Polygon newPolygon = new Polygon(pointsArray);
        return newPolygon;
    }

    private ClippingPolygon convertClippingPolygonToDisplayCoordinates(ClippingPolygon input) {
        return new ClippingPolygon(input.id, input.key, convertPolygonToDisplayCoordinates(input.clippingPolygon));
    }

    private ArrayList<ClippingPolygon> convertClippingPolygonsToDisplayCoordinates(ArrayList<ClippingPolygon> input) {
        ArrayList<ClippingPolygon> output = new ArrayList<>();
        for (ClippingPolygon inputPolygon : input) {
            output.add(convertClippingPolygonToDisplayCoordinates(inputPolygon));
        }
        return output;
    }

    private ArrayList<ArrayList<ClippingPolygon>> convertToDisplayCoordinates(ArrayList<ArrayList<ClippingPolygon>> input) {
        ArrayList<ArrayList<ClippingPolygon>> output = new ArrayList<>();
        for (ArrayList<ClippingPolygon> clippingPolygons : input) {
            output.add(convertClippingPolygonsToDisplayCoordinates(clippingPolygons));
        }
        return output;
    }

    private Polygon convertPolygonToImageCoordinates(Polygon input) {
        ObservableList<Double> points = input.getPoints();
        double[] pointsArray = points.stream().mapToDouble(d -> (d.doubleValue() / widthConversion)).toArray();
        Polygon newPolygon = new Polygon(pointsArray);
        return newPolygon;
    }

    private ClippingPolygon convertClippingPolygonToImageCoordinates(ClippingPolygon input) {
        return new ClippingPolygon(input.id, input.key, convertPolygonToImageCoordinates(input.clippingPolygon));
    }

    private ArrayList<ClippingPolygon> convertClippingPolygonsToImageCoordinates(ArrayList<ClippingPolygon> input) {
        ArrayList<ClippingPolygon> output = new ArrayList<>();
        for (ClippingPolygon inputPolygon : input) {
            output.add(convertClippingPolygonToImageCoordinates(inputPolygon));
        }
        return output;
    }

    private ArrayList<ArrayList<ClippingPolygon>> convertToImageCoordinates(ArrayList<ArrayList<ClippingPolygon>> input) {
        ArrayList<ArrayList<ClippingPolygon>> output = new ArrayList<>();
        for (ArrayList<ClippingPolygon> inputPolygons : input) {
            output.add(convertClippingPolygonsToImageCoordinates(inputPolygons));
        }
        return output;
    }

    private void drawBackground(Polygon polygon) {
        polygon.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(1, Color.YELLOW)));
    }

    private int closeWindowEvent(Stage stage) {
        logger.trace("Entering closeWindowEvent()");
        System.out.println("Window close request ...");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.getButtonTypes().remove(ButtonType.OK);
        alert.getButtonTypes().remove(ButtonType.CANCEL);
        ButtonType exit = new ButtonType("Exit");
        alert.getButtonTypes().add(exit);
        ButtonType saveAndExit = new ButtonType("Save and Exit");
        alert.getButtonTypes().add(saveAndExit);
        alert.getButtonTypes().add(ButtonType.CANCEL);
        alert.setTitle("Quit application");
        alert.setContentText(String.format("You have not saved some changes. Are you sure you want to close the window ?"));
        alert.initOwner(stage.getOwner());
        Optional<ButtonType> res = alert.showAndWait();

        if (res.isPresent()) {
            if (res.get().equals(exit)) {
                return 1;
            }
            if (res.get().equals(saveAndExit)) {
                return 2;
            }
        }
        return -1; // cancel
    }

    private String echoStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Current Image Processor at index ").append(currentProcessorIndex).append(comma).append("@").append(String.format("%08X", currentImageProcessor.hashCode())).append(" = ").append(currentImageProcessor).append(newLine)
        .append("Current Clipping Polygon at index ").append(currentClippingPolygonIndex).append(comma).append("@").append(String.format("%08X", currentClippingPolygon.hashCode())).append(" = ").append(currentClippingPolygon).append(newLine)
        .append("Clipping Polygons ").append(comma).append("@").append(String.format("%08X", currentClippingPolygons.hashCode())).append(" are ");
        for (ClippingPolygon cp : currentClippingPolygons) {
            sb.append(cp.description());
            sb.append("  ");
        }
        return sb.toString();
    }

}

/** A pane with an image background */
class ImagePane extends Pane {

    BufferedImage bufferedImage;
    ImagePane(BufferedImage anImage) {

        this.bufferedImage = anImage;
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
        BackgroundImage backgroundimage = new BackgroundImage(image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.DEFAULT,
                BackgroundSize.DEFAULT);
        Background background = new Background(backgroundimage);
        setBackground(background);
    }
}

/** A pane with an image background */
class FileImagePane extends Pane {
    // size an image by placing it in a pane.
    FileImagePane(String imageLoc) {
        this(imageLoc, "-fx-background-size: cover; -fx-background-repeat: no-repeat;");
    }

    // size an image by placing it in a pane.
    FileImagePane(String imageLoc, String style) {
        this(new SimpleStringProperty(imageLoc), new SimpleStringProperty(style));
    }

    // size a replacable image in a pane and add a replaceable style.
    FileImagePane(StringProperty imageLocProperty, StringProperty styleProperty) {
        styleProperty().bind(
                new SimpleStringProperty("-fx-background-image: url(\"")
                        .concat(imageLocProperty)
                        .concat(new SimpleStringProperty("\");"))
                        .concat(styleProperty)
        );
    }


}