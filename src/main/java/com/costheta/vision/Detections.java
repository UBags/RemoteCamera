package com.costheta.vision;

import java.util.ArrayList;
import java.util.List;

public class Detections {

    public List<String> detections = new ArrayList<>();

    public static final Detections EMPTY_DETECTIONS = new Detections(new ArrayList<String>());

    public Detections(List<String> detections) {
        this.detections = detections;
    }
}
