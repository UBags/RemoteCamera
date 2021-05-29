package com.costheta.text.utils;

public class MatchedString {
    public String string;
    public int score;

    private static final String startString = "[";
    private static final String endString = "]";
    private static final String comma = ",";


    public MatchedString(String string, int score) {
        this.string = string;
        this.score = score;
    }

    public String toString() {
        return startString + string + comma + score + endString;
    }
}
