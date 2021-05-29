package com.costheta.machine;

import com.costheta.camera.processor.ImageProcessor;
import com.costheta.camera.remote.client.RemoteShutdown;
import com.costheta.camera.remote.image.DefaultImages;
import com.costheta.image.BasePaneConstants;
import com.costheta.image.CosThetaGridPane;
import com.costheta.image.CosThetaImageView;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class Part extends BaseApplication implements MachiningNode, RemoteShutdown, BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(Part.class);
    private static final String pathOfPictures = "pictures";
    private static final String pathOfLogs = "logs";
    private static Font connectionFont = TREBUCHET_12;
    private static Font boldConnectionFont = TREBUCHET_BOLD_12;
    private final String INSPECTION_POINT = "Inspection Point ";

    public static final Hashtable<Integer, Integer> screenCombinations = new Hashtable<>();
    private static final double widthMultiple1 = 0.35;
    private static final double widthMultiple2 = 0.55;
    private static final double widthMultiple3 = 0.65;
    private static final double heightCutoff = 0.6;

    static {
        populateCellSizeCombinations();
    }

    private static int defaultIndividualImageHeight = 160;
    private int imageHeight = defaultIndividualImageHeight;
    public int getIndividualImageHeight() {
        return imageHeight;
    }

    private static int defaultIndividualImageWidth = 240;
    private int imageWidth = defaultIndividualImageWidth;
    public int getIndividualImageWidth() {
        return imageWidth;
    }

    private MachiningNode parent;
    private String partName;
    private String partId;
    private String description = EMPTY_STRING; // in case one is needed

    private int numberOfRows;
    private int numberOfColumns;

    private static final ArrayList<String> partIds = new ArrayList<>();
    private static final ArrayList<String> partNames = new ArrayList<>();
    private final ArrayList<InspectionPoint> inspectionPoints = new ArrayList<>();
    private ArrayList<ArrayList<ProcessingResult>> results = new ArrayList<>();
    private ArrayList<EvaluatedResult> evaluations = new ArrayList<>();
    private FinalResult finalResult = null;

    private int numberOfInspectionPoints = 0;

    protected CosThetaGridPane pictureViewPane;
    protected TextArea textArea;
    protected VBox displayPane;
    protected HBox connectionPane;
    protected FlowPane connectionStatus;
    private static final VBox defaultVBox = new VBox();

    int partDetailsHeight = 18;
    private int labelHeight = 30;
    private int separatorHeight = 8;
    private int textareaHeight = 100;
    private int connectionPaneHeight = 30;

    protected String baseStringPathOfPart = null;
    protected Path basePathOfPart = null;

    static {
        defaultVBox.getChildren().add(new Label("No Inspection Points defined for this part"));
    }

    public VBox getDisplayPane() {
        logger.trace("  Entering displayPane() in " + this);
        boolean displayAvailable = createDisplayPane();
        if (!displayAvailable) {
            logger.trace("  Returning defaultVBox as displayPane not available in " + this);
            return defaultVBox;
        }
        return displayPane;
    }

    protected static final String EMPTY_STRING = "";

    // should be called after all the InspectionPoint objects have been made
    protected boolean createDisplayPane() {
        logger.trace("    Entering createDisplayPane() in " + this);
        if (displayPane != null) {
            logger.trace("    As displayPane already exists in " + this + " , returning true");
            return true;
        }
        if (inspectionPoints.size() <= 0) {
            logger.trace("    As there are no inspection points in " + this + " , no need for a display Pane and returning false");
            return false;
        }
        // int numberOfImagesToBeDisplayed = inspectionPoints.size();

        int cellWidth = inspectionPoints.get(0).getImageView().getTotalWidth();
        int cellHeight = inspectionPoints.get(0).getImageView().getTotalHeight();
        logger.trace("    cellWidth in " + this + " is calculated as " + cellWidth);
        logger.trace("    cellHeight in " + this + " is calculated as " + cellHeight);
        pictureViewPane = new CosThetaGridPane(numberOfRows, numberOfColumns, cellWidth, cellHeight);
        logger.trace("    Created pictureViewPane in " + this);
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            CosThetaImageView anImageView = inspectionPoints.get(i).getImageView();
            pictureViewPane.setCellWidth(anImageView.getTotalWidth());
            pictureViewPane.setCellHeight(anImageView.getTotalHeight());
            pictureViewPane.add(anImageView, i % numberOfColumns, i / numberOfColumns);
            anImageView.setVisible(true);
            logger.trace("    Added imageView " + anImageView + " to pictureViewPane in " + this + " with width = " + anImageView.getTotalWidth() + " and height " + anImageView.getTotalHeight());
        }

        displayPane = new VBox();

        HBox partDetailsBox = new HBox();
        partDetailsBox.setAlignment(Pos.CENTER_LEFT);
        Label partDetailsLabel = new Label(partName + " - " + partId + " - " + description);
        // String partDetailsStyle = "-fx-background-color: rgb(63, 72, 204); -fx-foreground-color: #eeeeee;" +
        //        "-fx-font: 16px SansSerif; -fx-font-weight: bold;" +
        //        "-fx-stroke: rgb(63, 72, 204); -fx-stroke-width: 2; -fx-text-fill: rgb(220, 220, 220)";

        // partDetailsLabel.setStyle(partDetailsStyle);
        // partDetailsBox.setStyle(partDetailsStyle);
        partDetailsLabel.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        partDetailsBox.setBackground(MEDIUM_GRAYISH_BACKGROUND);
        partDetailsLabel.setFont(TREBUCHET_BOLD_15);
        partDetailsLabel.setTextFill(VERY_LIGHT_GRAY);
        Region partSpacer = new Region();
        partSpacer.setPrefSize(10, 5);
        HBox.setHgrow(partSpacer, Priority.NEVER);
        partDetailsBox.getChildren().add(partSpacer);
        partDetailsBox.getChildren().add(partDetailsLabel);
        HBox.setHgrow(partDetailsLabel, Priority.ALWAYS);
        HBox.setHgrow(partDetailsBox, Priority.ALWAYS);
        partDetailsLabel.setPrefWidth(pictureViewPane.getTotalWidth());
        partDetailsBox.setPrefHeight(partDetailsHeight);
        partDetailsBox.setFillHeight(true);
        displayPane.getChildren().add(partDetailsBox);

        connectionPane = new HBox();
        connectionPane.setPrefHeight(connectionPaneHeight);
        connectionPane.setBackground(YELLOWISH_BACKGROUND);
        // String statusBackgroundColor = "-fx-background-color: #eeeeee";
        // String statusFontString = "-fx-font: 12px Trebuchet; -fx-font-weight: normal;" +
                // "-fx-stroke: black;" +
                // "-fx-stroke-width: 2";
        // connectionPane.setStyle(statusBackgroundColor);
        connectionPane.setAlignment(Pos.CENTER);
        connectionStatus = new FlowPane();
        connectionPane.getChildren().add(connectionStatus);
        HBox.setHgrow(connectionPane, Priority.ALWAYS);
        HBox.setHgrow(connectionStatus, Priority.ALWAYS);
        Separator horSeparator0 = new Separator(Orientation.HORIZONTAL);
        displayPane.getChildren().add(horSeparator0);
        updateConnectionStatus();
        displayPane.getChildren().add(connectionPane);
        Separator horSeparator1 = new Separator(Orientation.HORIZONTAL);
        horSeparator1.setPrefHeight(separatorHeight);
        displayPane.getChildren().add(horSeparator1);
        displayPane.getChildren().add(pictureViewPane);
        logger.trace("    Added pictureViewPane " + pictureViewPane + " to displayPane in " + this);
        Separator horSeparator2 = new Separator(Orientation.HORIZONTAL);
        horSeparator2.setPrefHeight(separatorHeight);
        displayPane.getChildren().add(horSeparator2);
        HBox resultLabelHolder = new HBox();
        resultLabelHolder.setBackground(DARK_GRAYISH_BACKGROUND);
        // String resultBoxBackgroundColor = "-fx-background-color: #0080cf";
        // String resultBoxFontString = "-fx-font: 15px ; -fx-font-weight: bold;" +
            // "-fx-stroke: black;" +
            // "-fx-stroke-width: 3";
        // resultLabelHolder.setStyle(resultBoxBackgroundColor);
        Label resultLabel = new Label("  Results");
        resultLabel.setBackground(DARK_GRAYISH_BACKGROUND);
        resultLabel.setFont(TREBUCHET_BOLD_15);
        resultLabel.setPrefHeight(labelHeight);
        resultLabel.setTextFill(VERY_LIGHT_GRAY);
        // resultLabel.setStyle(resultBoxBackgroundColor + "; -fx-text-fill: #ffffff; " + resultBoxFontString);
        // resultLabel.setPrefWidth(getDisplayWidth());
        resultLabelHolder.getChildren().add(resultLabel);
        HBox.setHgrow(resultLabel, Priority.ALWAYS);
        logger.trace("    Total width of pictureViewPane " + pictureViewPane + " is " + pictureViewPane.getTotalWidth() + " in " + this);
        displayPane.getChildren().add(resultLabelHolder);
        Separator separator1 = new Separator();
        separator1.setStyle("-fx-background-color:#444444;");
        displayPane.getChildren().add(separator1);
        textArea = new TextArea();
        textArea.setFont(TAHOMA_15);
        textArea.setPrefHeight(textareaHeight);
        textArea.setPrefWidth(pictureViewPane.getTotalWidth());
        displayPane.getChildren().add(textArea);
        displayPane.setPrefHeight(pictureViewPane.getTotalHeight() + textareaHeight + connectionPaneHeight + partDetailsHeight + 15);
        logger.trace("    Total height of pictureViewPane " + pictureViewPane + " is " + pictureViewPane.getTotalHeight() + " in " + this);
        return true;
    }

    public Part(String partName, String partId, int numberOfInspectionPoints, String description) {
        logger.trace("Entering constructor()");
        if (partNames.contains(partName)) {
            throw new IllegalArgumentException("A Part with the name " + partName + " already exists. Part names must be unique.");
        }
        if (partIds.contains(partId)) {
            throw new IllegalArgumentException("A Part with the id " + partId + " already exists. Part id's must be unique.");
        }
        this.numberOfInspectionPoints = numberOfInspectionPoints;
        this.partName = partName;
        partNames.add(partName);
        this.description = description;
        this.partId = partId;
        partIds.add(partId);
        calculateCellWidthAndHeight(this.numberOfInspectionPoints);
        logger.trace("After calling constructor()");
    }

    public Part(String partName, String partId, int numberOfInspectionPoints) {
        this(partName, partId, numberOfInspectionPoints, EMPTY_STRING);
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
            if (inspectionPoints.contains(iPoint)) {
                logger.info("Attempt to add same inspection point " + iPoint + " again to the Part " + this + ". Did not add.");
                return;
            }
            inspectionPoints.add(iPoint);
            iPoint.setParent(this);
        }
    }

    public void showImages() {
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            inspectionPoints.get(i).showImage();
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
        createImageAndLogDirectories();
        createDisplayPane();
        results.clear();
        evaluations.clear();
        // results.ensureCapacity(inspectionPoints.size());
        // evaluations.ensureCapacity(inspectionPoints.size());
        Object[] tempResults = new Object[inspectionPoints.size()];
        Object[] tempEvals = new Object[inspectionPoints.size()];
        ArrayList<CompletableFuture<Boolean>> threads = new ArrayList<>();
        for (int i = 0; i < inspectionPoints.size(); ++i) {
            final int currentIndex = i;
            threads.add(CompletableFuture.supplyAsync(() -> {
                InspectionPoint iPoint = inspectionPoints.get(currentIndex);
                ArrayList<ProcessingResult> pResults = iPoint.inspect();
                EvaluatedResult eResult = iPoint.evaluateResult(pResults);
                // results.add(currentIndex, pResults);
                // evaluations.add(currentIndex,eResult);
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
        textArea.clear();
        textArea.appendText(getDisplayText(results));
        finalResult = tallyEvaluations(evaluations);
        return finalResult;
    }

//    private void appendResult(String msg) {
//        logger.trace("Entering appendResult()");
//        Platform.runLater(new Runnable() {
//            public void run() {
//                Document doc = webEngine.getDocument();
//                Element el = doc.getElementById("content");
//                String s = el.getTextContent();
//                el.setTextContent(s + msg);
//            }
//        });
//    }

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

    public int getDisplayHeight() {
        return (int) (pictureViewPane.getTotalHeight() + labelHeight + 2 * separatorHeight + textareaHeight + connectionPaneHeight + partDetailsHeight + 10);
    }

    public int getDisplayWidth() {
        return (int) (pictureViewPane.getTotalWidth() + 10);
    }

    public void requestLayout() {
        displayPane.requestLayout();
    }

    private void calculateCellWidthAndHeight(int numberOfInspectionPoints) {
        int numberOfImagesToBeDisplayed = numberOfInspectionPoints;
        logger.trace("    Number of images to be displayed in " + this + " = " + numberOfImagesToBeDisplayed);
        numberOfRows = (numberOfImagesToBeDisplayed <= 2) ? 1 :
                (numberOfImagesToBeDisplayed <= 6 ? 2 : 3);
        numberOfColumns = (numberOfImagesToBeDisplayed == 1) ? 1 :
                (numberOfImagesToBeDisplayed == 2 ? 2 :
                        (numberOfImagesToBeDisplayed <= 4 ? 2 :
                                3));

//        if (numberOfRows == 1) {
//            imageHeight = (int) (BaseCamera.screenHeight * 0.35);
//            imageWidth = (int) (BaseCamera.screenWidth * 0.35 / numberOfColumns);
//        } else {
//            if (numberOfRows == 2) {
//                imageHeight = (int) (BaseCamera.screenHeight * 0.45 / numberOfRows);
//                imageWidth = (int) (BaseCamera.screenWidth * 0.45 / numberOfColumns);
//            } else {
//                imageHeight = (int) (BaseCamera.screenHeight * 0.5 / numberOfRows);
//                imageWidth = (int) (BaseCamera.screenWidth * 0.5 / numberOfColumns);
//            }
//        }

        double baseColumnSize = numberOfColumns == 1 ? BaseCamera.screenWidth * widthMultiple1 :
                (numberOfColumns == 2 ? (BaseCamera.screenWidth * widthMultiple2 / numberOfColumns) :
                        (BaseCamera.screenWidth * widthMultiple3 / numberOfColumns));
        double baseRowSize = baseColumnSize * BaseCamera.screenRatio;
        boolean toBeAdjusted = false;
        if (baseRowSize * numberOfRows > BaseCamera.screenHeight * heightCutoff) {
            toBeAdjusted = true;
            baseRowSize = BaseCamera.screenHeight * heightCutoff / numberOfRows;
        }

        if (toBeAdjusted) {
            baseColumnSize = baseRowSize * 1.0 / BaseCamera.screenRatio;
        }
        imageWidth = (int) baseColumnSize;
        imageHeight = (int) baseRowSize;
        logger.trace("    Number of rows in " + this + " = " + numberOfRows + " and number of columns = " + numberOfColumns);
        logger.trace("    ImageHeight in " + this + " = " + imageHeight + " and ImageWidth = " + imageWidth);
        // System.out.println("    ImageHeight in " + this + " = " + imageHeight + " and ImageWidth = " + imageWidth);

    }

    private static void populateCellSizeCombinations() {

        int nRows = 1;
        int nColumns = 2;

        double cSize1 = BaseCamera.screenWidth * widthMultiple1;
        logger.trace("Got screenWidth as " + BaseCamera.screenWidth);
        logger.trace("Got screenHeight as " + BaseCamera.screenHeight);
        double rSize1 = cSize1 * BaseCamera.screenRatio;
        logger.trace("Got screenRatio as " + BaseCamera.screenRatio);
        boolean toBeAdjusted = false;
        if (rSize1 * 1 > BaseCamera.screenHeight * heightCutoff) {
            toBeAdjusted = true;
            rSize1 = BaseCamera.screenHeight * heightCutoff / 1;
        }
        if (toBeAdjusted) {
            cSize1 = (rSize1 / BaseCamera.screenRatio);
        }
        screenCombinations.put(Integer.valueOf((int) cSize1), Integer.valueOf((int) rSize1));
        logger.trace("Added " + (int) cSize1 + " and " + (int) rSize1 + " to Hashtable");

        double cSize2 = BaseCamera.screenWidth * widthMultiple2 / 2;
        double rSize2 = cSize2 * BaseCamera.screenRatio;
        toBeAdjusted = false;
        if (rSize2 * 1 > BaseCamera.screenHeight * heightCutoff / 2) {
            toBeAdjusted = true;
            rSize2 = BaseCamera.screenHeight * heightCutoff / 2;
        }
        if (toBeAdjusted) {
            cSize2 = rSize2 / BaseCamera.screenRatio;
        }
        screenCombinations.put(Integer.valueOf((int) cSize2), Integer.valueOf((int) rSize2));
        logger.trace("Added " + (int) cSize2 + " and " + (int) rSize2 + " to Hashtable");

        double cSize3 = BaseCamera.screenWidth * widthMultiple3 / 3;
        double rSize3 = cSize3 * BaseCamera.screenRatio;
        toBeAdjusted = false;
        if (rSize3 * 1 > BaseCamera.screenHeight * heightCutoff / 3) {
            toBeAdjusted = true;
            rSize3 = BaseCamera.screenHeight * heightCutoff / 3;
        }
        if (toBeAdjusted) {
            cSize3 = rSize3 / BaseCamera.screenRatio;
        }
        screenCombinations.put(Integer.valueOf((int) cSize3), Integer.valueOf((int) rSize3));
        logger.trace("Added " + (int) cSize3 + " and " + (int) rSize3 + " to Hashtable");

        DefaultImages.createImages(screenCombinations);
    }

    protected void createImageAndLogDirectories() {
        logger.trace("Entering createImageDirectory()");
        if (basePathOfPart != null) {
            return;
        }
        baseStringPathOfPart = partId;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String tempDirName = dtf.format(now);
        tempDirName = tempDirName.replace("/","");
        tempDirName = tempDirName.replace(" ","");
        tempDirName = tempDirName.replace(":","");
        basePathOfPart = Paths.get(getBaseDir(), new StringBuilder().append(baseStringPathOfPart).append("/").append(tempDirName).toString());
        try {
            Files.createDirectories(basePathOfPart);
        } catch (FileAlreadyExistsException e) {
            // do nothing
        } catch (IOException e) {
            // something else went wrong
            logger.info(e.getMessage());
        }
        baseStringPathOfPart = basePathOfPart.toAbsolutePath().toString();

        for (InspectionPoint iPoint : inspectionPoints) {
            String baseStringPathOfIPoint = new StringBuilder(baseStringPathOfPart).append("/").append(INSPECTION_POINT + iPoint.getCamera().getCameraID()).toString();
            Path basePathOfIPoint = Paths.get(baseStringPathOfIPoint);
            try {
                Files.createDirectories(basePathOfIPoint);
            } catch (FileAlreadyExistsException e) {
                // do nothing
            } catch (IOException e) {
                // something else went wrong
                logger.info(e.getMessage());
            }
            iPoint.setBasePathOfIPoint(basePathOfIPoint);

            for (ImageProcessor processor : iPoint.getImageProcessors()) {
                BaseImageProcessor bProcessor = (BaseImageProcessor) processor;

                String baseStringPathOfProcessorImages = new StringBuilder().append(baseStringPathOfIPoint).append("/").append(bProcessor.getProcessorName()).append("/").append(pathOfPictures).toString();
                Path basePathOfProcessorImages = Paths.get(baseStringPathOfProcessorImages);
                try {
                    Files.createDirectories(basePathOfProcessorImages);
                } catch (FileAlreadyExistsException e) {
                    // do nothing
                } catch (IOException e) {
                    // something else went wrong
                    logger.info(e.getMessage());
                }
                bProcessor.setBasePathOfProcessorImages(basePathOfProcessorImages);

                String baseStringPathOfProcessorLogs = new StringBuilder().append(baseStringPathOfIPoint).append("/").append(bProcessor.getProcessorName()).append("/").append(pathOfLogs).toString();
                Path basePathOfProcessorLogs = Paths.get(baseStringPathOfProcessorLogs);
                try {
                    Files.createDirectories(basePathOfProcessorLogs);
                } catch (FileAlreadyExistsException e) {
                    // do nothing
                } catch (IOException e) {
                    // something else went wrong
                    logger.info(e.getMessage());
                }
                bProcessor.setBasePathOfProcessorLogs(basePathOfProcessorLogs);
            }
        }
    }

    public String getDisplayText(ArrayList<ArrayList<ProcessingResult>> processingResults) {
        StringBuilder sb = new StringBuilder("");
        int inspectionPoint = 1;
        for (ArrayList<ProcessingResult> pResultArray : processingResults) {
            sb.append(inspectionPoint++).append(". ");
            for (ProcessingResult pResult : pResultArray) {
                ArrayList<ArrayList<String>> inspectionDetails = pResult.getDetails();
                for (ArrayList<String> eachResultBundle : inspectionDetails) {
                    for (String aResult : eachResultBundle) {
                        sb.append(aResult).append(" ");
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public FlowPane getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(FlowPane connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public void updateConnectionStatus() {
        new Thread() {
            public void run() {
                int count = 1;
                while (true) {
                    Platform.runLater(new Runnable() {
                        public void run() {
                            List children = connectionStatus.getChildren();
                            for (int i = children.size() - 1; i >= 0; --i) {
                                connectionStatus.getChildren().remove(i);
                            }
                            int count = 0;
                            for (InspectionPoint inspectionPoint : inspectionPoints) {
                                boolean connected = inspectionPoint.isCameraAlive();
                                Text nameText = new Text("   " + inspectionPoint.getName() + " : ");
                                nameText.setFont(connectionFont);
                                connectionStatus.getChildren().add(nameText);
                                Text aText = new Text();
                                aText.setFont(boldConnectionFont);
                                if (connected) {
                                    aText.setFill(Color.GREEN);
                                    aText.setText("Camera is ON");
                                } else {
                                    aText.setFill(Color.RED);
                                    aText.setText("Camera is OFF");
                                }
                                connectionStatus.getChildren().add(aText);
                                if (count < inspectionPoints.size() - 1) {
                                    connectionStatus.getChildren().add(new Text("  | "));
                                    ++count;
                                }
                            }
                        }
                    });
                    try {
                        TimeUnit time = TimeUnit.SECONDS;
                        time.sleep(count == 1 ? 10 : 20);
                        ++count;
                    } catch (InterruptedException ie) {

                    }
                }
            }
        }.start();
    }

    public HBox getConnectionPane() {
        return connectionPane;
    }

    public String getName() {
        return getPartName();
    }

    public MachiningNode getParent() {
        return this.parent;
    }

    public void setParent(MachiningNode machiningNode) {
        if (parent == null) {
            this.parent = machiningNode;
        }
    }

    public ArrayList<MachiningNode> getChildren() {
        ArrayList<MachiningNode> nodes = new ArrayList<>();
        for (MachiningNode node : inspectionPoints) {
            nodes.add(node);
        }
        return nodes;
    }

}
