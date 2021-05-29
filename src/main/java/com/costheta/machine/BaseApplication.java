package com.costheta.machine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

public abstract class BaseApplication {

    private static final Logger logger = LogManager.getLogger(BaseApplication.class);

    public static final String BASE_DIR_KEY = "base.Dir";
    public static final String PROPERTIES_FILE_KEY = "cameraProperties.File";

    public static final String separatorLine = "*************************************************************";
    public static final String generalCommandLine = "Expecting command line values for at least 1 mandatory argument - base.Dir and 2 optional arguments - cameraProperties.File & camera.Id";
    public static final String commandLineFormat = "The base directory file has to be provided as base.Dir={directory path to directory of initialisation file} cameraProperties.File={name of camera properties file} camera.Id={number id of camera i.e. 1/2/3... }";
    public static final String configurationFileStatement = "The default configuration file is camera.properties";
    public static final String exampleCommandStatement = new StringBuilder("Example command: javaw -jar RemoteCamera.jar base.Dir=/home/ubuntu/camera  cameraProperties.File=camera.properties camera.Id=1")
            .append("\n").append("OR javaw -jar RemoteCamera.jar base.Dir=C:/camera/remote  cameraProperties.File=camera.properties camera.Id=2").toString();
    public static final String serverIdStatement = "If the camera id is not specified or is specified as 0 or a negative number, this process will start as the common client that will request and receive images from all the cameras";
    public static final String serverErrorStatement = "If the camera id is specified, it must be a number for which a UDP port has been specified in the file named cameraProperties.File. The value of camera.Id has to be a positive integer.";
    public static final String udpPortMismatchStatement = "If the camera id is {i}, then the UDP Port in the file cameraProperties.File must be specified as {i}.udp.port={port number}";
    public static final String exitStatement = "Exiting. Restart after making the requisite changes.";

    protected static final String EMPTY_STRING = "";
    protected static final String BAD_ARGUMENT = "BAD ARGUMENT";

    private static String baseDir = "";
    public  static String getBaseDir() {
        return baseDir;
    }

    private static String initialisationFile = "camera.properties";
    public static String getInitialisationFile() {
        return initialisationFile;
    }
    private static boolean initialised = false;

    private static Path initialisationFilePath = null;
    public static Path getInitialisationFilePath() {
        return initialisationFilePath;
    }
    private static boolean propertiesLoaded = false;
    public static String[] initialisationArgs;

    protected static int isWindows = 2; // initialised to UNKNOWN OS
    public static int isWindows(int isWindows) {
        logger.trace("Entering inWindows() with " + isWindows);
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
        logger.info("Returning isWindows value as " + isWindows + ", which implies "
                    + ((isWindows == 1) ? "Windows" : "Unix"));
        return isWindows;
    }

    protected static void initialise(String[] args) {
        logger.trace("Entering initialise() with arguments " + args);
        if (initialised) {
            return;
        }
        logger.trace("Since the application has not been initialised, entering the initialisation section");
        int length = 0;
        if (args != null) {
            length = args.length;
        }
        for (int i = 0; i < length; ++i) {
            logger.trace("Argument " + (i + 1) + " is " + args[i]);
            System.out.println(args[i]);
        }
        String baseDirInput =  getValueOfArguments(args, BASE_DIR_KEY);
        String initFileInput =  getValueOfArguments(args, PROPERTIES_FILE_KEY);

        if (!EMPTY_STRING.equals(baseDirInput)) {
            baseDir = baseDirInput;
        } else {
            baseDir = System.getProperty("user.dir");
        }
        System.setProperty(BASE_DIR_KEY,baseDir);
        if (!EMPTY_STRING.equals(initFileInput)) {
            initialisationFile = initFileInput;
        }
        System.setProperty(PROPERTIES_FILE_KEY,initialisationFile);
        initialisationFilePath = Paths.get(baseDir, initialisationFile);
        isWindows = isWindows(isWindows);
        loadPropertiesFile(initialisationFilePath.toAbsolutePath().toString());
//        System.out.println(generalCommandLine);
//        System.out.println(separatorLine);
//        System.out.println(commandLineFormat);
//        System.out.println(separatorLine);
//        System.out.println(exampleCommandStatement);
//        System.out.println(separatorLine);
//        System.out.println(serverIdStatement);
//        System.out.println(separatorLine);
        System.out.println("initialisationFilePath = " + initialisationFilePath.toAbsolutePath().toString());
        initialised = true;
    }

    protected static String getValueOfArguments(String[] args, String key) {
        logger.trace("Entering getValueOfArguments()");
        if (args == null) {
            return EMPTY_STRING;
        }
        if (args.length == 0) {
            return EMPTY_STRING;
        }
        if (key == null) {
            return EMPTY_STRING;
        }
        if (EMPTY_STRING.equals(key)) {
            return EMPTY_STRING;
        }
        for (int i = 0; i < args.length; ++i) {
            String[] keyValuePair = args[i].split("=");
            if (keyValuePair.length != 2) {
                continue;
            }
            String thisKey = keyValuePair[0];
            if (key.equals(thisKey)) {
                return keyValuePair[1].trim();
            }
        }
        // no match found
        return EMPTY_STRING;
    }

    protected static void loadPropertiesFile(String initFileArgument) {
        logger.trace("Entering loadPropertiesFile() with " + initFileArgument);
        if (propertiesLoaded) {
            return;
        }
        logger.debug("initFile = " + initFileArgument);
        java.util.Properties props = new java.util.Properties();
        File initFile = new File(initFileArgument);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(initFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (Exception e) {
                logger.error("Property initialisation file " + initialisationFile + " not found in the classpath "
                        + System.getProperty("java.class.path"));
                logger.error(e.getMessage());
            }
        } else {
            logger.error("InputStream = null");
            logger.error("Property initialisation file " + initialisationFile + " not found in the classpath "
                    + System.getProperty("java.class.path"));
        }

        Set<Map.Entry<Object, Object>> entries = props.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.setProperty(entry.getKey().toString(), entry.getValue().toString());
            logger.info(entry.getKey().toString() + "=" + entry.getValue().toString());
            System.out.println(entry.getKey().toString() + "=" + entry.getValue().toString());
        }
        propertiesLoaded = true;
    }

    public static void populateInitialisationArgs(String[] args) {
        logger.trace("Entering populateInitialisationArgs() with " + args);
        if (!initialised) {
            initialisationArgs = args;
        }
    }
}
