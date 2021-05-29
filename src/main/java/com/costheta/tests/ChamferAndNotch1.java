package com.costheta.tests;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import com.costheta.image.utils.ImageUtils;
import net.sourceforge.lept4j.*;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.TesseractException;
import org.ddogleg.struct.DogArray;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ChamferAndNotch1 extends PartProcessor {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\KEMS\\Pictures";
    private static String fileName = "6-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    public static void main(String[] args) throws TesseractException, IOException {

        System.out.println("Running ChamferAndNotch1");
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        debugDirectory = debugDirectory + "/";

        // System.out.println("Reached here - 1");
        BufferedImage image = ImageIO.read(new File(inputDirectory + "/" + fileName));
        ChamferAndNotch1 aProcessor = new ChamferAndNotch1();
        aProcessor.process(image);
        System.exit(0);
    }

    public ChamferAndNotch1() {
        super(null);
    }

    @Override
    public void process(BufferedImage image) {
        // System.out.println(image);
        Pix originalPixLarge = ImageUtils.convertImageToPix(image);
        Box clipBox = Leptonica1.boxCreate(420,450, 1020, 1000);
        Pix originalPix = Leptonica1.pixClipRectangle(originalPixLarge, clipBox, null);
        LeptUtils.dispose(originalPixLarge);
        LeptUtils.dispose(clipBox);

        // Assumes 12 MP picture input
        // Hence, downsampling twice
        Pix originalPixReduced = Leptonica1.pixScaleAreaMap2(originalPix);
        Pix originalReduced8 = ImageUtils.getDepth8Pix(originalPixReduced);
        Leptonica1.pixWrite(debugDirectory + "1 - Original8.png", originalReduced8, ILeptonica.IFF_PNG);
        BufferedImage originalBI = ImageUtils.convertPixToImage(originalReduced8);

        LeptUtils.dispose(originalPix);
        LeptUtils.dispose(originalPixReduced);

        Pix bnPix = Leptonica1.pixBackgroundNormFlex(originalReduced8, 7, 7, 1, 1, 160);
        Leptonica1.pixWrite(debugDirectory + "2 - BN Image.png", bnPix, ILeptonica.IFF_PNG);

        Pix cnPix = Leptonica1.pixContrastNorm(null, bnPix, bnPix.w / 8,
                bnPix.h / 5, 100, 2, 2);
        Leptonica1.pixWrite(debugDirectory + "3 - CN Image.png", cnPix, ILeptonica.IFF_PNG);
        BufferedImage cnImage = ImageUtils.convertPixToImage(cnPix);
        LeptUtils.dispose(bnPix);

        Pix bnPix1 = Leptonica1.pixBackgroundNormFlex(cnPix, 7, 7, 2, 2, 160);
        Leptonica1.pixWrite(debugDirectory + "4 - BN Image1.png", bnPix1, ILeptonica.IFF_PNG);
        BufferedImage bnImage1 = ImageUtils.convertPixToImage(bnPix1);
        LeptUtils.dispose(bnPix1);

        Pix pixSobel = Leptonica1.pixSobelEdgeFilter(cnPix, ILeptonica.L_ALL_EDGES);
        Pix pixSobelInvert = Leptonica1.pixInvert(null, pixSobel);
        Pix mask1_Input_8bpp = Leptonica1.pixGammaTRC(null, pixSobelInvert, 0.5f, 0, 255);
        Leptonica1.pixWrite(debugDirectory + "5 - Mask 1 Input.png", mask1_Input_8bpp, ILeptonica.IFF_PNG);

        Pix mask1_Input_1bpp = ImageUtils.getDepth1Pix(mask1_Input_8bpp, 236);
        Pix mask1_1bpp = Leptonica1.pixCloseBrick(null, mask1_Input_1bpp,31,15);
        Pix mask1_1bpp_Temp1 = Leptonica1.pixOpenBrick(null, mask1_1bpp,9,1);
        Pix mask1_1bpp_Temp2 = Leptonica1.pixOpenBrick(null, mask1_1bpp_Temp1,1,9);

        LeptUtils.dispose(pixSobel);
        LeptUtils.dispose(pixSobelInvert);
        LeptUtils.dispose(mask1_Input_8bpp);
        LeptUtils.dispose(cnPix);
        LeptUtils.dispose(mask1_Input_1bpp);
        LeptUtils.dispose(mask1_1bpp);
        LeptUtils.dispose(mask1_1bpp_Temp1);

        int connectivity = 4;
        Boxa boxes = Leptonica1.pixConnCompBB(mask1_1bpp_Temp2, connectivity);
        int numberOfBoxes = Leptonica1.boxaGetCount(boxes);
        Pix copyOfMask1 = Leptonica1.pixCreate(Leptonica1.pixGetWidth(mask1_1bpp_Temp2),
                Leptonica1.pixGetHeight(mask1_1bpp_Temp2), 1);
        Leptonica1.pixSetBlackOrWhite(copyOfMask1, ILeptonica.L_SET_WHITE);
        for (int i = 0; i < numberOfBoxes; ++i) {
            Box aBox = Leptonica1.boxaGetBox(boxes, i, ILeptonica.L_CLONE);
            if ((aBox.w < 40) || (aBox.h < 15)) {
                LeptUtils.dispose(aBox);
                continue;
            }
            Pix pixOriginalX = Leptonica1.pixClipRectangle(mask1_1bpp_Temp2, aBox, null);
            Leptonica1.pixRasterop(copyOfMask1, aBox.x, aBox.y,
                    aBox.w, aBox.h,
                    ILeptonica.PIX_SRC, pixOriginalX, 0, 0);
            LeptUtils.dispose(aBox);
            LeptUtils.dispose(pixOriginalX);
        }

        Pix mask1_8bpp = ImageUtils.getDepth8Pix(copyOfMask1);
        BufferedImage initialmask = ImageUtils.convertPixToImage(mask1_8bpp);
        Leptonica1.pixWrite(debugDirectory + "6 - Mask 1.png", mask1_8bpp, ILeptonica.IFF_PNG);

        LeptUtils.dispose(copyOfMask1);
        LeptUtils.dispose(mask1_1bpp_Temp2);
        LeptUtils.dispose(boxes);
        LeptUtils.dispose(mask1_8bpp);

        BufferedImage bnSharp1 = ImageUtils.rankFilter(bnImage1, 9, 9, 40);
        bnSharp1 = ImageUtils.rankFilter(bnSharp1, 9, 9, 60);
        BufferedImage bnSharp1_Sharpened = ImageUtils.unsharpFilter.filter(bnSharp1, null);
        ImageUtils.writeFile(bnSharp1_Sharpened, "png", debugDirectory + "7A - Median Filter.png");
        BufferedImage sobel1_0 = ImageUtils.sobelVerticalFilter(bnSharp1_Sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel1_0, "png", debugDirectory + "7A - Sobel10.png");
        BufferedImage sobel2_0 = ImageUtils.sobelHorizontalFilter(bnSharp1_Sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel2_0, "png", debugDirectory + "7B - Sobel20.png");
        BufferedImage sobel3_0 = ImageUtils.sobelNESWDiagonalFilter(bnSharp1_Sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel3_0, "png", debugDirectory + "8C - Sobel30.png");
        BufferedImage sobel4_0 = ImageUtils.sobelNWSEDiagonalFilter(bnSharp1_Sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel4_0, "png", debugDirectory + "8D - Sobel40.png");

        BufferedImage edge11 = ImageUtils.edgeFilter(bnSharp1, 3, 3);
        BufferedImage edge21 = ImageUtils.edgeFilter(bnSharp1, 5, 5);
        BufferedImage edge10 = ImageUtils.imageGrayOr(edge11, edge21);
        ImageUtils.writeFile(edge10, "png", debugDirectory + "7B - Edge0.png");

        BufferedImage sobel0 = ImageUtils.imageGrayOr(sobel1_0, sobel2_0);
        // ImageUtils.writeFile(sobel0, "png", debugDirectory + "6 - Sobel0.png");
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel3_0);
        // ImageUtils.writeFile(sobel0, "png", debugDirectory + "6A - Sobel0.png");
        sobel0 = ImageUtils.imageGrayOr(sobel0, sobel4_0);
        ImageUtils.writeFile(sobel0, "png", debugDirectory + "7C - Sobel0.png");
        sobel0 = ImageUtils.imageGrayOr(sobel0, edge10);
        ImageUtils.writeFile(sobel0, "png", debugDirectory + "7D - Combined0.png");

        BufferedImage median = ImageUtils.rankFilter(bnImage1, 5, 5, 40);
        median = ImageUtils.rankFilter(median, 5, 5, 60);
        median = ImageUtils.rankFilter(median, 5, 5, 80);
        ImageUtils.writeFile(median, "png", debugDirectory + "8A - Median Filter.png");

        BufferedImage sharpened = ImageUtils.unsharpFilter.filter(median, null);
        // sharpened = ImageUtils.unsharpFilter.filter(sharpened, null);
        // ImageUtils.writeFile(median, "png", debugDirectory + "4B - Sharpened.png");

        BufferedImage sobel1 = ImageUtils.sobelVerticalFilter(sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel1, "png", debugDirectory + "5A - Sobel.png");
        BufferedImage sobel2 = ImageUtils.sobelHorizontalFilter(sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel2, "png", debugDirectory + "5B - Sobel.png");
        BufferedImage sobel3 = ImageUtils.sobelNESWDiagonalFilter(sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel3, "png", debugDirectory + "5C - Sobel.png");
        BufferedImage sobel4 = ImageUtils.sobelNWSEDiagonalFilter(sharpened, 3, 3, true);
        // ImageUtils.writeFile(sobel4, "png", debugDirectory + "5D - Sobel.png");

        BufferedImage edge1 = ImageUtils.edgeFilter(median, 3, 3);
        BufferedImage edge2 = ImageUtils.edgeFilter(median, 5, 5);
        BufferedImage edge = ImageUtils.imageGrayOr(edge1, edge2);
        ImageUtils.writeFile(edge, "png", debugDirectory + "8B - Edge.png");

        BufferedImage sobel = ImageUtils.imageGrayOr(sobel1, sobel2);
        // ImageUtils.writeFile(sobel, "png", debugDirectory + "6 - Sobel.png");
        sobel = ImageUtils.imageGrayOr(sobel, sobel3);
        // ImageUtils.writeFile(sobel, "png", debugDirectory + "6A - Sobel.png");
        sobel = ImageUtils.imageGrayOr(sobel, sobel4);
        ImageUtils.writeFile(sobel, "png", debugDirectory + "8C - Sobel.png");
        sobel = ImageUtils.imageGrayOr(sobel, edge);
        ImageUtils.writeFile(sobel, "png", debugDirectory + "8D - Combined.png");

        BufferedImage bin_closedBI_Temp = ImageUtils.binarize(sobel, 128);
        ImageUtils.writeFile(bin_closedBI_Temp, "png", debugDirectory + "10 - Binarized Image.png");

        BufferedImage aMaskBI = ImageUtils.invert(bin_closedBI_Temp);
        ImageUtils.writeFile(aMaskBI, "png", debugDirectory + "11 - Mask.png");

        BufferedImage edgesMasked = ImageUtils.invertedImageMask(sobel0, initialmask);
        ImageUtils.writeFile(edgesMasked, "png", debugDirectory + "12 - Edges Masked.png");

        BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(null, GrayU8.class);

        GrayU8 input = ConvertBufferedImage.convertFromSingle(edgesMasked, null, GrayU8.class);
        GrayU8 binary = new GrayU8(input.width,input.height);

        // Binarization is done outside to allows creative tricks.  For example, when applied to a chessboard
        // pattern where square touch each other, the binary image is eroded first so that they don't touch.
        // The squares are expanded automatically during the subpixel optimization step.
        // int threshold = (int) GThresholdImageOps.computeOtsu(input, 0, 255);
        int threshold = 192;
        ThresholdImageOps.threshold(input, binary, threshold, false);
        BufferedImage thresholdedImage = ConvertBufferedImage.convertTo(binary, null);
        ImageUtils.writeFile(edgesMasked, "png", debugDirectory + "13 - Thresholded.png");

        // it takes in a grey scale image and binary image
        // the binary image is used to do a crude polygon fit, then the grey image is used to refine the lines
        // using a sub-pixel algorithm
        detector.process(input, binary);

        // visualize results by drawing red polygons
        DogArray<BinaryEllipseDetector.EllipseInfo> found = detector.getFound();
        Graphics2D g2 = edgesMasked.createGraphics();
        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.RED);
        System.out.println("Just before adding ellipses");
        for (int i = 0; i < found.size; i++) {
            System.out.println("Found an ellipse : " + found.get(i).ellipse);
            VisualizeShapes.drawEllipse(found.get(i).ellipse, g2);
        }

        if (found.size() == 0) {
            System.out.println("No ellipses found");
        }

        GrayU8 gray = ConvertBufferedImage.convertFrom(originalBI,(GrayU8)null);
        GrayU8 edgeImage = gray.createSameShape();

        // Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
        // It has also been configured to save the trace as a graph.  This is the graph created while performing
        // hysteresis thresholding.
        CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(1,true, true, GrayU8.class, GrayS16.class);

        // The edge image is actually an optional parameter.  If you don't need it just pass in null
        canny.process(gray,0.1f,0.2f,edgeImage);

        // First get the contour created by canny
        List<EdgeContour> edgeContours = canny.getContours();
        // The 'edgeContours' is a tree graph that can be difficult to process.  An alternative is to extract
        // the contours from the binary image, which will produce a single loop for each connected cluster of pixels.
        // Note that you are only interested in external contours.
        List<Contour> contours = BinaryImageOps.contourExternal(edgeImage, ConnectRule.EIGHT);

        // display the results
        BufferedImage visualBinary = VisualizeBinaryData.renderBinary(edgeImage, false, null);
        BufferedImage visualCannyContour = VisualizeBinaryData.renderContours(edgeContours,null,
                gray.width,gray.height,null);
        BufferedImage visualEdgeContour = new BufferedImage(gray.width, gray.height,BufferedImage.TYPE_INT_RGB);
        VisualizeBinaryData.render(contours, (int[]) null, visualEdgeContour);

        ImageUtils.writeFile(edgesMasked, "png", debugDirectory + "14 - Ellipses.png");
        ImageUtils.writeFile(visualEdgeContour, "png", debugDirectory + "15 - CannyEdge.png");

        LeptUtils.dispose(originalReduced8);
        System.out.println("Done");
    }
}
