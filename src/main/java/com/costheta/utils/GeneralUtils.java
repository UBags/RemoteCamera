package com.costheta.utils;

import com.costheta.machine.BaseApplication;
import com.costheta.text.utils.LinkedProperties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.UnsupportedOptionsException;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GeneralUtils {

    private static final Logger logger = LogManager.getLogger(GeneralUtils.class);

    public static void rerouteLog(String[] args) {
        MainMapLookup.setMainArguments(args);
    }

    public static void updateLogger(String appender_name, String file_name, String package_name){
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        LoggerConfig oldConfig = configuration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        Level currentLevel = configuration.getRootLogger().getLevel();

        // boolean currentAdditive = configuration.; dont know how to get it
        Layout<? extends Serializable> old_layout = configuration.getAppender(appender_name).getLayout();

        //delete old appender/logger

        configuration.getAppender(appender_name).stop();
        configuration.removeLogger(package_name);

        //create new appender/logger
        LoggerConfig loggerConfig = new LoggerConfig(package_name, currentLevel, false);
        FileAppender appender = FileAppender.createAppender(file_name, "false", "false", appender_name, "true", "true", "true",
                "8192", old_layout, null, "false", "", configuration);
        appender.start();
        loggerConfig.addAppender(appender, currentLevel, null);
        configuration.addLogger(package_name, loggerConfig);

        context.updateLoggers();
    }

    public static byte[] decompress(byte[] toDecompress) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(toDecompress);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int readBufferSize = 32768;
        try {
            XZInputStream inXzInputStream = new XZInputStream(inputStream);
            //Chunked deflate process
            byte[] partialOutput = new byte[readBufferSize];
            while (inXzInputStream.read(partialOutput) != -1) {
                outputStream.write(partialOutput);
            }
            inXzInputStream.close();
        } catch (UnsupportedOptionsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static byte[] compress(byte[] toCompress) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(toCompress);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LZMA2Options options = new LZMA2Options();
        int readBufferSize = 32768;

        try {
            options.setPreset(7); // play with this number: 6 is default but 7 works better for mid sized archives ( > 8mb)
        } catch (UnsupportedOptionsException uoe) {
            uoe.printStackTrace();
        }

        XZOutputStream xzOutputStream = null;

        try {
            xzOutputStream = new XZOutputStream(outputStream, options);
            byte[] buf = new byte[readBufferSize];
            int size;
            while ((size = inputStream.read(buf)) != -1) {
                xzOutputStream.write(buf, 0, size);
            }
            xzOutputStream.finish();
            xzOutputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public static Properties loadPropertiesFile(String absolutePathToFile) {
        logger.debug("Entering loadPropertiesFile() with " + absolutePathToFile);
        Properties props = new Properties();
        File initFile = new File(absolutePathToFile);
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(initFile);
        } catch (Exception e) {
            // e.printStackTrace();
            logger.debug(e.toString());
        }
        if (inputStream != null) {
            try {
                props.load(inputStream);
            } catch (Exception e) {
                logger.error("Property initialisation file " + absolutePathToFile + " not found in the classpath "
                        + System.getProperty("java.class.path"));
                logger.error(e.getMessage());
            }
        } else {
            logger.error("InputStream = null");
            logger.error("Property initialisation file " + absolutePathToFile + " not found in the classpath "
                    + System.getProperty("java.class.path"));
        }

        // list the entries in the properties file
        Set<Map.Entry<Object, Object>> entries = props.entrySet();
        Iterator<Map.Entry<Object, Object>> entriesIterator = entries.iterator();
        while (entriesIterator.hasNext()) {
            Map.Entry<Object, Object> entry = entriesIterator.next();
            logger.info(entry.getKey().toString() + "=" + entry.getValue().toString());
            System.out.println(entry.getKey().toString() + "=" + entry.getValue().toString());
        }
        return props;
    }

    public static Properties loadPropertiesFile(String baseDir, String initFileArgument) {
        return loadPropertiesFile(baseDir + "/" + initFileArgument);
    }

    public static boolean savePropertiesFile(LinkedProperties props, String absolutePathToFile) {
        logger.debug("Entering savePropertiesFile() with " + absolutePathToFile);
        Path outputFilePath = Paths.get(absolutePathToFile);

        if(!Files.exists(outputFilePath.getParent())) {
            try {
                Files.createDirectory(outputFilePath.getParent());
            } catch (Exception e) {
                logger.debug("Couldn't create the path to properties file directory " + outputFilePath.getParent().toString());
                return false;
            }
        }
        if(!Files.exists(outputFilePath)) {
            try {
                Files.createFile(outputFilePath);
            } catch (Exception e) {
                logger.debug("File didn't exist. But, couldn't create the properties file " + absolutePathToFile);
                return false;
            }
        } else {
            try {
                Files.deleteIfExists(outputFilePath); // first delete the file
                Files.createFile(outputFilePath);
            } catch (Exception e) {
                logger.debug("File exists. Either can't delete it, or couldn't create the properties file " + absolutePathToFile);
                return false;
            }
        }

        try {
            Writer propWriter = Files.newBufferedWriter(outputFilePath);
            props.store(propWriter,"Application Properties");
            propWriter.close();
        } catch(IOException ex) {
            System.out.println("IO Exception :" + ex.getMessage());
            return false;
        }
        return true;
    }

    public static boolean savePropertiesFile(LinkedProperties props, String baseDir, String initFileArgument) {
        return savePropertiesFile(props, baseDir + "/" + initFileArgument);
    }

    public static final <K extends Comparable<K>,T> Hashtable<K,T> sortAscendingByKey(Hashtable<K,T> hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o1.getKey()).compareTo(o2.getKey());
            }
        });

        // put data from sorted list to hashmap
        Hashtable<K, T> temp = new Hashtable<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K extends Comparable<K>,T> Hashtable<K,T> sortDescendingByKey(Hashtable<K,T> hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o2.getKey()).compareTo(o1.getKey());
            }
        });

        // put data from sorted list to hashmap
        Hashtable<K, T> temp = new Hashtable<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K,T extends Comparable<T>> Hashtable<K,T> sortAscendingByValue(Hashtable<K,T> hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        Hashtable<K, T> temp = new Hashtable<K, T>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final <K,T extends Comparable<T>> Hashtable<K,T> sortDescendingByValue(Hashtable<K,T> hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<K, T> > list =
                new LinkedList<Map.Entry<K, T> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<K, T> >() {
            public int compare(Map.Entry<K, T> o1,
                               Map.Entry<K, T> o2)
            {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        Hashtable<K, T> temp = new Hashtable<>();
        for (Map.Entry<K, T> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    public static final Properties sortAscendingByKey(Properties hm)
    {
        // Doesn't work !! :(
        // Gets sorted, but again gets unsorted when a new Properties file is made with the sorted list

        // Create a list from elements of Hashtable
        List<Map.Entry<Object, Object> > list =
                new LinkedList<Map.Entry<Object, Object> >(hm.entrySet());

        // System.out.println("Linked list before sorting = " + list);

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Object, Object> >() {
            public int compare(Map.Entry<Object, Object> o1,
                               Map.Entry<Object, Object> o2)
            {
                return ((String) o1.getKey()).compareTo((String) o2.getKey());
            }
        });

        // System.out.println("Linked list after sorting = " + list);

        // put data from sorted list to hashmap
        Properties temp = new Properties();
        for (Map.Entry<Object, Object> aa : list) {
            temp.put((String) aa.getKey(), (String) aa.getValue());
        }
        return temp;
    }

    public static final Properties sortDescendingByKey(Properties hm)
    {
        // Doesn't work !! :(
        // Gets sorted, but again gets unsorted when a new Properties file is made with the sorted list
        // Create a list from elements of Hashtable
        List<Map.Entry<Object, Object> > list =
                new LinkedList<Map.Entry<Object, Object> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Object, Object> >() {
            public int compare(Map.Entry<Object, Object> o1,
                               Map.Entry<Object, Object> o2)
            {
                return ((String) o2.getKey()).compareTo((String) o1.getKey());
            }
        });

        // put data from sorted list to hashmap
        Properties temp = new Properties();
        for (Map.Entry<Object, Object> aa : list) {
            temp.put((String) aa.getKey(), (String) aa.getValue());
        }
        return temp;
    }

    public static final Properties sortAscendingByValue(Properties hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<Object, Object> > list =
                new LinkedList(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Object, Object> >() {
            public int compare(Map.Entry<Object, Object> o1,
                               Map.Entry<Object, Object> o2)
            {
                return ((String)o1.getValue()).compareTo((String)o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        Properties temp = new Properties();
        for (Map.Entry<Object, Object> aa : list) {
            temp.put((String) aa.getKey(), (String) aa.getValue());
        }
        return temp;
    }

    public static final Properties sortDescendingByValue(Properties hm)
    {
        // Create a list from elements of Hashtable
        List<Map.Entry<Object, Object>> list =
                new LinkedList<Map.Entry<Object, Object> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<Object, Object> >() {
            public int compare(Map.Entry<Object, Object> o1,
                               Map.Entry<Object, Object> o2)
            {
                return ((String)o2.getValue()).compareTo((String)o1.getValue());
            }
        });

        // put data from sorted list to hashmap
        Properties temp = new Properties();
        for (Map.Entry<Object, Object> aa : list) {
            temp.put((String) aa.getKey(), (String) aa.getValue());
        }
        return temp;
    }
}
