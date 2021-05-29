package com.costheta.image;

import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.List;

public interface BasePaneConstants extends FontConstants {

    public static final Color LIGHT_GREEN = Color.color(96 * 1.0/255, 200* 1.0/255, 120* 1.0/255);
    public static final Color GREEN = Color.color(32 * 1.0/255, 180* 1.0/255, 80* 1.0/255);
    public static final Color VERY_LIGHT_GRAY = Color.rgb(220,230,244);
    public static final Color LIGHT_GRAY = Color.rgb(204,216,232);
    public static final Color MEDIUM_GRAY = Color.rgb(156,164,204);
    public static final Color DARK_GRAY = Color.rgb(96,116,164);
    public static final Color VERY_LIGHT_YELLOW = Color.rgb(240,240,204);
    public static final Color LIGHT_YELLOW = Color.rgb(240,228,176);
    public static final Color MEDIUM_YELLOW = Color.rgb(255,200,20);
    public static final Color DARK_YELLOW = Color.rgb(216,164,0);
    public static final Color BLACK = Color.web("0x444444",1.0);
    public static final Color BLUE = Color.web("0x0000ff",1.0);
    public static final Color LIGHT_BLUE = Color.web("0x80ffff",1.0);
    public static final Color WHITE = Color.web("0xf8f8f8",1.0);
    public static final Color LIGHT_PINK = Color.web("#ffccff", 1.0);
    public static final Color LIGHT_AQUA = Color.rgb(165,210,210);
    public static final Color LIGHT_ORANGE = Color.rgb(255,190  ,130);
    public static final Color BRIGHT_PINK = Color.rgb(255,128  ,255);
    public static final Color LIGHT_RED = Color.rgb(255,164,164);
    public static final Color LIGHT_VIOLET = Color.rgb(164,164,212);

    public static final double DEFAULT_RATIO = 0.618033987;
    public static final int DEFAULT_PADDING = 5;
    public static final int DEFAULT_INSET_VALUE = 5;
    public static final int DEFAULT_MARGIN = 5;
    public static final int DEFAULT_BORDER_WIDTH_1 = 1;
    public static final int DEFAULT_BORDER_WIDTH_2 = 2;
    public static final int DEFAULT_CORNER_RADII_1 = 5;
    public static final int DEFAULT_CORNER_RADII_2 = 10;

    public static final StrokeType DEFAULT_STROKE_TYPE_INSIDE     = StrokeType.INSIDE;
    public static final StrokeType DEFAULT_STROKE_TYPE_OUTSIDE     = StrokeType.OUTSIDE;
    public static final StrokeLineJoin DEFAULT_STROKE_LINE_JOIN_MITER = StrokeLineJoin.MITER;
    public static final StrokeLineJoin DEFAULT_STROKE_LINE_JOIN_BEVEL = StrokeLineJoin.BEVEL;
    public static final StrokeLineCap DEFAULT_STROKELINE_CAP_BUTT  = StrokeLineCap.BUTT;
    public static final StrokeLineCap DEFAULT_STROKELINE_CAP_ROUND  = StrokeLineCap.ROUND;

    public static final double DEFAULT_MITER_LIMIT = 10;
    public static final double DEFAULT_DASH_OFFSET = 0;
    public static final List<Double> DEFAULT_DASH_ARRAY = null;

    public static final BorderStrokeStyle DEFAULT_BORDER_STROKE_STYLE_1 =
            new BorderStrokeStyle( DEFAULT_STROKE_TYPE_INSIDE, DEFAULT_STROKE_LINE_JOIN_MITER,
                    DEFAULT_STROKELINE_CAP_BUTT, DEFAULT_MITER_LIMIT, DEFAULT_DASH_OFFSET, DEFAULT_DASH_ARRAY);

    public static final BorderStrokeStyle DEFAULT_BORDER_STROKE_STYLE_2 =
            new BorderStrokeStyle( DEFAULT_STROKE_TYPE_OUTSIDE, DEFAULT_STROKE_LINE_JOIN_BEVEL,
                    DEFAULT_STROKELINE_CAP_ROUND, DEFAULT_MITER_LIMIT, DEFAULT_DASH_OFFSET, DEFAULT_DASH_ARRAY);

    public static final BorderStroke DEFAULT_BORDER_SROKE_1 =
            new BorderStroke( Color.valueOf("044fdf"), DEFAULT_BORDER_STROKE_STYLE_1,
                    new CornerRadii(10), new BorderWidths(DEFAULT_BORDER_WIDTH_1));

    public static final BorderStroke DEFAULT_BORDER_SROKE_2 =
            new BorderStroke( Color.valueOf("5f9206"), DEFAULT_BORDER_STROKE_STYLE_2,
                    new CornerRadii(5), new BorderWidths(DEFAULT_BORDER_WIDTH_2));


    public static final Border BLUE_BORDER = new Border(DEFAULT_BORDER_SROKE_1);
    public static final Border GREEN_BORDER = new Border(DEFAULT_BORDER_SROKE_2);

    public static final BackgroundFill LIGHTPINK_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#ffccff"),
                    new CornerRadii(DEFAULT_CORNER_RADII_1),
                    new Insets(2)
            );

    public static final BackgroundFill VERYLIGHTPINK_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#ffeeff"),
                    new CornerRadii(DEFAULT_CORNER_RADII_2),
                    new Insets(2)
            );

    public static final BackgroundFill LIGHTGREEN_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#baf7da"),
                    new CornerRadii(DEFAULT_CORNER_RADII_1),
                    new Insets(2)
            );

    public static final BackgroundFill LIGHT_BLUE_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#cfeafa"),
                    new CornerRadii(DEFAULT_CORNER_RADII_2),
                    new Insets(2)
            );

    public static final BackgroundFill DARKGRAY_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#6e6e6e"),
                    new CornerRadii(DEFAULT_CORNER_RADII_1),
                    new Insets(2)
            );

    public static final BackgroundFill LIGHTGRAY_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#dfdfdf"),
                    new CornerRadii(DEFAULT_CORNER_RADII_1),
                    new Insets(2)
            );

    public static final BackgroundFill VERYLIGHTGRAY_BACKGROUND_FILL =
            new BackgroundFill(
                    Color.valueOf("#efefef"),
                    new CornerRadii(DEFAULT_CORNER_RADII_2),
                    new Insets(2)
            );

    public static final BackgroundFill WHITE_BACKGROUND_FILL = new BackgroundFill(
            Color.valueOf("#ffffff"),
            new CornerRadii(DEFAULT_CORNER_RADII_2),
            new Insets(2)
    );

    public static final Background VERYLIGHTPINK_BACKGROUND =
            new Background(VERYLIGHTPINK_BACKGROUND_FILL, VERYLIGHTPINK_BACKGROUND_FILL);

    public static final Background LIGHTGREEN_BACKGROUND =
            new Background(LIGHTGREEN_BACKGROUND_FILL, LIGHTGREEN_BACKGROUND_FILL);

    public static final Background LIGHTBLUE_BACKGROUND =
            new Background(LIGHT_BLUE_BACKGROUND_FILL, LIGHT_BLUE_BACKGROUND_FILL);

    public static final Background DARKGRAY_BACKGROUND =
            new Background(DARKGRAY_BACKGROUND_FILL, DARKGRAY_BACKGROUND_FILL);

    public static final Background LIGHTGRAY_BACKGROUND =
            new Background(LIGHTGRAY_BACKGROUND_FILL, LIGHTGRAY_BACKGROUND_FILL);

    public static final Background VERYLIGHTGRAY_BACKGROUND =
            new Background(VERYLIGHTGRAY_BACKGROUND_FILL, VERYLIGHTGRAY_BACKGROUND_FILL);

    public static final Background WHITE_BACKGROUND =
            new Background(WHITE_BACKGROUND_FILL, WHITE_BACKGROUND_FILL);

    public static final Background BLUE_BACKGROUND = new Background(new BackgroundFill(Color.BLUE, new CornerRadii(1),
            new Insets(1)));
    public static final Background RED_BACKGROUND = new Background(new BackgroundFill(Color.RED, new CornerRadii(1),
            new Insets(1)));
    public static final Background GREEN_BACKGROUND = new Background(new BackgroundFill(GREEN, new CornerRadii(1),
            new Insets(1)));
    public static final Background BLACK_BACKGROUND = new Background(new BackgroundFill(Color.BLACK, new CornerRadii(1),
            new Insets(1)));

    public static final Background GRAYISH_BACKGROUND = new Background(new BackgroundFill(LIGHT_GRAY, new CornerRadii(1),
            new Insets(1)));
    public static final Background MEDIUM_GRAYISH_BACKGROUND = new Background(new BackgroundFill(MEDIUM_GRAY, new CornerRadii(1),
            new Insets(1)));
    public static final Background DARK_GRAYISH_BACKGROUND = new Background(new BackgroundFill(DARK_GRAY, new CornerRadii(1),
            new Insets(1)));

    public static final Background YELLOWISH_BACKGROUND = new Background(new BackgroundFill(VERY_LIGHT_YELLOW, new CornerRadii(1),
            new Insets(1)));
    public static final Background MEDIUM_YELLOWISH_BACKGROUND = new Background(new BackgroundFill(MEDIUM_YELLOW, new CornerRadii(1),
            new Insets(1)));
    public static final Background DARK_YELLOWISH_BACKGROUND = new Background(new BackgroundFill(DARK_YELLOW, new CornerRadii(1),
            new Insets(1)));


}
