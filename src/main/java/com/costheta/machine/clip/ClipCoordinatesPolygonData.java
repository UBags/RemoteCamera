package com.costheta.machine.clip;

import com.costheta.camera.processor.ImageProcessor;
import com.costheta.machine.BaseImageProcessor;
import com.costheta.machine.InspectionPoint;
import com.costheta.machine.Part;
import com.costheta.machine.clip.utils.ClippingUtils;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.util.*;

public class ClipCoordinatesPolygonData {

    private static final Logger logger = LogManager.getLogger(ClipCoordinatesPolygonData.class);

    private static int minDimensionsNeededForImage = 60 ;
    public static final void setMinDimensionsNeededForImage(int minDimensionsNeededForImage) {
        ClipCoordinatesPolygonData.minDimensionsNeededForImage = minDimensionsNeededForImage;
    }

    // *************
    // this property is common across all inspection points. Hence, static
    private static Properties clippingProperties;
    // *************

    private InspectionPoint inspectionPoint;
    private String partName;
    private String inspectionPointName;
    private BufferedImage inputImage;
    private LinkedHashMap<BaseImageProcessor, LinkedHashMap<String, Polygon>> clippingRegionsByProcessor;

    public ClipCoordinatesPolygonData(InspectionPoint inspectionPoint) {
        logger.trace("Entered ClipCoordinatesData() constructor");
        if (inspectionPoint == null) {
            logger.debug("Inspection Point cannot be null or empty string. Valid inspection point needed in ClipCoordinatesData() constructor");
            throw new IllegalArgumentException("Inspection Point cannot be null or empty string. Valid inspection point needed in ClipCoordinatesData() constructor");
        }
        this.inspectionPoint = inspectionPoint;
        Part part = (Part) inspectionPoint.getParent();
        if (part == null) {
            logger.debug("Parent of Inspection point cannot be null. Valid part needed in ClipCoordinatesData() constructor");
            throw new IllegalArgumentException("Parent of Inspection point cannot be null. Valid part needed in ClipCoordinatesData() constructor");
        }
        this.partName = part.getName();
        if ((partName == null) || ("".equals(partName))) {
            logger.debug("Part Name cannot be null or an Empty String");
            throw new IllegalArgumentException("Partname cannot be null or an empty string in ClipCoordinatesData() constructor");
        }
        this.inspectionPointName = inspectionPoint.getName();
        if ((inspectionPointName == null) || ("".equals(inspectionPointName))) {
            logger.debug("Inspection Point Name cannot be null or an Empty String");
            throw new IllegalArgumentException("Inspection Point cannot be null or an empty string in ClipCoordinatesData() constructor");
        }
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        if ((processors == null) || (processors.size() == 0)) {
            logger.debug("At least one processor needed for clipping to be meaningful");
            throw new IllegalArgumentException("At least one processor needed for clipping to be meaningful");
        }
        this.clippingRegionsByProcessor = ClippingUtils.getClipPolygonsForInspectionPoint(inspectionPoint);
        if (clippingRegionsByProcessor == null) {
            clippingRegionsByProcessor = new LinkedHashMap<>();
        }
        this.inputImage = inspectionPoint.getCurrentImage();
        if (inputImage == null) {
            logger.debug("A meaningful image is needed for clipping. Image cannot be null.");
            //throw new IllegalArgumentException("A meaningful image is needed for clipping. Image cannot be null.");
        }
//
//        if ((inputImage.getHeight() < 60) || (inputImage.getWidth() < 60)) {
//             logger.debug("A meaningful image is needed for clipping. Image must be at least 60 x 60.");
//            throw new IllegalArgumentException("A meaningful image is needed for clipping. Image must be at least 60 x 60.");
//        }
    }

    public String getPartName() {
        return partName;
    }

    public String getInspectionPointName() {
        return inspectionPointName;
    }

    public ArrayList<BaseImageProcessor> getProcessors() {
        ArrayList<ImageProcessor> processors = inspectionPoint.getImageProcessors();
        ArrayList<BaseImageProcessor> biProcessors = new ArrayList<>();
        for (ImageProcessor processor : processors) {
            BaseImageProcessor brProcessor = (BaseImageProcessor) processor;
            biProcessors.add(brProcessor);
        }
        return biProcessors;
    }

    public int getNumberOfAssessmentRegionsInProcessor(BaseImageProcessor processor) {
        return processor.getAssessmentRegions();
    }

    public BufferedImage getInputImage() {
        return inputImage;
    }

    public InspectionPoint getInspectionPoint() {
        return inspectionPoint;
    }

    public ArrayList<ClippingPolygon> getClippingPolygonsInProcessor(BaseImageProcessor processor) {
        ArrayList<ClippingPolygon> clippingPolygons = new ArrayList<>();
        LinkedHashMap<String, Polygon> clips = clippingRegionsByProcessor.get(processor);
        if (clips == null) {
            logger.debug("Invalid processor. Processor " + processor + " with name " + processor.getName() + " not found in LinkedHashMap");
        }
        Iterator <Map.Entry<String, Polygon>> entries = clips.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, Polygon> entry = entries.next();
            String key = entry.getKey();
            int indexOfLastDot = key.lastIndexOf(ClippingUtils.dot);
            String idString = key.substring(0,indexOfLastDot);
            indexOfLastDot = idString.lastIndexOf(ClippingUtils.dot);
            idString = idString.substring(indexOfLastDot + 1);
            int id = 1;
            try {
                id = Integer.parseInt(idString);
            } catch (NumberFormatException nfe) {
                logger.trace("Invalid assessment region number " + idString + " in getClippingRectanglesInProcessor()");
            }
            Polygon polygon = entry.getValue();
            ClippingPolygon thisClippingPolygon = new ClippingPolygon(id, key, polygon);
            clippingPolygons.add(thisClippingPolygon);
        }
        return clippingPolygons;
    }

    public void setClippingPolygon(int processorIndex, int assessmentId, Polygon polygon) {
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>>> iterator = clippingRegionsByProcessor.entrySet().iterator();
        Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>> entry = null;
        for (int i = 0; (i <= processorIndex) && (iterator.hasNext()); ++i) {
            entry = iterator.next();
        }
        if (entry == null) {
            return;
        }
        LinkedHashMap<String, Polygon> clipBoxes = entry.getValue();
        Iterator<Map.Entry<String, Polygon>> internalIterator = clipBoxes.entrySet().iterator();
        Map.Entry<String, Polygon> aClipBox = null;

        for (int i = 1; (i <= assessmentId) && internalIterator.hasNext(); ++i) {
            aClipBox = internalIterator.next();
        }
        String key = aClipBox.getKey();
        aClipBox.setValue(polygon);
    }

    public void setClippingPolygon(ClippingPolygon clippingPolygon) {
        Iterator<Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>>> iterator = clippingRegionsByProcessor.entrySet().iterator();
        Map.Entry<BaseImageProcessor, LinkedHashMap<String, Polygon>> entry = null;
        externalLoop: while (iterator.hasNext()) {
            entry = iterator.next();
            LinkedHashMap<String, Polygon> clipBoxes = entry.getValue();
            Iterator<Map.Entry<String, Polygon>> internalIterator = clipBoxes.entrySet().iterator();
            Map.Entry<String, Polygon> aClipBox = null;
            while (internalIterator.hasNext()) {
                aClipBox = internalIterator.next();
                String key = aClipBox.getKey();
                if (key.equals(clippingPolygon.key)) {
                    aClipBox.setValue(clippingPolygon.clippingPolygon);
                    break externalLoop;
                }
            }

        }
    }

    // hack to ensure that the clipping Rectangles ArrayList
    // does not throw IndexOutOfBoundsException if one outlines
    // an nth clipping region before assigning any of the previous (n-1) regions
//    public void populateWithEmptyRectangles() {
//        clippingRectangles= new ArrayList<>();
//        for (int i = 0; i < clippingRectangles.size(); ++i) {
//            ArrayList<Rectangle> innerList = new ArrayList<>();
//            for (int j = 0; j < assessmentPointsInProcessor.get(i); ++j) {
//                innerList.add(ClippingUtils.EMPTY_RECTANGLE);
//            }
//            clippingRectangles.add(innerList);
//        }
//    }

//    public Rectangle setRectangle(int processorIndex, int assessmentPoint, Rectangle rectangle) {
//        Rectangle oldClippingRegion = clippingRectangles.get(processorIndex).remove(assessmentPoint);
//        clippingRectangles.get(processorIndex).add(assessmentPoint, rectangle);
//        return oldClippingRegion;
//    }

}
