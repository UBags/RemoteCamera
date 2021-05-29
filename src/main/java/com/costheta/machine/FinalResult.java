package com.costheta.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FinalResult {

    private static final Logger logger = LogManager.getLogger(FinalResult.class);

    public static final int NOT_EVALUATED = 0;
    public static final int OK = 1;
    public static final int UNSURE = 2;
    public static final int NOT_OK = 3;

    public static final String EMPTY_STRING = "";

    private int okValue = NOT_EVALUATED;
    private String description = EMPTY_STRING;

    private static final String OKString = "Inspection Result is :";
    private static final String separator = "; ";
    private static final String descriptionString = "Details are :";

    public FinalResult(int okValue, String description) {
        logger.trace("Entering contructor()");
        this.okValue = okValue;
        this.description = description;
    }

    public int getOkValue() {
        return okValue;
    }

    public String getDescription() {
        return description;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(OKString);
        builder.append(okValue == 0 ? "Not Evaluated" :
                (okValue == 1 ? "OK" :
                        (okValue == 2 ? "Unsure. Please check manually" :
                                "Not OK")));
        builder.append(separator);
        builder.append(descriptionString);
        builder.append(description);
        return builder.toString();
    }


}
