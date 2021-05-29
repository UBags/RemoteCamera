package com.costheta.image;


import com.costheta.machine.BaseCamera;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;

public class CosThetaPane extends Pane implements BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(CosThetaPane.class);

    private static final String baseName = "CosThetaPane";
    private static int count = 1;

    private String name;
    private static final double DEFAULT_RATIO = 0.618033987;
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

    public CosThetaPane(int rowCount, int columnCount, int cellWidth, int cellHeight, double heightToWidthRatio) {
        logger.trace("Entered constructor()");
        name = baseName + count++;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.cellHeight = cellHeight;
        this.cellWidth = cellWidth;
        this.ratio = heightToWidthRatio;
        getStyleClass().add("costheta-tilepane");
        setPadding(new Insets(5));
        setBorder(BLUE_BORDER);
        setBackground(LIGHTGRAY_BACKGROUND);
//        setHgap(margin);
//        setVgap(margin);
        totalHeight = computePrefHeight(0.0);
        totalWidth = computePrefWidth(0.0);
        setPrefHeight(totalHeight);
        setPrefWidth(totalWidth);
        setHeight(totalHeight);
        setWidth(totalWidth);
        logger.trace("      Created CosThetaPane " + this + " with height " + totalHeight + " and width " + totalWidth);
    }

    public CosThetaPane(int rowCount, int columnCount, int cellWidth, int cellHeight) {
        this(rowCount, columnCount, cellWidth, cellHeight, BaseCamera.screenRatio);
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public void setHeightToWidthRatio(double ratio) {
        this.ratio = ratio;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected void layoutChildren() {

        totalHeight = computePrefHeight(0.0);
        totalWidth = computePrefWidth(0.0);
        setPrefHeight(totalHeight);
        setPrefWidth(totalWidth);
        setHeight(totalHeight);
        setWidth(totalWidth);
        // System.out.println("Rejigged CosThetaPane " + this + " to height " + totalHeight + " and width " + totalWidth);

        // double left = getInsets().getLeft() + borderWidth;
        // double top = getInsets().getTop() + borderWidth;

        double left = insetValue + margin + borderWidth - 3;
        double top = insetValue + margin + borderWidth - 3;


        double tileWidth = calculateTileWidth();
        double tileHeight = calculateTileHeight();

        ObservableList<Node> children = getChildren();
        double currentX = left;
        double currentY = top;
        for (int idx = 0; idx < children.size(); idx++) {
            if (idx > 0 && idx % columnCount == 0) {
                currentX = left;
                currentY = currentY + tileHeight;
            }
            children.get(idx).resize(tileWidth, tileHeight);
            children.get(idx).relocate(currentX, currentY);
            currentX = currentX + tileWidth;
        }
        setHeight(totalHeight);
        setWidth(totalWidth);
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

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnCount;
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


}