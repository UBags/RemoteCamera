package com.costheta.tests;

import com.costheta.camera.remote.client.NetworkClientCamera;
import com.costheta.kems.connectingrods.ConnectingRod_AshokLeyland;
import com.costheta.kems.connectingrods.LeftSideTextProcessor;
import com.costheta.kems.connectingrods.RightSideTextProcessor;
import com.costheta.machine.*;
import com.costheta.utils.GeneralUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Optional;

public class ConnectingRods1 extends Application {

    private static final Logger logger = LogManager.getLogger(ConnectingRods1.class);
    private static BorderPane root;
    private static VBox displayPane;
    private static Button actionButton;
    private static ComboBox<Part> cameraOptions;

    private static ArrayList<Part> partsToBeInspected = new ArrayList<>();

    private static boolean windowMinimised = false;

    protected static final int topPaneHeight = 40;

    public static void main(String[] args) {
        // GeneralUtils.rerouteLog(new String[]{"client"});
        GeneralUtils.updateLogger("RemoteCameraLog", "logs/client.log", "com.costheta");
        logger.trace("Entering main()");
        BaseApplication.populateInitialisationArgs(args);
        Part part1 = new ConnectingRod_AshokLeyland("Ashok Leyland Type 1", "100001", 2);

        NetworkClientCamera leftCamera = new NetworkClientCamera("Left Side Camera", 1);
        InspectionPoint leftCameraInspectionPoint = new InspectionPoint("Left Side Inspection Point", leftCamera);
        ArrayList<String> leftSideStringPatterns = new ArrayList<>();
        leftSideStringPatterns.add("DDA");
        leftSideStringPatterns.add("AADD");
        leftSideStringPatterns.add("DDDDD");
        leftSideStringPatterns.add("DDDDD");
        LeftSideTextProcessor leftSideTextProcessor = new LeftSideTextProcessor("Left Side Processor", leftSideStringPatterns);
        leftCameraInspectionPoint.addImageProcessor(leftSideTextProcessor);

        NetworkClientCamera rightCamera = new NetworkClientCamera("Right Side Camera", 2);
        InspectionPoint rightCameraInspectionPoint = new InspectionPoint("Right Side Inspection Point", rightCamera);
        ArrayList<String> rightSideStringPatterns = new ArrayList<>();
        rightSideStringPatterns.add("ADDDA");
        rightSideStringPatterns.add("AADDD");
        RightSideTextProcessor rightSideTextProcessor = new RightSideTextProcessor("Right Side Processor", rightSideStringPatterns);
        rightCameraInspectionPoint.addImageProcessor(rightSideTextProcessor);

        part1.addInspectionPoint(leftCameraInspectionPoint);
        part1.addInspectionPoint(rightCameraInspectionPoint);

        Part part2 = new ConnectingRod_AshokLeyland("Ashok Leyland Type 2", "100002", 2);
        part2.addInspectionPoint(leftCameraInspectionPoint);
        part2.addInspectionPoint(rightCameraInspectionPoint);

        partsToBeInspected.add(part1);
        partsToBeInspected.add(part2);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.trace("Entering start()");
        root = new BorderPane();

        HBox topPane = new HBox();
        topPane.setPrefHeight(topPaneHeight);
        topPane.setPadding(new Insets(2));
        topPane.setAlignment(Pos.BOTTOM_CENTER);
        topPane.setStyle("-fx-background-color: #ddd");

        Label lbInfoLabel = new Label("Select Your Camera");
        lbInfoLabel.setStyle("-fx-foreground-color: #f00");
        lbInfoLabel.setPrefHeight(30);
        ObservableList<Part> options = FXCollections.observableArrayList();

        for (int i = 0; i < partsToBeInspected.size(); ++i) {
            options.add(partsToBeInspected.get(i));
            logger.info("Added Part " + partsToBeInspected.get(i) + " to options");
        }

        cameraOptions = new ComboBox<>();
        cameraOptions.setItems(options);
        cameraOptions.setValue(partsToBeInspected.get(0));
        displayPane = partsToBeInspected.get(0).getDisplayPane();
        cameraOptions.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Part>() {

            @Override
            public void changed(ObservableValue<? extends Part> arg0, Part arg1, Part arg2) {
                if (arg2 != null) {
                    System.out.println(
                            "Part Chosen: " + arg2);
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
                Part currentPart = cameraOptions.getValue();
                // currentPart.getDisplayPane().getChildren().clear();
                root.getChildren().remove(displayPane);
                displayPane = currentPart.getDisplayPane();
                root.getChildren().add(displayPane);
                displayPane.requestLayout();
                root.requestLayout();
                root.getParent().requestLayout();
                FinalResult finalResult = currentPart.inspect();
                // sends an ImageRequest to the camera and when ImageResponse comes back,
                // it is automatically processed because a listener has been attached to the
                // processor
                actionButton.setDisable(false);
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

        root.setTop(topPane);
        root.setCenter(displayPane);

        StackPane stackPane = new StackPane();
        stackPane.getChildren().add(root);

        Scene scene = new Scene(stackPane, displayPane.getPrefWidth() + 10, displayPane.getPrefHeight() + 10);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                int response = ConnectingRods1.this.closeWindowEvent(primaryStage);
                if (response == 1) {
                    logger.trace("Closing down the application and shutting down remote cameras");
                    cameraOptions.getValue().shutdownServer();
                    Platform.exit();
                    System.exit(0);
                }
                if (response == 2) {
                    logger.trace("Closing down the application");
                    cameraOptions.getValue().close();
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

}
