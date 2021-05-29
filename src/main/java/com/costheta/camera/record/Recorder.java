package com.costheta.camera.record;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDevice;
import com.github.sarxos.webcam.WebcamDiscoveryService;
import com.github.sarxos.webcam.WebcamDriver;
import com.github.sarxos.webcam.WebcamLockException;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.ds.openimaj.OpenImajDriver;

import net.sourceforge.lept4j.Leptonica1;

public class Recorder extends JFrame implements Runnable, WebcamPanel.Painter {

    private String outputDirectory = "E:\\TechWerx\\CosTheta\\Recorder";

    private static final long serialVersionUID = 1L;
    private static int isWindows = 2;

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private Webcam webcam = null;
    private WebcamPanel.Painter painter = null;
    private BufferedImage troll = null;

    private String propFilePath = "recorderConfig.properties";
    private File propFile = new File(propFilePath);

    private boolean visibility = true;
    private String webcamKeyWord = "USB";
    private String imageOutputDirectory = "images";
    private long sleepTime = 10000;
    private int count = 0;

    public Recorder() throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        super();

        troll = ImageIO.read(new File("Initialisation.jpg")); // loads up libraries

        java.util.Properties props = new java.util.Properties();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(propFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (Exception e) {
                System.out.println("Property initialisation file " + propFilePath + " not found in the classpath "
                        + System.getProperty("java.class.path"));
                e.printStackTrace();
            }
        } else {
            System.out.println("InputStream = null");
            System.out.println("Property initialisation file " + propFilePath + " not found in the classpath "
                    + System.getProperty("java.class.path"));
        }
        outputDirectory = props.getProperty("outputDirectory", "/TechWerx/CosTheta/Recorder");
        visibility = Boolean.parseBoolean(props.getProperty("visibility", "true"));
        webcamKeyWord = props.getProperty("webcamKeyword", "USB");
        imageOutputDirectory = props.getProperty("imageOutputDirectory", "output");
        String sleep = props.getProperty("sleepTime", "10000");
        sleepTime = Long.parseLong(sleep);

        WebcamDriver ourDriver = null;
        if (isWindows != 1) {
            ourDriver = new OpenImajDriver();
            Webcam.setDriver(ourDriver);
        } else {
            ourDriver = Webcam.getDriver();
        }

        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams.isEmpty()) {
            System.exit(0);
        } else {
            // for (Webcam cam : webcams) {
            // System.out.println("Current view settings for " + cam.getName() + " are : " +
            // cam.getViewSize());
            // }
        }

        for (Webcam cam : webcams) {
            if ((cam.getName()).contains(webcamKeyWord)) {
                webcam = cam;
                break;
            }
        }

        if (webcam == null) {
            System.out.println("Current webcam = " + webcam);
            try {
                webcam = Webcam.getDefault(2000);
            } catch (Exception e) {

            }
        }

        if (webcam == null) {
            System.out.println("No viable webcam found");
            System.exit(0);
        }

        if (ourDriver == null) {
            ourDriver = Webcam.getDriver();
        }

        List<WebcamDevice> devices = ourDriver.getDevices();
        if (devices.size() < 2) {
            webcam = Webcam.getDefault();
        }
        Dimension preferredDimension = WebcamResolution.HD.getSize();
        for (WebcamDevice device : devices) {
            Dimension[] dimensions = device.getResolutions();
            int currentHeight = 0;
            for (Dimension dimension : dimensions) {
                if (dimension.height > currentHeight) {
                    preferredDimension = dimension;
                    currentHeight = dimension.height;
                }
            }
            device.setResolution(preferredDimension);
        }

        webcam.setViewSize(preferredDimension);
        try {
            webcam.open(true);
        } catch (WebcamLockException e) {
            System.out.println("Webcam is already in use and locked by another process");
            System.exit(0);
        }

        WebcamPanel panel = new WebcamPanel(webcam, false);
        // panel.setPreferredSize(WebcamResolution.HD.getSize());
        panel.setPreferredSize(preferredDimension);
        panel.setPainter(this);
        panel.setFPSDisplayed(true);
        panel.setFPSLimited(true);
        panel.setFPSLimit(25);
        panel.setPainter(this);
        panel.start();

        painter = panel.getDefaultPainter();

        add(panel);

        setTitle("Recorder");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(visibility);

        for (WebcamDevice device : devices) {
            System.out.println("Set resolution of device " + device.getName() + " to : width = "
                    + preferredDimension.width + "; height = " + preferredDimension.height);
        }

        WebcamDiscoveryService discovery = Webcam.getDiscoveryService();
        discovery.stop();


        // create the output directory
        try {
            Files.createDirectories(
                    Paths.get(new StringBuffer(outputDirectory).append("/").append(imageOutputDirectory).toString())
                            .toAbsolutePath());
        } catch (Exception e) {

        }

        System.out.println("Ready to record");

        EXECUTOR.execute(this);

    }

    @Override
    public void run() {

        while (true) {
            if (!webcam.isOpen()) {
                return;
            }
            webcam.getCustomViewSizes();
            BufferedImage input = webcam.getImage();
            processInput(input);
            try {
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (Exception e) {

            }
        }
    }

    private void processInput(BufferedImage image) {
        StringBuffer outputFilePath = new StringBuffer(outputDirectory).append("/").append(imageOutputDirectory)
                .append("/").append("image-").append(count++).append(".png");
        try {
            writeFile(image, "png", Paths.get(outputFilePath.toString()).toAbsolutePath().toString());
        } catch (Exception e) {

        }
    }

    @Override
    public void paintPanel(WebcamPanel panel, Graphics2D g2) {
        if (painter != null) {
            painter.paintPanel(panel, g2);
        }
    }

    @Override
    public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {

        if (painter != null) {
            painter.paintImage(panel, image, g2);
        }

    }

    private static int isWindows() {

        if (isWindows != 2) {
            return isWindows;
        }
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.indexOf("win") >= 0) {
            isWindows = 1;
        } else {
            isWindows = 0;
            Process plsof = null;
            // if Unix, then umount debugfs to save time during the loop that checks if a
            // file change triggered by watchservice response has finished being in writable
            // state
            try {
                plsof = new ProcessBuilder(new String[] { "sudo", "mount", "|", "grep", "debugfs" }).start();
                plsof.destroy();
            } catch (Exception ex) {
                // TODO: handle exception ...
            }
            try {
                plsof = new ProcessBuilder(new String[] { "sudo", "umount", "$(mount", "|", "grep", "debugfs", "|",
                        "awk", "'{print", "$3}')" }).start();
                plsof.destroy();
            } catch (Exception ex) {
                // TODO: handle exception ...
            }
        }
        return isWindows;

    }

    public static void main(String[] args) throws Exception {
        Leptonica1.setLeptDebugOK(0);
        isWindows();
        new Recorder();
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile) throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, 300);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi)
            throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                              float compressionQuality) throws Exception {

        if (bufferedImage == null) {
            return false;
        }
        RenderedImage[] input = new RenderedImage[1];
        input[0] = bufferedImage;
        return writeFile(input, formatName, localOutputFile, dpi, compressionQuality);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi)
            throws Exception {
        return writeFile(images, formatName, localOutputFile, dpi, 0.5f);
    }

    private boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
                              float compressionQuality) throws Exception {

        if (images == null) {
            throw new IllegalArgumentException("No images available for writing to : " + formatName + " file");
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);

        if (!writers.hasNext()) {
            throw new IllegalArgumentException("No writer available for: " + formatName + " files");
        }

        IIOImage temp = null;
        ImageTypeSpecifier its = null;
        IIOMetadata md = null;
        ImageWriter writer = null;
        ImageWriteParam writeParam = null;
        ImageOutputStream output = null;
        its = ImageTypeSpecifier.createFromRenderedImage(images[0]);
        boolean writerFound = false;

        try {
            // Loop until we get the best driver, i.e. one that supports
            // setting dpi in the standard metadata format; however we'd also
            // accept a driver that can't, if a better one can't be found
            for (Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) {
                if (writer != null) {
                    writer.dispose();
                }
                writer = iw.next();
                if (writer == null) {
                    continue;
                }
                writeParam = writer.getDefaultWriteParam();
                md = writer.getDefaultImageMetadata(its, writeParam);
                if (md == null) {
                    continue;
                }
                if (md.isReadOnly() || !md.isStandardMetadataFormatSupported()) {
                    writerFound = false;
                } else {
                    writerFound = true;
                    break;
                }
            }

            if (!writerFound) {
                StringBuilder sb = new StringBuilder();
                String[] writerFormatNames = ImageIO.getWriterFormatNames();
                for (String fmt : writerFormatNames) {
                    sb.append(fmt);
                    sb.append(' ');
                }
                throw new IllegalArgumentException("No suitable writer found. Metadata of all writers for : "
                        + formatName
                        + " files are either Read-Only or don't support standard metadata format. Supported formats are : "
                        + sb);
            }

            try {

                // compression
                if ((writeParam != null) && writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    if (formatName.toLowerCase().startsWith("tif")) {
                        writeParam.setCompressionType("LZW"); // org.apache.pdfbox.filter.TIFFExtension.COMPRESSION_LZW
                        writeParam.setCompressionQuality(compressionQuality);
                    }
                }

                if (formatName.toLowerCase().startsWith("tif")) {
                    // TIFF metadata
                    // Convert default metadata to TIFF metadata
                    TIFFDirectory dir = TIFFDirectory.createFromMetadata(md);

                    // Get {X,Y} resolution tags
                    BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
                    TIFFTag tagXRes = base.getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION);
                    TIFFTag tagYRes = base.getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION);

                    // Create {X,Y} resolution fields
                    TIFFField resolution = new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
                            BaselineTIFFTagSet.RESOLUTION_UNIT_INCH);
                    TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });
                    TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });

                    // Add {X,Y} resolution fields to TIFFDirectory
                    dir.addTIFFField(resolution);
                    dir.addTIFFField(fieldXRes);
                    dir.addTIFFField(fieldYRes);

                    // Add unit field to TIFFDirectory (change to RESOLUTION_UNIT_CENTIMETER if
                    // necessary)
                    dir.addTIFFField(new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT),
                            BaselineTIFFTagSet.RESOLUTION_UNIT_INCH));

                    // assign the new dir as the new IIOImageMetadata
                    md = dir.getAsMetadata();
                } else if ("jpeg".equals(formatName.toLowerCase()) || "jpg".equals(formatName.toLowerCase())) {
                    Element root = (Element) md.getAsTree("javax_imageio_jpeg_image_1.0");
                    NodeList jvarNodeList = root.getElementsByTagName("JPEGvariety");
                    Element jvarChild;
                    if (jvarNodeList.getLength() == 0) {
                        jvarChild = new IIOMetadataNode("JPEGvariety");
                        root.appendChild(jvarChild);
                    } else {
                        jvarChild = (Element) jvarNodeList.item(0);
                    }

                    NodeList jfifNodeList = jvarChild.getElementsByTagName("app0JFIF");
                    Element jfifChild;
                    if (jfifNodeList.getLength() == 0) {
                        jfifChild = new IIOMetadataNode("app0JFIF");
                        jvarChild.appendChild(jfifChild);
                    } else {
                        jfifChild = (Element) jfifNodeList.item(0);
                    }
                    if (jfifChild.getAttribute("majorVersion").isEmpty()) {
                        jfifChild.setAttribute("majorVersion", "1");
                    }
                    if (jfifChild.getAttribute("minorVersion").isEmpty()) {
                        jfifChild.setAttribute("minorVersion", "2");
                    }
                    jfifChild.setAttribute("resUnits", "1"); // inch
                    jfifChild.setAttribute("Xdensity", Integer.toString(dpi));
                    jfifChild.setAttribute("Ydensity", Integer.toString(dpi));
                    if (jfifChild.getAttribute("thumbWidth").isEmpty()) {
                        jfifChild.setAttribute("thumbWidth", "0");
                    }
                    if (jfifChild.getAttribute("thumbHeight").isEmpty()) {
                        jfifChild.setAttribute("thumbHeight", "0");
                    }

                    // mergeTree doesn't work for ARGB
                    md.setFromTree("javax_imageio_jpeg_image_1.0", root);

                } else {
                    // write metadata is possible
                    if ((md != null) && !md.isReadOnly() && md.isStandardMetadataFormatSupported()) {
                        IIOMetadataNode root = (IIOMetadataNode) md.getAsTree("javax_imageio_1.0");

                        IIOMetadataNode dimension = null;
                        NodeList nodeList = root.getElementsByTagName("Dimension");
                        if (nodeList.getLength() > 0) {
                            dimension = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            dimension = new IIOMetadataNode("Dimension");
                            root.appendChild(dimension);
                        }

                        float res = "PNG".equals(formatName.toUpperCase()) ? dpi / 25.4f : 25.4f / dpi;

                        IIOMetadataNode hps = null;

                        nodeList = dimension.getElementsByTagName("HorizontalPixelSize");
                        if (nodeList.getLength() > 0) {
                            hps = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            hps = new IIOMetadataNode("HorizontalPixelSize");
                            dimension.appendChild(hps);
                        }

                        hps.setAttribute("value", Double.toString(res));

                        IIOMetadataNode vps = null;

                        nodeList = dimension.getElementsByTagName("VerticalPixelSize");
                        if (nodeList.getLength() > 0) {
                            vps = (IIOMetadataNode) nodeList.item(0);
                        } else {
                            vps = new IIOMetadataNode("VerticalPixelSize");
                            dimension.appendChild(vps);
                        }

                        vps.setAttribute("value", Double.toString(res));

                        md.mergeTree("javax_imageio_1.0", root);
                    }
                }

                // Create output stream
                output = ImageIO.createImageOutputStream(new File(localOutputFile));

                writer.setOutput(output);

                // Optionally, listen to progress, warnings, etc.

                // writeParam = writer.getDefaultWriteParam();
                if (images.length > 1) {
                    writer.prepareWriteSequence(md);
                }

                temp = new IIOImage(images[0], null, md);
                writer.write(null, temp, writeParam);
                if (images.length > 1) {
                    if (!writer.canInsertImage(1)) {
                        throw new IllegalArgumentException("The writer for " + formatName
                                + " files is not able to add more than one image to the file : " + localOutputFile);
                    } else {
                        for (int i = 1; i < images.length; i++) {
                            temp = new IIOImage(images[i], null, md);
                            writer.writeInsert(i, temp, writeParam);
                        }
                    }
                }
            } finally

            {
                // Close stream in finally block to avoid resource leaks
                if (output != null) {
                    output.close();
                }
            }
        } finally

        {
            // Dispose writer in finally block to avoid memory leaks
            if (writer != null) {
                writer.dispose();
            }
        }
        return true;
    }

}