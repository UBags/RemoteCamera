package com.costheta.image.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CleaningKernel {

    private static final Logger logger = LogManager.getLogger(CleaningKernel.class);

    public int width;
    public int height;

    public CleaningKernel(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
