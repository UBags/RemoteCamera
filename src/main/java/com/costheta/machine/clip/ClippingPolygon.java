package com.costheta.machine.clip;

import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

public class ClippingPolygon {
    public int id;
    public String key;
    public Polygon clippingPolygon;

    public ClippingPolygon(int id, String key, Polygon clippingPolygon) {
        this.id = id;
        this.key = key;
        this.clippingPolygon = clippingPolygon;
    }

    public String description() {
        return ("object@" + String.format("%08X", hashCode()) + "; id : " + id + "; key = " + key + "; clippingPolygon = " + clippingPolygon);
    }

    public String toString() {
        return ("id : " + id + "; key = " + key + "; clippingPolygon = " + clippingPolygon);
    }
}
