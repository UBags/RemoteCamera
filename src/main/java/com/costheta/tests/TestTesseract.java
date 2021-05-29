package com.costheta.tests;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class TestTesseract {

    private static String inputDirectory = "E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\100001\\20210412014840\\Inspection Point 1\\Text Inspection\\pictures";
    private static String fileName = "3 - 6 - BNCN_GE.png";

    public static void main(String[] args) throws TesseractException, IOException {

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

        ITesseract instance = new Tesseract();  // JNA Interface Mapping
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        instance.setDatapath("E:\\TechWerx\\CosTheta\\IntelliJ IDEA Projects\\RemoteCamera\\External Libraries\\tessdata"); // path to tessdata directory

        BufferedImage tessImage = ImageIO.read(new File(inputDirectory + "/" + fileName));

        try {
            String result = instance.doOCR(tessImage);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("Done");

    }




}
