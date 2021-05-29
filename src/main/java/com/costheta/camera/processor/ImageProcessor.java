package com.costheta.camera.processor;

import com.costheta.machine.MachiningNode;
import com.costheta.machine.ProcessingResult;

import java.awt.image.BufferedImage;

public interface ImageProcessor extends MachiningNode {

    public static final String EMPTY_STRING = "";

    ProcessingResult process(BufferedImage image);

}
