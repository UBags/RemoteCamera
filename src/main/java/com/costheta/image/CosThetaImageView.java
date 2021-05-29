package com.costheta.image;

import com.costheta.machine.BaseCamera;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CosThetaImageView extends HBox implements BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(CosThetaImageView.class);

    private static String baseName = "CosThetaImageView";
    private static int count = 1;

    private String              name;
    private double              ratio;
    private int                 cellWidth;
    private int                 cellHeight;
    private double              totalHeight;
    private double              totalWidth;
    private ImageView           imageView;

    private int padding = 2;
    private int margin = 2;
    private Border border = GREEN_BORDER;

    public CosThetaImageView(int cellWidth, int cellHeight, ImageView iView) {
        super();
        logger.trace("Entered constructor()");
        name = baseName + count++;

        this.cellHeight = cellHeight;
        this.cellWidth = cellWidth;
        this.ratio = (BaseCamera.screenHeight * 1.0) / BaseCamera.screenWidth;

        if (iView == null) {
            this.imageView = new ImageView();
        } else {
            this.imageView = iView;
        }
        this.imageView.setPreserveRatio(true);
        this.imageView.setSmooth(false);
        this.imageView.setFitHeight(this.cellHeight);
        this.imageView.setFitWidth(this.cellWidth);

        getStyleClass().add("costheta-imageview");

        setPadding(new Insets(padding));
        setBorder(border);
        setBackground(WHITE_BACKGROUND);
        setAlignment(Pos.CENTER);

        VBox holder = new VBox();
        // holder.setPadding(new Insets(0));
        holder.setAlignment(Pos.CENTER);
        holder.getChildren().add(this.imageView);
        VBox.setMargin(this.imageView, new Insets(margin));

        getChildren().add(holder);
        HBox.setMargin(holder, new Insets(margin));

        this.totalHeight = computePrefHeight(0.0);
        this.totalWidth = computePrefWidth(0.0);
        setPrefHeight(totalHeight);
        setPrefWidth(totalWidth);
        setHeight(totalHeight);
        setWidth(totalWidth);

        logger.trace("Setup and added imageView " + this.imageView + " in " + this + " with width = " + this.cellWidth + " and height " + this.cellHeight);
        logger.trace("Current dimensions of " + this + " is width = " + this.totalWidth + " and height " + this.totalHeight);
        logger.trace("Current dimensions of " + this + " is width = " + this.totalWidth + " and height " + this.totalHeight);
    }

    public CosThetaImageView(int cellWidth, int cellHeight) {
        this(cellWidth, cellHeight, null);
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    public void setImage(Image anImage) {
        imageView.setImage(anImage);
        // centerImage();
    }

    public Image getImage() {
        return imageView.getImage();
    }

    public ImageView getImageView() {
        return imageView;
    }


//    @Override
//    protected void layoutChildren() {
//        double left = margin + border.getInsets().getLeft() + padding;
//        double top = margin + border.getInsets().getTop() + padding;
//
//        imageView.resize(cellWidth, cellHeight);
//        imageView.relocate(left, top);
//    }

//    @Override
//    protected void layoutChildren() {
//        super.layoutChildren();
//        centerImage();
//    }


    @Override
    protected double computePrefHeight(double height) {
        double h = calculateTileHeight();
        // System.out.println("Pref Height = " + h);
        return h + 2 * margin + 2 * padding + 2 * 1; // borderWidth = 1;
    }

    @Override
    protected double computePrefWidth(double width) {
        double w = calculateTileWidth();
        // System.out.println("Pref Width = " + w);
        return w + 2 * margin + 2 * padding + 2 * 1; // borderWidth = 1;
    }

    private double calculateTileHeight() {
        return (cellHeight + 2 * margin);
        // System.out.println("border.getInsets().getTop() = " + border.getInsets().getTop());
        // return (cellHeight +  2 * padding + 4 + 2 * margin);
    }

    private double calculateTileWidth() {

        return (cellWidth + 2 * margin);
    }

    public int getTotalHeight() {
        return (int) totalHeight;
    }

    public int getTotalWidth() {
        return (int) totalWidth;
    }

    public String toString() {
        return name;
    }

    public void centerImage() {
        Image img = imageView.getImage();
        if (img != null) {
            double w = 0;
            double h = 0;

            double ratioX = imageView.getFitWidth() / img.getWidth();
            double ratioY = imageView.getFitHeight() / img.getHeight();

            double reducCoeff = 0;
            if(ratioX >= ratioY) {
                reducCoeff = ratioY;
            } else {
                reducCoeff = ratioX;
            }

            w = img.getWidth() * reducCoeff;
            h = img.getHeight() * reducCoeff;

            imageView.setX((imageView.getFitWidth() - w) / 2 + 20);
            imageView.setY((imageView.getFitHeight() - h) / 2 + 4);

        }
    }

}
