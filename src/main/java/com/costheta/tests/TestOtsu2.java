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

public class TestOtsu2 {

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
            ImageUtils.writeFile(contrastEnhanced, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "14x14contrastEnhanced1.png");
            // contrastEnhanced = ImageUtils.enhanceContrastLocally(contrastEnhanced, (int) (image.getWidth() * 1.0/6),(int) (image.getHeight() * 1.0/6),160, false);
            // ImageUtils.writeFile(contrastEnhanced, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "14x14contrastEnhanced2.png");
            // BufferedImage blurred = ImageUtils.blur(contrastEnhanced, 5, 5);
            // blurred = ImageUtils.blur(blurred, 5, 5);
            // blurred = ImageUtils.blur(blurred, 5, 5);
            // ImageUtils.writeFile(blurred, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "blurred.png");
            // blurred = ImageUtils.rankFilter(blurred, 3, 3, 50);
            // blurred = ImageUtils.rankFilter(blurred, 3, 3, 50);
            // ImageUtils.writeFile(blurred, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "blurMedianFiltered.png");
            // blurred = ImageUtils.rankFilter(blurred, 3, 5, 50);
            // blurred = ImageUtils.findEdgesAggressively(blurred);
            // blurred = ImageUtils.invert(blurred);
            // ImageUtils.writeFile(blurred, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "edges.png");
            // blurred = ImageUtils.stretchHistogram(blurred);
            // ImageUtils.writeFile(blurred, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "edgesStretched.png");
            // blurred = ImageUtils.binarize(blurred, 196);
            // ImageUtils.writeFile(blurred, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "edgeBoundaries.png");
            int average = ImageUtils.getAverage(image);
            double requiredGamma = ImageUtils.getGamma(average);
            double edgeGamma = requiredGamma;

            BufferedImage geImage = ImageUtils.constantGammaEnhancement(image, edgeGamma);
            geImage = ImageUtils.stretchHistogram(geImage);
            // ImageUtils.writeFile(geImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "stretchedOriginal.png");

            Pix pix = ImageUtils.getDepth8Pix(geImage);
            Pix normBackPix = Leptonica1.pixBackgroundNormFlex(pix, 7, 7, 3, 3, 50);
            BufferedImage nbImageOriginal = ImageUtils.convertPixToImage(normBackPix);

            // ImageUtils.writeFile(geImage, "png", debugDirectory + fileName  + "-" + imageCounter++ + "-" + "geImage.png");

            BufferedImage medianFilterImage = ImageUtils.medianFilter(nbImageOriginal, 3, 3);
            // ImageUtils.writeFile(medianFilterImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "medianFilter.png");

            BufferedImage sobelEdgeImage = ImageUtils.findEdgesAggressively(medianFilterImage, true);
            sobelEdgeImage = ImageUtils.invert(sobelEdgeImage);

            // ImageUtils.writeFile(sobelEdgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "sobelEdgesRaw.png");

            // sobelEdgeImage = ImageUtils.sharpen(sobelEdgeImage, 3);
            sobelEdgeImage = ImageUtils.binarize(sobelEdgeImage, 64); // conservative
            // ImageUtils.writeFile(sobelEdgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "sobelEdges.png");

            GrayU8 gray = ConvertBufferedImage.convertFrom(medianFilterImage, (GrayU8) null);
            GrayU8 edgeGray = gray.createSameShape();
            // Create a canny edge detector which will dynamically compute the threshold based on maximum edge intensity
            // It has also been configured to save the trace as a graph.  This is the graph created while performing
            // hysteresis thresholding.
            CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(1, true, true, GrayU8.class, GrayS16.class);
            // The edge image is actually an optional parameter.  If you don't need it just pass in null
            canny.process(gray, 0.1f, 0.3f, edgeGray);
            BufferedImage edgeImage = new BufferedImage(edgeGray.width, edgeGray.height, BufferedImage.TYPE_INT_ARGB);
            edgeImage = ConvertBufferedImage.convertTo(edgeGray, null);

            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalEdgeImage.png");

            edgeImage = ImageUtils.fullImageGammaEnhancementWithBias(edgeImage);

            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalEdgeGE.png");

            edgeImage = ImageUtils.invert(edgeImage);

            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "edgeImage_GE.png");

            average = ImageUtils.getAverage(edgeImage);
            edgeImage = ImageUtils.binarize(edgeImage, average);

            // ImageUtils.writeFile(edgeImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "canny.png");

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

            // BufferedImage histogramEqualised = ImageUtils.stretchHistogram(image);
            // BufferedImage localHistogram = ImageUtils.stretchLocalHistogram(histogramEqualised, (histogramEqualised.getWidth() + 5) / 6, (histogramEqualised.getWidth() + 5) / 6);

            // BufferedImage regionOfInterest = ImageUtils.clipBI(image, characterBounds);
//            BufferedImage regionOfInterest = ImageUtils.clipBI(contrastEnhanced, characterBounds);
//            average = ImageUtils.getAverage(regionOfInterest);
//            if (average > 240) {
//                regionOfInterest = ImageUtils.constantGammaEnhancement(regionOfInterest, 0.10);
//            } else {
//                if (average > 235) {
//                    regionOfInterest = ImageUtils.constantGammaEnhancement(regionOfInterest, 0.4);
//                } else {
//                    if (average > 230) {
//                        regionOfInterest = ImageUtils.constantGammaEnhancement(regionOfInterest, 0.60);
//                    } else {
//                        if (average > 225) {
//                            regionOfInterest = ImageUtils.constantGammaEnhancement(regionOfInterest, 0.80);
//                        }
//                    }
//                }
//            }
            // ImageUtils.writeFile(regionOfInterest, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "regionOfInterest.png");
            // BufferedImage RoI = ImageUtils.clipBI(geImage, characterBounds);
            BufferedImage RoI = ImageUtils.clipBI(contrastEnhanced, characterBounds);
            ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalRoI.png");
            BufferedImage originalClipped = ImageUtils.clipBI(image, characterBounds);
            ImageUtils.writeFile(originalClipped, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "originalClipped.png");
            average = ImageUtils.getAverage(originalClipped);
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
            ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "workingRoI.png");
            RoI = ImageUtils.stretchLocalHistogram(RoI,RoI.getWidth(),(RoI.getHeight() + 2) / 3);
            ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-LocallyStretched.png");
            BufferedImage otsued = ImageUtils.relativeGammaContrastEnhancement(RoI, (int) RoI.getWidth() / 2, (int) RoI.getHeight(), 33);
            ImageUtils.writeFile(otsued, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-RGE.png");
            // otsued = ImageUtils.enhanceContrastLocally(otsued, (int) RoI.getWidth() / 2, (int) RoI.getHeight() / 2, 255, false);
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

            RoI = ImageUtils.stretchHistogram(RoI);
            Pix pix1 = ImageUtils.getDepth8Pix(RoI);
            Pix normBackPix1 = Leptonica1.pixBackgroundNormFlex(pix1, 4, 4, 2, 2, 0);
            RoI = ImageUtils.convertPixToImage(normBackPix1);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-BN.png");
            // RoI = ImageUtils.enhanceContrastLocally(RoI, (int) (characterBounds.getHeight() * 0.75), (int) (characterBounds.getHeight() * 0.75), 250, false);
            // ImageUtils.writeFile(RoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-CN.png");

            // BufferedImage contrastEnhancedImage = ImageUtils.enhanceContrastLocally(regionOfInterest, (int) (characterBounds.getHeight() * 1.5), (int) (characterBounds.getHeight() * 1.5), 250, false);
            // ImageUtils.writeFile(contrastEnhancedImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "contrastEnhancedImage.png");
            // BufferedImage regionOfInterest = ImageUtils.clipBI(localHistogram, characterBounds);

            // BufferedImage contrastEnhancedImage1 = ImageUtils.enhanceContrastLocally(contrastEnhancedImage, (int) (characterBounds.getHeight() * 0.5), (int) (characterBounds.getHeight() * 0.5), 250, true);
            // ImageUtils.writeFile(contrastEnhancedImage1, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "contrastEnhancedImage.png");

            // BufferedImage edges = ImageUtils.findEdgesConservatively(contrastEnhancedImage1);
            BufferedImage edges = ImageUtils.findEdgesConservatively(RoI, true);
            edges = ImageUtils.invert(edges);
            edges = ImageUtils.sharpen(edges, 3);
            edges = ImageUtils.binarize(edges, 6);
            cleaningKernels.clear();
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(3,5));
            cleaningKernels.add(new CleaningKernel(5,3));
            cleaningKernels.add(new CleaningKernel(7,3));
            cleaningKernels.add(new CleaningKernel(3,7));
            cleaningKernels.add(new CleaningKernel(5,7));
            cleaningKernels.add(new CleaningKernel(7,5));
            cleaningKernels.add(new CleaningKernel(9,5));
            cleaningKernels.add(new CleaningKernel(11,5));
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(9,5));
            cleaningKernels.add(new CleaningKernel(9,5));
            cleaningKernels.add(new CleaningKernel(7,5));
            cleaningKernels.add(new CleaningKernel(3,3));
            cleaningKernels.add(new CleaningKernel(5,5));
            cleaningKernels.add(new CleaningKernel(11,5));
            cleaningKernels.add(new CleaningKernel(11,13));
            cleaningKernels.add(new CleaningKernel(17,4));
            cleaningKernels.add(new CleaningKernel(17,3));
            edges = ImageUtils.removeSmallTrails(edges,cleaningKernels,true);
            ImageUtils.writeFile(edges, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "RoI-edges.png");

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
            // ImageUtils.writeFile(gammaCorrectedRoI, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "gammaCorrectedRoIFinal.png");
            // gammaCorrectedRoI = ImageUtils.relativeScaledGammaContrastEnhancementSkipWhites(gammaCorrectedRoI, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 50, 242);
            // BufferedImage histStretched = ImageUtils.stretchHistogram(regionOfInterest);

            // ImageUtils.writeFile(histStretched, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "histStretched.png");

            // BufferedImage roIGE = ImageUtils.fullImageGammaEnhancement(regionOfInterest, ImageUtils.GAMMA_ENHANCEMENT_HIGH);
            // ImageUtils.writeFile(roIGE, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "roIGE.png");

            // BufferedImage histogramEq = ImageUtils.stretchHistogram(contrastEnhancedImage);
            // ImageUtils.writeFile(histogramEq, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "histogramEq.png");

            // BufferedImage roILocalHistogram = ImageUtils.stretchLocalHistogram(contrastEnhancedImage1, (contrastEnhancedImage1.getWidth() + 1) / 2, (contrastEnhancedImage1.getWidth() + 1) / 2);
            // ImageUtils.writeFile(roILocalHistogram, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "roilocalHistogram.png");

            // BufferedImage sharpenedImage = ImageUtils.sharpen(contrastEnhancedImage1, 3);
            // ImageUtils.writeFile(sharpenedImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "sharpenedImage.png");

            // Pix preFinalPix = ImageUtils.getDepth8Pix(sharpenedImage);
            Pix preFinalPix = ImageUtils.getDepth8Pix(relevantImage);
            Pix contNormPix = Leptonica1.pixContrastNorm(null, preFinalPix, preFinalPix.h / 2, preFinalPix.h / 2, 60, 2, 2);
            BufferedImage preFinalImage = ImageUtils.convertPixToImage(contNormPix);
            // ImageUtils.writeFile(preFinalImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "preFinalImage.png");
            LeptUtils.dispose(preFinalPix);
            LeptUtils.dispose(contNormPix);

            BufferedImage otsuImage = OtsuThreshold.getLocalOtsuImageStandard(preFinalImage, (preFinalImage.getWidth() + 0) / 1, (preFinalImage.getHeight() + 0) / 1);
            otsuImage = ImageUtils.extractClips(otsuImage, wordBoxes);
            // BufferedImage gammaCorrectedOriginal = ImageUtils.relativeGammaContrastEnhancementWithPercentTolerance(geImage, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 5);
            // BufferedImage gammaCorrectedOriginal = ImageUtils.relativeGammaContrastEnhancement(geImage, (int) characterBounds.getHeight(), (int) characterBounds.getHeight(), 75);
            // ImageUtils.writeFile(gammaCorrectedOriginal, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "otsuInput.png");
            // BufferedImage otsuImage = OtsuThreshold.getLocalOtsuImageStandard(gammaCorrectedOriginal, (geImage.getWidth() + 1) / 2, (geImage.getHeight() + 1) / 2);
            // BufferedImage otsuImage = ImageUtils.binarize(gammaCorrectedOriginal, 8);
            // ImageUtils.writeFile(nbImageOriginal, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "otsuInput.png");
            // BufferedImage otsuImage = ImageUtils.binarize(gammaCorrectedRoI, 16);

            // ImageUtils.writeFile(otsuImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "otsuImage.png");

            // BufferedImage finalImage = ImageUtils.retainClip(otsuImage, characterBounds);
            // ImageUtils.writeFile(finalImage, "png", debugDirectory + fileName + "-" + imageCounter++ + "-" + "finalImage.png");
            LeptUtils.dispose(pix);
            LeptUtils.dispose(normBackPix);
        }
        long timeElapsed = Duration.between(t, Instant.now()).toMillis();
        System.out.println("Time taken per image = " + timeElapsed / (files.size() > 0 ? files.size() : 1));
        System.out.println("Done");
    }




}
