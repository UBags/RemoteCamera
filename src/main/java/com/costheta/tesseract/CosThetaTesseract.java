/**
 * Copyright @ 2012 Quan Nguyen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.costheta.tesseract;

import static net.sourceforge.lept4j.ILeptonica.L_CLONE;
import static net.sourceforge.tess4j.ITessAPI.FALSE;
import static net.sourceforge.tess4j.ITessAPI.TRUE;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.ptr.PointerByReference;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import net.sourceforge.lept4j.Box;
import net.sourceforge.lept4j.Boxa;
import net.sourceforge.lept4j.Leptonica;
import net.sourceforge.lept4j.Leptonica1;
import net.sourceforge.lept4j.Pix;
import net.sourceforge.lept4j.util.LeptUtils;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITessAPI.TessBaseAPI;
import net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode;
import net.sourceforge.tess4j.ITessAPI.TessPageIterator;
import net.sourceforge.tess4j.ITessAPI.TessPageSegMode;
import net.sourceforge.tess4j.ITessAPI.TessResultIterator;
import net.sourceforge.tess4j.ITessAPI.TessResultRenderer;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.OCRResult;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.ImageIOHelper;
import net.sourceforge.tess4j.util.LoadLibs;
import net.sourceforge.tess4j.util.LoggHelper;

/**
 * An object layer on top of <code>TessAPI</code>, provides character
 * recognition support for common image formats, and multi-page TIFF images
 * beyond the uncompressed, binary TIFF format supported by Tesseract OCR
 * engine. The extended capabilities are provided by the
 * <code>Java Advanced Imaging Image I/O Tools</code>.<br>
 * <br>
 * Support for PDF documents is available through <code>Ghost4J</code>, a
 * <code>JNA</code> wrapper for <code>GPL Ghostscript</code>, which should be
 * installed and included in system path. If Ghostscript is not available,
 * PDFBox will be used.<br>
 * <br>
 * Any program that uses the library will need to ensure that the required
 * libraries (the <code>.jar</code> files for <code>jna</code>,
 * <code>jai-imageio</code>, and <code>ghost4j</code>) are in its compile and
 * run-time <code>classpath</code>.
 */

public class CosThetaTesseract implements ITesseract {



    private static boolean checkWhatsWrongWithLoadLibs = false;
    public static String datapath = System.getProperty("tesseract.datapath");
    // public static final String language = "eng+hin";
    public static String language = "eng";
    public static String initialisationImagesDir = System.getProperty("input.folder") + "/InitialisationImages";
    // private int psm = TessPageSegMode.PSM_AUTO_OSD; // default mode, but misses
    // reading some text
    // private int psm = TessPageSegMode.PSM_SPARSE_TEXT; // doesn't work in this
    // mode
    // private int psm = TessPageSegMode.PSM_SINGLE_COLUMN; // works accurately in this mode
    private int psm = TessPageSegMode.PSM_SINGLE_LINE; // trying this mode
    // private int psm = -1;
    private int ocrEngineMode = TessOcrEngineMode.OEM_DEFAULT;
    private final Properties prop = new Properties();
    private final List<String> configList = new ArrayList<String>();

    // private static HashMap<String, String> results = new HashMap<>();
    private static List<String> imageFiles = new ArrayList<>();
    private static boolean imageFilesExtracted = false;
    String initialisationDir;

    // private boolean strict = false;
    private int debugLevel = 9;

    private TessAPI api;
    private TessBaseAPI handle;

    private int processInstance;
    private static int pInstance = 1;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(new LoggHelper().toString());

    // static {
    // System.out.println("In CosThetaTesseract : Temp folder = " +
    // System.getProperty("java.io.tmpdir"));
    // System.out.println("In CosThetaTesseract : Library path = " +
    // System.getProperty("jna.library.path"));
    // }

    public CosThetaTesseract(String datapath, String language, String initialisationImagesPath, boolean strict,
                             int processInstance, int debugL) {

        CosThetaTesseract.datapath = datapath;
        CosThetaTesseract.language = language;
        // this.strict = strict;
        this.debugLevel = debugL;
        this.initialisationDir = initialisationImagesPath;
        this.processInstance = processInstance;
        // System.setProperty("jna.library.path", "");
        if (this.debugLevel <= 8) {
            System.out.println("Creating a CosThetaTesseract object in processInstance - " + pInstance++);
        }
        this.init();
        this.setTessVariables();
        /*
         * boolean goodness = this.initialiseHandle(); if (this.debugLevel <= 4) {
         * System.out.println("The result of goodness check for processInstance " +
         * pInstance + " is - " + goodness); }
         */
    }

    public CosThetaTesseract(int debugLevel) {

        this.debugLevel = debugLevel;
        this.initialisationDir = initialisationImagesDir;
        this.processInstance = pInstance++;
        if (this.debugLevel <= 8) {
            System.out.println("Creating a CosThetaTesseract object in processInstance - " + this.processInstance);
        }

        if (checkWhatsWrongWithLoadLibs) {
            String model = System.getProperty("sun.arch.data.model", System.getProperty("com.ibm.vm.bitmode"));
            String resourcePrefix = "32".equals(model) ? "win32-x86" : "win32-x86-64";
            System.out.println("resourcePrefix = " + resourcePrefix);
            File targetTempFolder = this.extractTessResources(resourcePrefix);
            System.out.println("targetTempFolder = " + targetTempFolder.getAbsolutePath());
            if ((targetTempFolder != null) && targetTempFolder.exists()) {
                String userCustomizedPath = System.getProperty("jna.library.path");
                System.out.println("userCustomizedPath = " + userCustomizedPath);
                if ((null == userCustomizedPath) || userCustomizedPath.isEmpty()) {
                    System.setProperty("jna.library.path", targetTempFolder.getPath());
                } else {
                    System.setProperty("jna.library.path",
                            userCustomizedPath + File.pathSeparator + targetTempFolder.getPath());
                }
                System.out.println("reset jna.library.path to = " + System.getProperty("jna.library.path"));
                System.out.println("current java.io.tmpdir to = " + System.getProperty("java.io.tmpdir"));
            }
        }

        try {
            this.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(
                    "In CosThetaTesseract constructor : Temp folder = " + System.getProperty("java.io.tmpdir"));
            System.out.println(
                    "In CosThetaTesseract constructor : Library path = " + System.getProperty("jna.library.path"));
        }
        this.setTessVariables();
        /*
         * boolean goodness = this.initialiseHandle(); if (this.debugLevel <= 4) {
         * System.out.println("The result of goodness check for processInstance " +
         * pInstance + " is - " + goodness); }
         *
         */
    }

    public CosThetaTesseract(boolean useAutoOSD, int debugLevel) {
        // System.out.println("Datapath = " + datapath);
        if (!useAutoOSD) {
            this.debugLevel = debugLevel;
            this.initialisationDir = initialisationImagesDir;
            this.processInstance = pInstance++;
            if (this.debugLevel <= 8) {
                System.out.println("Creating a CosThetaTesseract object in processInstance - " + this.processInstance);
            }
            try {
                this.init();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(
                        "In CosThetaTesseract constructor : Temp folder = " + System.getProperty("java.io.tmpdir"));
                System.out.println(
                        "In CosThetaTesseract constructor : Library path = " + System.getProperty("jna.library.path"));
            }
            this.setTessVariables();
        } else {
            this.debugLevel = debugLevel;
            this.initialisationDir = initialisationImagesDir;
            this.processInstance = pInstance++;
            // this.psm = TessPageSegMode.PSM_AUTO_OSD;
            this.psm = -1;
            if (this.debugLevel <= 8) {
                System.out.println("Creating a PSM_AUTO_OSD CosThetaTesseract object in processInstance - "
                        + this.processInstance);
            }
            try {
                this.init();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(
                        "In CosThetaTesseract constructor : Temp folder = " + System.getProperty("java.io.tmpdir"));
                System.out.println(
                        "In CosThetaTesseract constructor : Library path = " + System.getProperty("jna.library.path"));
            }
            this.setTessVariables();
        }

    }

    public CosThetaTesseract(int processInstance, int debugLevel) {

        this.debugLevel = debugLevel;
        this.initialisationDir = initialisationImagesDir;
        this.processInstance = processInstance;
        if (this.debugLevel <= 8) {
            System.out.println("Creating a CosThetaTesseract object in processInstance - " + this.processInstance);
        }
        try {
            this.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(
                    "In CosThetaTesseract constructor : Temp folder = " + System.getProperty("java.io.tmpdir"));
            System.out.println(
                    "In CosThetaTesseract constructor : Library path = " + System.getProperty("jna.library.path"));
        }
        this.setTessVariables();
        /*
         * boolean goodness = this.initialiseHandle(); if (this.debugLevel <= 4) {
         * System.out.println("The result of goodness check for processInstance " +
         * pInstance + " is - " + goodness); }
         *
         */
    }

    public void reinitiliase() {
        this.release();
        try {
            this.init();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(
                    "In CosThetaTesseract reinitialise() : Temp folder = " + System.getProperty("java.io.tmpdir"));
            System.out.println(
                    "In CosThetaTesseract reinitialise() : Library path = " + System.getProperty("jna.library.path"));
        }
        this.setTessVariables();
        // boolean goodness = this.initialiseHandle();
        /*
         * boolean goodness = true; if (this.debugLevel <= 4) { System.out
         * .println("The result of goodness check for processInstance " +
         * this.processInstance + " is - " + goodness); }
         */
    }

    /*
     * private boolean initialiseHandle() { // boolean goodness =
     * this.checkGoodness(this.initialisationDir, this.strict); boolean goodness =
     * true; return goodness; }
     */

    /**
     * Returns TessAPI object.
     *
     * @return api
     */
    protected TessAPI getAPI() {
        return this.api;
    }

    /**
     * Returns API handle.
     *
     * @return handle
     */
    protected TessBaseAPI getHandle() {
        return this.handle;
    }

    /**
     * Sets path to <code>tessdata</code>.
     *
     * @param datapath the tessdata path to set
     */
    @Override
    public void setDatapath(String datapath) {
        CosThetaTesseract.datapath = datapath;
    }

    /**
     * Sets language for OCR.
     *
     * @param language the language code, which follows ISO 639-3 standard.
     */
    @Override
    public void setLanguage(String language) {
        CosThetaTesseract.language = language;
    }

    /**
     * Sets OCR engine mode.
     *
     * @param ocrEngineMode the OcrEngineMode to set
     */
    @Override
    public void setOcrEngineMode(int ocrEngineMode) {
        this.ocrEngineMode = ocrEngineMode;
    }

    /**
     * Sets page segmentation mode.
     *
     * @param mode the page segmentation mode to set
     */
    @Override
    public void setPageSegMode(int mode) {
        this.psm = mode;
    }

    /**
     * Enables hocr output.
     *
     * @param hocr to enable or disable hocr output
     */
    public void setHocr(boolean hocr) {
        this.prop.setProperty("tessedit_create_hocr", hocr ? String.valueOf(TRUE) : String.valueOf(FALSE));
    }

    /**
     * Set the value of Tesseract's internal parameter.
     *
     * @param key   variable name, e.g., <code>tessedit_create_hocr</code>,
     *              <code>tessedit_char_whitelist</code>, etc.
     * @param value value for corresponding variable, e.g., "1", "0", "0123456789",
     *              etc.
     */
    @Override
    public void setTessVariable(String key, String value) {
        this.prop.setProperty(key, value);
    }

    /**
     * Sets configs to be passed to Tesseract's <code>Init</code> method.
     *
     * @param configs list of config filenames, e.g., "digits", "bazaar", "quiet"
     */
    @Override
    public void setConfigs(List<String> configs) {
        this.configList.clear();
        if (configs != null) {
            this.configList.addAll(configs);
        }
    }

    /**
     * Performs OCR operation.
     *
     * @param imageFile an image file
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(File imageFile) throws TesseractException {
        if (this.debugLevel <= 2) {
            System.out.println("Doing OCR for file - " + imageFile.getAbsolutePath());
        }
        return this.doOCR(imageFile, null);
    }

    /**
     * Performs OCR operation.
     *
     * @param inputFile an image file
     * @param rect      the bounding rectangle defines the region of the image to be
     *                  recognized. A rectangle of zero dimension or
     *                  <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(File inputFile, Rectangle rect) throws TesseractException {
        try {
            File imageFile = ImageIOHelper.getImageFile(inputFile);
            String imageFileFormat = ImageIOHelper.getImageFileFormat(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            if (!readers.hasNext()) {
                throw new RuntimeException(ImageIOHelper.JAI_IMAGE_READER_MESSAGE);
            }
            ImageReader reader = readers.next();
            StringBuilder result = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile);) {
                reader.setInput(iis);
                int imageTotal = reader.getNumImages(true);

                for (int i = 0; i < imageTotal; i++) {
                    IIOImage oimage = reader.readAll(i, reader.getDefaultReadParam());
                    result.append(this.doOCR(oimage, inputFile.getPath(), rect, i + 1));
                }

                if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_hocr"))) {
                    result.insert(0, htmlBeginTag).append(htmlEndTag);
                }
            } finally {
                // delete temporary TIFF image for PDF
                if ((imageFile != null) && imageFile.exists() && (imageFile != inputFile)
                        && imageFile.getName().startsWith("multipage")
                        && imageFile.getName().endsWith(ImageIOHelper.TIFF_EXT)) {
                    imageFile.delete();
                }
                reader.dispose();
                this.release();
            }
            return result.toString();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    /**
     * Performs OCR operation.
     *
     * @param bi a buffered image
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(BufferedImage bi) throws TesseractException {
        if (this.debugLevel <= 2) {
            System.out.println("CosThetaTesseract processInstance - " + this.processInstance
                    + " : Doing OCR of BufferedImage - " + bi);
        }
        return this.doOCR(bi, null);
    }

    /**
     * @return the processInstance
     */
    public int getProcessInstance() {
        return this.processInstance;
    }

    /**
     * Performs OCR operation.
     *
     * @param bi   a buffered image
     * @param rect the bounding rectangle defines the region of the image to be
     *             recognized. A rectangle of zero dimension or <code>null</code>
     *             indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(BufferedImage bi, Rectangle rect) throws TesseractException {
        try {
            return this.doOCR(ImageIOHelper.getIIOImageList(bi), rect);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    /**
     * Performs OCR operation.
     *
     * @param imageList a list of <code>IIOImage</code> objects
     * @param rect      the bounding rectangle defines the region of the image to be
     *                  recognized. A rectangle of zero dimension or
     *                  <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(List<IIOImage> imageList, Rectangle rect) throws TesseractException {
        return this.doOCR(imageList, null, rect);
    }

    /**
     * Performs OCR operation.
     *
     * @param imageList a list of <code>IIOImage</code> objects
     * @param filename  input file name. Needed only for training and reading a UNLV
     *                  zone file.
     * @param rect      the bounding rectangle defines the region of the image to be
     *                  recognized. A rectangle of zero dimension or
     *                  <code>null</code> indicates the whole image.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(List<IIOImage> imageList, String filename, Rectangle rect) throws TesseractException {
        try {
            StringBuilder sb = new StringBuilder();
            int pageNum = 0;

            for (IIOImage oimage : imageList) {
                pageNum++;
                try {
                    this.setImage(oimage.getRenderedImage(), rect);
                    sb.append(this.getOCRText(filename, pageNum));
                } catch (IOException ioe) {
                    // skip the problematic image
                    logger.error(ioe.getMessage(), ioe);
                }
            }

            if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_hocr"))) {
                sb.insert(0, htmlBeginTag).append(htmlEndTag);
            }

            return sb.toString();
        } finally {
            this.release();
        }
    }

    /**
     * Performs OCR operation. <br>
     * Note: <code>init()</code> and <code>setTessVariables()</code> must be called
     * before use; <code>dispose()</code> should be called afterwards.
     *
     * @param oimage   an <code>IIOImage</code> object
     * @param filename input file nam
     * @param rect     the bounding rectangle defines the region of the image to be
     *                 recognized. A rectangle of zero dimension or
     *                 <code>null</code> indicates the whole image.
     * @param pageNum  page number
     * @return the recognized text
     * @throws TesseractException
     */
    private String doOCR(IIOImage oimage, String filename, Rectangle rect, int pageNum) throws TesseractException {
        String text = "";

        try {
            this.setImage(oimage.getRenderedImage(), rect);
            text = this.getOCRText(filename, pageNum);
        } catch (IOException ioe) {
            // skip the problematic image
            logger.warn(ioe.getMessage(), ioe);
        }

        return text;
    }

    /**
     * Performs OCR operation. Use <code>SetImage</code>, (optionally)
     * <code>SetRectangle</code>, and one or more of the <code>Get*Text</code>
     * functions.
     *
     * @param xsize width of image
     * @param ysize height of image
     * @param buf   pixel data
     * @param rect  the bounding rectangle defines the region of the image to be
     *              recognized. A rectangle of zero dimension or <code>null</code>
     *              indicates the whole image.
     * @param bpp   bits per pixel, represents the bit depth of the image, with 1
     *              for binary bitmap, 8 for gray, and 24 for color RGB.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) throws TesseractException {
        return this.doOCR(xsize, ysize, buf, null, rect, bpp);
    }

    /**
     * Performs OCR operation. Use <code>SetImage</code>, (optionally)
     * <code>SetRectangle</code>, and one or more of the <code>Get*Text</code>
     * functions.
     *
     * @param xsize    width of image
     * @param ysize    height of image
     * @param buf      pixel data
     * @param filename input file name. Needed only for training and reading a UNLV
     *                 zone file.
     * @param rect     the bounding rectangle defines the region of the image to be
     *                 recognized. A rectangle of zero dimension or
     *                 <code>null</code> indicates the whole image.
     * @param bpp      bits per pixel, represents the bit depth of the image, with 1
     *                 for binary bitmap, 8 for gray, and 24 for color RGB.
     * @return the recognized text
     * @throws TesseractException
     */
    @Override
    public String doOCR(int xsize, int ysize, ByteBuffer buf, String filename, Rectangle rect, int bpp)
            throws TesseractException {
        try {
            this.setImage(xsize, ysize, buf, rect, bpp);
            return this.getOCRText(filename, 1);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TesseractException(e);
        } finally {
            this.release();
        }
    }

    /**
     * Initializes Tesseract engine.
     */
    protected void init() {
        if (this.debugLevel <= 4) {
            System.out.println("Entered init() of CosThetaTesseract - " + this.processInstance);
        }
        this.api = TessAPI.INSTANCE;
        if (this.debugLevel <= 4) {
            System.out.println("Got the TessAPI processInstance in CosThetaTesseract - " + this.processInstance);
        }
        this.handle = this.api.TessBaseAPICreate();
        StringArray sarray = new StringArray(this.configList.toArray(new String[0]));
        PointerByReference configs = new PointerByReference();
        configs.setPointer(sarray);
        this.api.TessBaseAPIInit1(this.handle, datapath, language, this.ocrEngineMode, configs, this.configList.size());
        if (this.psm > -1) {
            this.api.TessBaseAPISetPageSegMode(this.handle, this.psm);
        }
        if (this.debugLevel <= 4) {
            System.out.println("Exiting init() of CosThetaTesseract - " + this.processInstance);
        }
        Leptonica1.lept_free(configs.getValue());
    }

    /**
     * Sets Tesseract's internal parameters.
     */
    protected void setTessVariables() {
        if (this.debugLevel <= 4) {
            System.out.println("Entered setTessVariables() of CosThetaTesseract - " + this.processInstance);
        }
        this.setTessVariable("tessedit_parallelize", "1");
        this.setTessVariable("debug_file", "/dev/null");
        this.setTessVariable("tessedit_char_whitelist",
                "�$0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz, .'\\\"-/+%@");
        Enumeration<?> em = this.prop.propertyNames();
        while (em.hasMoreElements()) {
            String key = (String) em.nextElement();
            this.api.TessBaseAPISetVariable(this.handle, key, this.prop.getProperty(key));
        }
        if (this.debugLevel <= 4) {
            System.out.println("Exiting setTessVariables() of CosThetaTesseract - " + this.processInstance);
        }
    }

    /**
     * A wrapper for {@link #setImage(int, int, ByteBuffer, Rectangle, int)}.
     *
     * @param image a rendered image
     * @param rect  region of interest
     * @throws java.io.IOException
     */
    protected void setImage(RenderedImage image, Rectangle rect) throws IOException {
        ByteBuffer buff = ImageIOHelper.getImageByteBuffer(image);
        int bpp;
        DataBuffer dbuff = image.getData(new Rectangle(1, 1)).getDataBuffer();
        if (dbuff instanceof DataBufferByte) {
            bpp = image.getColorModel().getPixelSize();
        } else {
            bpp = 8; // BufferedImage.TYPE_BYTE_GRAY image
        }
        this.setImage(image.getWidth(), image.getHeight(), buff, rect, bpp);
    }

    /**
     * Sets image to be processed.
     *
     * @param xsize width of image
     * @param ysize height of image
     * @param buf   pixel data
     * @param rect  the bounding rectangle defines the region of the image to be
     *              recognized. A rectangle of zero dimension or <code>null</code>
     *              indicates the whole image.
     * @param bpp   bits per pixel, represents the bit depth of the image, with 1
     *              for binary bitmap, 8 for gray, and 24 for color RGB.
     */
    protected void setImage(int xsize, int ysize, ByteBuffer buf, Rectangle rect, int bpp) {
        int bytespp = bpp / 8;
        int bytespl = (int) Math.ceil((xsize * bpp) / 8.0);
        this.api.TessBaseAPISetImage(this.handle, buf, xsize, ysize, bytespp, bytespl);

        if ((rect != null) && !rect.isEmpty()) {
            this.api.TessBaseAPISetRectangle(this.handle, rect.x, rect.y, rect.width, rect.height);
        }
    }

    /**
     * Gets recognized text.
     *
     * @param filename input file name. Needed only for reading a UNLV zone file.
     * @param pageNum  page number; needed for hocr paging.
     * @return the recognized text
     */
    protected String getOCRText(String filename, int pageNum) {
        if ((filename != null) && !filename.isEmpty()) {
            this.api.TessBaseAPISetInputName(this.handle, filename);
        }

        Pointer textPtr;
        if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_hocr"))) {
            textPtr = this.api.TessBaseAPIGetHOCRText(this.handle, pageNum - 1);
        } else if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_write_unlv"))) {
            textPtr = this.api.TessBaseAPIGetUNLVText(this.handle);
        } else if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_alto"))) {
            textPtr = this.api.TessBaseAPIGetAltoText(this.handle, pageNum - 1);
        } else if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_lstmbox"))) {
            textPtr = this.api.TessBaseAPIGetLSTMBoxText(this.handle, pageNum - 1);
        } else if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_tsv"))) {
            textPtr = this.api.TessBaseAPIGetTsvText(this.handle, pageNum - 1);
        } else if (String.valueOf(TRUE).equals(this.prop.getProperty("tessedit_create_wordstrbox"))) {
            textPtr = this.api.TessBaseAPIGetWordStrBoxText(this.handle, pageNum - 1);
        } else {
            textPtr = this.api.TessBaseAPIGetUTF8Text(this.handle);
        }
        String str = textPtr.getString(0);
        this.api.TessDeleteText(textPtr);
        return str;
    }

    /**
     * Creates renderers for given formats.
     *
     * @param outputbase
     * @param formats
     * @return
     */
    private TessResultRenderer createRenderers(String outputbase, List<RenderedFormat> formats) {
        TessResultRenderer renderer = null;

        for (RenderedFormat format : formats) {
            switch (format) {
                case TEXT:
                    if (renderer == null) {
                        renderer = this.api.TessTextRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessTextRendererCreate(outputbase));
                    }
                    break;
                case HOCR:
                    if (renderer == null) {
                        renderer = this.api.TessHOcrRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessHOcrRendererCreate(outputbase));
                    }
                    break;
                case PDF:
                    String dataPath = this.api.TessBaseAPIGetDatapath(this.handle);
                    boolean textonly = String.valueOf(TRUE).equals(this.prop.getProperty("textonly_pdf"));
                    if (renderer == null) {
                        renderer = this.api.TessPDFRendererCreate(outputbase, dataPath, textonly ? TRUE : FALSE);
                    } else {
                        this.api.TessResultRendererInsert(renderer,
                                this.api.TessPDFRendererCreate(outputbase, dataPath, textonly ? TRUE : FALSE));
                    }
                    break;
                case BOX:
                    if (renderer == null) {
                        renderer = this.api.TessBoxTextRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessBoxTextRendererCreate(outputbase));
                    }
                    break;
                case UNLV:
                    if (renderer == null) {
                        renderer = this.api.TessUnlvRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessUnlvRendererCreate(outputbase));
                    }
                    break;
                case ALTO:
                    if (renderer == null) {
                        renderer = this.api.TessAltoRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessAltoRendererCreate(outputbase));
                    }
                    break;
                case TSV:
                    if (renderer == null) {
                        renderer = this.api.TessTsvRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessTsvRendererCreate(outputbase));
                    }
                    break;
                case LSTMBOX:
                    if (renderer == null) {
                        renderer = this.api.TessLSTMBoxRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessLSTMBoxRendererCreate(outputbase));
                    }
                    break;
                case WORDSTRBOX:
                    if (renderer == null) {
                        renderer = this.api.TessWordStrBoxRendererCreate(outputbase);
                    } else {
                        this.api.TessResultRendererInsert(renderer, this.api.TessWordStrBoxRendererCreate(outputbase));
                    }
                    break;
            }
        }

        return renderer;
    }

    /**
     * Creates documents for given renderer.
     *
     * @param filename   input image
     * @param outputbase output filename without extension
     * @param formats    types of renderer
     * @throws TesseractException
     */
    @Override
    public void createDocuments(String filename, String outputbase, List<RenderedFormat> formats)
            throws TesseractException {
        this.createDocuments(new String[] { filename }, new String[] { outputbase }, formats);
    }

    /**
     * Creates documents for given renderer.
     *
     * @param filenames   array of input files
     * @param outputbases array of output filenames without extension
     * @param formats     types of renderer
     * @throws TesseractException
     */
    @Override
    public void createDocuments(String[] filenames, String[] outputbases, List<RenderedFormat> formats)
            throws TesseractException {
        if (filenames.length != outputbases.length) {
            throw new RuntimeException("The two arrays must match in length.");
        }
        try {
            for (int i = 0; i < filenames.length; i++) {
                File inputFile = new File(filenames[i]);
                File imageFile = null;

                try {
                    // if PDF, convert to multi-page TIFF
                    imageFile = ImageIOHelper.getImageFile(inputFile);

                    TessResultRenderer renderer = this.createRenderers(outputbases[i], formats);
                    this.createDocuments(imageFile.getPath(), renderer);
                    this.api.TessDeleteResultRenderer(renderer);
                } catch (Exception e) {
                    // skip the problematic image file
                    logger.warn(e.getMessage(), e);
                } finally {
                    // delete temporary TIFF image for PDF
                    if ((imageFile != null) && imageFile.exists() && (imageFile != inputFile)
                            && imageFile.getName().startsWith("multipage")
                            && imageFile.getName().endsWith(ImageIOHelper.TIFF_EXT)) {
                        imageFile.delete();
                    }
                }
            }
        } finally {
            this.release();
        }
    }

    /**
     * Creates documents for given renderer.
     *
     * @param filename input file
     * @param renderer renderer
     * @return the average text confidence for Tesseract page result
     * @throws TesseractException
     */
    private int createDocuments(String filename, TessResultRenderer renderer) throws TesseractException {
        this.api.TessBaseAPISetInputName(this.handle, filename); // for reading a UNLV zone file
        int result = this.api.TessBaseAPIProcessPages(this.handle, filename, null, 0, renderer);

        if (result == ITessAPI.FALSE) {
            throw new TesseractException("Error during processing page in CosThetaTesseract - " + this.processInstance);
        }

        return this.api.TessBaseAPIMeanTextConf(this.handle);
    }

    /**
     * Creates documents for given renderer.
     *
     * @param bi       buffered image
     * @param filename filename (optional)
     * @param renderer renderer
     * @return the average text confidence for Tesseract page result
     * @throws Exception
     */
    private int createDocuments(BufferedImage bi, String filename, TessResultRenderer renderer) throws Exception {
        Pix pix = LeptUtils.convertImageToPix(bi);
        this.api.TessResultRendererBeginDocument(renderer, filename);
        int result = this.api.TessBaseAPIProcessPage(this.handle, pix, 0, filename, null, 0, renderer);
        this.api.TessResultRendererEndDocument(renderer);
        LeptUtils.dispose(pix);

        if (result == ITessAPI.FALSE) {
            throw new TesseractException("Error during processing page in CosThetaTesseract - " + this.processInstance);
        }

        return this.api.TessBaseAPIMeanTextConf(this.handle);
    }

    /**
     * Gets segmented regions at specified page iterator level.
     *
     * @param bi                input buffered image
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return list of <code>Rectangle</code>
     * @throws TesseractException
     */
    @Override
    public List<Rectangle> getSegmentedRegions(BufferedImage bi, int pageIteratorLevel) throws TesseractException {
        try {
            List<Rectangle> list = new ArrayList<Rectangle>();
            this.setImage(bi, null);

            Boxa boxes = this.api.TessBaseAPIGetComponentImages(this.handle, pageIteratorLevel, TRUE, null, null);
            Leptonica leptInstance = Leptonica.INSTANCE;
            int boxCount = leptInstance.boxaGetCount(boxes);
            for (int i = 0; i < boxCount; i++) {
                Box box = leptInstance.boxaGetBox(boxes, i, L_CLONE);
                if (box == null) {
                    continue;
                }
                list.add(new Rectangle(box.x, box.y, box.w, box.h));
                PointerByReference pRef = new PointerByReference();
                pRef.setValue(box.getPointer());
                leptInstance.boxDestroy(pRef);
            }

            PointerByReference pRef = new PointerByReference();
            pRef.setValue(boxes.getPointer());
            leptInstance.boxaDestroy(pRef);

            return list;
        } catch (IOException ioe) {
            // skip the problematic image
            logger.warn(ioe.getMessage(), ioe);
            throw new TesseractException(ioe);
        } finally {
            this.release();
        }
    }

    /**
     * Gets recognized words at specified page iterator level.
     *
     * @param bi                input buffered image
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return list of <code>Word</code>
     */
    @Override
    public List<Word> getWords(BufferedImage bi, int pageIteratorLevel) {

        List<Word> words = new ArrayList<Word>();

        try {
            this.setImage(bi, null);

            this.api.TessBaseAPIRecognize(this.handle, null);
            TessResultIterator ri = this.api.TessBaseAPIGetIterator(this.handle);
            TessPageIterator pi = this.api.TessResultIteratorGetPageIterator(ri);
            this.api.TessPageIteratorBegin(pi);

            do {
                Pointer ptr = this.api.TessResultIteratorGetUTF8Text(ri, pageIteratorLevel);
                String text = ptr.getString(0);
                this.api.TessDeleteText(ptr);
                float confidence = this.api.TessResultIteratorConfidence(ri, pageIteratorLevel);
                IntBuffer leftB = IntBuffer.allocate(1);
                IntBuffer topB = IntBuffer.allocate(1);
                IntBuffer rightB = IntBuffer.allocate(1);
                IntBuffer bottomB = IntBuffer.allocate(1);
                this.api.TessPageIteratorBoundingBox(pi, pageIteratorLevel, leftB, topB, rightB, bottomB);
                int left = leftB.get();
                int top = topB.get();
                int right = rightB.get();
                int bottom = bottomB.get();
                Word word = new Word(text, confidence, new Rectangle(left, top, right - left, bottom - top));
                words.add(word);
            } while (this.api.TessPageIteratorNext(pi, pageIteratorLevel) == TRUE);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            this.release();
        }

        return words;
    }

    /**
     * Creates documents with OCR result for given renderers at specified page
     * iterator level.
     *
     * @param bi                input buffered image
     * @param filename          filename (optional)
     * @param outputbase        output filenames without extension
     * @param formats           types of renderer
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return OCR result
     * @throws TesseractException
     */
    @Override
    public OCRResult createDocumentsWithResults(BufferedImage bi, String filename, String outputbase,
                                                List<ITesseract.RenderedFormat> formats, int pageIteratorLevel) throws TesseractException {
        List<OCRResult> results = this.createDocumentsWithResults(new BufferedImage[] { bi }, new String[] { filename },
                new String[] { outputbase }, formats, pageIteratorLevel);
        if (!results.isEmpty()) {
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Creates documents with OCR results for given renderers at specified page
     * iterator level.
     *
     * @param bis               array of input buffered images
     * @param filenames         array of filenames
     * @param outputbases       array of output filenames without extension
     * @param formats           types of renderer
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return list of OCR results
     * @throws TesseractException
     */
    @Override
    public List<OCRResult> createDocumentsWithResults(BufferedImage[] bis, String[] filenames, String[] outputbases,
                                                      List<ITesseract.RenderedFormat> formats, int pageIteratorLevel) throws TesseractException {
        if ((bis.length != filenames.length) || (bis.length != outputbases.length)) {
            throw new RuntimeException("The three arrays must match in length.");
        }

        List<OCRResult> results = new ArrayList<OCRResult>();

        try {
            for (int i = 0; i < bis.length; i++) {
                try {
                    TessResultRenderer renderer = this.createRenderers(outputbases[i], formats);
                    int meanTextConfidence = this.createDocuments(bis[i], filenames[i], renderer);
                    List<Word> words = meanTextConfidence > 0 ? this.getRecognizedWords(pageIteratorLevel)
                            : new ArrayList<Word>();
                    results.add(new OCRResult(meanTextConfidence, words));
                    this.api.TessDeleteResultRenderer(renderer);
                } catch (Exception e) {
                    // skip the problematic image file
                    logger.warn(e.getMessage(), e);
                }
            }
        } finally {
            this.release();
        }

        return results;
    }

    /**
     * Creates documents with OCR result for given renderers at specified page
     * iterator level.
     *
     * @param filename          input file
     * @param outputbase        output filenames without extension
     * @param formats           types of renderer
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return OCR result
     * @throws TesseractException
     */
    @Override
    public OCRResult createDocumentsWithResults(String filename, String outputbase,
                                                List<ITesseract.RenderedFormat> formats, int pageIteratorLevel) throws TesseractException {
        List<OCRResult> results = this.createDocumentsWithResults(new String[] { filename },
                new String[] { outputbase }, formats, pageIteratorLevel);
        if (!results.isEmpty()) {
            return results.get(0);
        } else {
            return null;
        }
    }

    /**
     * Creates documents with OCR results for given renderers at specified page
     * iterator level.
     *
     * @param filenames   array of input files
     * @param outputbases array of output filenames without extension
     * @param formats     types of renderer
     * @return list of OCR results
     * @throws TesseractException
     */
    @Override
    public List<OCRResult> createDocumentsWithResults(String[] filenames, String[] outputbases,
                                                      List<ITesseract.RenderedFormat> formats, int pageIteratorLevel) throws TesseractException {
        if (filenames.length != outputbases.length) {
            throw new RuntimeException("The two arrays must match in length.");
        }

        List<OCRResult> results = new ArrayList<OCRResult>();

        try {
            for (int i = 0; i < filenames.length; i++) {
                File inputFile = new File(filenames[i]);
                File imageFile = null;

                try {
                    // if PDF, convert to multi-page TIFF
                    imageFile = ImageIOHelper.getImageFile(inputFile);

                    TessResultRenderer renderer = this.createRenderers(outputbases[i], formats);
                    int meanTextConfidence = this.createDocuments(imageFile.getPath(), renderer);
                    List<Word> words = meanTextConfidence > 0 ? this.getRecognizedWords(pageIteratorLevel)
                            : new ArrayList<Word>();
                    results.add(new OCRResult(meanTextConfidence, words));
                    this.api.TessDeleteResultRenderer(renderer);
                } catch (Exception e) {
                    // skip the problematic image file
                    logger.warn(e.getMessage(), e);
                } finally {
                    // delete temporary TIFF image for PDF
                    if ((imageFile != null) && imageFile.exists() && (imageFile != inputFile)
                            && imageFile.getName().startsWith("multipage")
                            && imageFile.getName().endsWith(ImageIOHelper.TIFF_EXT)) {
                        imageFile.delete();
                    }
                }
            }
        } finally {
            this.release();
        }

        return results;
    }

    /**
     * Gets result words at specified page iterator level from recognized pages.
     *
     * @param pageIteratorLevel TessPageIteratorLevel enum
     * @return list of <code>Word</code>
     */
    private List<Word> getRecognizedWords(int pageIteratorLevel) {
        List<Word> words = new ArrayList<Word>();

        try {
            TessResultIterator ri = this.api.TessBaseAPIGetIterator(this.handle);
            TessPageIterator pi = this.api.TessResultIteratorGetPageIterator(ri);
            this.api.TessPageIteratorBegin(pi);

            do {
                Pointer ptr = this.api.TessResultIteratorGetUTF8Text(ri, pageIteratorLevel);
                String text = ptr.getString(0);
                this.api.TessDeleteText(ptr);
                float confidence = this.api.TessResultIteratorConfidence(ri, pageIteratorLevel);
                IntBuffer leftB = IntBuffer.allocate(1);
                IntBuffer topB = IntBuffer.allocate(1);
                IntBuffer rightB = IntBuffer.allocate(1);
                IntBuffer bottomB = IntBuffer.allocate(1);
                this.api.TessPageIteratorBoundingBox(pi, pageIteratorLevel, leftB, topB, rightB, bottomB);
                int left = leftB.get();
                int top = topB.get();
                int right = rightB.get();
                int bottom = bottomB.get();
                Word word = new Word(text, confidence, new Rectangle(left, top, right - left, bottom - top));
                words.add(word);
            } while (this.api.TessPageIteratorNext(pi, pageIteratorLevel) == TRUE);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }

        return words;
    }

    /**
     * Releases all of the native resources used by this processInstance.
     */
    public void dispose() {
        if ((this.api != null) && (this.handle != null)) {
            this.api.TessBaseAPIDelete(this.handle);
        }
    }

    public boolean release() {
        try {
            if ((this.api != null) && (this.handle != null)) {
                this.api.TessBaseAPIClear(this.handle);
            }
        } catch (Exception e) {
            this.api.TessBaseAPIEnd(this.handle);
            this.dispose();
            return false;
        }
        return true;
    }

    public boolean destroy() {
        try {
            if ((this.api != null) && (this.handle != null)) {
                this.api.TessBaseAPIClearPersistentCache(this.handle);
                this.api.TessBaseAPIEnd(this.handle);
                this.dispose();
            }
        } catch (Exception e) {
            this.api.TessBaseAPIEnd(this.handle);
            this.dispose();
            return false;
        }
        return true;
    }

    public boolean isValid() {
        PointerByReference language = this.api.TessBaseAPIGetLoadedLanguagesAsVector(this.handle);
        if ((language.getValue() == null) || (language.getValue().getString(0) == null)
                || ("".contentEquals(language.getValue().getString(0)))) {
            Leptonica1.lept_free(language.getValue());
            return false;
        }
        Leptonica1.lept_free(language.getValue());
        return true;
    }

    public List<String> listFiles(String dir, int depth) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            return stream.filter(file -> !Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    public synchronized List<String> getImageFiles(String dir, int depth) throws IOException {
        if (imageFilesExtracted) {
            return imageFiles;
        }
        List<String> allFiles = null;
        try (Stream<Path> stream = Files.walk(Paths.get(dir), depth)) {
            allFiles = stream.filter(file -> !Files.isDirectory(file)).map(Path::toAbsolutePath).map(Path::toString)
                    .collect(Collectors.toList());
        }
        if (this.debugLevel <= 1) {
            System.out.println("Files from directory - " + dir + " are : " + allFiles);
        }
        String regex = "^.+\\.(gif|png|jpg|jpeg|tif|tiff)$";
        Pattern p = Pattern.compile(regex);
        for (String file : allFiles) {
            Matcher matcher = p.matcher(file);
            if (matcher.find()) {
                imageFiles.add(file);
            }
        }
        imageFilesExtracted = true;
        return imageFiles;
    }

    /*
     * private String getResultFile(String imageFile) { int index =
     * imageFile.lastIndexOf("."); String first = imageFile.substring(0, index);
     * String second = " Result.txt"; if (this.debugLevel <= 1) {
     * System.out.println("Instance - " + this.processInstance +
     * " : Result file is " + first + second); } return first + second; }
     */

    /*
     * private String readResults(String file) { String result = results.get(file);
     * if (result != null) { if (this.debugLevel <= 2) { System.out.println(
     * "Instance - " + this.processInstance +
     * " : Got result from results HashMap for file " + file); } return result; }
     * BufferedReader br = null; try { br = new BufferedReader(new
     * FileReader(file)); } catch (Exception e) { e.printStackTrace(); if
     * (this.debugLevel <= 5) { System.out.println("Instance - " +
     * this.processInstance + " : Could not read from file " + file); } return ""; }
     * StringBuffer out = new StringBuffer(); try { String line = br.readLine();
     * while (line != null) { this.append(out, line); line = br.readLine(); }
     * br.close(); } catch (IOException e) { } results.put(file, out.toString()); if
     * (this.debugLevel <= 2) { System.out.println( "Instance - " +
     * this.processInstance + " : Added result to results HashMap for file " +
     * file); } if (this.debugLevel <= 1) { System.out.println("Instance - " +
     * this.processInstance + " : Result = " + out.toString()); }
     *
     * return out.toString(); }
     */

    public void append(StringBuffer out, String line) {
        if ((line != null) && !("".equals(line.trim()))) {
            // if ((line != null) && (line.length() > 0)) {
            out.append(System.lineSeparator());
            out.append(line);
        }
    }

    public boolean compare(String ocrOutput, String fileResult, boolean strict) {
        // do a line by line comparison
        // String[] ocrArray = ocrOutput.lines().toArray(String[]::new);
        // List<String> ocrList = Arrays.asList(ocrArray);
        // ArrayList<String> ocrStringArray = new ArrayList<>();
        String[] ocrLines = ocrOutput.split("\\r?\\n");
        // String[] ocrLines = ocrOutput.split(System.lineSeparator());
        ArrayList<String> ocrStringArray = new ArrayList<>();
        for (String line : ocrLines) {
            if ("".equals(line.trim())) {
                continue;
            }
            ocrStringArray.add(line);
        }

        String[] resultLines = fileResult.split("\\r?\\n");
        ArrayList<String> resultStringArray = new ArrayList<>();
        for (String line : resultLines) {
            if ("".equals(line.trim())) {
                continue;
            }
            resultStringArray.add(line);
        }
        if (this.debugLevel <= 1) {
            System.out.println(
                    "Instance - " + this.processInstance + " : Inside compare: Result array is - " + resultStringArray);
        }

        if (this.debugLevel <= 1) {
            System.out.println(
                    "Instance - " + this.processInstance + " : Inside compare: OCR array is - " + ocrStringArray);
        }

        if (strict) {
            if (ocrStringArray.size() != resultStringArray.size()) {
                return false;
            }
        }
        int min = Math.min(ocrStringArray.size(), resultStringArray.size());

        int similarityCount = 0;
        for (int i = 0; i < min; ++i) {
            int similar = FuzzySearch.weightedRatio(ocrStringArray.get(i), resultStringArray.get(i));
            if (this.debugLevel <= 1) {
                System.out.println("Instance - " + this.processInstance + " : Similarity weightedRatio of string " + i
                        + " is - " + similar);
            }

            if (similar > 89) {
                ++similarityCount;
            }
        }
        double threshold = strict ? 0.5 : 0.8;
        if (this.debugLevel <= 2) {
            System.out.println("Instance - " + this.processInstance + " : Against threshold of " + threshold
                    + ", the actual value is " + ((similarityCount * 1.0) / min));
        }
        if (((similarityCount * 1.0) / min) >= threshold) {
            return true;
        }
        return false;
    }

    /*
     * private boolean checkGoodness(String directory, boolean strict) { if
     * (this.debugLevel <= 4) {
     * System.out.println("Running the checkGoodness() test in CosThetaTesseract - "
     * + this.processInstance); } if (!this.isValid()) { return false; } try { if
     * (this.debugLevel <= 1) { System.out.println( "Instance - " +
     * this.processInstance + "Extracting image files from directory - " +
     * directory); } this.getImageFiles(directory, 1); if (this.debugLevel <= 1) {
     * System.out.println( "Instance - " + this.processInstance +
     * "Got image files for Tesseract testing - " + imageFiles); } } catch
     * (IOException ioe) { ioe.printStackTrace(); if (strict) { return false; } else
     * { return true; } } ArrayList<Boolean> comparisonResults = new
     * ArrayList<>(imageFiles.size()); for (String imageFile : imageFiles) { String
     * resultFile = this.getResultFile(imageFile); String result =
     * this.readResults(resultFile); String ocrResult = null; try { ocrResult =
     * this.doOCR(ImageIO.read(new File(imageFile))); } catch (IOException ioe) {
     * ocrResult = ""; } catch (TesseractException te) { ocrResult = ""; } if
     * (this.debugLevel <= 1) { System.out.println("Instance - " +
     * this.processInstance + " : Result file data is - " + result); } if
     * (this.debugLevel <= 1) { System.out.println("Instance - " +
     * this.processInstance + " : OCR file data is - " + ocrResult); } boolean compR
     * = this.compare(ocrResult, result, strict); if (this.debugLevel <= 2) {
     * System.out.println("Instance - " + this.processInstance +
     * " : Comparison of OCR for file " + imageFile + " with results file is " +
     * compR); } comparisonResults.add(compR); } int trueCount = 0; for (Boolean
     * comparisonResult : comparisonResults) { if
     * (comparisonResult.equals(Boolean.TRUE)) { ++trueCount; } } double threshold =
     * strict ? 0.5 : 0.8; if (this.debugLevel <= 2) {
     * System.out.println("Instance - " + this.processInstance +
     * " : Final estimation : Against threshold of " + threshold +
     * ", the actual value is " + ((trueCount * 1.0) / imageFiles.size())); } if
     * (((trueCount * 1.0) / imageFiles.size()) >= threshold) { return true; }
     * return false; }
     */

    void copyResources(URL resourceUrl, File targetPath) throws IOException, URISyntaxException {
        if (resourceUrl == null) {
            return;
        }

        URLConnection urlConnection = resourceUrl.openConnection();
        System.out.println("urlConnection = " + urlConnection + "; date = " + urlConnection.getDate()
                + "; contentLength = " + urlConnection.getContentLengthLong());
        /**
         * Copy resources either from inside jar or from project folder.
         */
        if (urlConnection instanceof JarURLConnection) {
            System.out.println("copying urlConnection from jar");
            this.copyJarResourceToPath((JarURLConnection) urlConnection, targetPath);
        } else if ("vfs".equals(resourceUrl.getProtocol())) {
            System.out.println("copying urlConnection from virtual file system");
            VirtualFile virtualFileOrFolder = VFS.getChild(resourceUrl.toURI());
            this.copyFromWarToFolder(virtualFileOrFolder, targetPath);
        } else {
            System.out.println("creating a new file from urlConnection");
            File file = new File(resourceUrl.getPath());
            System.out.println("file = " + file);
            if (file.isDirectory()) {
                System.out.println(file + " is a directory. So, will iterate through the contents of the directory");
                for (Object resourceFile : FileUtils.listFiles(file, null, true)) {
                    int index = ((File) resourceFile).getPath().lastIndexOf(targetPath.getName())
                            + targetPath.getName().length();
                    File targetFile = new File(targetPath, ((File) resourceFile).getPath().substring(index));
                    System.out.println("targetFile = " + targetFile);
                    if (!targetFile.exists() || (targetFile.length() != ((File) resourceFile).length())
                            || (targetFile.lastModified() != ((File) resourceFile).lastModified())) {
                        if (((File) resourceFile).isFile()) {
                            System.out.println("copying file = " + targetFile + " to resource file");
                            FileUtils.copyFile(((File) resourceFile), targetFile, true);
                        }
                    }
                }
            } else {
                if (!targetPath.exists() || (targetPath.length() != file.length())
                        || (targetPath.lastModified() != file.lastModified())) {
                    System.out.println("copying file = " + targetPath + " to resource file");
                    FileUtils.copyFile(file, targetPath, true);
                }
            }
        }
    }

    /**
     * Copies resources from the jar file of the current thread and extract it to
     * the destination path.
     *
     * @param jarConnection
     * @param destPath      destination file or directory
     */
    void copyJarResourceToPath(JarURLConnection jarConnection, File destPath) {
        try (JarFile jarFile = jarConnection.getJarFile()) {
            String jarConnectionEntryName = jarConnection.getEntryName();
            if (!jarConnectionEntryName.endsWith("/")) {
                jarConnectionEntryName += "/";
            }

            /**
             * Iterate all entries in the jar file.
             */
            for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                JarEntry jarEntry = e.nextElement();
                String jarEntryName = jarEntry.getName();

                /**
                 * Extract files only if they match the path.
                 */
                if (jarEntryName.startsWith(jarConnectionEntryName)) {
                    String filename = jarEntryName.substring(jarConnectionEntryName.length());
                    File targetFile = new File(destPath, filename);

                    if (jarEntry.isDirectory()) {
                        targetFile.mkdirs();
                    } else {
                        if (!targetFile.exists() || (targetFile.length() != jarEntry.getSize())) {
                            try (InputStream is = jarFile.getInputStream(jarEntry);
                                 OutputStream out = FileUtils.openOutputStream(targetFile)) {
                                IOUtils.copy(is, out);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        }
    }

    /**
     * Copies resources from WAR to target folder.
     *
     * @param virtualFileOrFolder
     * @param targetFolder
     * @throws IOException
     */
    void copyFromWarToFolder(VirtualFile virtualFileOrFolder, File targetFolder) throws IOException {
        if (virtualFileOrFolder.isDirectory() && !virtualFileOrFolder.getName().contains(".")) {
            if (targetFolder.getName().equalsIgnoreCase(virtualFileOrFolder.getName())) {
                for (VirtualFile innerFileOrFolder : virtualFileOrFolder.getChildren()) {
                    this.copyFromWarToFolder(innerFileOrFolder, targetFolder);
                }
            } else {
                File innerTargetFolder = new File(targetFolder, virtualFileOrFolder.getName());
                innerTargetFolder.mkdir();
                for (VirtualFile innerFileOrFolder : virtualFileOrFolder.getChildren()) {
                    this.copyFromWarToFolder(innerFileOrFolder, innerTargetFolder);
                }
            }
        } else {
            File targetFile = new File(targetFolder, virtualFileOrFolder.getName());
            if (!targetFile.exists() || (targetFile.length() != virtualFileOrFolder.getSize())) {
                FileUtils.copyURLToFile(virtualFileOrFolder.asFileURL(), targetFile);
            }
        }
    }

    File extractTessResources(String resourceName) {
        File targetPath = null;

        try {
            targetPath = new File(new File(System.getProperty("java.io.tmpdir"), "tess4j").getPath(), resourceName);
            System.out.println("targetPath = " + targetPath);
            Enumeration<URL> resources = LoadLibs.class.getClassLoader().getResources(resourceName);
            int i = 1;
            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                System.out.println("URL " + i++ + " = " + resourceUrl);
                this.copyResources(resourceUrl, targetPath);
            }
        } catch (IOException | URISyntaxException e) {
            logger.warn(e.getMessage(), e);
        }

        System.out.println("extractTessResources() : Returning targetPath = " + targetPath);
        return targetPath;
    }

    public static final void changeDatapath(String datapath) {
        CosThetaTesseract.datapath = datapath;
    }

}
