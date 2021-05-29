package com.costheta.camera.processor;

import com.costheta.machine.ProcessingResult;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javafx.scene.shape.Rectangle;

public interface ClipImageProcessor extends ImageProcessor {

    ProcessingResult process(BufferedImage image);

    ArrayList<Rectangle> getClipBoxRectangles();

}
