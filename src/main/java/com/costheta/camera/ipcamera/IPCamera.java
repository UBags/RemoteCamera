package com.costheta.camera.ipcamera;

import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFTag;
import de.onvif.discovery.OnvifDiscovery;
import de.onvif.discovery.OnvifPointer;
import de.onvif.soap.OnvifDevice;
import de.onvif.soap.devices.ImagingDevices;
import de.onvif.soap.devices.InitialDevices;
import de.onvif.soap.devices.MediaDevices;
import de.onvif.soap.devices.PtzDevices;
import org.onvif.ver10.schema.FloatRange;
import org.onvif.ver10.schema.Profile;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.soap.SOAPException;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class IPCamera {

    public IPCamera() {

    }

    public static void main(String[] args) throws Exception {
        System.out.println("Started");

//		Configuration.initialize();
//		OnvifIPCamera ipCamera = new OnvifIPCamera();
//		ipCamera.setUsername("admin");
//		ipCamera.setPassword("abcd1234");
//		ipCamera.setFps(5);
//		ipCamera.setResizeWidth(480);
//		ipCamera.setResizeHeight(640);
//		ipCamera.setHostIP("169.254.91.212");
//		System.out.println("Finished initCamera()");
//		int count = 1;
//		String baseDir = "E:/TechWerx/CosTheta/KEMS/temp";
//		while (true) {
//			try {
//				System.out.println("Getting BufferedImage - " + count);
//				BufferedImage image = ipCamera.captureRaw();
//				System.out.println("Got BufferedImage - " + image);
//				writeFile(image, "png", baseDir + "/" + count++ + ".png", false, 100);
//				System.out.println("Wrote BufferedImage to file - " + count);
//				TimeUnit.MILLISECONDS.sleep(5000);
//			} catch (Exception e) {
//				ipCamera.close();
//				System.exit(0);
//			}
//		}



        try {
            // DeviceDiscovery.WS_DISCOVERY_ADDRESS_IPv4 = "169.254.91.212";
            List<OnvifPointer> onvifDevices = OnvifDiscovery.discoverOnvifDevices();

            System.out.println("Onvif Devices are : " + onvifDevices);
            OnvifDevice nvt = new OnvifDevice("169.254.91.212", "admin", "abcd1234");
            System.out.println("Done1");

            Date nvtDate = nvt.getDevices().getDate();
            System.out.println("Done2");

            System.out.println(new SimpleDateFormat().format(nvtDate));
            System.out.println("Done3");

            InitialDevices id = nvt.getDevices(); // Basic methods for configuration and information
            System.out.println("Initial Devices are " + id);

            MediaDevices md = nvt.getMedia(); // Methods to get media information like stream or screenshot URIs or to
            // change your video encoder configuration
            System.out.println("Media Devices are " + md);

            ImagingDevices imd = nvt.getImaging();
            // A few functions to change your image settings, really just for your image
            // settings!
            System.out.println("Imaging Media Devices are " + imd);

            PtzDevices ptz = nvt.getPtz(); // Functionality to move your camera (if supported!)
            System.out.println("Ptz Devices are " + ptz);

            List<Profile> profiles = nvt.getDevices().getProfiles();
            System.out.println("Profiles are " + profiles);

            String profileToken = profiles.get(0).getToken();
            System.out.println("Profile Token is " + profileToken);

            String snapshotUri = md.getSnapshotUri(profileToken);
            System.out.println("Snapshot URI is : " + snapshotUri);
            URI snapshotURI = new URI(snapshotUri);
            URL snapshotURL = snapshotURI.toURL();

            boolean ptzSupported = ptz.isAbsoluteMoveSupported(profileToken);
            System.out.println("PTZ supported : " + ptzSupported);

            if (ptzSupported) {
                FloatRange panRange = ptz.getPanSpaces(profileToken);
                FloatRange tiltRange = ptz.getTiltSpaces(profileToken);
                float zoom = ptz.getZoomSpaces(profileToken).getMin();
                float x = (panRange.getMax() + panRange.getMin()) / 2f;
                float y = (tiltRange.getMax() + tiltRange.getMin()) / 2f;
                ptz.absoluteMove(profileToken, x, y, zoom);
            }
            int count = 1;
            String baseDir = "E:/TechWerx/CosTheta/KEMS/temp";

            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("admin", "abcd1234".toCharArray());
                }
            });

            while (true) {
                try {
                    System.out.println("Getting BufferedImage - " + count);
                    BufferedImage image = ImageIO.read(snapshotURL);
                    System.out.println("Got BufferedImage - " + image);
                    writeFile(image, "png", baseDir + "/" + count++ + ".png", false, 100);
                    System.out.println("Wrote BufferedImage to disk");
                    TimeUnit.MILLISECONDS.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }

        } catch (ConnectException e) {
            e.printStackTrace();
            System.err.println("Could not connect to NVT.");
        } catch (SOAPException e) {
            e.printStackTrace();
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        }


    }

    public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile,
                                    boolean debug, int debugLevel) throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, 300, debug, debugLevel);
    }

    public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                                    boolean debug, int debugLevel) throws Exception {
        return writeFile(bufferedImage, formatName, localOutputFile, dpi, 0.5f, debug, debugLevel);
    }

    public static boolean writeFile(RenderedImage bufferedImage, String formatName, String localOutputFile, int dpi,
                                    float compressionQuality, boolean debug, int debugLevel) throws Exception {

        if (bufferedImage == null) {
            return false;
        }
        RenderedImage[] input = new RenderedImage[1];
        input[0] = bufferedImage;
        return writeFile(input, formatName, localOutputFile, dpi, compressionQuality, debug, debugLevel);
    }

    public static boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
                                    boolean debug, int debugLevel) throws Exception {
        return writeFile(images, formatName, localOutputFile, dpi, 0.5f, debug, debugLevel);
    }

    public static boolean writeFile(RenderedImage[] images, String formatName, String localOutputFile, int dpi,
                                    float compressionQuality, boolean debug, int debugLevel) throws Exception {

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
                    // This segment must be run before other meta operations,
                    // or else "IIOInvalidTreeException: Invalid node: app0JFIF"
                    // The other (general) "meta" methods may not be used, because
                    // this will break the reading of the meta data in tests
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

                        // PNG writer doesn't conform to the spec which is
                        // "The width of a pixel, in millimeters"
                        // but instead counts the pixels per millimeter
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

                // Optionally, control format specific settings of param (requires casting), or
                // control generic write settings like sub sampling, source region, output type
                // etc.

                // Optionally, provide thumbnails and image/stream metadata

                /*
                 * final String pngMetadataFormatName = "javax_imageio_1.0";
                 *
                 * // Convert dpi (dots per inch) to dots per meter final double metersToInches
                 * = 39.3701; int dotsPerMeter = (int) Math.round(dpi * metersToInches);
                 *
                 * IIOMetadataNode pHYs_node = new IIOMetadataNode("pHYs");
                 * pHYs_node.setAttribute("pixelsPerUnitXAxis", Integer.toString(dotsPerMeter));
                 * pHYs_node.setAttribute("pixelsPerUnitYAxis", Integer.toString(dotsPerMeter));
                 * pHYs_node.setAttribute("unitSpecifier", "meter");
                 *
                 * IIOMetadataNode root = new IIOMetadataNode(pngMetadataFormatName);
                 * root.appendChild(pHYs_node);
                 *
                 * md.mergeTree(pngMetadataFormatName, root);
                 */

                /*
                 * double dotsPerMilli = ((1.0 * dpi) / 10) / 2.54; IIOMetadataNode horiz = new
                 * IIOMetadataNode("HorizontalPixelSize"); horiz.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode vert = new
                 * IIOMetadataNode("VerticalPixelSize"); vert.setAttribute("value",
                 * Double.toString(dotsPerMilli)); IIOMetadataNode dim = new
                 * IIOMetadataNode("Dimension"); dim.appendChild(horiz); dim.appendChild(vert);
                 * IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
                 * root.appendChild(dim); md.mergeTree("javax_imageio_1.0", root);
                 */

                // writer.prepareWriteEmpty(md, its, images[0].getWidth(),
                // images[0].getHeight(), null, null, writeParam);
                temp = new IIOImage(images[0], null, md);
                writer.write(null, temp, writeParam);
                // writer.endWriteEmpty();
                if (images.length > 1) {
                    if (!writer.canInsertImage(1)) {
                        throw new IllegalArgumentException("The writer for " + formatName
                                + " files is not able to add more than one image to the file : " + localOutputFile);
                    } else {
                        for (int i = 1; i < images.length; i++) {
                            // writer.prepareWriteEmpty(md, its, images[i].getWidth(),
                            // images[i].getHeight(), null, null,
                            // writeParam);
                            temp = new IIOImage(images[i], null, md);
                            writer.writeInsert(i, temp, writeParam);
                            // writer.endWriteEmpty();
                        }
                    }
                }
                // writer.endWriteSequence();
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
