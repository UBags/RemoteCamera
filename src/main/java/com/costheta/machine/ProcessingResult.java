package com.costheta.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import javafx.scene.shape.Rectangle;

public class ProcessingResult {

    private static final Logger logger = LogManager.getLogger(ProcessingResult.class);

    public static final String BAD_RESULT = "NO";
    public static final String GOOD_RESULT = "YES";
    public static final String UNKNOWN_RESULT = "CHECK MANUALLY";

    public static final ProcessingResult EMPTY_PROCESSING_RESULT = new ProcessingResult(new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<Rectangle>>(), new ArrayList<ArrayList<String>>(), false);

    private ArrayList<ArrayList<Rectangle>> okBoxes;
    private ArrayList<ArrayList<Rectangle>> errorBoxes;
    private ArrayList<ArrayList<Rectangle>> unclearBoxes;
    private ArrayList<ArrayList<String>> details;

    private boolean processedResult;
    private String result = BAD_RESULT;

    public ProcessingResult(ArrayList<ArrayList<Rectangle>> okBoxes, ArrayList<ArrayList<Rectangle>> errorBoxes, ArrayList<ArrayList<Rectangle>> unclearBoxes, ArrayList<ArrayList<String>> details, boolean processedResult) {
        this.processedResult = processedResult;
        this.okBoxes = okBoxes;
        this.errorBoxes = errorBoxes;
        this.unclearBoxes = unclearBoxes;
        this.details = details;
        if (processedResult) {
            if (details.size() == 0) {
                result = GOOD_RESULT;
            } else {
                result = UNKNOWN_RESULT;
            }
        } else {
            result = BAD_RESULT;
        }
    }

    public String getResult() {
        return result;
    }

    public boolean getProcessedResult() {
        return processedResult;
    }

    public ArrayList<ArrayList<String>> getDetails() {
        return details;
    }

    public ArrayList<ArrayList<Rectangle>> getUnclearBoxes() {
        return unclearBoxes;
    }

    public ArrayList<ArrayList<Rectangle>> getErrorBoxes() {
        return errorBoxes;
    }

    public ArrayList<ArrayList<Rectangle>> getOkBoxes() {
        return okBoxes;
    }

}
