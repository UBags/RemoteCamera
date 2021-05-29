package com.costheta.machine.clip.utils;

import com.costheta.camera.processor.ImageProcessor;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseApplication;
import com.costheta.machine.BaseImageProcessor;
import com.costheta.machine.InspectionPoint;
import com.costheta.machine.Part;
import com.costheta.machine.clip.ClippingPolygon;
import com.costheta.machine.clip.ClippingRectangle;
import com.costheta.utils.GeneralUtils;
import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ClippingUtils extends BaseApplication {

    public static final String CLIP_BOX_SUFFIX = "clipBox";
    public static final String comma = ",";
    public static final String dot = ".";

    static {
        initialise(initialisationArgs);
        loadClippingProperties();
    }

    private static final Logger logger = LogManager.getLogger(ClippingUtils.class);
    private static final String CLIPPING_PROPERTIES_BASE_FILE = "clipping.properties";
    private static int minDimensionsNeeded = 6;
    public static final Rectangle EMPTY_RECTANGLE = new Rectangle (0,0,200,200);
    public static final Polygon EMPTY_POLYGON = new Polygon();
    static {
        EMPTY_POLYGON.getPoints().addAll(new Double[] {0.0,0.0,200.0,200.0});
    }

    private static Properties clippingProperties;
    private static Path filePath;
    private static long lastTimeLoaded = 0;
    public static String headerString = "#define the clipbox coordinates\n" +
            "#format is {PartName}.{Inspection Point Name}.{Processor Name}.clipBox=startX,startY,width,height e.g.\n" +
            "#AL1.Left\\ IP.Text\\ Inspection.clipBox=475,670,860,310\n" +
            "#Note: If the names have a space ' ' in the key name, ensure that the space is\n" +
            "#represented by a '\\ '\n" +
            "# if a clipping region is not defined for a processor, the full image is used";

    public static final boolean isEmptyRectangle(Rectangle rectangle) {
        if (rectangle == EMPTY_RECTANGLE) {
            return true;
        }
        if ((rectangle.getWidth() < minDimensionsNeeded) && (rectangle.getHeight() < minDimensionsNeeded)) {
            return true;
        }
        return false;
    }
    public static Path getFilePath() {
        if (filePath != null) {
            return filePath;
        }
        filePath = Paths.get(getBaseDir() + '/' + CLIPPING_PROPERTIES_BASE_FILE);
        return filePath;
    }

    public static Rectangle getClippingRectangle(String key) {
        if ((key == null) || ("".equals(key))) {
            logger.trace("No rectangle returned for key = " + key);
            return EMPTY_RECTANGLE;
        }
        String value = getProperty(key);
        if ((value == null) || (value.length() == 0)) {
            logger.trace("Found no data for key " + key + ". Empty rectangle returned.");
            // System.out.println("Found no data for key " + key + ". Empty rectangle returned.");
            return EMPTY_RECTANGLE;
        }
        String[] data = value.split(",");
        if (data.length != 4) {
            logger.trace("Found data : " + value + " - for key " + key + ". Empty rectangle returned as number of data points != 4");
            // System.out.println("Found data : " + value + " - for key " + key + ". Empty rectangle returned as number of data points != 4");
            return EMPTY_RECTANGLE;
        }
        int[] coords = new int[4];
        for (int i = 0; i < 4; ++i) {
            try {
                double doubleValue = Double.valueOf(data[i]);
                coords[i] = (int) (doubleValue);
            } catch (NumberFormatException e) {
                logger.trace("Value of integer at index " + i + " is " + data[i] + " which is not a number. Hence. returning Empty Rectangle");
                return EMPTY_RECTANGLE;
            }
        }
        Rectangle clipRegion = new Rectangle(coords[0], coords[1], coords[2], coords[3]);
        if ((clipRegion.getWidth() < minDimensionsNeeded) || (clipRegion.getHeight() < minDimensionsNeeded)) {
            logger.debug("Since min dimensions not there in clippingRegion " + clipRegion + ", returning an Empty Rectangle for the property " + key);
            return EMPTY_RECTANGLE;
        }
        return clipRegion;
    }

    public static Polygon getClippingPolygon(String key) {
        if ((key == null) || ("".equals(key))) {
            logger.trace("No polygon returned for key = " + key);
            return EMPTY_POLYGON;
        }
        String value = getProperty(key);
        if ((value == null) || (value.length() == 0)) {
            logger.trace("Found no data for key " + key + ". Empty polygon returned.");
            // System.out.println("Found no data for key " + key + ". Empty rectangle returned.");
            return EMPTY_POLYGON;
        }
        String[] data = value.split(",");
        if ((data.length % 2 != 0) || (data.length < 6)) {
            logger.trace("Found data : " + value + " - for key " + key + ". Empty polygon returned as number of data points = " + data.length);
            // System.out.println("Found data : " + value + " - for key " + key + ". Empty rectangle returned as number of data points != 4");
            return EMPTY_POLYGON;
        }
        Double[] coords = new Double[data.length];
        for (int i = 0; i < data.length; ++i) {
            try {
                double doubleValue = Double.valueOf(data[i]);
                coords[i] = doubleValue;
            } catch (NumberFormatException e) {
                logger.trace("Value of coordinate at index " + i + " is " + data[i] + " which is not a number. Hence. returning Empty Polygon");
                return EMPTY_POLYGON;
            }
        }
        Polygon clipRegion = new Polygon();
        clipRegion.getPoints().addAll(coords);
        Rectangle boundingRectangle = ImageUtils.getBoundingRectangle(clipRegion);
        if ((boundingRectangle.getWidth() < minDimensionsNeeded) || (boundingRectangle.getHeight() < minDimensionsNeeded)) {
            logger.debug("Since min dimensions not there in clippingRegion " + clipRegion + ", returning an Empty Polygon for the property " + key);
            return EMPTY_POLYGON;
        }
        return clipRegion;
    }

    public static Rectangle setClippingRectangle(String key, Rectangle rectangle) {
        Rectangle oldClippingRegion = getClippingRectangle(key);
        if ((rectangle.getWidth() < minDimensionsNeeded) || (rectangle.getHeight() < minDimensionsNeeded)) {
            logger.debug("Since min dimensions not there in clippingRegion " + rectangle + ", an Empty Rectangle is assigned to the property " + key);
            addProperty(key, EMPTY_RECTANGLE);
        } else {
            addProperty(key, rectangle);
        }
        return oldClippingRegion;
    }

    public static Properties loadClippingProperties() {
        return getClippingProperties();
    }

    public static boolean saveClippingProperties() {
        return setClippingProperties();
    }

    public static Properties getClippingProperties() {
        Path pathToFile = getFilePath();
        boolean newFileCreated = false;
        if (clippingProperties == null) {
            // application is loading it for the first time
            clippingProperties = GeneralUtils.loadPropertiesFile(pathToFile.toAbsolutePath().toString());
            // commented out because GeneralUtils may return an empty Properties file
            // Also, it will never return null, so this block is not needed
//            if (clippingProperties == null) {
//                clippingProperties = new Properties();
//            }
//            else {
//                return clippingProperties;
//            }
        }
        if (clippingProperties.isEmpty()) {
            // if clippingProperties has just been loaded, then create the clipping.properties file as well
            if (!Files.exists(pathToFile)) {
                newFileCreated = true;
                try {
                    Files.createDirectories(pathToFile.toAbsolutePath().getParent());
                } catch (IOException ie) {
                    logger.trace("Base directory path already exists. Hence, not created");
                }
                try {
                    Files.createFile(pathToFile);
                } catch (IOException ie) {
                    logger.trace("Clipping properties file doesn't exist. Couldn't create it either");
                    return clippingProperties;
                }
                // lastTimeLoaded = pathToFile.toFile().lastModified();
            }
        }
        // check if the properties have been updated since the last time they were loaded
        long fileCreated = pathToFile.toFile().lastModified();
        if (fileCreated > lastTimeLoaded) {
            newFileCreated = true;
        }
        if ((clippingProperties != null) && (! newFileCreated)) {
            return clippingProperties;
        }
        try {
            FileInputStream inputStream = new FileInputStream(pathToFile.toAbsolutePath().toString());
            clippingProperties.load(inputStream);
            inputStream.close();
            lastTimeLoaded = pathToFile.toFile().lastModified();
        } catch (IOException ie) {
            logger.trace("Clipping properties file exists. But, couldn't read properties from there - check file permissions");
        }
        System.out.println("Loaded clipping properties from file path : " + filePath.toAbsolutePath().toString());
        // System.out.println("Clipping Properties before sorting = " + clippingProperties);
        // clippingProperties = GeneralUtils.sortAscendingByKey(clippingProperties);
        // System.out.println("Clipping Properties after sorting = " + clippingProperties);
        return clippingProperties;
    }

    public static boolean setClippingProperties() {
        if (clippingProperties == null) {
            logger.trace("No clipping properties available for saving as clippingProperties is null");
            return false;
        }
        if (clippingProperties.isEmpty()) {
            logger.trace("No clipping properties available for saving as clippingProperties has no properties and comments");
            return false;
        }
        Path pathToFile = getFilePath();
        if (! Files.exists(pathToFile)) {
            try {
                Files.createDirectories(pathToFile.toAbsolutePath().getParent());
            } catch (IOException ie) {
                logger.trace("Base directory path already exists. Hence, not created");
            }
            try {
                Files.createFile(pathToFile);
            } catch (IOException ie) {
                logger.trace("Clipping properties file doesn't exist. Couldn't create it now. Check file operation permissions");
                return false;
            }
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(pathToFile.toAbsolutePath().toString(), false);
            // clippingProperties = GeneralUtils.sortAscendingByKey(clippingProperties);
            clippingProperties.store(outputStream,headerString);
            outputStream.close();
        } catch (IOException ie) {
            logger.trace("Clipping properties file exists. But, couldn't write properties to it - check file permissions");
        }
        return true;
    }

    protected static void setFilePath(Path filePath) {
        ClippingUtils.filePath = filePath;
    }

    protected static String getProperty(String key) {
        return clippingProperties.getProperty(key,"");
    }

    protected static String getClippingPropertiesBaseFile() {
        return CLIPPING_PROPERTIES_BASE_FILE;
    }

    public static void setMinDimensionsNeeded(int minDimensionsNeeded) {
        ClippingUtils.minDimensionsNeeded = minDimensionsNeeded;
    }

    public static String addProperty(String key, String value) {
        Object oldVal = clippingProperties.setProperty(key, value);
        // clippingProperties = GeneralUtils.sortAscendingByKey(clippingProperties);
        if (oldVal == null) {
            return "";
        }
        return (String) oldVal;
    }

    public static String setProperty(String key, String value) {
        return addProperty(key, value);
    }

    public static String addProperty(String key, Rectangle rectangle) {
        StringBuilder sb = new StringBuilder();
        sb.append(rectangle.getX()).append(comma).append(rectangle.getY())
                .append(comma).append(rectangle.getWidth()).append(comma).append(rectangle.getHeight());
        String rectCoords = sb.toString();
        return addProperty(key, rectCoords);
    }

    public static String addProperty(String key, Polygon polygon) {
        StringBuilder sb = new StringBuilder();
        ObservableList<Double> points = polygon.getPoints();
        for (int i = 0; i < points.size(); ++i) {
            int coord = (int) points.get(i).doubleValue();
            sb.append(coord);
            if (i < points.size() - 1) {
                sb.append(comma);
            }
        }
        String rectCoords = sb.toString();
        return addProperty(key, rectCoords);
    }

    public static String setProperty(String key, Rectangle rectangle) {
        return addProperty(key, rectangle);
    }

    public static String setProperty(String key, Polygon polygon) {
        return addProperty(key, polygon);
    }

    public static final LinkedHashMap<String, ArrayList<String>> getClipBoxKeys(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<String, ArrayList<String>> keys = new LinkedHashMap<>();
        if (inspectionPoint == null) {
            return keys;
        }
        Part part = (Part) inspectionPoint.getParent();
        if (part == null) {
            return keys;
        }
        StringBuilder sb = new StringBuilder(part.getName()).append(dot)
                .append(inspectionPoint.getName());
        String baseIPName = sb.toString();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || (processors.size() == 0)) {
            return keys;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor pr = (BaseImageProcessor) processor;
            String basePName = new StringBuilder(baseIPName).append(dot).append(pr.getName()).toString();
            int assessmentPoints = pr.getAssessmentRegions();
            ArrayList<String> assessmentCoordKeys = new ArrayList<>();
            for (int i = 1; i <= assessmentPoints; ++i) {
                String aKey = new StringBuilder(basePName).append(dot).append(i)
                        .append(dot).append(CLIP_BOX_SUFFIX).toString();
                assessmentCoordKeys.add(aKey);
            }
            keys.put(pr.getName(), assessmentCoordKeys);
        }
        return keys;
    }

    public static ArrayList<String> getClipBoxKeys(BaseImageProcessor imageProcessor) {
        loadClippingProperties();
        ArrayList<String> keys = new ArrayList<>();
        if (imageProcessor == null) {
            return keys;
        }
        InspectionPoint inspectionPoint = (InspectionPoint) imageProcessor.getParent();
        if (inspectionPoint == null) {
            return keys;
        }
        Part part = (Part) inspectionPoint.getParent();
        if (part == null) {
            return keys;
        }
        StringBuilder sb = new StringBuilder(part.getName()).append(dot)
                .append(inspectionPoint.getName()).append(dot).append(imageProcessor.getName());
        String basePrName = sb.toString();
        int assessmentPoints = imageProcessor.getAssessmentRegions();
        logger.trace("In ClippingUtils.getClipBoxKeys(), the processor " + imageProcessor + " has " + assessmentPoints + " assessmentRegions");
        for (int i = 1; i <= assessmentPoints; ++i) {
            String aKey = new StringBuilder(basePrName).append(dot).append(i)
                    .append(dot).append(CLIP_BOX_SUFFIX).toString();
            keys.add(aKey);
        }
        return keys;
    }

    public static final LinkedHashMap<String, Rectangle> getClipBoxKeysAndRectangles(BaseImageProcessor imageProcessor) {
        loadClippingProperties();
        LinkedHashMap<String, Rectangle> map = new LinkedHashMap<>();
        ArrayList<String> keys = getClipBoxKeys(imageProcessor);
        if (keys.size() == 0) {
            return map;
        }
        Collections.sort(keys);
        for (String key : keys) {
            Rectangle clipRegion = getClippingRectangle(key);
            map.put(key, clipRegion);
        }
        return map;
    }

    public static final LinkedHashMap<String, Polygon> getClipBoxKeysAndPolygons(BaseImageProcessor imageProcessor) {
        loadClippingProperties();
        LinkedHashMap<String, Polygon> map = new LinkedHashMap<>();
        ArrayList<String> keys = getClipBoxKeys(imageProcessor);
        if (keys.size() == 0) {
            return map;
        }
        Collections.sort(keys);
        for (String key : keys) {
            Polygon clipRegion = getClippingPolygon(key);
            map.put(key, clipRegion);
        }
        return map;
    }

    public static final LinkedHashMap<String, LinkedHashMap<String, Rectangle>> getClipBoxKeysAndRectangles(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<String, LinkedHashMap<String, Rectangle>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            String processorName = aProcessor.getName();
            LinkedHashMap<String, Rectangle> mappings = getClipBoxKeysAndRectangles(aProcessor);
            map.put(processorName, mappings);
        }
        return map;
    }

    public static final LinkedHashMap<String, LinkedHashMap<String, Polygon>> getClipBoxKeysAndPolygons(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<String, LinkedHashMap<String, Polygon>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            String processorName = aProcessor.getName();
            LinkedHashMap<String, Polygon> mappings = getClipBoxKeysAndPolygons(aProcessor);
            map.put(processorName, mappings);
        }
        return map;
    }

    public static final void updateProperties(LinkedHashMap<String, LinkedHashMap<String, Rectangle>> inspectionPointData) {
        loadClippingProperties();
        Iterator<Map.Entry<String, LinkedHashMap<String, Rectangle>>> ipLevelIterator = inspectionPointData.entrySet().iterator();
        while (ipLevelIterator.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, Rectangle>> anEntry = ipLevelIterator.next();
            // String baseIPLevelKey = anEntry.getKey();
            LinkedHashMap<String, Rectangle> clipRegions = anEntry.getValue();
            Iterator<Map.Entry<String, Rectangle>> processorLevelIterator = clipRegions.entrySet().iterator();
            while(processorLevelIterator.hasNext()) {
                Map.Entry<String, Rectangle> regionEntry = processorLevelIterator.next();
                String key = regionEntry.getKey();
                Rectangle clippingRectangle = regionEntry.getValue();
                setProperty(key, clippingRectangle);
            }
        }
        saveClippingProperties();
    }

    public static final void updatePolygonProperties(LinkedHashMap<String, LinkedHashMap<String, Polygon>> inspectionPointData) {
        loadClippingProperties();
        Iterator<Map.Entry<String, LinkedHashMap<String, Polygon>>> ipLevelIterator = inspectionPointData.entrySet().iterator();
        while (ipLevelIterator.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, Polygon>> anEntry = ipLevelIterator.next();
            // String baseIPLevelKey = anEntry.getKey();
            LinkedHashMap<String, Polygon> clipRegions = anEntry.getValue();
            Iterator<Map.Entry<String, Polygon>> processorLevelIterator = clipRegions.entrySet().iterator();
            while(processorLevelIterator.hasNext()) {
                Map.Entry<String, Polygon> regionEntry = processorLevelIterator.next();
                String key = regionEntry.getKey();
                Polygon clippingPolygon = regionEntry.getValue();
                setProperty(key, clippingPolygon);
            }
        }
        saveClippingProperties();
    }

    public static final LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> getClipBoxesForInspectionPoint(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Rectangle> mappings = getClipBoxKeysAndRectangles(aProcessor);
            map.put(aProcessor, mappings);
        }
        System.out.println("map = " + map);
        return map;
    }

    public static final LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> getClipPolygonsForInspectionPoint(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Polygon> mappings = getClipBoxKeysAndPolygons(aProcessor);
            map.put(aProcessor, mappings);
        }
        return map;
    }

    public static final void updateClippingProperties(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> inspectionPointData) {
        loadClippingProperties();
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>>> ipLevelIterator = inspectionPointData.entrySet().iterator();
        while (ipLevelIterator.hasNext()) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>> anEntry = ipLevelIterator.next();
            // String baseIPLevelKey = anEntry.getKey();
            LinkedHashMap<String, Rectangle> clipRegions = anEntry.getValue();
            Iterator<Map.Entry<String, Rectangle>> processorLevelIterator = clipRegions.entrySet().iterator();
            while(processorLevelIterator.hasNext()) {
                Map.Entry<String, Rectangle> regionEntry = processorLevelIterator.next();
                String key = regionEntry.getKey();
                Rectangle clippingRectangle = regionEntry.getValue();
                setProperty(key, clippingRectangle);
            }
        }
        saveClippingProperties();
    }

    public static final void updateClippingPolygonProperties(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> inspectionPointData) {
        loadClippingProperties();
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>>> ipLevelIterator = inspectionPointData.entrySet().iterator();
        while (ipLevelIterator.hasNext()) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>> anEntry = ipLevelIterator.next();
            // String baseIPLevelKey = anEntry.getKey();
            LinkedHashMap<String, Polygon> clipRegions = anEntry.getValue();
            Iterator<Map.Entry<String, Polygon>> processorLevelIterator = clipRegions.entrySet().iterator();
            while(processorLevelIterator.hasNext()) {
                Map.Entry<String, Polygon> regionEntry = processorLevelIterator.next();
                String key = regionEntry.getKey();
                Polygon clippingPolygon = regionEntry.getValue();
                setProperty(key, clippingPolygon);
            }
        }
        saveClippingProperties();
    }

    public static final LinkedHashMap<Integer, LinkedHashMap<String, Rectangle>> getClippingRegionsInSequence(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<Integer, LinkedHashMap<String, Rectangle>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        int count = 1;
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Rectangle> mappings = getClipBoxKeysAndRectangles(aProcessor);
            map.put(count, mappings);
            ++count;
        }
        return map;
    }

    public static final LinkedHashMap<Integer, LinkedHashMap<String, Polygon>> getClippingPolygonsInSequence(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        LinkedHashMap<Integer, LinkedHashMap<String, Polygon>> map = new LinkedHashMap<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return map;
        }
        int count = 1;
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Polygon> mappings = getClipBoxKeysAndPolygons(aProcessor);
            map.put(count, mappings);
            ++count;
        }
        return map;
    }

    public static final ArrayList<ArrayList<ClippingRectangle>> getClippingRegionsArray(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        ArrayList<ArrayList<ClippingRectangle>> clips = new ArrayList<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return clips;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Rectangle> mappings = getClipBoxKeysAndRectangles(aProcessor);
            Iterator<Map.Entry<String,Rectangle>> entries = mappings.entrySet().iterator();
            ArrayList<ClippingRectangle> innerClips = new ArrayList<>();
            int count = 1;
            while(entries.hasNext()) {
                Map.Entry<String,Rectangle> anEntry = entries.next();
                String key = anEntry.getKey();
                Rectangle aClip = anEntry.getValue();
                innerClips.add(new ClippingRectangle(count, key, aClip));
                ++count;
            }
            clips.add(innerClips);
        }
        return clips;
    }

    public static final ArrayList<ArrayList<ClippingPolygon>> getClippingPolygonsArray(InspectionPoint inspectionPoint) {
        loadClippingProperties();
        ArrayList<ArrayList<ClippingPolygon>> clips = new ArrayList<>();
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || processors.size() == 0) {
            return clips;
        }
        for (ImageProcessor processor : processors) {
            BaseImageProcessor aProcessor = (BaseImageProcessor) processor;
            LinkedHashMap<String, Polygon> mappings = getClipBoxKeysAndPolygons(aProcessor);
            Iterator<Map.Entry<String,Polygon>> entries = mappings.entrySet().iterator();
            ArrayList<ClippingPolygon> innerClips = new ArrayList<>();
            int count = 1;
            while(entries.hasNext()) {
                Map.Entry<String,Polygon> anEntry = entries.next();
                String key = anEntry.getKey();
                Polygon aClip = anEntry.getValue();
                innerClips.add(new ClippingPolygon(count, key, aClip));
                ++count;
            }
            clips.add(innerClips);
        }
        return clips;
    }

    public static Rectangle copy(Rectangle old) {
        Rectangle copy = new Rectangle(old.getX(), old.getY(), old.getWidth(), old.getHeight());
        return copy;
    }

    public static Polygon copy(Polygon old) {
        ObservableList<Double> points = old.getPoints();
        double[] pointsArray = points.stream().mapToDouble(d -> d).toArray();;
        Polygon copy = new Polygon(pointsArray);
        return copy;
    }

    public static ClippingRectangle copy(ClippingRectangle old) {
        ClippingRectangle newClippingRectangle = new ClippingRectangle(old.id, old.key, copy(old.clippingRectangle));
        return newClippingRectangle;
    }

    public static ClippingPolygon copy(ClippingPolygon old) {
        ClippingPolygon newClippingPolygon = new ClippingPolygon(old.id, old.key, copy(old.clippingPolygon));
        return newClippingPolygon;
    }

    public static ArrayList<ClippingRectangle> copy(ArrayList<ClippingRectangle> oldRectangles) {
        ArrayList<ClippingRectangle> copy = new ArrayList<>();
        for (ClippingRectangle rect : oldRectangles) {
            copy.add(copy(rect));
        }
        return copy;
    }

    public static ArrayList<ClippingPolygon> copyPolygons(ArrayList<ClippingPolygon> oldPolygons) {
        ArrayList<ClippingPolygon> copy = new ArrayList<>();
        for (ClippingPolygon rect : oldPolygons) {
            copy.add(copy(rect));
        }
        return copy;
    }

    public static ArrayList<ArrayList<ClippingRectangle>> deepCopy(ArrayList<ArrayList<ClippingRectangle>> oldRectangles) {
        ArrayList<ArrayList<ClippingRectangle>> copy = new ArrayList<>();
        for (ArrayList<ClippingRectangle> rects : oldRectangles) {
            copy.add(copy(rects));
        }
        return copy;
    }

    public static ArrayList<ArrayList<ClippingPolygon>> deepCopyPolygons(ArrayList<ArrayList<ClippingPolygon>> oldPolygons) {
        ArrayList<ArrayList<ClippingPolygon>> copy = new ArrayList<>();
        for (ArrayList<ClippingPolygon> rects : oldPolygons) {
            copy.add(copyPolygons(rects));
        }
        return copy;
    }

    public static LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> copy(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> input) {
        LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> copy = new LinkedHashMap<>();
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>>> externalIterator = input.entrySet().iterator();
        while (externalIterator.hasNext()) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>> mainEntry = externalIterator.next();
            BaseImageProcessor externalKey = mainEntry.getKey();
            LinkedHashMap<String, Rectangle> externalValue = mainEntry.getValue();
            LinkedHashMap<String, Rectangle> innerCopy = new LinkedHashMap<>();
            Iterator<Map.Entry<String, Rectangle>> internalIterator = externalValue.entrySet().iterator();
            while(internalIterator.hasNext()) {
                Map.Entry<String, Rectangle> innerEntry = internalIterator.next();
                innerCopy.put(innerEntry.getKey(), copy(innerEntry.getValue()));
            }
            copy.put(externalKey, innerCopy);
        }
        return copy;
    }

    public static LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> copyPolygonHashMap(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> input) {
        LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> copy = new LinkedHashMap<>();
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>>> externalIterator = input.entrySet().iterator();
        while (externalIterator.hasNext()) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>> mainEntry = externalIterator.next();
            BaseImageProcessor externalKey = mainEntry.getKey();
            LinkedHashMap<String, Polygon> externalValue = mainEntry.getValue();
            LinkedHashMap<String, Polygon> innerCopy = new LinkedHashMap<>();
            Iterator<Map.Entry<String, Polygon>> internalIterator = externalValue.entrySet().iterator();
            while(internalIterator.hasNext()) {
                Map.Entry<String, Polygon> innerEntry = internalIterator.next();
                innerCopy.put(innerEntry.getKey(), copy(innerEntry.getValue()));
            }
            copy.put(externalKey, innerCopy);
        }
        return copy;
    }

    public static void saveClippingProperties(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Rectangle>> originalHashMap, ArrayList<ArrayList<ClippingRectangle>> updatedClippingRectangles) {
        // Note: The input HashMap and the clipping properties have EXACTLY the
        // same number of entries, and they are EXACTLY in the same order.
        // This ensure that we do not have to match keys before updating the HashMap
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>>> externalIterator = originalHashMap.entrySet().iterator();
        for (ArrayList<ClippingRectangle> line : updatedClippingRectangles) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Rectangle>> processorToClippingRectanglesMapping = externalIterator.next();
            LinkedHashMap<String, Rectangle> clippingKeyToClippingRectanglesMapping = processorToClippingRectanglesMapping.getValue();
            Iterator<Map.Entry<String, Rectangle>> internalIterator = clippingKeyToClippingRectanglesMapping.entrySet().iterator();
            for (ClippingRectangle word : line) {
                Map.Entry<String, Rectangle> aMapping = internalIterator.next();
                String key = aMapping.getKey();
                clippingKeyToClippingRectanglesMapping.put(key, word.clippingRectangle);
            }
        }
        updateClippingProperties(originalHashMap);
    }

    public static void saveClippingPolygonProperties(LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> originalHashMap, ArrayList<ArrayList<ClippingPolygon>> updatedClippingPolygons) {
        // Note: The input HashMap and the clipping properties have EXACTLY the
        // same number of entries, and they are EXACTLY in the same order.
        // This ensure that we do not have to match keys before updating the HashMap
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>>> externalIterator = originalHashMap.entrySet().iterator();
        for (ArrayList<ClippingPolygon> line : updatedClippingPolygons) {
            Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>> processorToClippingPolygonsMapping = externalIterator.next();
            LinkedHashMap<String, Polygon> clippingKeyToClippingPolygonsMapping = processorToClippingPolygonsMapping.getValue();
            Iterator<Map.Entry<String, Polygon>> internalIterator = clippingKeyToClippingPolygonsMapping.entrySet().iterator();
            for (ClippingPolygon word : line) {
                Map.Entry<String, Polygon> aMapping = internalIterator.next();
                String key = aMapping.getKey();
                clippingKeyToClippingPolygonsMapping.put(key, word.clippingPolygon);
            }
        }
        updateClippingPolygonProperties(originalHashMap);
    }

}
