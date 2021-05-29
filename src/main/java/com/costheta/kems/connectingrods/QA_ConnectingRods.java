package com.costheta.kems.connectingrods;

import com.costheta.camera.processor.SingleWordTextProcessor_LowLight;
import com.costheta.camera.remote.client.NetworkClientCamera;
import com.costheta.image.BasePaneConstants;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.*;
import com.costheta.machine.clip.ClipCoordinatesRectangleData;
import com.costheta.machine.clip.GetClipCoordinates;
import com.costheta.utils.GeneralUtils;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.tools.Helper;
import eu.hansolo.tilesfx.tools.MatrixIcon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

public class QA_ConnectingRods extends Application implements MachiningNode, BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(QA_ConnectingRods.class);
    private static String name = "ConnectingRods";
    private static Font boldFont = VERDANA_BOLD_15;

    private BorderPane root;
    private StackPane displayPane;
    private Button inspectButton;
    private ComboBox<String> operationMode;

    private ArrayList<Part> partsToBeInspected = new ArrayList<>();
    private MachiningNode parent;

    private static boolean windowMinimised = false;

    protected final int topPaneHeight = 40;

    private TabPane overallTabPane;
    private Tab[] partTabs;
    private ArrayList<Part> parts;

    private int TILE_WIDTH = 150;

    private Tab currentTabSelected;
    private Part currentPart;
    private Scene scene;

    private Tile percentageTile;
    private Tile numberTile;
    private Tile imageTile;
    private Tile rollingResultsTile;
    private GridPane rollingResultsPane;

    private int rollingResultsToBeDisplayed = 60;
    private HBox[] gridBoxes = new HBox[rollingResultsToBeDisplayed];

    private Tile lineChartTile;
    private MatrixIcon matrixIcon = new MatrixIcon();
    private XYChart.Series<String, Number> series2 = new XYChart.Series();
    private static ArrayList<Boolean> resultsHistory = new ArrayList<>();
    public static Image greenTick;
    public static Image redCross;
    public static Image questionMark;
    private VBox tileBox;
    private ImageView resultImageView = new ImageView();

    private static final String CONFIGURE = "Configure";
    private static final String TRAINING = "Training";
    private static final String PRODUCTION = "Production";
    private String currentMode = PRODUCTION;

    public static void main(String[] args) {
        System.out.println("Entered Main");
        // GeneralUtils.rerouteLog(new String[]{"client"});
        GeneralUtils.updateLogger("RemoteCameraLog", "logs/client.log", "com.costheta");
        logger.trace("Entering main()");
        BaseApplication.populateInitialisationArgs(args);
        System.out.println("Populated args");
        try {
            BufferedImage greenTick = ImageIO.read(new File("Green Tick.jpg"));
            // System.out.println(gT);
            QA_ConnectingRods.greenTick = SwingFXUtils.toFXImage(greenTick,null);
            BufferedImage redCross = ImageIO.read(new File("Red Cross.jpg"));
            // System.out.println(rC);
            QA_ConnectingRods.redCross = SwingFXUtils.toFXImage(redCross,null);
            BufferedImage questionMark = ImageIO.read(new File("Question Mark.jpg"));
            // System.out.println(rC);
            QA_ConnectingRods.questionMark = SwingFXUtils.toFXImage(questionMark,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        populateResultsHistory();
        launch(args);
    }

    public void process() {
        logger.trace("Entering process() in " + this);

        NetworkClientCamera leftCamera = new NetworkClientCamera("Left Side Camera", 1);
        NetworkClientCamera rightCamera = new NetworkClientCamera("Right Side Camera", 2);

        //--------------------
        Part part1 = new ConnectingRod_AshokLeyland("AL1", "100001", 2, "Ashok Leyland Connecting Rod - Single Notch");

        InspectionPoint leftCameraInspectionPoint1 = new InspectionPoint("Left IP", leftCamera);
        ArrayList<String> leftSideStringPatterns1 = new ArrayList<>();
        leftSideStringPatterns1.add("DDA");
        leftSideStringPatterns1.add("AADD");
        leftSideStringPatterns1.add("DDDDD");
        leftSideStringPatterns1.add("DDDDD");
        // LeftSideTextProcessor leftSideTextProcessor1 = new LeftSideTextProcessor("Text Inspection", leftSideStringPatterns1);
        SingleWordTextProcessor_LowLight leftSideTextProcessor1 = new SingleWordTextProcessor_LowLight("Text Inspection", leftSideStringPatterns1);
        // SingleWordTextProcessor_OverExposure leftSideTextProcessor1 = new SingleWordTextProcessor_OverExposure("Text Inspection", leftSideStringPatterns1);

        InspectionPoint rightCameraInspectionPoint1 = new InspectionPoint("Right IP", rightCamera);
        ArrayList<String> rightSideStringPatterns1 = new ArrayList<>();
        rightSideStringPatterns1.add("ADDDA");
        rightSideStringPatterns1.add("AADDD");
        RightSideTextProcessor rightSideTextProcessor1 = new RightSideTextProcessor("Text Inspection", rightSideStringPatterns1);

        // Add the inspection points to the part
        part1.addInspectionPoint(leftCameraInspectionPoint1);
        part1.addInspectionPoint(rightCameraInspectionPoint1);
        // Add the processors to the Inspection Points
        leftCameraInspectionPoint1.addImageProcessor(leftSideTextProcessor1);
        rightCameraInspectionPoint1.addImageProcessor(rightSideTextProcessor1);
        logger.trace("Created Part 1 and added inspection points and image processors");

        //-----------------

        Part part2 = new ConnectingRod_AshokLeyland("AL2", "100002", 2, "Ashok Leyland Connecting Rod - Double Notch");

        InspectionPoint leftCameraInspectionPoint2 = new InspectionPoint("Left IP", leftCamera);
        ArrayList<String> leftSideStringPatterns2 = new ArrayList<>();
        leftSideStringPatterns2.add("DDA");
        leftSideStringPatterns2.add("AADD");
        leftSideStringPatterns2.add("DDDDD");
        leftSideStringPatterns2.add("DDDDD");
        // LeftSideTextProcessor leftSideTextProcessor2 = new LeftSideTextProcessor("Text Inspection", leftSideStringPatterns2);
        SingleWordTextProcessor_LowLight leftSideTextProcessor2 = new SingleWordTextProcessor_LowLight("Text Inspection", leftSideStringPatterns2);

        InspectionPoint rightCameraInspectionPoint2 = new InspectionPoint("Right IP", rightCamera);
        ArrayList<String> rightSideStringPatterns2 = new ArrayList<>();
        rightSideStringPatterns2.add("ADDDA");
        rightSideStringPatterns2.add("AADDD");
        RightSideTextProcessor rightSideTextProcessor2 = new RightSideTextProcessor("Text Inspection", rightSideStringPatterns2);

        // Add the inspection points to the part
        part2.addInspectionPoint(leftCameraInspectionPoint2);
        part2.addInspectionPoint(rightCameraInspectionPoint2);
        // Add the processors to the Inspection Points
        leftCameraInspectionPoint2.addImageProcessor(leftSideTextProcessor2);
        rightCameraInspectionPoint2.addImageProcessor(rightSideTextProcessor2);
        logger.trace("Created Part 2 and added inspection points and image processors");

        //-----------------

        add(part1);
        add(part2);
        // System.out.println("Added parts to ArrayList");
        // System.out.println("Size of partsToBeInspected is " + partsToBeInspected.size());
    }

    protected void add(Part aPart) {
        partsToBeInspected.add(aPart);
        aPart.setParent(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.trace("Entering start() in " + this);

        process();
        root = new BorderPane();

        ObservableList<Part> options = FXCollections.observableArrayList();
        logger.trace("Inside start(Stage) - Size of partsToBeInspected is " + partsToBeInspected.size());
        for (int i = 0; i < partsToBeInspected.size(); ++i) {
            options.add(partsToBeInspected.get(i));
            logger.info("Added Part " + partsToBeInspected.get(i) + " to options");
        }
        overallTabPane = new TabPane();
        logger.trace("Created TabPane in " + this);
        partTabs = new Tab[options.size()];
        int prefHeight = Integer.MIN_VALUE;
        int prefWidth = Integer.MIN_VALUE;
        logger.trace("Size of options ObservableList is " + options.size());
        for (int i = 0; i < options.size(); ++i) {
            VBox displayPane =  options.get(i).getDisplayPane();
            partTabs[i] = new Tab(options.get(i).toString(), displayPane);
            partTabs[i].setId(options.get(i).getPartId());
            overallTabPane.getTabs().add(partTabs[i]);
            logger.trace("Added displayPane() in " + this);
            prefHeight = Math.max(prefHeight, options.get(i).getDisplayHeight());
            prefWidth = Math.max(prefWidth, options.get(i).getDisplayWidth());
        }
        currentTabSelected = partTabs[0];
        currentPart = partsToBeInspected.get(0);
        overallTabPane.getSelectionModel().selectedItemProperty().addListener((obs,ov,nv)->{
            currentTabSelected = nv;
            String selectedText = currentTabSelected.getText();
            logger.trace("Tab text selected is " + selectedText);
            for (int i = 0; i < options.size(); ++i) {
                if (options.get(i).toString().equals(selectedText)) {
                    currentPart = partsToBeInspected.get(i);
                    break;
                }
            }
            logger.debug("Current part selected is " + currentPart);
            VBox dPane = (VBox) currentTabSelected.getContent();
            dPane.requestLayout();
            root.requestLayout();
            root.getParent().requestLayout();
            currentPart.showImages();
            logger.trace("Current Tab id is : " + currentTabSelected.getId());
            logger.trace("Current Tab id is : " + currentTabSelected.getText());
        });
        prefHeight += 1;
        prefWidth += 1;
        overallTabPane.setPrefHeight(prefHeight);
        overallTabPane.setPrefWidth(prefWidth);

        int topPaneInset = 2;
        HBox topPane = new HBox();
        topPane.setPrefWidth(prefWidth);
        topPane.setPrefHeight(topPaneHeight + 2 * topPaneInset);
        topPane.setPadding(new Insets(topPaneInset));
        topPane.setAlignment(Pos.CENTER_LEFT);
        topPane.setBackground(GRAYISH_BACKGROUND);
        // topPane.setStyle("-fx-background-color: #c8fdd8");

        //        VBox topSubPane = new VBox();
        //        topSubPane.setPrefWidth(prefWidth);
        //        topSubPane.setPrefHeight(2 * (topPaneHeight - 2));
        //        topSubPane.setPadding(new Insets(1));
        //        topSubPane.setAlignment(Pos.CENTER_LEFT);
        //        topSubPane.setStyle("-fx-background-color: #c8fdd8");

        //        HBox labelHolder = new HBox();
        //        labelHolder.setAlignment(Pos.CENTER);
        // String labelHolderBackgroundColor = "-fx-background-color: #c8fdd8";
        // String labelFontString = "-fx-font: 18px Tahoma; -fx-font-weight: bold;" +
                // "-fx-stroke: black;" +
        //        "-fx-stroke-width: 3";
        //        labelHolder.setStyle(labelHolderBackgroundColor);
        Label lbInfoLabel = new Label("Connecting Rods Inspection");
        lbInfoLabel.setFont(TREBUCHET_BOLD_20);
        lbInfoLabel.setTextFill(Color.web("0x0060bf",1.0));
        lbInfoLabel.setAlignment(Pos.CENTER);
        lbInfoLabel.setBackground(GRAYISH_BACKGROUND);
        lbInfoLabel.setPrefHeight(topPaneHeight - 7);
        // lbInfoLabel.setStyle(labelHolderBackgroundColor + "; -fx-text-fill: #0060bf; " + labelFontString);
//        labelHolder.getChildren().add(lbInfoLabel);
//        topSubPane.getChildren().add(labelHolder);

        int inspectButtonWidth = 150;
        inspectButton = new Button("Inspect Part");
        inspectButton.setPrefWidth(inspectButtonWidth);
        inspectButton.setPrefHeight(topPaneHeight - 7);
        // inspectButton.setFont(Font.font("Courier", FontWeight.BOLD, 14));
        inspectButton.setFont(TREBUCHET_BOLD_15);
        inspectButton.setTextFill(BLACK);
        // String inspectButtonFontString = "-fx-foreground-color: #444444; -fx-font: 16px Courier; -fx-font-weight: bold";
        // inspectButton.setStyle(inspectButtonFontString);
        inspectButton.setDisable(false);
        inspectButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent arg0) {
                // don't take any action if a false event is fired when the window is in
                // minimised state. (Sigh ! That happens !!)
                if (windowMinimised) {
                    return;
                }
                inspectButton.setDisable(true);
                // showQuestionMark();
                // Tab selectedTab = overallTabPane.getSelectionModel().getSelectedItem();
                // String selectedId = selectedTab.getId();
                String selectedText = currentTabSelected.getText();
                // System.out.println("Tab id selected is " + selectedId);
                logger.trace("Tab text selected is " + selectedText);
                for (int i = 0; i < options.size(); ++i) {
                    if (options.get(i).toString().equals(selectedText)) {
                        currentPart = partsToBeInspected.get(i);
                        break;
                    }
                }
                System.out.println("Current part selected is " + currentPart);
                VBox dPane = (VBox) currentTabSelected.getContent();
                FinalResult finalResult = currentPart.inspect();
                updateTiles(false);
                // sends an ImageRequest to the camera and when ImageResponse comes back,
                // it is automatically processed because a listener has been attached to the
                // processor
                inspectButton.setDisable(false);
                dPane.requestLayout();
                root.requestLayout();
                root.getParent().requestLayout();
            }
        });

        ObservableList<String> operationModes =
                FXCollections.observableArrayList(
                        PRODUCTION,
                        CONFIGURE,
                        TRAINING
                );
        operationMode = new ComboBox(operationModes);
        operationMode.getSelectionModel().select(0);
        operationMode.setEditable(false);
        operationMode.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue observable, String oldValue, String newValue) {
                if (CONFIGURE.equals(newValue)) {
                    for (Part aPart: partsToBeInspected) {
                        ArrayList<InspectionPoint> iPoints = aPart.getInspectionPoints();
                        for (final InspectionPoint ip : iPoints) {
                            ImageView imView = ip.getImageView().getImageView();
                            imView.setOnMouseEntered(new HandCursorEventHandler());
                            imView.setOnMouseExited(new DefaultCursorEventHandler());
                            imView.setOnMouseClicked(new EventHandler() {

                                @Override
                                public void handle(Event event) {
                                    // System.out.println("Entered MouseClicked for ImageView in " + ip.getName());
                                    ClipCoordinatesRectangleData ccd = new ClipCoordinatesRectangleData(ip);
                                    GetClipCoordinates gcc = new GetClipCoordinates(ccd, primaryStage);
                                    gcc.process();
                                }
                            });
                        }
                    }
                    inspectButton.setDisable(true);
                } else {
                    for (Part aPart: partsToBeInspected) {
                        ArrayList<InspectionPoint> iPoints = aPart.getInspectionPoints();
                        for (InspectionPoint ip : iPoints) {
                            ImageView imView = ip.getImageView().getImageView();
                            imView.setOnMouseEntered(null);
                            imView.setOnMouseExited(null);
                        }
                    }
                    inspectButton.setDisable(false);
                }
                currentMode = newValue;
            }
        });
//        operationMode.setCellFactory(
//            new Callback<ListView<String>, ListCell<String>>() {
//                @Override public ListCell<String> call(ListView<String> param) {
//                    final ListCell<String> cell = new ListCell<String>() {
//                        {
//                            super.setPrefWidth(100);
//                        }
//                        @Override public void updateItem(String item,
//                                                         boolean empty) {
//                            super.updateItem(item, empty);
//                            if (item != null) {
//                                setText(item);
//                                if (item.contains(TRAINING)) {
//                                    setTextFill(Color.RED);
//                                } else {
//                                    if (item.contains(PRODUCTION)) {
//                                        setTextFill(Color.GREEN);
//                                    } else {
//                                        setTextFill(Color.BLACK);
//                                    }
//                                }
//                            } else {
//                                setText(null);
//                            }
//                            setFont(TREBUCHET_BOLD_15);
//                        }
//                    };
//                    return cell;
//                }
//            }
//        );

        operationMode.setCellFactory(lv -> new OpModeListCell());
        operationMode.setButtonCell(new OpModeListCell());
        operationMode.setPrefHeight(topPaneHeight - 7);
        // operationMode.getEditor().setFont(TREBUCHET_BOLD_15);

        percentageTile = TileBuilder.create()
                .skinType(Tile.SkinType.PERCENTAGE)
                .prefSize(TILE_WIDTH, prefHeight / 4)
                .title("% Passed")
                .unit(Helper.PERCENTAGE)
                .description(" Passed")
                .maxValue(100)
                .textColor(Color.BLACK)
                .backgroundColor(Color.DARKGREEN)
                .foregroundColor(Color.WHITE)
                .value(98.5)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        numberTile = TileBuilder.create()
                .skinType(Tile.SkinType.NUMBER)
                .prefSize(TILE_WIDTH, prefHeight / 4)
                .title("Number Inspected")
                .text("Number of Parts Inspected")
                .value(200)
                .unit(" Parts")
                .description(" ")
                .textVisible(true)
                .backgroundColor(Color.BLACK)
                .textColor(Color.WHITE)
                .decimals(0)
                .textSize(Tile.TextSize.BIGGER)
                .build();

//        imageTile = TileBuilder.create()
//                .skinType(Tile.SkinType.IMAGE)
//                .prefSize(TILE_WIDTH, prefHeight / 4)
//                .title("Result of current Inspection")
//                .image(greenTick)
//                .imageMask(Tile.ImageMask.RECTANGULAR)
//                .text(" Green Tick = OK;  Red Cross = Not OK")
//                .backgroundColor(Color.DARKBLUE)
//                .textSize(Tile.TextSize.BIGGER)
//                .textAlignment(TextAlignment.LEFT)
//                .build();

        makeImageTile(prefHeight);
        updateImageTile(true);

//        series2.setName("Last 10 results");
//        series2.getData().add(new XYChart.Data("1", 1));
//        series2.getData().add(new XYChart.Data("2", 1));
//        series2.getData().add(new XYChart.Data("3", 0));
//        series2.getData().add(new XYChart.Data("4", 1));
//        series2.getData().add(new XYChart.Data("5", 1));
//        series2.getData().add(new XYChart.Data("6", 1));
//        series2.getData().add(new XYChart.Data("7", 1));
//        series2.getData().add(new XYChart.Data("8", 1));
//        series2.getData().add(new XYChart.Data("9", 1));
//        series2.getData().add(new XYChart.Data("10", 1));
//
//        lineChartTile = TileBuilder.create()
//                .skinType(Tile.SkinType.SMOOTHED_CHART)
//                .prefSize(TILE_WIDTH, prefHeight / 4)
//                .title("Last 10 Inspection Results")
//                .text("1 = OK ; 0 = Not OK")
//                //.animated(true)
//                .smoothing(false)
//                .series(series2)
//                .backgroundColor(Color.LIGHTBLUE)
//                .textColor(Color.BLACK)
//                .titleColor(Color.BLACK)
//                .textSize(Tile.TextSize.BIGGER)
//                .tickLabelsXVisible(false)
//                .chartGridColor(Color.WHITE)
//                .maxMeasuredValueVisible(true)
////                .barColor(Color.GREEN)
////                .barBackgroundColor(Color.BLUE)
//                .build();

        makeRollingResultsTile(prefHeight, true);

        tileBox = new VBox();
        tileBox.getChildren().add(percentageTile);
        tileBox.getChildren().add(numberTile);
        tileBox.getChildren().add(imageTile);
        tileBox.getChildren().add(rollingResultsTile);

        int spacerWidth = 15;
        Region spacer1 = new Region();
        spacer1.setPrefWidth(spacerWidth);
        HBox.setHgrow(spacer1, Priority.NEVER);
        topPane.getChildren().add(spacer1);
        topPane.getChildren().add(inspectButton);
        HBox.setHgrow(inspectButton, Priority.NEVER);
        Region spacer2 = new Region();
        spacer2.setPrefWidth(spacerWidth);
        HBox.setHgrow(spacer2, Priority.NEVER);
        topPane.getChildren().add(spacer2);
        topPane.getChildren().add(lbInfoLabel);
        HBox.setHgrow(lbInfoLabel, Priority.ALWAYS);
        Region spacer3 = new Region();
        spacer3.setPrefWidth(spacerWidth);
        HBox.setHgrow(spacer3, Priority.ALWAYS);
        topPane.getChildren().add(spacer3);
        HBox.setHgrow(operationMode, Priority.NEVER);
        topPane.getChildren().add(operationMode);
        lbInfoLabel.setPrefWidth(prefWidth - 2 * spacerWidth - inspectButtonWidth - 5);
        topPane.setVisible(true);

        root.setPrefWidth(prefWidth + 5 + TILE_WIDTH);
        root.setPrefHeight(prefHeight + 2 * topPaneHeight + 5);
        root.setTop(topPane);
        root.setCenter(overallTabPane);
        root.setRight(tileBox);
        // installTabHandlers(overallTabPane);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(root);

        scene = new Scene(stackPane, root.getPrefWidth() + 10, root.getPrefHeight() + 10);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                int response = QA_ConnectingRods.this.closeWindowEvent(primaryStage);
                if (response == 1) {
                    logger.trace("Closing down the application and shutting down remote cameras");
                    Tab selectedTab = overallTabPane.getSelectionModel().getSelectedItem();
                    if (selectedTab != null) {
                        String selectedId = selectedTab.getId();
                        Part currentPart = null;
                        for (int i = 0; i < options.size(); ++i) {
                            if (options.get(i).toString().equals(selectedId)) {
                                currentPart = partsToBeInspected.get(i);
                                break;
                            }
                        }
                        if (currentPart != null) {
                            currentPart.shutdownServer();
                        }
                    }
                    Platform.exit();
                    System.exit(0);
                }
                if (response == 2) {
                    logger.trace("Closing down the application");
                    Tab selectedTab = overallTabPane.getSelectionModel().getSelectedItem();
                    if (selectedTab != null) {
                        String selectedId = selectedTab.getId();
                        Part currentPart = null;
                        for (int i = 0; i < options.size(); ++i) {
                            if (options.get(i).toString().equals(selectedId)) {
                                currentPart = partsToBeInspected.get(i);
                                currentPart.updateConnectionStatus();
                                break;
                            }
                        }
                        if (currentPart != null) {
                            currentPart.close();
                        }
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
        updateImages();
    }

    private int closeWindowEvent(Stage primaryStage) {
        logger.trace("Entering closeWindowEvent()");
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

    protected void updateImages() {
        String partString = currentTabSelected.getText();
        Part currentPartInFocus = null;
        for (Part part : partsToBeInspected) {
            if (part.toString().equals(partString)) {
                currentPartInFocus = part;
                break;
            }
        }
        if (currentPartInFocus != null) {
            currentPartInFocus.showImages();
        }
    }

    public String toString() {
        return name;
    }

/*

    private void installTabHandlers(TabPane tabPane) {
        Set<Node> headers = tabPane.lookupAll(".tab-container");
        headers.forEach(node -> {
            // implementation detail: header of tabContainer is the TabHeaderSkin
            Parent parent = node.getParent();
            parent.setOnMouseClicked(ev -> handleHeader(parent));
        });
    }

    */

    /*

    private void handleHeader(Node tabHeaderSkin) {
        // implementation detail: skin keeps reference to associated Tab
        Tab tab = (Tab) tabHeaderSkin.getProperties().get(Tab.class);
        VBox dPane = (VBox) tab.getContent();
        dPane.requestLayout();
        root.requestLayout();
        root.getParent().requestLayout();
        System.out.println("Requested layout for tabId : " + tab.getId() + " and tabText " + tab.getText());
    }
*/
    protected void updateTiles(boolean success) {
        int numberDone = (int) numberTile.getValue();
        double percentageValue = percentageTile.getValue();
        int numberSucceeded = (int) (numberDone * percentageValue);
        numberTile.setValue(++numberDone);
        if (success) {
            ++numberSucceeded;
        }
        percentageTile.setValue(numberSucceeded * 1.0 / numberDone);
        // imageTile.setText(success ? "Not OK" : "OK");
        // imageTile.setTextColor(success ? Color.GREEN : Color.RED);
//        imageTile.setImage(success ? greenTick : redCross);
//        imageTile.setDescriptionAlignment(Pos.CENTER);
        updateImageTile(success);
        updateRollingResults(success);
//        ImageUtils.runAndWait(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        });
//        series2.getData().remove(0);
//        if (success) {
//            series2.getData().add(new XYChart.Data("", 1));
//        } else {
//            series2.getData().add(new XYChart.Data("", 0));
//        }
        //numberTile.getParent().requestLayout();
        //percentageTile.getParent().requestLayout();
        //textTile.getParent().requestLayout();
        //lineChartTile.getParent().requestLayout();
        tileBox.requestLayout();
    }

    public void makeImageTile(int prefHeight) {

        String textStyle = "-fx-font: 8px Tahoma; -fx-font-weight: normal;" +
                "-fx-stroke-width: 2";
        // + -fx-stroke: white;
//        Label name = new Label("The current part is :");
//        name.setStyle(textStyle);
//        name.setTextFill(Color.BLACK);
//        name.setAlignment(Pos.CENTER);
//        name.setPrefHeight(11);

//        HBox top = new HBox();
//        top.getChildren().add(name);
//        HBox.setHgrow(name, Priority.ALWAYS);
//        HBox.setHgrow(top, Priority.ALWAYS);
//        top.setPrefHeight(11);
//        top.setFillHeight(false);

        Label footer1 = new Label(" Green Tick = OK ");
        footer1.setAlignment(Pos.CENTER_LEFT);
        footer1.setTextFill(Color.GREEN);
        footer1.setStyle(textStyle);
        footer1.setPrefHeight(11);
        HBox.setHgrow(footer1, Priority.NEVER);

        Label footer2 = new Label(" Red Cross = Not OK");
        footer2.setAlignment(Pos.CENTER_LEFT);
        footer2.setTextFill(Color.RED);
        footer2.setStyle(textStyle);
        footer2.setPrefHeight(11);
        HBox.setHgrow(footer2, Priority.NEVER);

        HBox footer = new HBox();
        footer.getChildren().add(footer1);
        footer.getChildren().add(footer2);
        HBox.setHgrow(footer, Priority.ALWAYS);
        footer.setPrefHeight(11);
        footer.setAlignment(Pos.BOTTOM_CENTER);
        footer.setFillHeight(true);

        VBox imageViewBox = new VBox();
        imageViewBox.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        spacer.setPrefSize(5,5);
        VBox.setVgrow(spacer, Priority.NEVER);
        imageViewBox.setAlignment(Pos.CENTER);
        imageViewBox.getChildren().add(spacer);
        imageViewBox.getChildren().add(resultImageView);
        resultImageView.setPreserveRatio(true);
        resultImageView.setSmooth(false);
        resultImageView.setFitHeight((prefHeight / 4) - (2 * 11) - (2 * 12));
        resultImageView.setFitWidth(TILE_WIDTH - 10);

        HBox.setHgrow(imageViewBox, Priority.NEVER);
        imageViewBox.setPrefHeight(prefHeight / 4 - (2 * 11) - (2 * 12));
        imageViewBox.setFillWidth(true);

        // VBox tileData = new VBox(0, name, imageViewBox, footer);
        VBox tileData = new VBox(0, imageViewBox, footer);
        tileData.setFillWidth(true);

        imageTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM)
                .prefSize(TILE_WIDTH, prefHeight / 4)
                .title("The inspected component is : ")
                .backgroundColor(Color.LIGHTGRAY)
                .titleColor(Color.BLACK)
                .graphic(tileData)
                .text("Inspection Result")
                .textColor(Color.BLACK)
                .textSize(Tile.TextSize.BIGGER)
                .build();


    }

    private void updateImageTile(boolean success) {
        resultImageView.setImage(success ? greenTick : redCross);
        // if (resultImageView.getParent() != null) {
        //      resultImageView.getParent().requestLayout();
        // }
    }

    private void showQuestionMark() {
        ImageUtils.runAndWait(new Runnable() {
            public void run() {
                resultImageView.setImage(questionMark);
                // resultImageView.getParent().requestLayout();
            }
        });
    }

    public void makeRollingResultsTile(int prefHeight, boolean success) {

        if (rollingResultsTile != null) {
            updateRollingResults(success);
            return;
        }
        String textStyle = "-fx-font: 8px Tahoma; -fx-font-weight: normal;" +
                "-fx-stroke-width: 2";
        // + -fx-stroke: white;

        int footerHeight = 11;
        Label footer1 = new Label(" Green Box = OK ");
        footer1.setAlignment(Pos.CENTER_LEFT);
        footer1.setTextFill(GREEN);
        footer1.setStyle(textStyle);
        footer1.setPrefHeight(footerHeight);
        HBox.setHgrow(footer1, Priority.NEVER);

        Label footer2 = new Label(" Red Box = Not OK");
        footer2.setAlignment(Pos.CENTER_LEFT);
        footer2.setTextFill(Color.RED);
        footer2.setStyle(textStyle);
        footer2.setPrefHeight(footerHeight);
        HBox.setHgrow(footer2, Priority.NEVER);

        HBox footer = new HBox();
        footer.getChildren().add(footer1);
        footer.getChildren().add(footer2);
        HBox.setHgrow(footer, Priority.ALWAYS);
        footer.setPrefHeight(footerHeight);
        footer.setAlignment(Pos.BOTTOM_CENTER);
        footer.setFillHeight(true);

        int hGap = 3;
        int vGap = 3;
        int columns = 12;
        int rows = gridBoxes.length / columns;
        int spacerHeight = 10;
        int titleAndTextHeight = 11;
        int headroomForError = 10;
        int requiredHeight = (prefHeight / 4) - (2 * titleAndTextHeight); // for the title and bottom text of the tile
        requiredHeight = requiredHeight - footerHeight - spacerHeight - headroomForError; // for the footer, spacer, and some headroom
        int tileWidth = (TILE_WIDTH - columns * (2 * hGap) - 10) / (columns);
        int tileHeight = requiredHeight / rows;
        int prefTileWidth = Math.min(tileWidth, tileHeight);
        int prefTileHeight = prefTileWidth;

        rollingResultsPane = new GridPane();
        rollingResultsPane.setHgap(hGap);
        rollingResultsPane.setVgap(vGap);
        rollingResultsPane.setAlignment(Pos.CENTER);
        rollingResultsPane.setPrefHeight((prefTileHeight + vGap) * rows);
        rollingResultsPane.setPrefWidth(TILE_WIDTH);
        rollingResultsPane.setGridLinesVisible(false);

        for (int i = 0; i < gridBoxes.length; ++i) {
            gridBoxes[i] = new HBox();
            // gridBoxes[i].getChildren().add(new Label("  "));
            Region aRegion = new Region();
            aRegion.setPrefSize(prefTileWidth - 2, prefTileHeight - 2);
            gridBoxes[i].getChildren().add(aRegion);
            gridBoxes[i].setPrefWidth(prefTileWidth);
            gridBoxes[i].setPrefHeight(prefTileHeight);
            gridBoxes[i].setVisible(true);
        }

        for (int x = 0; x < rows; ++x) {
            for (int y = 0; y < columns; ++y) {
                rollingResultsPane.add(gridBoxes[x * columns + y],y,x);
            }
        }

        VBox gridHolder = new VBox();
        gridHolder.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        spacer.setPrefSize((prefTileWidth + 2 * vGap) * columns,spacerHeight);
        VBox.setVgrow(spacer, Priority.NEVER);
        gridHolder.setAlignment(Pos.CENTER);
        gridHolder.getChildren().add(spacer);
        HBox rpHolder = new HBox();
        rpHolder.getChildren().add(rollingResultsPane);
        rpHolder.setAlignment(Pos.TOP_CENTER);
        gridHolder.getChildren().add(rpHolder);
        // gridHolder.getChildren().add(rollingResultsPane);
        gridHolder.getChildren().add(footer);
        gridHolder.setFillWidth(true);
        // gridHolder.setPrefHeight(requiredHeight);
        gridHolder.setVisible(true);

        rollingResultsTile = TileBuilder.create()
                .skinType(Tile.SkinType.CUSTOM)
                .prefSize(TILE_WIDTH, prefHeight / 4)
                .title("The last " + rollingResultsToBeDisplayed + " results are : ")
                .backgroundColor(Color.WHITE)
                .titleColor(Color.BLACK)
                .graphic(gridHolder)
                .text("Last " + rollingResultsToBeDisplayed + " Results")
                .textColor(Color.BLACK)
                .textSize(Tile.TextSize.BIGGER)
                .build();

        updateRollingResults(success);
    }

    private void updateRollingResults(boolean success) {
        resultsHistory.add(success);
        int size = resultsHistory.size();
        int excessResults = Math.max(size - rollingResultsToBeDisplayed, 0);
        for (int i =  size - 1; i >= Math.max(0, (size - rollingResultsToBeDisplayed)); --i) {
            Boolean value = resultsHistory.get(i);
            gridBoxes[i - excessResults].setBackground(value.booleanValue() ? GREEN_BACKGROUND : RED_BACKGROUND);
        }
        if (excessResults > 0) {
            for (int i = rollingResultsToBeDisplayed - size - 1; i >= 0; --i) {
                gridBoxes[i].setBackground(BLACK_BACKGROUND);
            }
        }
        if (rollingResultsTile.getParent() != null) {
            rollingResultsTile.requestLayout();
            rollingResultsTile.getParent().requestLayout();
        }
    }

    private static void populateResultsHistory() {
        for (int i = 0; i < 25; ++i) {
            resultsHistory.add(Boolean.TRUE);
        }
        resultsHistory.add(Boolean.FALSE);
        for (int i = 27; i < 50; ++i) {
            resultsHistory.add(Boolean.TRUE);
        }
        resultsHistory.add(Boolean.FALSE);
        for (int i = 51; i < 60; ++i) {
            resultsHistory.add(Boolean.TRUE);
        }
    }

    public String getName() {
        return name;
    }

    public MachiningNode getParent() {
        return this;
    }

    public void setParent(MachiningNode mNode) {
        // do nothing
    }

    public ArrayList<MachiningNode> getChildren() {
        ArrayList<MachiningNode> nodes = new ArrayList<>();
        for (MachiningNode node : partsToBeInspected) {
            nodes.add(node);
        }
        return nodes;
    }

    class OpModeListCell extends ListCell<String> {

        OpModeListCell() {
            super.setPrefWidth(100);
        }

        @Override public void updateItem(String item,
                                         boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                setText(item);
                if (item.contains(TRAINING)) {
                    setTextFill(Color.RED);
                } else {
                    if (item.contains(PRODUCTION)) {
                        setTextFill(Color.GREEN);
                    } else {
                        if (item.contains(CONFIGURE)) {
                            setTextFill(Color.DARKBLUE);
                        } else {
                            setTextFill(Color.BLACK);
                        }
                    }
                }
            } else {
                setText(null);
            }
            setFont(TREBUCHET_BOLD_15);
        }
    };

    class HandCursorEventHandler implements EventHandler {

        @Override
        public void handle(Event event) {
            scene.setCursor(Cursor.HAND);
        }
    };

    class DefaultCursorEventHandler implements EventHandler {

        @Override
        public void handle(Event event) {
            scene.setCursor(Cursor.DEFAULT);
        }
    };

}
