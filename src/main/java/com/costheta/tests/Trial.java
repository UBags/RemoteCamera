package com.costheta.tests;

import com.costheta.image.utils.ImageUtils;
import com.costheta.tesseract.CosThetaTesseract;
import net.sourceforge.lept4j.*;
import net.sourceforge.tess4j.TesseractException;


import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Trial {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\temp\\20210316172130";
    private static String fileName = "Trial.jpg";
    public static String debugDirectory = inputDirectory + "/" + "debug";
    public static String datapath = "E:/TechWerx/CosTheta/IntelliJ IDEA Projects/RemoteCamera/External Libraries/tessdata";

    static {
        ImageUtils.initialise();
        CosThetaTesseract.changeDatapath(datapath);
    }

    public static void main(String[] args) throws TesseractException {

        System.out.println("Running Trial");
        try {
            Files.createDirectories(Paths.get(debugDirectory));
        } catch (Exception e) {

        }
        debugDirectory = debugDirectory + "/";

        System.setProperty("java.io.tmpdir", System.getProperty("user.dir") + "/External Libraries");
        System.setProperty("java.library.path", System.getProperty("user.dir") + "/External Libraries");

        try {
            Field field = ClassLoader.class.getDeclaredField("sys_paths");
            field.setAccessible(true);

            // Create override for sys_paths
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            ArrayList<String> newSysPaths = new ArrayList<String>();
            newSysPaths.add(System.getProperty("user.dir"));
            newSysPaths.addAll(Arrays.asList((String[]) field.get(classLoader)));
            field.set(classLoader, newSysPaths.toArray(new String[newSysPaths.size()]));
        } catch (Exception e) {

        }

        try {
            // System.out.println("java.library.path = " +
            // System.getProperty("java.library.path"));
            // System.out.println("leptLib = " + leptLib);
            // System.loadLibrary(leptLib);
            String library = System.getProperty("user.dir") + File.separator + "libLept1790.dll";
            // System.out.println("Trying to load : " + library);
            System.load(library); // using this as it takes the full path, unlike all the other methods
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not load library " + "libLept1790.dll");
        }
        try {
            // System.out.println("java.library.path = " +
            // System.getProperty("java.library.path"));
            // System.out.println("tesseractLib = " + tesseractLib);
            // System.loadLibrary(tesseractLib);
            String library = System.getProperty("user.dir") + File.separator + "libTesseract411" + ".dll";
            // System.out.println("Trying to load : " + library);
            System.load(library); // using this as it takes the full path, unlike all the other methods
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not load library " + "libTesseract411.dll");
        }
        ImageUtils.initialise();
        Leptonica1.setLeptDebugOK(0);
        Leptonica1.setMsgSeverity(ILeptonica.L_SEVERITY_NONE);
        Pix originalPix = Leptonica.INSTANCE.pixRead(inputDirectory + "/" + fileName);
        BufferedImage originalMask1BI = ImageUtils.convertPixToImage(originalPix);
        ImageUtils.writeFile(originalMask1BI, "jpg", debugDirectory + "Trial - OriginalBI.jpg");
        BufferedImage copy = ImageUtils.copy(originalMask1BI);
        ImageUtils.writeFile(originalMask1BI, "jpg", debugDirectory + "Trial - Copy.jpg");
        BufferedImage original_Masked_Morphed = ImageUtils.dilateGrayBIWithFilter(originalMask1BI, 5, 3,192,ImageUtils.BLACK);
        ImageUtils.writeFile(original_Masked_Morphed, "jpg", debugDirectory + "Trial - Dilated.jpg");

        System.out.println("Done");

        System.exit(0);
    }
}
