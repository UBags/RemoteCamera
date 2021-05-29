package com.costheta.kems.connectingrods;

import com.costheta.tesseract.TesseractUtils;

import java.awt.*;
import java.util.ArrayList;
import javafx.scene.shape.Rectangle;

public class LineArrangementUtils {

    public static final ArrayList<ArrayList<Rectangle>> rearrangeLines(ArrayList<ArrayList<Rectangle>> lines, int characterHeight, int characterWidth, int minimumRequiredBoxesPerLine) {
        // takes a first cut from TesseractUtils.segregateBoxesIntoLines(), then
        // a) merges boxes from different lines that fall in the same line
        // b) orders them
        // c) drops boxes from a line that are overlapping in area with another box from the same line
        // d) drops boxes longer than 1.3 times expected width
        // e) drops boxes that are less than 0.75 times expected height
        // f) splits long lines into smaller lines based on the gap between contiguous boxes
        // g) drops lines that do not have the min required boxes

        ArrayList<ArrayList<Rectangle>> tempLines = TesseractUtils.sortLines(lines, true);
        ArrayList<ArrayList<Rectangle>> mergedLines = TesseractUtils.mergeLinesBasedOnYOverlap(tempLines, 0.375);
        ArrayList<ArrayList<Rectangle>> orderedAndMergedLines = TesseractUtils.sortLines(mergedLines, false);
        ArrayList<ArrayList<Rectangle>> noOverlappingBoxLines = TesseractUtils.dropOverlappingBoxesInLines(orderedAndMergedLines, characterHeight * characterWidth * 0.075);
        ArrayList<ArrayList<Rectangle>> okSizedBoxes = TesseractUtils.dropShortOrWideBoxes(noOverlappingBoxLines, characterHeight * 0.75, characterWidth * 1.3);
        ArrayList<ArrayList<Rectangle>> linesSplit = TesseractUtils.splitLinesBasedOnGap(okSizedBoxes, characterWidth * 2.5);
        ArrayList<ArrayList<Rectangle>> linesWithMinimumLength = TesseractUtils.dropLinesWithInsufficientBoxes(linesSplit, minimumRequiredBoxesPerLine);
        return linesWithMinimumLength;
    }
}
