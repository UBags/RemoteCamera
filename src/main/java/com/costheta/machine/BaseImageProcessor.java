package com.costheta.machine;

import com.costheta.camera.processor.ImageProcessor;
import com.costheta.image.TraceLevel;
import com.costheta.image.utils.ImageUtils;
import com.costheta.tesseract.TesseractUtils;
import com.costheta.text.utils.CharacterUtils;
import com.costheta.text.utils.MatchedString;
import com.costheta.text.utils.PatternMatchedStrings;
import javafx.geometry.Dimension2D;
import javafx.scene.shape.Rectangle;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseImageProcessor implements ImageProcessor {

    private static final Logger logger = LogManager.getLogger(BaseImageProcessor.class);
    protected static ExecutorService parallelThreadPool = Executors.newFixedThreadPool(12);

    private static final ArrayList<MachiningNode> children = new ArrayList<>();
    private static final HashMap<String, BaseImageProcessor> processorRegistry = new HashMap();

    private static final String IDL_KEY = "idl";

    protected static TraceLevel imageDebugLevel = TraceLevel.FATAL;
    public static TraceLevel getImageDebugLevel() {
        return imageDebugLevel;
    }

    protected String processorName;
    private MachiningNode parent;

    protected ArrayList<String> patterns; // e.g. "DDDDD", "AADD", "DDA" map to 04439, KA20, 10I respectively

    // For each String pattern, we generate a few regexPatterns.
    // Each regex pattern looks for the boundary of crossover from a Numeric to Alphabet and vice-versa
    // TBD

    protected int assessmentRegions;
    protected ArrayList<Pattern> regexPatterns = new ArrayList<>();

    private String baseStringPathOfProcessorImages = null;
    private Path basePathOfProcessorImages = null;

    private String baseStringPathOfProcessorLogs = null;
    private Path basePathOfProcessorLogs = null;

    private String initialKeyString;

    // hack to ensure that it can be extended by subclasses that do not
    // explicity make a call to super() in the constructor
    protected BaseImageProcessor() {

    }

    public BaseImageProcessor(String name) {
        this(name, null, 1);
     }

    public BaseImageProcessor(String name, ArrayList<String> patterns) {
        this(name, patterns, 1);
    }

    public BaseImageProcessor(String name, ArrayList<String> patterns, int assessmentRegions) {
        logger.trace("Entering constructor()");
        if (name == null) {
            throw new IllegalArgumentException("The name of an Image Processor cannot be null");
        }
//        if (name == null) {
//            this.processorName = "";
//        } else {
//            this.processorName = name;
//        }
        this.processorName = name;
        if (patterns == null) {
            this.patterns = new ArrayList<>();
        } else {
            setPatterns(patterns);
        }
        if (assessmentRegions > 1) {
            this.assessmentRegions = assessmentRegions;
        } else {
            this.assessmentRegions = 1;
        }
        logger.trace("Set assessment regions to " + this.assessmentRegions + " in " + this.getName() + " Object id " + this);
        setImageDebugLevel();
    }

    public void setPatterns(ArrayList<String> patterns) {
        logger.trace("Entering setPatterns()");
        regexPatterns.clear();
        // these patterns will be used to figure out what information is missing from the found result
        this.patterns = patterns;
        int index = 0;
//        for (String pattern : patterns) {
//            ArrayList<Pattern> thePatterns = new ArrayList<>();
//            StringBuilder patternString = new StringBuilder("");
//            char[] chars = pattern.toCharArray();
//            for (char c : chars) {
//                if (c == 'A') {
//                    patternString.append("[A-Za-z]");
//                }
//                if (c == 'D') {
//                    patternString.append("[0-9]");
//                }
//            }
//            String thisPattern = patternString.toString();
//            Pattern aPattern = Pattern.compile(thisPattern);
//            thePatterns.add(aPattern);
//            regexPatterns.put(index,thePatterns);
//        }

        for (String pattern : patterns) {
            // StringBuilder patternString = new StringBuilder("\\"); only needed for '\\d', \\s' etc
            StringBuilder patternString = new StringBuilder("");
            char[] chars = pattern.toCharArray();
            for (char c : chars) {
                if (c == 'A') {
                    patternString.append("[A-Za-z]");
                }
                if (c == 'D') {
                    patternString.append("[0-9]");
                }
            }
            String thisPattern = patternString.toString();
            Pattern aPattern = Pattern.compile(thisPattern);
            regexPatterns.add(aPattern);
        }

    }

    public ArrayList<String> getPatterns() {
        return this.patterns;
    }

    public ArrayList<String> rectifyResults(ArrayList<String> initialResult) {
        logger.trace("Entering rectifyResults()");
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

    protected void pixWrite(String fileName, Pix pix, int pixType, TraceLevel debugLevel) {
        // System.out.println("Entered pixWrite");
        if (debugLevel.getValue() >= imageDebugLevel.getValue()) {
            // System.out.println("About to output pix to " + baseStringPathOfProcessorImages + "/" + fileName);
            Leptonica1.pixWrite(baseStringPathOfProcessorImages + "/" + fileName, pix, pixType);
        }
    }


    protected void imageWrite(String fileName, BufferedImage image, int imageType, TraceLevel debugLevel) {
        // System.out.println("Entered imageWrite");
        if (debugLevel.getValue() >= imageDebugLevel.getValue()) {
            // System.out.println("About to output image to " + baseStringPathOfProcessorImages + "/" + fileName);
            ImageUtils.writeFile(image, (imageType == BaseCamera.JPEG ? "jpg" :(imageType == BaseCamera.PNG ? "png" : "jpg")), baseStringPathOfProcessorImages + "/" + fileName);
        }
    }

    public abstract ProcessingResult process(BufferedImage image);

    protected String getBaseStringPathOfProcessorImages() {
        return baseStringPathOfProcessorImages;
    }

    protected Path getBasePathOfProcessorImages() {
        return basePathOfProcessorImages;
    }

    protected void setBasePathOfProcessorImages(Path basePathOfProcessorImages) {
        if (this.basePathOfProcessorImages != null) {
            return;
        }
        this.basePathOfProcessorImages = basePathOfProcessorImages;
        this.baseStringPathOfProcessorImages = this.basePathOfProcessorImages.toAbsolutePath().toString();
    }

    protected Path getBasePathOfProcessorLogs() {
        return basePathOfProcessorLogs;
    }

    protected void setBasePathOfProcessorLogs(Path basePathOfProcessorLogs) {
        if (this.basePathOfProcessorLogs != null) {
            return;
        }
        this.basePathOfProcessorLogs = basePathOfProcessorLogs;
        this.baseStringPathOfProcessorLogs = basePathOfProcessorLogs.toAbsolutePath().toString();
    }

    protected String getBaseStringPathOfProcessorLogs() {
        return baseStringPathOfProcessorLogs;
    }

    public String getProcessorName() {
        return processorName;
    }

    protected void setImageDebugLevel() {
        int level = Integer.parseInt(System.getProperty(IDL_KEY, "1000"));
        if (level >= 6) {
            imageDebugLevel = TraceLevel.FATAL;
        } else {
            if (level <= 1) {
                imageDebugLevel = TraceLevel.TRACE;
            } else {
                switch(level){
                    case 2:
                        imageDebugLevel = TraceLevel.DEBUG;
                        break;
                    case 3:
                        imageDebugLevel = TraceLevel.INFO;
                        break;
                    case 4:
                        imageDebugLevel = TraceLevel.WARN;
                        break;
                    case 5:
                        imageDebugLevel = TraceLevel.ERROR;
                        break;
                    default:
                        imageDebugLevel = TraceLevel.FATAL;
                }
            }
        }
        logger.warn("Current image debug level is " + imageDebugLevel);
    }

    protected int getMinimumCharactersPerLine() {
        if (patterns.size() == 0) {
            return 1;
        }
        int min = Integer.MAX_VALUE;
        for (String pattern : patterns) {
            min = Math.min(min, pattern.length());
        }
        return min;
    }

    protected final void drawBoundingBoxesOnPix(Pix pix, ArrayList<ArrayList<Rectangle>> words, String fileName, TraceLevel debugLevel) {
        if (debugLevel.getValue() < imageDebugLevel.getValue()) {
            return;
        }
        L_Bmf bbNumberingFont = Leptonica1.bmfCreate(null, 4);
        Pix pix1 = Leptonica1.pixConvertTo32(pix);
        int[] colors = {0xFF000000,0x00FF0000,0x0000FF00};
        byte[] rval = {(byte) 0xFF,0x00,0x00};
        byte[] gval = {0x00,(byte)0xFF,0x00};
        byte[] bval = {0x00,0x00,(byte)0xFF};
        int lineNo = 1;
        for (ArrayList<Rectangle> word : words) {
            int wordNo = 1;
            for (Rectangle letterBox : word) {
                Box box = Leptonica1.boxCreate((int) letterBox.getX(), (int) letterBox.getY(), (int) letterBox.getWidth(), (int) letterBox.getHeight());
                Leptonica1.pixRenderBox(pix1, box, 1, ILeptonica.L_FLIP_PIXELS);
                Leptonica1.pixRenderBoxArb(pix1, box, 1, rval[lineNo % rval.length], gval[lineNo % gval.length], bval[lineNo % bval.length]);
                Leptonica1.pixSetTextline(pix1, bbNumberingFont, lineNo + "." + wordNo, colors[lineNo % colors.length], (int) letterBox.getX(),
                        (int) letterBox.getY() - 1, null, null);
                LeptUtils.dispose(box);
                ++wordNo;
            }
            ++lineNo;
        }
        // Leptonica1.pixSetTextline(pix1, bbNumberingFont, identifier, 0xFF000000, 3, 10, null, null);
        pixWrite(fileName, pix1, ILeptonica.IFF_PNG, debugLevel);
        LeptUtils.dispose(pix1);
        LeptUtils.dispose(bbNumberingFont);
    }

    protected ArrayList<ArrayList<Rectangle>> getLines(Pix rotatedPix, double minLineHeightOverlap, double maxWidthGapMultiple, Dimension2D characterDimensions, int imageCounter, int drawingSeries, TraceLevel debugLevel) {
        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, (int) (characterDimensions.getHeight() / 2), (int) (characterDimensions.getWidth() / 4), getMinimumCharactersPerLine(), (int) characterDimensions.getHeight(), (int) characterDimensions.getWidth());
        logger.debug("Lines found from rotatedPix are : " + lines);
        drawBoundingBoxesOnPix(rotatedPix, lines, imageCounter + " - " + drawingSeries + "A - boundingBoxes.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> tempLines = TesseractUtils.sortLines(lines, true);
        ArrayList<ArrayList<Rectangle>> mergedLines = TesseractUtils.mergeLinesBasedOnYOverlap(tempLines, minLineHeightOverlap); // usually 0.375
        ArrayList<ArrayList<Rectangle>> orderedAndMergedLines = TesseractUtils.sortLines(mergedLines, false);
        drawBoundingBoxesOnPix(rotatedPix, orderedAndMergedLines, imageCounter + " - " + drawingSeries + "B - orderedAndMergedLines.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> noOverlappingBoxLines = TesseractUtils.dropOverlappingBoxesInLines(orderedAndMergedLines, characterDimensions.getHeight() * characterDimensions.getWidth() * 0.075);
        drawBoundingBoxesOnPix(rotatedPix, noOverlappingBoxLines, imageCounter + " - " + drawingSeries + "C - overlappingBoxesDropped.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> okSizedBoxes = TesseractUtils.dropShortOrWideBoxes(noOverlappingBoxLines, characterDimensions.getHeight() * 0.5, characterDimensions.getWidth() * 2.1);
        drawBoundingBoxesOnPix(rotatedPix, okSizedBoxes, imageCounter + " - " + drawingSeries + "D - shortAndWideDropped.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> linesSplit = TesseractUtils.splitLinesBasedOnGap(okSizedBoxes, characterDimensions.getWidth() * maxWidthGapMultiple);
        drawBoundingBoxesOnPix(rotatedPix, linesSplit, imageCounter + " - " + drawingSeries + "E - splitLines.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> linesWithMinimumLength = TesseractUtils.dropLinesWithInsufficientBoxes(linesSplit, getMinimumCharactersPerLine());
        drawBoundingBoxesOnPix(rotatedPix, linesWithMinimumLength, imageCounter + " - " + drawingSeries + "F - dropSmallLines.png", debugLevel);

        return linesWithMinimumLength;
    }

    protected ArrayList<ArrayList<Rectangle>> getWord(Pix rotatedPix, double minLineHeightOverlap, double maxWidthGapMultiple, Dimension2D characterDimensions, int runCounter, int drawingSeries, int threadNo, TraceLevel debugLevel) {
        ArrayList<ArrayList<Rectangle>> lines = TesseractUtils.getBoundingBoxes(rotatedPix, (int) (characterDimensions.getHeight() / 2), (int) (characterDimensions.getWidth() / 4), getMinimumCharactersPerLine(), (int) characterDimensions.getHeight(), (int) characterDimensions.getWidth());
        logger.debug("Lines found from rotatedPix are : " + lines);
        drawBoundingBoxesOnPix(rotatedPix, lines, runCounter + " - " + threadNo + " - " + drawingSeries + "A - boundingBoxes.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> tempLines = TesseractUtils.sortLines(lines, true);
        ArrayList<ArrayList<Rectangle>> mergedLines = TesseractUtils.mergeLinesBasedOnYOverlap(tempLines, minLineHeightOverlap); // usually 0.375
        ArrayList<ArrayList<Rectangle>> orderedAndMergedLines = TesseractUtils.sortLines(mergedLines, true);
        drawBoundingBoxesOnPix(rotatedPix, orderedAndMergedLines, runCounter + " - " + threadNo + " - " + drawingSeries + "B - orderedAndMergedLines.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> noOverlappingBoxLines = TesseractUtils.dropOverlappingBoxesInLines(orderedAndMergedLines, characterDimensions.getHeight() * characterDimensions.getWidth() * 0.25);
        drawBoundingBoxesOnPix(rotatedPix, noOverlappingBoxLines, runCounter + " - " + threadNo + " - " + drawingSeries + "C - overlappingBoxesDropped.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> okSizedBoxes = TesseractUtils.dropShortOrWideBoxes(noOverlappingBoxLines, characterDimensions.getHeight() * 0.5, characterDimensions.getWidth() * 2.5);
        drawBoundingBoxesOnPix(rotatedPix, okSizedBoxes, runCounter + " - " + threadNo + " - " + drawingSeries + "D - shortAndWideDropped.png", debugLevel);

        ArrayList<ArrayList<Rectangle>> preFinalLines = TesseractUtils.mergeLinesBasedOnYOverlap(okSizedBoxes, minLineHeightOverlap); // usually 0.375
        drawBoundingBoxesOnPix(rotatedPix, preFinalLines, runCounter + " - " + threadNo + " - " + drawingSeries + "E - mergedBasedOnYOverlap.png", debugLevel);

        preFinalLines = TesseractUtils.removeLettersStartingAtWrongY(preFinalLines);
        drawBoundingBoxesOnPix(rotatedPix, preFinalLines, runCounter + " - " + threadNo + " - " + drawingSeries + "F - removedBasedOnYStart.png", debugLevel);

        int indexOfLongestLine = -1;
        int longestLength = 0;
        int lineNumber = 0;
        for (ArrayList<Rectangle> line : preFinalLines) {
            if (line.size() > longestLength) {
                indexOfLongestLine = lineNumber;
                longestLength = line.size();
            }
            ++lineNumber;
        }

        ArrayList<ArrayList<Rectangle>> finalLines = new ArrayList<>();
        if (indexOfLongestLine >= 0) {
            finalLines.add(preFinalLines.get(indexOfLongestLine));
        }

        drawBoundingBoxesOnPix(rotatedPix, finalLines, runCounter + " - " + threadNo + " - " + drawingSeries + "H - longestLine.png", debugLevel);
        return finalLines;
    }

    protected PatternMatchedStrings matchPattern(String input, Pattern pattern, String stringPattern) {
        PatternMatchedStrings pms = new PatternMatchedStrings();
        Matcher matcher = pattern.matcher(input);
        ArrayList<MatchedString> matchedStrings = new ArrayList<>();
        while (matcher.find()) {
            String tempLine = EMPTY_STRING;
            int start = matcher.start();
            int end = matcher.end();
            // tempLine = input.substring(0, start) + input.substring(end - 1, input.length());
            tempLine = input.substring(start, end);
            int score = tempLine.length() * 100 / input.length();
            matchedStrings.add(new MatchedString(tempLine, score));
        }
        if (matchedStrings.size() == 0) {
            if (input.length() >= stringPattern.length()) {
                matchedStrings.add(
                        new MatchedString(input,
                                (int) (60 * (1 -
                                        (Math.abs(input.length() - stringPattern.length()) * 1.0) /
                                                Math.max(input.length(), stringPattern.length()))
                                )
                        )
                );
            }
        }
        System.out.println("Input : " + input + "; Pattern : " + pattern + "; Output : " + matchedStrings);
        pms.setMatchedStrings(matchedStrings);
        return pms;
    }

    protected ArrayList<String> matchPattern(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        boolean present = false;
        ArrayList<String> matchedPatterns = new ArrayList<>();
        while (matcher.find()) {
            String tempLine = EMPTY_STRING;
            int start = matcher.start();
            int end = matcher.end();
            // tempLine = input.substring(0, start) + input.substring(end - 1, input.length());
            tempLine = input.substring(start, end);
            matchedPatterns.add(tempLine);
        }
        if (matchedPatterns.size() == 0) {
            matchedPatterns.add(input);
        }
        System.out.println("Input : " + input + "; Pattern : " + pattern + "; Output : " + matchedPatterns);
        return matchedPatterns;
    }

    public String getName() {
        return getProcessorName();
    }

    public MachiningNode getParent() {
        return parent;
    }

    public void setParent(MachiningNode machiningNode) {
        // sets the node only if the current node is null
        if (parent == null) {
            this.parent = machiningNode;
        }
        register(this);
    }

    private static final boolean register(BaseImageProcessor processor) {
        if (processor.parent == null) {
            return false;
        }
        InspectionPoint inspectionPoint = (InspectionPoint) processor.parent;
        if ((inspectionPoint == null) || ("".equals(inspectionPoint.getName()))) {
            return false;
        }
        Part part = (Part) inspectionPoint.getParent();
        if ((part == null) || ("".equals(part.getName()))) {
            return false;
        }
        String pName = part.getName();
        String ipName = inspectionPoint.getName();
        String processorName = processor.getName();
        String key = new StringBuilder(pName).append(".").append(ipName).append(".").append(processorName).toString();
        if (processorRegistry.containsKey(key)) {
            logger.debug("A processor with the fully qualified name " + key + " is already registered in the processorRegistry.");
            throw new IllegalStateException("A processor with the fully qualified name " + key + " is already registered in the processorRegistry.");
        }
        processorRegistry.put(key, processor);
        return true;
    }

    public ArrayList<MachiningNode> getChildren() {
        // A processor does not have any children
        // However, we do not want to return null; instead, we return an empty ArrayList
        return children;
    }

    protected String getInitalKeyString() {
        if (initialKeyString != null) {
            return initialKeyString;
        }
        StringBuilder sb = new StringBuilder();
        MachiningNode parent = getParent();
        MachiningNode grandParent = parent.getParent();
        sb.append(grandParent.getName());
        sb.append(".");
        sb.append(parent.getName());
        sb.append(".");
        sb.append(getName());
        initialKeyString = sb.toString();
        return initialKeyString;
    }

    public int getAssessmentRegions() {
        return assessmentRegions;
    }

    public static final BaseImageProcessor getProcessorByName(String key) {
        if (key == null) {
            throw new IllegalArgumentException("The search key cannot be null in getProcessorByName()");
        }
        BaseImageProcessor processor = processorRegistry.get(key);
        if (processor == null) {
            logger.debug("Did not find any processor registered with the name " + key);
        }
        return processor;
    }

    public static final String getProcessorNameByReference(BaseImageProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("The search key cannot be null in getProcessorNameByReference()");
        }
        Iterator<Map.Entry<String, BaseImageProcessor>> entries = processorRegistry.entrySet().iterator();
        String name = "";
        while (entries.hasNext()) {
            Map.Entry<String, BaseImageProcessor> anEntry = entries.next();
            if (anEntry.getValue() == processor) {
                name = anEntry.getKey();
                break;
            }
        }
        if (name == null) {
            logger.debug("Did not find any name in processorRegistry for the processor  " + processor);
            return "";
        }
        return name;
    }

}
