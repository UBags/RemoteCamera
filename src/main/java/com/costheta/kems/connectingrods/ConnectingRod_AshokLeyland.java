package com.costheta.kems.connectingrods;

import com.costheta.machine.EvaluatedResult;
import com.costheta.machine.FinalResult;
import com.costheta.machine.Part;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class ConnectingRod_AshokLeyland extends Part {

    private static final Logger logger = LogManager.getLogger(ConnectingRod_AshokLeyland.class);

    public ConnectingRod_AshokLeyland(String partName, String partId, int numberOfInspectionPoints, String description) {
        super(partName, partId, numberOfInspectionPoints, description);
        logger.trace("After calling super() in constructor");
    }

    public ConnectingRod_AshokLeyland(String partName, String partId, int numberOfInspectionPoints) {
        this(partName, partId, numberOfInspectionPoints, EMPTY_STRING);

    }
    @Override
    public FinalResult tallyEvaluations(ArrayList<EvaluatedResult> eResults) {
        logger.trace("Entering tallyEvaluations()");
        return null;
    }
}
