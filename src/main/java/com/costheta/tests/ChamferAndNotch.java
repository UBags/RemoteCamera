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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ChamferAndNotch extends PartProcessor {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\KEMS\\Pictures";
    private static String fileName = "6-original.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";

    public static void main(String[] args) throws TesseractException, IOException {

        System.out.println("Running ChamferAndNotch");
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {
        }
        debugDirectory = debugDirectory + "/";

        // System.out.println("Reached here - 1");
        BufferedImage image = ImageIO.read(new File(inputDirectory + "/" + fileName));
        ChamferAndNotch aProcessor = new ChamferAndNotch();
        aProcessor.process(image);
        System.exit(0);
    }

    public ChamferAndNotch() {
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

        GrayU8 gray = ConvertBufferedImage.convertFrom(bnImage1,(GrayU8)null);
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

        ImageUtils.writeFile(visualEdgeContour, "png", debugDirectory + "15 - CannyEdge.png");

        LeptUtils.dispose(originalReduced8);
        System.out.println("Done");
    }
}
