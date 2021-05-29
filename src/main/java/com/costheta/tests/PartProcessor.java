package com.costheta.tests;

import com.costheta.text.utils.CharacterUtils;
import com.costheta.image.utils.ImageUtils;
import com.costheta.tesseract.CosThetaTesseract;
import com.costheta.tesseract.CosThetaTesseractHandlePool;
import com.costheta.tesseract.TesseractInitialiser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class PartProcessor {

    private static final Logger logger = LogManager.getLogger(PartProcessor.class);

    protected ArrayList<String> patterns; // e.g. "DDDDD", "AADD", "DDA" map to 04439, KA20, 10I respectively

    // For each String pattern, we generate a few regexPatterns.
    // Each regex pattern looks for the boundary of crossover from a Numeric to Alphabet and vice-versa
    // TBD
    private ArrayList<HashMap<Integer, Pattern>> regexPatterns;

    public static String datapath = "E:/TechWerx/CosTheta/IntelliJ IDEA Projects/RemoteCamera/External Libraries/tessdata";

    static {
        TesseractInitialiser.initialise();
        CosThetaTesseract.changeDatapath(datapath);
        CosThetaTesseractHandlePool.initialise();
        ImageUtils.initialise();
    }

    public PartProcessor(ArrayList<String> patterns) {
        this.patterns = patterns;
    }

    public void setPatterns(ArrayList<String> patterns) {
        // these patterns will be used to figure out what information is missing from the found result
        this.patterns = patterns;
        for (String pattern : patterns) {
            // look for places where D's and A's changeover
        }
    }

    public ArrayList<String> getPatterns() {
        return this.patterns;
    }

    public ArrayList<String> rectifyResults(ArrayList<String> initialResult) {

        if (initialResult.size() != patterns.size()) {
            return initialResult;
        }
        ArrayList<String> finalResult = new ArrayList<>();
        for (int i = 0; i < initialResult.size(); ++i) {
            String rectifiedOutcome = CharacterUtils.matchStringToPattern(initialResult.get(i), patterns.get(i));
            finalResult.add(rectifiedOutcome);
        }
        return finalResult;
    }

    public abstract void process(BufferedImage image);
}
