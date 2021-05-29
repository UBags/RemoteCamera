package com.costheta.tests;

import boofcv.alg.enhance.EnhanceImageOps;
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

public class TestOtsu3 {

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
            int imageCounter = 1;
            Path filePath = Paths.get(filePathString);
            String fileNameWithExt = filePath.getFileName().toString();
            System.out.println("Processing " + fileNameWithExt);
            System.out.println("----------------------------");
            int indexOfDot = fileNameWithExt.indexOf(".");
            String fileName = fileNameWithExt.substring(0,indexOfDot);

            BufferedImage image = ImageIO.read(new File(filePathString));
            int lesserDimension = (int) Math.min(image.getHeight(), image.getWidth());
            ImageUtils.writeFile(image, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "original.png");
            BufferedImage contrastEnhanced = ImageUtils.enhanceContrastLocally(image, (int) (lesserDimension * 2.0/3),(int) (lesserDimension * 2.0/3),256, false);
            // ImageUtils.writeFile(contrastEnhanced, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "14x14contrastEnhanced1.png");
            int average = ImageUtils.getAverage(image);
            double requiredGamma = ImageUtils.getGamma(average);
            double edgeGamma = requiredGamma;

            BufferedImage geImage = ImageUtils.constantGammaEnhancement(image, edgeGamma);
            geImage = ImageUtils.stretchHistogram(geImage);

            Pix pix = ImageUtils.getDepth8Pix(geImage);
            Pix normBackPix = Leptonica1.pixBackgroundNormFlex(pix, 7, 7, 3, 3, 50);
            BufferedImage nbImageOriginal = ImageUtils.convertPixToImage(normBackPix);
            // ImageUtils.writeFile(geImage, "png", debugDirectory + fileName  + "-" + imageCounter++ + "-" + "geImage.png");

            BufferedImage medianFilterImage = ImageUtils.medianFilter(nbImageOriginal, 3, 3);
            // ImageUtils.writeFile(medianFilterImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "medianFilter.png");

            BufferedImage sobelEdgeImage = ImageUtils.findEdgesAggressively(medianFilterImage, true);
            sobelEdgeImage = ImageUtils.invert(sobelEdgeImage);
            ImageUtils.writeFile(sobelEdgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "sobelEdges.png");
            sobelEdgeImage = ImageUtils.binarize(sobelEdgeImage, 64); // conservative

            BufferedImage edgeImage = ImageUtils.findCannyEdges(medianFilterImage);
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalEdgeImage.png");
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalCannyEdge.png");

            // Combine the 2 edge images to get the best edge image possible,
            // and then clean it
            edgeImage = ImageUtils.imageBinaryAnd(edgeImage, sobelEdgeImage);
            ArrayList<CleaningKernel> cleaningKernels = new ArrayList<>();
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(3,5));
            cleaningKernels.add(new CleaningKernel(5,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(edgeImage.getWidth() / 3,5));
            cleaningKernels.add(new CleaningKernel(edgeImage.getWidth() / 3,5));
            cleaningKernels.add(new CleaningKernel(5,edgeImage.getHeight() / 2));
            cleaningKernels.add(new CleaningKernel(5,edgeImage.getHeight() / 2));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(11,3));
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            edgeImage = ImageUtils.removeSmallTrails(edgeImage,cleaningKernels,true);
            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "combinedEdgesCleaned.png");

            Rectangle characterBounds = ImageUtils.getBoundingBoxEnvelope(edgeImage, 20, 21);
            // expand the bounds of the rectangle by 1 pixel up and down and 2 pixels to the left and right
            int xExpansion = 2;
            int yExpansion = 1;
            characterBounds = new Rectangle(characterBounds.getX() - xExpansion, characterBounds.getY() - yExpansion, characterBounds.getWidth() + 2 * xExpansion, characterBounds.getHeight() + 2 * yExpansion);
            BufferedImage relevantEdges = ImageUtils.retainClip(edgeImage, characterBounds);
            // ImageUtils.writeFile(relevantEdges, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "relevantEdges.png");

            // BufferedImage RoI = ImageUtils.clipBI(contrastEnhanced, characterBounds);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalRoI.png");

            BufferedImage originalClipped = ImageUtils.clipBI(image, characterBounds);
            ImageUtils.writeFile(originalClipped, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalClipped.png");
            BufferedImage RoI = originalClipped;
            average = ImageUtils.getAverage(originalClipped);
            if (average > 245) {
                RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.08);
            } else {
                if (average > 240) {
                    RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.14);
                } else {
                    if (average > 235) {
                        RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.25);
                    } else {
                        if (average > 230) {
                            RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.40);
                        } else {
                            if (average > 225) {
                                RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.60);
                            } else {
                                if (average > 220) {
                                    RoI = ImageUtils.constantGammaEnhancement(originalClipped, 0.80);
                                }
                            }
                        }
                    }
                }
            }
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "workingRoI.png");
            RoI = ImageUtils.stretchLocalHistogram(RoI,RoI.getWidth(),(RoI.getHeight() + 2) / 3);
            BufferedImage edges = ImageUtils.findEdgesConservatively(RoI, true);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-LocallyStretched.png");
            BufferedImage otsued = ImageUtils.relativeGammaContrastEnhancement(RoI, (int) RoI.getWidth() / 2, (int) RoI.getHeight(), 33);
            // ImageUtils.writeFile(otsued, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-RGE.png");
            otsued = OtsuThreshold.getLocalOtsuImageStandard(otsued, otsued.getWidth(), otsued.getHeight());
            ImageUtils.writeFile(otsued, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "otsued.png");
            otsued = ImageUtils.makeBorderWhite(otsued, 1);
            cleaningKernels = new ArrayList<>();
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(3,5));
            cleaningKernels.add(new CleaningKernel(5,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(5,7));
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(3,5));
            otsued = ImageUtils.removeSmallTrails(otsued,cleaningKernels,true);
            ImageUtils.writeFile(otsued, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "otsuCleaned.png");

            ArrayList<Rectangle> wordBoxes = ImageUtils.getBoundingBoxesInLine(edges, 20, 21);
            // System.out.println("In image " + fileName + " there are " + wordBoxes.size() + " rectangles");
            BufferedImage relevantImage = ImageUtils.extractClips(contrastEnhanced, wordBoxes);
            // ImageUtils.writeFile(relevantImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "relevantImage.png");

            RoI = ImageUtils.extractBackgroundNormalisedClips(RoI, wordBoxes);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "bn-RoI.png");
            for (Rectangle word : wordBoxes) {
                RoI = ImageUtils.enhanceContrastLocally(RoI, word, (int) (word.getWidth() *0.1), (int) (word.getHeight() * 0.1), 245, false);
            }
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "ec-RoI.png");
            // RoI = ImageUtils.stretchHistogramWithWhiteBias(RoI, 240);

            int smallestHeight = Integer.MAX_VALUE;
            int smallestWidth = Integer.MAX_VALUE;
            for (Rectangle word : wordBoxes) {
                smallestHeight = Math.min(smallestHeight, (int) word.getHeight());
                smallestWidth = Math.min(smallestWidth, (int) word.getWidth());
            }

            RoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(RoI, smallestWidth / 2, smallestHeight / 2, 50, 250);
            RoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(RoI, smallestWidth, smallestHeight, 50, 250);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI.png");

            BufferedImage gammaCorrectedRoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(relevantImage, smallestWidth / 2, smallestHeight / 2, 55, 250);
            // ImageUtils.writeFile(gammaCorrectedRoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "gammaCorrectedRoI.png");
            for (Rectangle box : wordBoxes) {
                gammaCorrectedRoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(gammaCorrectedRoI, box, smallestWidth, smallestHeight, 55, 240);
            }
            gammaCorrectedRoI = ImageUtils.binarize(gammaCorrectedRoI, 64);
            Pix preFinalPix = ImageUtils.getDepth8Pix(relevantImage);
            Pix contNormPix = Leptonica1.pixContrastNorm(null, preFinalPix, preFinalPix.h / 2, preFinalPix.h / 2, 60, 2, 2);
            BufferedImage preFinalImage = ImageUtils.convertPixToImage(contNormPix);
            // ImageUtils.writeFile(preFinalImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "preFinalImage.png");
            LeptUtils.dispose(preFinalPix);
            LeptUtils.dispose(contNormPix);

            BufferedImage otsuImage = OtsuThreshold.getLocalOtsuImageStandard(preFinalImage, (preFinalImage.getWidth() + 0) / 1, (preFinalImage.getHeight() + 0) / 1);
            otsuImage = ImageUtils.extractClips(otsuImage, wordBoxes);
            LeptUtils.dispose(pix);
            LeptUtils.dispose(normBackPix);
        }
        long timeElapsed = Duration.between(t, Instant.now()).toMillis();
        System.out.println("Time taken per image = " + timeElapsed / (files.size() > 0 ? files.size() : 1));
        System.out.println("Done");
    }




}
