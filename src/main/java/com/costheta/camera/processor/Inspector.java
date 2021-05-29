package com.costheta.camera.processor;

import com.costheta.machine.ProcessingResult;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public interface Inspector {

    // Inspects a Part at a particular InspectionPoint, and returns the result.
    // Interpretation of the result as "OK" or "Not OK" is left to the Part for implementation
    public ArrayList<ProcessingResult> inspect();

    // After getting the result of inspection, one may need to update the input image
    // with the results e.g. if letters are found, then it draws the bounding boxes,
    // or if a 2D matrix is found, it draws the outer boundary of the 2D matrix,
    // or if a profile is matched, it highlights the profile and errors in it
    // This method does this updation and returns the modified image
    public BufferedImage updateImage(BufferedImage inputImage, ArrayList<ProcessingResult> result);
}
