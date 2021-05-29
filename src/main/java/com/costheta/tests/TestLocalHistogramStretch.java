package com.costheta.tests;

import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import com.costheta.image.thresholds.OtsuThreshold;
import com.costheta.image.utils.CleaningKernel;
import com.costheta.image.utils.ImageUtils;
import javafx.scene.shape.Rectangle;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestLocalHistogramStretch {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\Test pictures";
    private static String debugDirectory = inputDirectory + "/debug/";
    private static String ext = "png";

    public static void main(String[] args) throws TesseractException, IOException {

        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        List<String> files = ImageUtils.filterFiles(ImageUtils.listFiles(inputDirectory, 1), ext, true);
        Instant t = Instant.now();
        for (String filePathString : files) {
            Path filePath = Paths.get(filePathString);
            String fileNameWithExt = filePath.getFileName().toString();
            System.out.println("Processing " + fileNameWithExt);
            System.out.println("----------------------------");
            int indexOfDot = fileNameWithExt.indexOf(".");
            String fileName = fileNameWithExt.substring(0,indexOfDot);

            BufferedImage image = ImageIO.read(new File(filePathString));
            ImageUtils.writeFile(image, "png", debugDirectory + fileName + "A-" + "original.png");

            BufferedImage histogramEqualised = ImageUtils.stretchHistogram(image);
            ImageUtils.writeFile(histogramEqualised, "png", debugDirectory + fileName + "B-" + "HistEq.png");

            BufferedImage localHistogram = ImageUtils.stretchLocalHistogram(histogramEqualised, (histogramEqualised.getWidth() + 5) / 6, (histogramEqualised.getWidth() + 5) / 6);
            ImageUtils.writeFile(localHistogram, "png", debugDirectory + fileName + "C-" + "LocalHistEq.png");

        }
        long timeElapsed = Duration.between(t, Instant.now()).toMillis();
        System.out.println("Time taken per image = " + timeElapsed / (files.size() > 0 ? files.size() : 1));
        System.out.println("Done");
    }




}
