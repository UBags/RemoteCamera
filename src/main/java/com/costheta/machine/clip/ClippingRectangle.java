package com.costheta.machine.clip;

import javafx.scene.shape.Rectangle;

public class ClippingRectangle {
    public int id;
    public String key;
    public Rectangle clippingRectangle;

    public ClippingRectangle(int id, String key, Rectangle clippingRectangle) {
        ClippingRectangle.this.id = id;
        ClippingRectangle.this.key = key;
        ClippingRectangle.this.clippingRectangle = clippingRectangle;
    }

    public String description() {
        return ("id : " + id + "; key = " + key + "; clippingRectangle = " + clippingRectangle);
    }

    public String toString() {
        return ("id : " + id + "; key = " + key + "; clippingRectangle = " + clippingRectangle);
    }
}
