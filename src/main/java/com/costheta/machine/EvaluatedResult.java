package com.costheta.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class EvaluatedResult {

    private static final Logger logger = LogManager.getLogger(EvaluatedResult.class);

    ArrayList<ProcessingResult> pResult = new ArrayList<>();

    public EvaluatedResult(ArrayList<ProcessingResult> pResult) {
        this.pResult = pResult;
    }
}
