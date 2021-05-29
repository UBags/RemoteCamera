package com.costheta.tests;

import com.costheta.camera.remote.client.NetworkClientCamera;
import com.costheta.image.BasePaneConstants;
import com.costheta.kems.connectingrods.ConnectingRod_AshokLeyland;
import com.costheta.kems.connectingrods.LeftSideTextProcessor;
import com.costheta.kems.connectingrods.RightSideTextProcessor;
import com.costheta.machine.BaseImageProcessor;
import com.costheta.machine.InspectionPoint;
import com.costheta.machine.Part;
import com.costheta.machine.clip.ClippingRectangle;
import com.costheta.machine.clip.utils.ClippingUtils;
import com.costheta.machine.BasePopupWindow;
import com.costheta.machine.clip.ClipCoordinatesRectangleData;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class GetClipCoordinates1 extends BasePopupWindow implements BasePaneConstants {

//    private static final String FX_EXPERIENCE_LOGO_URL = "http://fxexperience.com/wp-content/uploads/2010/06/logo.png";
//    final ObjectProperty<Image> poster = new SimpleObjectProperty<Image>(new Image(FX_EXPERIENCE_LOGO_URL));

    private static final Logger logger = LogManager.getLogger(GetClipCoordinates1.class);
    private static final String comma = ",";

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\KEMS\\Pictures";
    private static String fileName = "6-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    private double screenScaleFactor = 1.8/3;

    private int state = 1;
    private int xStart = -1;
    private int yStart = -1;
    private int xEnd = -1;
    int yEnd = -1;
    private int mouseClickedState = 0;
    Rectangle selection = new Rectangle(xStart, yStart, xEnd - xStart, yEnd - yStart);
    boolean windowMinimised = false;
    double widthConversion;
    double heightConversion;

    private ClipCoordinatesRectangleData clipCoordinatesData;
    private String partName;
    private String inspectionPointName;
    private BufferedImage inputImage;

    ObservableList<BaseImageProcessor> processorList;
    ComboBox<BaseImageProcessor> processorComboBox;
    StringConverter<BaseImageProcessor> converter;
    ChangeListener<BaseImageProcessor> processorChangeListener;

    private ArrayList<BaseImageProcessor> processors;
    private BaseImageProcessor currentProcessor;
    private int currentProcessorIndex = 0;

    ObservableList<ClippingRectangle> assessmentRegionList;
    ComboBox<ClippingRectangle> assessmentRegionComboBox;
    ArrayList<ClippingRectangle> currentAssessmentRegions;
    StringConverter<ClippingRectangle> assessmentRegionConverter;
    ChangeListener<ClippingRectangle> assessmentRegionChangeListener;
    ClippingRectangle currentAssessmentRegion;
    int currentAssessmentIndex = 1;

    private boolean mouseDragged = false;

    private LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> clippingRegions;

    public GetClipCoordinates1(ClipCoordinatesRectangleData clipCoordinatesData) {
        this.clipCoordinatesData = clipCoordinatesData;
        this.partName = clipCoordinatesData.getPartName();
        this.inspectionPointName = clipCoordinatesData.getInspectionPointName();
        this.inputImage = clipCoordinatesData.getInputImage();
        this.processors = clipCoordinatesData.getProcessors();
        clippingRegions = ClippingUtils.getClipBoxesForInspectionPoint(clipCoordinatesData.getInspectionPoint());
    }

    public static void main(String[] args) {

        System.out.println("Testing ChamferAndNotch.processImage()");

        debugDirectory = debugDirectory + "/";
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {
        }

        Path filePath = Paths.get(inputDirectory + "/" + "clipping.properties");
        BufferedImage original = null;
        try {
            original = ImageIO.read(new File(inputDirectory + "/" + fileName));
        } catch (Exception e) {

        }

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
        LeftSideTextProcessor leftSideTextProcessor1 = new LeftSideTextProcessor("Text Inspection", leftSideStringPatterns1, 3);


        ArrayList<String> rightSideStringPatterns1 = new ArrayList<>();
        rightSideStringPatterns1.add("ADDDA");
        RightSideTextProcessor rightSideTextProcessor1 = new RightSideTextProcessor("Text Inspection", rightSideStringPatterns1, 4);

        // Add the inspection points to the part
        part1.addInspectionPoint(leftCameraInspectionPoint1);
        // Add the processors to the Inspection Points
        leftCameraInspectionPoint1.addImageProcessor(leftSideTextProcessor1);
        leftCameraInspectionPoint1.addImageProcessor(rightSideTextProcessor1);
        logger.trace("Created Part 1 and added inspection points and image processors");

//        ArrayList<String> pn = new ArrayList<String>();
//        pn.add("a");
//        pn.add("b");
//        pn.add("c");
        try {
            ClipCoordinatesRectangleData ccd = new ClipCoordinatesRectangleData(leftCameraInspectionPoint1);
            GetClipCoordinates1 gcc = new GetClipCoordinates1(ccd);
            gcc.inputImage = original;
            gcc.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Object processImage() {

        if ((partName == null) || ("".equals(partName))) {
            logger.debug("Unable to process as there is no Part Name");
            return Boolean.FALSE;
        }
        if ((inspectionPointName == null) || ("".equals(inspectionPointName))) {
            logger.debug("Unable to process as there is no Inspection Point Name");
            return Boolean.FALSE;
        }
        if ((processors == null) || (processors.size() == 0)) {
            logger.debug("Unable to process as there is no Processor Name");
            return Boolean.FALSE;
        }
        if (inputImage == null) {
            logger.debug("Unable to process as no input image is provided");
            return Boolean.FALSE;
        }

        logger.trace("Past the checks in processImage()");

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

        widthConversion = targetWidth * 1.0 / w;
        heightConversion = targetHeight * 1.0 / h;
        BufferedImage bufferedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(widthConversion, heightConversion);
        AffineTransformOp scaleOp =
                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        bufferedImage = scaleOp.filter(inputImage, bufferedImage);
        ImagePane imagePane = new ImagePane(bufferedImage);

        final int imageWidth = targetWidth;
        final int imageHeight = targetHeight;

        final int partPaneHeight = 40;
        final int topPaneHeight = 25;
        final int bottomPaneHeight = 60;
        final int spacerWidth = 5;
        final int topPaneInset = 2;
        final int bottomPaneInset = 2;
        final int saveButtonWidth = 80;
        final int strokeWidth = 3;
        final int partLabelWidth = 125;
        final int ipLabelWidth = 250;
        final int processorLabelWidth = 80;
        final int processorComboBoxWidth = 180;
        final int regionLabelWidth = 50;
        final int assessmentRegionComboBoxWidth = 30;
        final int labelHeight = 15;

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

        HBox partPane2 = new HBox();
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

        assessmentRegionConverter = new StringConverter<ClippingRectangle>() {
            @Override
            public String toString(ClippingRectangle assessmentRegion) {
                return String.valueOf(assessmentRegion.id);
            }

            @Override
            public ClippingRectangle fromString(String id) {
                return assessmentRegionList.stream()
                        .filter(item -> String.valueOf(item.id).equals(id))
                        .collect(Collectors.toList()).get(0);
            }
        };
        assessmentRegionChangeListener = new ChangeListener<ClippingRectangle>() {
            @Override
            public void changed(ObservableValue observable, ClippingRectangle oldValue, ClippingRectangle newValue) {
                currentAssessmentRegion = newValue;
                for (int i = 0; i < currentAssessmentRegions.size(); ++i) {
                    if (currentAssessmentRegion == currentAssessmentRegions.get(i)) {
                        currentAssessmentIndex = i + 1;
                        break;
                    }
                }
            }
        };
        converter = new StringConverter<BaseImageProcessor>() {
            @Override
            public String toString(BaseImageProcessor processor) {
                return processor.getName();
            }

            @Override
            public BaseImageProcessor fromString(String id) {
                return processorList.stream()
                        .filter(item -> item.getName().equals(id))
                        .collect(Collectors.toList()).get(0);
            }
        };
        processorChangeListener = new ChangeListener<BaseImageProcessor>() {
            @Override
            public void changed(ObservableValue observable, BaseImageProcessor oldValue, BaseImageProcessor newValue) {
                currentProcessor = newValue;
                currentAssessmentRegions = clipCoordinatesData.getClippingRectanglesInProcessor(currentProcessor);
                assessmentRegionList = FXCollections.observableArrayList(currentAssessmentRegions);
                assessmentRegionComboBox = new ComboBox(assessmentRegionList);
                assessmentRegionComboBox.setConverter(assessmentRegionConverter);
                assessmentRegionComboBox.getSelectionModel().select(0);
                currentAssessmentRegion = currentAssessmentRegions.get(0);
                assessmentRegionComboBox.setEditable(false);
                assessmentRegionComboBox.valueProperty().addListener(assessmentRegionChangeListener);
                assessmentRegionComboBox.setPrefWidth(assessmentRegionComboBoxWidth);
                for (int i = 0; i < processors.size(); ++i) {
                    if (currentProcessor == processors.get(i)) {
                        currentProcessorIndex = i;
                        break;
                    }
                }
            }
        };

        processorList = FXCollections.observableArrayList(processors);
        processorComboBox = new ComboBox(processorList);
        processorComboBox.setConverter(converter);
        processorComboBox.getSelectionModel().select(0);
        currentProcessor = processors.get(0);
        processorComboBox.setEditable(false);
        processorComboBox.valueProperty().addListener(processorChangeListener);
        processorComboBox.setPrefWidth(targetWidth - 5 * spacerWidth - processorLabelWidth - regionLabelWidth - assessmentRegionComboBoxWidth - 20);
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

        currentAssessmentRegions = clipCoordinatesData.getClippingRectanglesInProcessor(currentProcessor);
        assessmentRegionList = FXCollections.observableArrayList(currentAssessmentRegions);
        assessmentRegionComboBox = new ComboBox(assessmentRegionList);
        assessmentRegionComboBox.setConverter(assessmentRegionConverter);
        assessmentRegionComboBox.getSelectionModel().select(0);
        currentAssessmentRegion = currentAssessmentRegions.get(0);
        assessmentRegionComboBox.setEditable(false);
        assessmentRegionComboBox.valueProperty().addListener(assessmentRegionChangeListener);
        assessmentRegionComboBox.setPrefWidth(assessmentRegionComboBoxWidth);

        Region partPaneSpacer8 = new Region();
        partPaneSpacer8.setPrefWidth(spacerWidth);
        HBox.setHgrow(partPaneSpacer4, Priority.NEVER);
        HBox.setHgrow(processorLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer5, Priority.NEVER);
        HBox.setHgrow(processorComboBox, Priority.ALWAYS);
        HBox.setHgrow(partPaneSpacer6, Priority.NEVER);
        HBox.setHgrow(regionLabel, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer7, Priority.NEVER);
        HBox.setHgrow(assessmentRegionComboBox, Priority.NEVER);
        HBox.setHgrow(partPaneSpacer8, Priority.NEVER);

        partPane2.getChildren().add(partPaneSpacer4);
        partPane2.getChildren().add(processorLabel);
        partPane2.getChildren().add(partPaneSpacer5);
        partPane2.getChildren().add(processorComboBox);
        partPane2.getChildren().add(partPaneSpacer6);
        partPane2.getChildren().add(regionLabel);
        partPane2.getChildren().add(partPaneSpacer7);
        partPane2.getChildren().add(assessmentRegionComboBox);
        partPane2.getChildren().add(partPaneSpacer8);
        partPane2.setVisible(true);

        HBox topPaneMessage = new HBox();
        topPaneMessage.setPrefWidth(targetWidth);
        topPaneMessage.setPrefHeight(topPaneHeight + 2 * topPaneInset);
        topPaneMessage.setPadding(new Insets(topPaneInset));
        topPaneMessage.setAlignment(Pos.CENTER_LEFT);
        topPaneMessage.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        Label lbInfoLabel = new Label("Use the mouse to draw a rectangle on the clip region; then click the SAVE button");
        lbInfoLabel.setFont(TREBUCHET_BOLD_10);
        lbInfoLabel.setTextFill(BLUE);
        lbInfoLabel.setAlignment(Pos.CENTER);
        lbInfoLabel.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        lbInfoLabel.setPrefHeight(topPaneHeight - 7);
        lbInfoLabel.setPrefWidth(targetWidth - 2 * spacerWidth - 5);
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

        int bPaneHeight = Math.max(bottomPaneHeight + 2 * bottomPaneInset, processors.size() * labelHeight);
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
                logger.trace("Saving following coordinate " + selection);

            }
        });

        int noOfProcessors = processors.size();
        final Label[] clipCoordLabel = new Label[noOfProcessors];
        for (int i = 0; i < noOfProcessors; ++i) {
            clipCoordLabel[i] = new Label(processors.get(i) + " : ");
            clipCoordLabel[i].setFont(TREBUCHET_BOLD_10);
            clipCoordLabel[i].setTextFill(BLUE);
            clipCoordLabel[i].setAlignment(Pos.CENTER_LEFT);
            clipCoordLabel[i].setBackground(VERYLIGHTPINK_BACKGROUND);
            clipCoordLabel[i].setPrefHeight(bottomPaneHeight - 7);
            clipCoordLabel[i].setPrefWidth(targetWidth - 2 * spacerWidth - saveButtonWidth - 5);
            HBox.setHgrow(clipCoordLabel[i], Priority.ALWAYS);
        }
        Region spacer3 = new Region();
        spacer3.setPrefWidth(spacerWidth);
        Region spacer4 = new Region();
        spacer4.setPrefWidth(spacerWidth);

        HBox.setHgrow(spacer3, Priority.NEVER);
        HBox.setHgrow(saveButton, Priority.NEVER);
        HBox.setHgrow(spacer4, Priority.NEVER);

        VBox labels = new VBox();
        for (int i = 0; i < clipCoordLabel.length; ++i) {
            labels.getChildren().add(clipCoordLabel[i]);
        }
        bottomPane.getChildren().add(spacer3);
        bottomPane.getChildren().add(saveButton);
        bottomPane.getChildren().add(spacer4);
        bottomPane.getChildren().add(labels);
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

        imagePane.setOnMousePressed(new EventHandler() {
            @Override
            public void handle(Event event) {
                mouseDragged = false;
                imagePane.getChildren().remove(selection);
                // clipCoordLabel[processorIndex].setText("");
                if (mouseClickedState == 0) {
                    mouseClickedState = 1;
                    MouseEvent me = (MouseEvent) event;
                    xStart = (int) Math.max(Math.min(imageWidth - strokeWidth, me.getX()), strokeWidth);
                    yStart = (int) Math.max(Math.min(imageHeight - strokeWidth, me.getY()), strokeWidth);
                    selection.setX(xStart);
                    selection.setY(yStart);
                    selection.setWidth(0);
                    selection.setHeight(0);
                    // System.out.println("Started drawing rectangle from [" + xStart + "," + yStart + "]");
                    scene.setCursor(Cursor.CROSSHAIR);
                }
            }
        });

        imagePane.setOnMouseDragged(new EventHandler() {
            @Override
            public void handle(Event event) {
                mouseDragged = true;
                MouseEvent me = (MouseEvent) event;
                if (mouseClickedState == 1) {
                    imagePane.getChildren().remove(selection);
                    xEnd = (int) me.getX();
                    yEnd = (int) me.getY();
                    // System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
                    scene.setCursor(Cursor.CROSSHAIR);
                    if (xEnd >= xStart) {
                        xEnd = Math.min(imageWidth - strokeWidth, xEnd);
                        selection.setWidth(xEnd - xStart);
                    } else {
                        xEnd = Math.max(0,xEnd);
                        selection.setX(xEnd);
                        selection.setWidth(xStart - xEnd);
                    }
                    if (yEnd >= yStart) {
                        yEnd = Math.min(imageHeight - 3 * strokeWidth, yEnd);
                        selection.setHeight(yEnd - yStart);
                    } else {
                        yEnd = Math.max(yEnd, 0);
                        selection.setY(yEnd);
                        selection.setHeight(yStart - yEnd);
                    }
                    selection.setFill(null); // transparent
                    selection.setStroke(Color.YELLOW); // border
                    selection.setStrokeWidth(strokeWidth);
                    selection.getStrokeDashArray().add(10.0);
                    imagePane.getChildren().add(selection);
                    selection.relocate(Math.min(xStart, xEnd), Math.min(yStart,yEnd));
                    // drawBackground(rect);
                }
            }
        });

        imagePane.setOnMouseReleased(new EventHandler() {
            @Override
            public void handle(Event event) {
                MouseEvent me = (MouseEvent) event;
                if (mouseClickedState == 1) {
                    imagePane.getChildren().remove(selection);
                    mouseClickedState = 0;
                    xEnd = (int) me.getX();
                    yEnd = (int) me.getY();
                    if (xEnd >= xStart) {
                        xEnd = Math.min(imageWidth - strokeWidth, xEnd);
                        selection.setWidth(xEnd - xStart);
                    } else {
                        xEnd = Math.max(0,xEnd);
                        selection.setX(xEnd);
                        selection.setWidth(xStart - xEnd);
                    }
                    if (yEnd >= yStart) {
                        yEnd = Math.min(imageHeight - 3 * strokeWidth, yEnd);
                        selection.setHeight(yEnd - yStart);
                    } else {
                        yEnd = Math.max(yEnd, 0);
                        selection.setY(yEnd);
                        selection.setHeight(yStart - yEnd);
                    }
                    selection.setFill(null); // transparent
                    selection.setStroke(Color.YELLOW); // border
                    selection.setStrokeWidth(strokeWidth);
                    selection.getStrokeDashArray().add(10.0);
                    System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
                    scene.setCursor(Cursor.HAND);
                    imagePane.getChildren().add(selection);
                    selection.relocate(Math.min(xStart, xEnd), Math.min(yStart, yEnd));
                    int actualX = (int) (selection.getX() / widthConversion);
                    int actualY = (int) (selection.getY() / heightConversion);
                    int actualWidth =  (int) (selection.getWidth() / widthConversion);
                    int actualHeight =  (int) (selection.getHeight() / heightConversion);
                    if (mouseDragged) {
                        String coords = new StringBuilder(processors.get(currentProcessorIndex).getName()).append(": (startX :").append(actualX)
                                .append(", startY :").append(actualY).append(", width :").append(actualWidth)
                                .append(", height :").append(actualHeight).append(")").toString();
                        logger.info("The final coords are " + coords);
                        clipCoordLabel[currentProcessorIndex].setText(coords);
                    }
                    mouseDragged = true;
                }
            }
        });

        Stage thisStage = new Stage();
        thisStage.setTitle("Configuration");
        thisStage.setScene(scene);
        thisStage.setResizable(false);
        thisStage.show();
        return Boolean.TRUE;

    }


//    @Override public void start(Stage primaryStage) {
//
//        System.out.println("Running ChamferAndNotch");
//        primaryStage.setTitle("Training");
//
//        double screenScaleFactor = 2.0/3;
//        try {
//            Files.createDirectories(Paths.get(debugDirectory));
//        } catch (Exception e) {
//        }
//        debugDirectory = debugDirectory + "/";
//
//        // System.out.println("Reached here - 1");
//        BufferedImage original = null;
//        try {
//            original = ImageIO.read(new File(inputDirectory + "/" + fileName));
//        } catch (Exception e) {
//
//        }
//
//        Toolkit defaultToolKit = Toolkit.getDefaultToolkit();
//        Dimension screenSize = defaultToolKit.getScreenSize();
//
//        int screenWidth = screenSize.width;
//        int screenHeight = screenSize.height;
//        double screenRatio = screenHeight * 1.0 / screenWidth;
//
//        int w = original.getWidth();
//        int h = original.getHeight();
//        double currentRatio = h * 1.0 / w;
//
//        boolean fixHeightFirst = false;
//        if (screenRatio < currentRatio) {
//            fixHeightFirst = true;
//        }
//
//        int targetWidth = 0;
//        int targetHeight = 0;
//
//        if (fixHeightFirst) {
//            targetHeight = (int) (screenHeight * screenScaleFactor);
//            targetWidth = (int) (targetHeight / currentRatio);
//        } else {
//            targetWidth = (int) (screenWidth * screenScaleFactor);
//            targetHeight = (int) (targetWidth * currentRatio);
//        }
//
//        widthConversion = targetWidth * 1.0 / w;
//        heightConversion = targetHeight * 1.0 / h;
//        BufferedImage bufferedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
//        AffineTransform at = new AffineTransform();
//        at.scale(widthConversion, heightConversion);
//        AffineTransformOp scaleOp =
//                new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
//        bufferedImage = scaleOp.filter(original, bufferedImage);
//        ImagePane imagePane = new ImagePane(bufferedImage);
//
//        final int imageWidth = targetWidth;
//        final int imageHeight = targetHeight;
//
//        final int topPaneHeight = 40;
//        final int bottomPaneHeight = 40;
//        final int spacerWidth = 15;
//        final int topPaneInset = 2;
//        final int bottomPaneInset = 2;
//        final int saveButtonWidth = 200;
//        final int strokeWidth = 3;
//
//        HBox topPane = new HBox();
//        topPane.setPrefWidth(targetWidth);
//        topPane.setPrefHeight(topPaneHeight + 2 * topPaneInset);
//        topPane.setPadding(new Insets(topPaneInset));
//        topPane.setAlignment(Pos.CENTER_LEFT);
//        topPane.setBackground(LIGHTPINK_BACKGROUND);
//        Label lbInfoLabel = new Label("Use the mouse to draw a rectangle on the clip region; then click the SAVE button");
//        lbInfoLabel.setFont(TREBUCHET_BOLD_12);
//        lbInfoLabel.setTextFill(BLUE);
//        lbInfoLabel.setAlignment(Pos.CENTER);
//        lbInfoLabel.setBackground(LIGHTPINK_BACKGROUND);
//        lbInfoLabel.setPrefHeight(topPaneHeight - 7);
//        lbInfoLabel.setPrefWidth(targetWidth - 2 * spacerWidth - 5);
//        Region spacer1 = new Region();
//        spacer1.setPrefWidth(spacerWidth);
//        Region spacer2 = new Region();
//        spacer2.setPrefWidth(spacerWidth);
//        HBox.setHgrow(spacer1, Priority.NEVER);
//        HBox.setHgrow(lbInfoLabel, Priority.NEVER);
//        HBox.setHgrow(spacer2, Priority.ALWAYS);
//        topPane.getChildren().add(spacer1);
//        topPane.getChildren().add(lbInfoLabel);
//        topPane.getChildren().add(spacer2);
//        topPane.setVisible(true);
//
//        HBox bottomPane = new HBox();
//        bottomPane.setPrefWidth(targetWidth);
//        bottomPane.setPrefHeight(bottomPaneHeight + 2 * bottomPaneInset);
//        bottomPane.setPadding(new Insets(bottomPaneInset));
//        bottomPane.setAlignment(Pos.CENTER_LEFT);
//        bottomPane.setBackground(VERYLIGHTPINK_BACKGROUND);
//        Button saveButton = new Button("Save the Clip Coordinates");
//        saveButton.setPrefWidth(saveButtonWidth);
//        saveButton.setPrefHeight(bottomPaneHeight - 7);
//        saveButton.setFont(TREBUCHET_BOLD_12);
//        saveButton.setTextFill(BLACK);
//        saveButton.setDisable(false);
//        saveButton.setOnAction(new EventHandler<ActionEvent>() {
//            @Override
//            public void handle(ActionEvent arg0) {
//                // don't take any action if a false event is fired when the window is in
//                // minimised state. (Sigh ! That happens !!)
//                if (windowMinimised) {
//                    return;
//                }
//                System.out.println("Saving following coordinate " + selection);
//            }
//        });
//        final Label clipCoordLabel = new Label("");
//        clipCoordLabel.setFont(TREBUCHET_BOLD_12);
//        clipCoordLabel.setTextFill(BLACK);
//        clipCoordLabel.setAlignment(Pos.CENTER_LEFT);
//        clipCoordLabel.setBackground(VERYLIGHTPINK_BACKGROUND);
//        clipCoordLabel.setPrefHeight(bottomPaneHeight - 7);
//        clipCoordLabel.setPrefWidth(targetWidth - 2 * spacerWidth - saveButtonWidth - 5);
//        Region spacer3 = new Region();
//        spacer3.setPrefWidth(spacerWidth);
//        Region spacer4 = new Region();
//        spacer4.setPrefWidth(spacerWidth);
//
//        HBox.setHgrow(spacer3, Priority.NEVER);
//        HBox.setHgrow(saveButton, Priority.NEVER);
//        HBox.setHgrow(spacer4, Priority.NEVER);
//        HBox.setHgrow(clipCoordLabel, Priority.ALWAYS);
//        bottomPane.getChildren().add(spacer3);
//        bottomPane.getChildren().add(saveButton);
//        bottomPane.getChildren().add(spacer4);
//        bottomPane.getChildren().add(clipCoordLabel);
//        bottomPane.setVisible(true);
//
//        BorderPane constrainingPane = new BorderPane();
//
//        // just placing the image in the constraining pane will make the constrainingpane go above it's max size.
//        // constrainingPane.getChildren().add(new ImageView(poster.get()));
//
//        // to get around this, you could embed the image in a scrollpane.
//        // constrainingPane.getChildren().add(getScrollPane(poster));
//
//        // or perhaps preferably, you could use a slightly customized standard pane
//        // and style it's background to display the image.
//
//        // FileImagePane imagePane = new FileImagePane(FX_EXPERIENCE_LOGO_URL, "-fx-background-size: contain; -fx-background-repeat: no-repeat;");
//
//        constrainingPane.setTop(topPane);
//        constrainingPane.setCenter(imagePane);
//        constrainingPane.setBottom(bottomPane);
//        constrainingPane.setStyle("-fx-border-color: red; -fx-border-width: 1; -fx-border-insets: -2;");
//
//        // layout the scene.
//        StackPane layout = new StackPane();
//        layout.getChildren().add(constrainingPane);
//        layout.setStyle("-fx-background-color: whitesmoke;");
//        Scene scene = new Scene(layout, targetWidth, targetHeight + topPaneHeight + bottomPaneHeight);
//
//        // clamp the pane to the scene size.
//        constrainingPane.maxWidthProperty().bind(scene.widthProperty().divide(1));
//        constrainingPane.minWidthProperty().bind(scene.widthProperty().divide(1));
//        constrainingPane.maxHeightProperty().bind(scene.heightProperty().divide(1));
//        constrainingPane.minHeightProperty().bind(scene.heightProperty().divide(1));
//
//        imagePane.setOnMouseEntered(new EventHandler() {
//            @Override
//            public void handle(Event event) {
//                scene.setCursor(Cursor.HAND); //Change cursor to hand
//            }
//        });
//
//        imagePane.setOnMouseExited(new EventHandler() {
//            public void handle(Event me) {
//                scene.setCursor(Cursor.DEFAULT);
//            }
//        });
//
//        imagePane.setOnMousePressed(new EventHandler() {
//            @Override
//            public void handle(Event event) {
//                constrainingPane.getChildren().remove(selection);
//                clipCoordLabel.setText("");
//                if (mouseClickedState == 0) {
//                    mouseClickedState = 1;
//                    MouseEvent me = (MouseEvent) event;
//                    xStart = (int) me.getX();
//                    yStart = (int) me.getY();
//                    selection.setX(xStart);
//                    selection.setY(yStart);
//                    selection.setWidth(0);
//                    selection.setHeight(0);
//                    // System.out.println("Started drawing rectangle from [" + xStart + "," + yStart + "]");
//                    scene.setCursor(Cursor.CROSSHAIR);
//                }
//            }
//        });
//
//        imagePane.setOnMouseDragged(new EventHandler() {
//            @Override
//            public void handle(Event event) {
//                MouseEvent me = (MouseEvent) event;
//                if (mouseClickedState == 1) {
//                    imagePane.getChildren().remove(selection);
//                    xEnd = (int) me.getX();
//                    yEnd = (int) me.getY();
//                    // System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
//                    scene.setCursor(Cursor.CROSSHAIR);
//                    if (xEnd >= xStart) {
//                        xEnd = Math.min(imageWidth - strokeWidth, xEnd);
//                        selection.setWidth(xEnd - xStart);
//                    } else {
//                        xEnd = Math.max(0,xEnd);
//                        selection.setX(xEnd);
//                        selection.setWidth(xStart - xEnd);
//                    }
//                    if (yEnd >= yStart) {
//                        yEnd = Math.min(imageHeight - 3 * strokeWidth, yEnd);
//                        selection.setHeight(yEnd - yStart);
//                    } else {
//                        yEnd = Math.max(yEnd, 0);
//                        selection.setY(yEnd);
//                        selection.setHeight(yStart - yEnd);
//                    }
//                    selection.setFill(null); // transparent
//                    selection.setStroke(Color.YELLOW); // border
//                    selection.setStrokeWidth(strokeWidth);
//                    selection.getStrokeDashArray().add(10.0);
//                    imagePane.getChildren().add(selection);
//                    selection.relocate(Math.min(xStart, xEnd), Math.min(yStart,yEnd));
//                    // drawBackground(rect);
//                }
//            }
//        });
//
////        imagePane.setOnMouseDragReleased(new EventHandler() {
////            @Override
////            public void handle(Event event) {
////                System.out.println("Entered setOnMouseDragReleased()");
////                MouseEvent me = (MouseEvent) event;
////                if (mouseClickedState == 1) {
////                    imagePane.getChildren().remove(selection);
////                    mouseClickedState = 0;
////                    xEnd = (int) me.getX();
////                    yEnd = (int) me.getY();
////                    if (xEnd >= xStart) {
////                        selection.setWidth(xEnd - xStart);
////                    } else {
////                        selection.setX(xEnd);
////                        selection.setWidth(xStart - xEnd);
////                    }
////                    if (yEnd >= yStart) {
////                        selection.setHeight(yEnd - yStart);
////                    } else {
////                        selection.setY(yEnd);
////                        selection.setHeight(yStart - yEnd);
////                    }
////                    selection.setFill(null); // transparent
////                    selection.setStroke(Color.YELLOW); // border
////                    selection.setStrokeWidth(3.0);
////                    selection.getStrokeDashArray().add(10.0);
////                    System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
////                    scene.setCursor(Cursor.HAND);
////                    imagePane.getChildren().add(selection);
////                    selection.relocate(Math.min(xStart, xEnd), Math.min(yStart,yEnd));
////                    String coords = new StringBuilder("(").append(selection.getX())
////                            .append(", ").append(selection.getY()).append(", ").append(selection.getWidth())
////                            .append(", ").append(selection.getHeight()).append(")").toString();
////                    System.out.println("The final coords are " + coords);
////                    clipCoordLabel.setText(coords);
////                }
////            }
////        });
//
//        imagePane.setOnMouseReleased(new EventHandler() {
//            @Override
//            public void handle(Event event) {
//                MouseEvent me = (MouseEvent) event;
//                if (mouseClickedState == 1) {
//                    imagePane.getChildren().remove(selection);
//                    mouseClickedState = 0;
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
//                        yEnd = Math.min(imageHeight - 3 * strokeWidth, yEnd);
//                        selection.setHeight(yEnd - yStart);
//                    } else {
//                        yEnd = Math.max(yEnd, 0);
//                        selection.setY(yEnd);
//                        selection.setHeight(yStart - yEnd);
//                    }
//                    selection.setFill(null); // transparent
//                    selection.setStroke(Color.YELLOW); // border
//                    selection.setStrokeWidth(strokeWidth);
//                    selection.getStrokeDashArray().add(10.0);
//                    System.out.println("Drawing rectangle till [" + xEnd + "," + yEnd + "]");
//                    scene.setCursor(Cursor.HAND);
//                    imagePane.getChildren().add(selection);
//                    selection.relocate(Math.min(xStart, xEnd), Math.min(yStart, yEnd));
//                    int actualX = (int) (selection.getX() / widthConversion);
//                    int actualY = (int) (selection.getY() / heightConversion);
//                    int actualWidth =  (int) (selection.getWidth() / widthConversion);
//                    int actualHeight =  (int) (selection.getHeight() / heightConversion);
//                    String coords = new StringBuilder("(startX :").append(actualX)
//                            .append(", startY :").append(actualY).append(", width :").append(actualWidth)
//                            .append(", height :").append(actualHeight).append(")").toString();
//                    System.out.println("The final coords are " + coords);
//                    clipCoordLabel.setText(coords);
//                }
//            }
//        });
//
////        constrainingPane.setOnMouseClicked(new EventHandler() {
////            @Override
////            public void handle(Event event) {
////                Task task = new Task() {
////                    @Override
////                    protected Integer call() throws Exception {
////                        state = 2;
////                        int iterations;
////                        scene.setCursor(Cursor.WAIT); //Change cursor to wait style
////                        for (iterations = 0; iterations < 500000; iterations++) {
////                            System.out.println("Iteration " + iterations);
////                        }
////                        state = 1;
////                        scene.setCursor(Cursor.HAND); //Change cursor to default style
////                        return iterations;
////                    }
////                };
////                Thread th = new Thread(task);
////                th.setDaemon(true);
////                th.start();
////            }
////        });
//
//        // show the scene.
//        primaryStage.setScene(scene);
//        primaryStage.setResizable(false);
//        primaryStage.show();
//    }

    // size a replaceable image by placing it in a scrollpane.
//    private Node getScrollPane(final ObjectProperty<Image> poster) {
//        return new ScrollPane() {{
//            final ReadOnlyDoubleProperty widthProperty = widthProperty();
//            final ReadOnlyDoubleProperty heightProperty = heightProperty();
//
//            setHbarPolicy(ScrollBarPolicy.NEVER);
//            setVbarPolicy(ScrollBarPolicy.NEVER);
//
//            setContent(new ImageView() {{
//                imageProperty().bind(poster);
//                setPreserveRatio(true);
//                setSmooth(true);
//
//                fitWidthProperty().bind(widthProperty);
//                fitHeightProperty().bind(heightProperty);
//            }});
//        }};
//    }

    private void drawBackground(Rectangle rect) {
        rect.setFill(new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.REFLECT,
                new Stop(0, Color.RED),
                new Stop(1, Color.YELLOW)));
    }

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
}
