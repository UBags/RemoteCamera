package com.costheta.image;

public enum TraceLevel { TRACE(1), DEBUG(2), INFO(3), WARN(4), ERROR(5), FATAL(6);
    private int value;

    public int getValue() {
        return value;
    }

    private TraceLevel(int aValue) {
        this.value = aValue;
    }

    public String toString() {
        return new StringBuilder().append(value).toString();
    }
};
