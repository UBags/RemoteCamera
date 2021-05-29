package com.costheta.machine;

import com.costheta.camera.processor.ClipImageProcessor;
import com.costheta.machine.clip.utils.ClippingUtils;
import javafx.geometry.Dimension2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import javafx.scene.shape.Rectangle;

public abstract class BaseClipImageProcessor extends BaseImageProcessor implements ClipImageProcessor {

    // these processors keep a reference to the Inspection Point Owner

    private static final Logger logger = LogManager.getLogger(BaseClipImageProcessor.class);
    private static final String FULL_STOP = ".";
    private static final String CHARACTER_DIMENSIONS_SUFFIX = "characterDimensions";
    private static final String CHARACTER_THICKNESS_SUFFIX = "thickness";

    private String clipBoxKey;
    private String characterDimensionsKey;
    private String characterThicknessKey;

    protected ArrayList<Rectangle> clipBoxRectangles = null;
    protected Dimension2D characterDimensions = null;
    protected String characterThickness = null;

    public BaseClipImageProcessor(String name) {
        this(name, null);
    }

    public BaseClipImageProcessor(String name, ArrayList<String> patterns) {
        this(name, patterns, 1);
    }

    public BaseClipImageProcessor(String name, ArrayList<String> patterns, int assessmentRegions) {
        super(name, patterns, assessmentRegions);
        logger.trace("Leaving constructor()");
    }

    // this is called when setParent() is called; if called before that, it'll return NullPointerException
    // because it's parent is null
    public void initialiseParameterKeys() {
        clipBoxRectangles = getClipBoxRectangles(); // populates the clipBox
        characterDimensions = getCharacterDimensions(); // populates the character dimensions
        characterThickness = getCharacterThickness(); // populates the character thickness
        System.out.println(getInitalKeyString() + " : clipBoxRectangles = " + clipBoxRectangles);
        System.out.println(getInitalKeyString() + ".characterDimensions = " + characterDimensions);
        System.out.println(getInitalKeyString() + ".characterThickness = " + characterThickness);
    }

//    public ArrayList<Rectangle> getClipBoxRectangles() {
//        if (clipBoxRectangles != null) {
//            return clipBoxRectangles;
//        }
//        clipBoxRectangles = createClipBoxRectangles(getClipBoxKey());
//        return clipBoxRectangles;
//    }

//    protected ArrayList<Rectangle> createClipBoxRectangles(String clipBoxKey) {
//        String[] keys = new String[assessmentRegions];
//        for (int i = 0; i < assessmentRegions; ++i) {
//            keys[i] = new StringBuilder(clipBoxKey).append(FULL_STOP).append(i).toString();
//        }
//        ArrayList<Rectangle> clipRectangles = new ArrayList<>();
//        for (int i = 0; i < assessmentRegions; ++i) {
//            Rectangle thisRectangle = ClippingProperties.getClippingRectangle(keys[i]);
//            clipRectangles.add(thisRectangle);
//        }
//        return clipRectangles;
//    }

//    protected String getClipBoxKey() {
//        if (clipBoxKey != null) {
//            return clipBoxKey;
//        }
//        StringBuilder sb = new StringBuilder(getInitalKeyString()).append(FULL_STOP).append(ClippingProperties.CLIP_BOX_SUFFIX);
//        clipBoxKey = sb.toString();
//        return clipBoxKey;
//    }

    protected String getCharacterDimensionsKey() {
        if (characterDimensionsKey != null) {
            return characterDimensionsKey;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getInitalKeyString());
        sb.append(FULL_STOP);
        sb.append(CHARACTER_DIMENSIONS_SUFFIX);
        characterDimensionsKey = sb.toString();
        return characterDimensionsKey;
    }

    protected Dimension2D createCharacterDimensions() {
        String characterDimensionsKey = getCharacterDimensionsKey();
        String characterDimensionString = System.getProperty(characterDimensionsKey);
        if ((characterDimensionString == null) || ("".equals(characterDimensionString))) {
            logger.debug("No Character Dimensions defined for key " + characterDimensionsKey + " in processor " + processorName + (getParent() != null ? " of Inspection Point : " + (getParent().getName()) : ""));
            return new Dimension2D(10,16);
        }
        String[] dimensions = characterDimensionString.split(",");
        if (dimensions.length < 2) {
            logger.info("Insufficient dimension data provided for key " + characterDimensionsKey);
            return new Dimension2D(10,16);
        }
        for (int i = 0; i < dimensions.length; ++i) {
            dimensions[i] = dimensions[i].trim();
        }
        int width = 0;
        int height = 0;
        try {
            width = Integer.parseInt(dimensions[0]);
            height = Integer.parseInt(dimensions[1]);
        } catch (NumberFormatException nfe) {
            logger.info("Incorrect inputs for Dimensions properties for dimensions key " + characterDimensionsKey + ". Should be positive integers");
            return new Dimension2D(10,16);
        }
        return new Dimension2D(width,height);
    }

    public Dimension2D getCharacterDimensions() {
        if (characterDimensions != null) {
            return characterDimensions;
        }
        characterDimensions = createCharacterDimensions();
        return characterDimensions;
    }

    protected String getCharacterThicknessKey() {
        if (characterThicknessKey != null) {
            return characterThicknessKey;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getInitalKeyString());
        sb.append(FULL_STOP);
        sb.append(CHARACTER_THICKNESS_SUFFIX);
        characterThicknessKey = sb.toString();
        return characterThicknessKey;
    }

    protected String createCharacterThickness() {
        String charThicknessKey = getCharacterThicknessKey();
        String characterThicknessString = System.getProperty(charThicknessKey, "Thin");
        return characterThicknessString;
    }

    public String getCharacterThickness() {
        if (characterThickness != null) {
            return characterThickness;
        }
        characterThickness = createCharacterThickness();
        return characterThickness;
    }

    public void setParent(MachiningNode machiningNode) {
        super.setParent(machiningNode);
        initialiseParameterKeys();
    }

    public ArrayList<Rectangle> getClipBoxRectangles() {
        LinkedHashMap<String, Rectangle> regions = ClippingUtils.getClipBoxKeysAndRectangles(this);
        ArrayList<Rectangle> rectangles = new ArrayList<>();
        Iterator<Map.Entry<String, Rectangle>> iterator = regions.entrySet().iterator();
        while (iterator.hasNext()) {
            rectangles.add(iterator.next().getValue());
        }
        return rectangles;
    }

}
