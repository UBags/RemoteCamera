package com.costheta.camera.processor;

import com.costheta.machine.EvaluatedResult;
import com.costheta.machine.ProcessingResult;

import java.util.ArrayList;

public interface ResultEvaluator {
    public EvaluatedResult evaluateResult(ArrayList<ProcessingResult> inputResults);
}
