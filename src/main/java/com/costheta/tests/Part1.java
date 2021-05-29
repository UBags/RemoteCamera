package com.costheta.tests;

import com.costheta.camera.remote.client.RemoteShutdown;
import com.costheta.camera.remote.image.DefaultImages;
import com.costheta.image.CosThetaImageView;
import com.costheta.machine.EvaluatedResult;
import com.costheta.machine.FinalResult;
import com.costheta.machine.InspectionPoint;
import com.costheta.machine.ProcessingResult;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public abstract class Part1 implements RemoteShutdown {

    private static final Logger logger = LogManager.getLogger(Part1.class);

    private static int individualImageHeight = 160;
    public static int getIndividualImageHeight() {
        return getIndividualImageHeight();
    }
    private static int individualImageWidth = 240;
    public static int getIndividualImageWidth() {
        return individualImageWidth;
    }

    protected static final int webPaneHeight = 100;

    private String partName;
    private String partId;
    private String description = EMPTY_STRING; // in case one is needed

    private static final ArrayList<String> partIds = new ArrayList<>();
    private static final ArrayList<String> partNames = new ArrayList<>();
    private final ArrayList<InspectionPoint> inspectionPoints = new ArrayList<>();
    private ArrayList<ArrayList<ProcessingResult>> results = new ArrayList<>();
    private ArrayList<EvaluatedResult> evaluations = new ArrayList<>();
    private FinalResult finalResult = null;

    protected StackPane displayPane;
    public StackPane getDisplayPane() {
        logger.trace("Entering getDisplayPane()");
        if (displayPane == null) {
            createDisplayPane();
        }
        return displayPane;
    }

    protected WebView webView;
    public WebView getWebView() {return webView;}

    protected WebEngine webEngine;
    public WebEngine getWebEngine() {return webEngine;}

    protected static final String EMPTY_STRING = "";

    // should be called after all the InspectionPoint objects have been made
    protected void createDisplayPane() {
        logger.trace("Entering createDisplayPane()");
        if (displayPane != null) {
            return;
        }
        displayPane = new StackPane();

        int numberOfImagesToBeDisplayed = inspectionPoints.size();
        int numberOfRows = (numberOfImagesToBeDisplayed <= 2) ? 1 :
                (numberOfImagesToBeDisplayed <= 6 ? 2 : 3);
        int numberOfColumns = (numberOfImagesToBeDisplayed == 1) ? 1 :
                (numberOfImagesToBeDisplayed == 2 ? 2 :
                        (numberOfImagesToBeDisplayed <= 4 ? 2 :
                3));
        if (numberOfRows == 1) {
            individualImageHeight = 320;
            individualImageHeight = 480;
        } else {
            if (numberOfRows == 2) {
                individualImageHeight = 160;
                individualImageHeight = 240;
            } else {
                individualImageHeight = 110;
                individualImageHeight = 160;
            }
        }
        HBox[] horizontalRows = new HBox[numberOfRows];
        for (int i = 0; i < numberOfRows; ++i) {
            horizontalRows[i] = new HBox();
            horizontalRows[i].setAlignment(Pos.CENTER);
            horizontalRows[i].setPrefSize(480, 160);
            horizontalRows[i].setPadding(new Insets(2));
        }

        FlowPane[][] imageFlowPanes = new FlowPane[numberOfRows][numberOfColumns];
        for (int i = 0; i < numberOfRows; ++i) {
            for (int j = 0; j < numberOfColumns; ++j) {
                imageFlowPanes[i][j] = new FlowPane(Orientation.HORIZONTAL);
                imageFlowPanes[i][j].setAlignment(Pos.CENTER);
                imageFlowPanes[i][j].setPadding(new Insets(2));
                imageFlowPanes[i][j].setPrefWrapLength(individualImageWidth); // preferred width = 240
                imageFlowPanes[i][j].setPrefSize(individualImageWidth, individualImageHeight);
                // imageFlowPanes[i][j].setStyle("-fx-background-color: #ccc;");
            }
        }

        for (int i = 0; i < numberOfImagesToBeDisplayed; ++i) {
            CosThetaImageView imageView = inspectionPoints.get(i).getImageView();
            imageFlowPanes[i / numberOfColumns] [ i % numberOfColumns].getChildren().add(imageView);
            horizontalRows[i / numberOfColumns].getChildren().add(new Separator(Orientation.VERTICAL));
            horizontalRows[i / numberOfColumns].getChildren().add(imageFlowPanes[i / numberOfColumns] [ i % numberOfColumns]);
            if (i % numberOfColumns == (numberOfColumns - 1)) {
                horizontalRows[i / numberOfColumns].getChildren().add(new Separator(Orientation.VERTICAL));
            }
            imageView.setImage(SwingFXUtils.toFXImage(DefaultImages.EMPTY_BUFFERED_IMAGE,null));
            imageView.setVisible(true);
        }

        VBox imagePane = new VBox();
        imagePane.setAlignment(Pos.CENTER);
        imagePane.setPadding(new Insets(2));
        imagePane.setPrefSize((individualImageWidth + 10) * numberOfColumns, (individualImageHeight + 10) * numberOfRows);
        //imagePane.setStyle("-fx-background-color: #ccc;");

        for (int i = 0; i < numberOfRows; ++i) {
            imagePane.getChildren().add(new Separator(Orientation.HORIZONTAL));
            imagePane.getChildren().add(horizontalRows[i]);
            if (i == numberOfRows - 1) {
                imagePane.getChildren().add(new Separator(Orientation.HORIZONTAL));
            }
        }

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefSize(imagePane.getPrefWidth(), webPaneHeight);
        scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);

        Platform.runLater(new Runnable() {
            public void run() {
                logger.trace("Entering Platform.runlater() for webView");
                webView = new WebView();
                webEngine = webView.getEngine();
                webEngine.getLoadWorker().stateProperty().addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                                logger.trace("Entering changed() in webView ChangeListener()");
                                webView.requestLayout();
                                scrollPane.requestLayout();
                                scrollPane.setVvalue(1.0f);
                                //if (newValue == Worker.State.SUCCEEDED) {
                                //document finished loading
                                //}
                            }
                        }
                );
                webEngine.loadContent("<body><div id='content'></div></body>");
                scrollPane.setContent(webView);
            }
        });

        Label resultLabel = new Label("Results");
        resultLabel.setFont(Font.font("Verdana", FontWeight.EXTRA_BOLD, 15));

        FlowPane resultLabelFlowPane = new FlowPane(Orientation.HORIZONTAL);
        resultLabelFlowPane.setStyle("-fx-background-color: #eee;");
        resultLabelFlowPane.setStyle("-fx-foreground-color: #00f;");
        resultLabelFlowPane.getChildren().add(resultLabel);
        resultLabelFlowPane.setPrefWrapLength(imagePane.getWidth());
        resultLabelFlowPane.setPrefSize(imagePane.getPrefWidth(), resultLabel.getHeight() + 5);

        VBox resultPane = new VBox();
        resultPane.setAlignment(Pos.CENTER);
        //resultPane.setVgap(4);
        //resultPane.setHgap(4);
        //resultPane.setPrefWrapLength(200);
        resultPane.setPadding(new Insets(2));
        resultPane.setPrefSize(imagePane.getPrefWidth(), webPaneHeight + resultLabelFlowPane.getHeight() + 20);
        resultPane.getChildren().add(resultLabelFlowPane);
        resultPane.getChildren().add(new Separator(Orientation.HORIZONTAL));
        resultPane.getChildren().add(scrollPane);
        resultPane.setStyle("-fx-background-color: #ccc;");

        VBox overall = new VBox();
        overall.setAlignment(Pos.CENTER);
        overall.setPadding(new Insets(1));
        overall.setPrefSize(imagePane.getPrefWidth(), imagePane.getPrefHeight() + resultPane.getPrefHeight() + 10);
        overall.getChildren().add(imagePane);
        // overall.getChildren().add(new Separator(Orientation.HORIZONTAL));
        overall.getChildren().add(resultPane);
        overall.setStyle("-fx-background-color: #ccc;");

        displayPane.getChildren().add(overall);
    }

    public Part1(String partName, String partId, String description) {
        logger.trace("Entering constructor()");
        if (partNames.contains(partName)) {
            throw new IllegalArgumentException("A Part with the name " + partName + " already exists. Part names must be unique.");
        }
        if (partIds.contains(partId)) {
            throw new IllegalArgumentException("A Part with the id " + partId + " already exists. Part id's must be unique.");
        }
        this.partName = partName;
        partNames.add(partName);
        this.description = description;
        this.partId = partId;
        partIds.add(partId);
    }

    public Part1(String partName, String partId) {
        this(partName, partId, EMPTY_STRING);
        logger.trace("After calling constructor()");
    }

    public String getPartName() {
        return partName;
    }

    public String getPartId() {
        return partId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addInspectionPoint(InspectionPoint iPoint) {
        logger.trace("Entering addInspectionPoint()");
        if (iPoint != null) {
            displayPane = null; // important
            inspectionPoints.add(iPoint);
            createDisplayPane();
        }
    }

    public ArrayList<InspectionPoint> getInspectionPoints() {
        return inspectionPoints;
    }

    public InspectionPoint getInspectionPoint(int index) {
        if (index < 0) {
            return null;
        }
        if (index > inspectionPoints.size() - 1) {
            return null;
        }
        return inspectionPoints.get(index);
    }

    public String toString() {
        return (partName + " - " + partId);
    }

    public String doProcessing() {
        return "";
    }

    public FinalResult inspect() {
        logger.trace("Entering inspect()");
        createDisplayPane();
        results.clear();
        evaluations.clear();
//        results.ensureCapacity(inspectionPoints.size());
//        evaluations.ensureCapacity(inspectionPoints.size());
        Object[] tempResults = new Object[inspectionPoints.size()];
        Object[] tempEvals = new Object[inspectionPoints.size()];
        ArrayList<CompletableFuture<Boolean>> threads = new ArrayList<>();
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            final int currentIndex = i;
            threads.add(CompletableFuture.supplyAsync(() -> {
                InspectionPoint iPoint = inspectionPoints.get(currentIndex);
                ArrayList<ProcessingResult> pResults = iPoint.inspect();
                EvaluatedResult eResult = iPoint.evaluateResult(pResults);
//                results.add(currentIndex, pResults);
//                evaluations.add(currentIndex,eResult);
                // These 2 will not work as the threads may come in random order
                // Effectively, it may lead to a condition where we are trying to add
                // an object in these arraylists at index 1 when the size is still 0;
                tempResults[currentIndex] = pResults;
                tempEvals[currentIndex] = eResult;
                return Boolean.TRUE;
            }));
        }
        CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            results.add((ArrayList<ProcessingResult>) tempResults[i]);
            evaluations.add((EvaluatedResult) tempEvals[i]);
        }
        finalResult = tallyEvaluations(evaluations);
        return finalResult;
    }

    private void appendResult(String msg) {
        logger.trace("Entering appendResult()");
        Platform.runLater(new Runnable() {
            public void run() {
                Document doc = webEngine.getDocument();
                Element el = doc.getElementById("content");
                String s = el.getTextContent();
                el.setTextContent(s + msg);
            }
        });
    }

    public abstract FinalResult tallyEvaluations(ArrayList<EvaluatedResult> eResults);

    public void shutdownServer() {
        logger.info("Entering shutdownServer()");
        ArrayList<CompletableFuture<Boolean>> threads = new ArrayList<>();
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            final int currentIndex = i;
            threads.add(CompletableFuture.supplyAsync(() -> {
                inspectionPoints.get(currentIndex).shutdownServer();
                return Boolean.TRUE;
            }));
        }
        CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
        logger.warn("Closed all inspection points and shutdown remote cameras.");
        logger.warn("Shutting down application.");
        System.exit(0);
    }

    public void close() {
        logger.warn("Entering close()");
        ArrayList<CompletableFuture<Boolean>> threads = new ArrayList<>();
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            final int currentIndex = i;
            threads.add(CompletableFuture.supplyAsync(() -> {
                inspectionPoints.get(currentIndex).close();
                return Boolean.TRUE;
            }));
        }
        CompletableFuture.allOf(threads.toArray(new CompletableFuture[threads.size()])).join();
        logger.warn("Closed all inspection points.");
        logger.warn("Shutting down application.");
        System.exit(0);
    }

}
