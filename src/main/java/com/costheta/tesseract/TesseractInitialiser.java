package com.costheta.tesseract;

import com.costheta.machine.BaseCamera;
import net.sourceforge.lept4j.ILeptonica;
import net.sourceforge.lept4j.Leptonica1;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class TesseractInitialiser {

    private static final Logger logger = LogManager.getLogger(TesseractInitialiser.class);

    public static void initialise() {

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
        Leptonica1.setLeptDebugOK(0);
        Leptonica1.setMsgSeverity(ILeptonica.L_SEVERITY_NONE);
    }
}
