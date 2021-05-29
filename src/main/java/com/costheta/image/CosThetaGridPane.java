package com.costheta.image;

import com.costheta.machine.BaseCamera;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CosThetaGridPane extends GridPane implements BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(CosThetaGridPane.class);

    private static final String baseName = "CosThetaGridPane";
    private static int count = 1;

    private String name;
    private static final int insetValue = 4;
    private static final int margin = 3;
    private static final int borderWidth = 1;

    private int                 rowCount;
    private int                 columnCount;
    private double              ratio;
    private int                 cellWidth;
    private int                 cellHeight;
    private double              totalHeight;
    private double              totalWidth;

    public CosThetaGridPane(int rowCount, int columnCount, int cellWidth, int cellHeight, double heightToWidthRatio) {
        super();
        logger.trace("After call to super in constructor()");
        name = baseName + count++;

        this.cellHeight = cellHeight;
        this.cellWidth = cellWidth;
        this.ratio = heightToWidthRatio;
        this.rowCount = rowCount;
        this.columnCount = columnCount;

        getStyleClass().add("costheta-gridpane");
        setPadding(new Insets(insetValue, insetValue, insetValue, insetValue));
        setBorder(BLUE_BORDER);
        setBackground(LIGHTGRAY_BACKGROUND);
        setHgap(margin);
        setVgap(margin);
        setAlignment(Pos.CENTER);

        ColumnConstraints columnConstraints = new ColumnConstraints();
        columnConstraints.setPercentWidth(100.0 / columnCount);
        columnConstraints.setFillWidth(false);
        columnConstraints.setHalignment(HPos.CENTER);
        columnConstraints.setHgrow(Priority.NEVER);
        columnConstraints.setMaxWidth(cellWidth);
        columnConstraints.setMinWidth(cellWidth);
        getColumnConstraints().add(columnConstraints);

        RowConstraints rowConstraints = new RowConstraints();
        rowConstraints.setPercentHeight(100.0 / rowCount);
        rowConstraints.setFillHeight(true);
        rowConstraints.setValignment(VPos.CENTER);
        rowConstraints.setVgrow(Priority.NEVER);
        rowConstraints.setMaxHeight(cellHeight);
        rowConstraints.setMinHeight(cellHeight);
        getRowConstraints().add(rowConstraints);

        setGridLinesVisible(false);
        setAlignment(Pos.CENTER);

        totalHeight = computePrefHeight(0.0);
        totalWidth = computePrefWidth(0.0);
        setPrefHeight(totalHeight);
        setPrefWidth(totalWidth);
        setHeight(totalHeight);
        setWidth(totalWidth);
        logger.trace("      Created CosThetaGridPane " + this + " with height " + totalHeight + " and width " + totalWidth);
    }

    public CosThetaGridPane(int rowCount, int columnCount, int cellWidth, int cellHeight) {
        this(rowCount, columnCount, cellWidth, cellHeight, BaseCamera.screenRatio);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = calculateTileHeight() * rowCount;
        // System.out.println("Pref height of CosThetaPane = " + (2 * insetValue + h + 2 * borderWidth));
        return 2 * insetValue + h + 2 * borderWidth;

    }

    @Override
    protected double computePrefWidth(double width) {
        double w = calculateTileWidth() * columnCount;
        // System.out.println("Pref width of CosThetaPane = " + (2 * insetValue + w + 2 * borderWidth));
        return 2 * insetValue + w + 2 * borderWidth;
    }

    private double calculateTileHeight() {
        return (cellHeight +  2 * margin);
    }

    private double calculateTileWidth() {
        return (cellWidth + 2 * margin);
    }

    public double getTotalWidth() {
        return totalWidth;
    }

    public double getTotalHeight() {
        return totalHeight;
    }

    public String toString() {
        return name;
    }

    @Override
    public void addRow(int rowIndex, Node... children) {
        // does nothing
        // you cannot change the dimensions of this Node
    }

    @Override
    public void addColumn(int columnIndex, Node... children) {
        // does nothing
        // you cannot change the dimensions of this Node
    }

    public void add(Node child, int columnIndex, int rowIndex) {
        if (columnIndex >= columnCount) {
            return;
        }
        super.add(child, columnIndex, rowIndex);
    }

    public void add(Node child, int columnIndex, int rowIndex, int colspan, int rowspan) {
        if (columnIndex >= columnCount) {
            return;
        }
        if (rowIndex >= rowCount) {
            return;
        }
        super.add(child, columnIndex, rowIndex, colspan, rowspan);
    }

    public void setCellWidth(int cellWidth) {
        if (this.cellWidth == cellWidth) {
            return;
        }
        this.cellWidth = cellWidth;
        totalWidth = computePrefWidth(0.0);
        setPrefWidth(totalWidth);
        setWidth(totalWidth);
    }

    public void setCellHeight(int cellHeight) {
        if (this.cellHeight == cellHeight) {
            return;
        }
        this.cellHeight = cellHeight;
        totalHeight = computePrefHeight(0.0);
        setPrefHeight(totalHeight);
        setHeight(totalHeight);

    }

}
