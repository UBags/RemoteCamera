package com.costheta.camera.remote;

import com.costheta.camera.imagemessages.ImageBytes;
import com.costheta.camera.imagemessages.ImageRequest;
import com.costheta.camera.imagemessages.ImageResponse;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RemoteCameraUtils {

    private static Logger logger = LogManager.getLogger(RemoteCameraUtils.class);

    public static final String NUMBER_OF_CAMERAS_KEY = "cameras.total";
    public static final String TCP_PORT_KEY = "tcp.port";
    public static final String UDP_PORT_KEY = "udp.port";
    public static final String TIMEOUT_MILLIS_KEY = "timeout.millis";
    public static final int baseUdpPort = 54776;
    public static final ArrayList<Integer> udpPortsDefinedInProperties = new ArrayList<>();
    private static boolean udpPortsListPopulated = false;

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ImageRequest.class);
        kryo.register(byte[].class);
        kryo.register(ImageResponse.class);
        kryo.register(ImageBytes.class);
    }

    public static boolean ensurePropertiesFileIntegrity(int numberOfCameras) {
        if (udpPortsListPopulated) {
            // this is true only if this call finishes successfully once
            return true;
        }
        int lastUdpPortDefined = baseUdpPort;
        for (int i = 0; i < numberOfCameras; ++i) {
            int port = 0;
            try {
                port = Integer.parseInt(
                        System.getProperty(new StringBuilder().append(i + 1).append(".").append(UDP_PORT_KEY).toString(), "0"));
            } catch (NumberFormatException nfe) {
                port = 0;
            }

            if (port == 0) {
                logger.debug("Found udp port as 0 for (i+1) = " + (i + 1));
                String error1 = new StringBuilder().append(i + 1).append(".").append(UDP_PORT_KEY)
                        .append(" needs to be defined in camera.properties").toString();
                logger.fatal(error1);
                System.out.println(error1);
                String suggestion = new StringBuilder().append(i + 1).append(".").append(UDP_PORT_KEY).append("=")
                        .append(baseUdpPort + (i+1)).toString();
                String error2 = new StringBuilder().append("Add the line ").append(suggestion)
                        .append(" in the camera.properties file and run the program again").toString();
                logger.fatal(error2);
                System.out.println(error2);
                return false;
            } else {
                boolean duplicate = udpPortsDefinedInProperties.contains(port);
                if (duplicate) {
                    logger.debug("Found udp port as non-zero for (i+1) = " + (i + 1));
                    String error1 = new StringBuilder().append(i + 1).append(".").append(UDP_PORT_KEY)
                            .append(" needs to be defined in camera.properties").toString();
                    logger.fatal(error1);
                    System.out.println(error1);
                    String suggestion = new StringBuilder().append(i + 1).append(".").append(UDP_PORT_KEY).append("=")
                            .append(baseUdpPort + (i+1)).toString();
                    String error2 = new StringBuilder().append("Add the line ").append(suggestion)
                            .append(" in the camera.properties file and run the program again").toString();
                    logger.fatal(error2);
                    System.out.println(error2);
                    return false;
                } else {
                    if (!udpPortsListPopulated) {
                        udpPortsDefinedInProperties.add(port);
                    }
                }
            }
        }
        String tPort = System.getProperty(TCP_PORT_KEY, "0");
        int tcp_Port =  Integer.parseInt(tPort);
        if ((tcp_Port == 0) || (udpPortsDefinedInProperties.contains(tcp_Port))) {
            String error1 = new StringBuilder().append(TCP_PORT_KEY)
                    .append(" needs to be uniquey defined in camera.properties.")
                    .append(" It should be greater than 0 and should not be same as any of the UDP ports").toString();
            logger.fatal(error1);
            System.out.println(error1);
            String suggestion = new StringBuilder().append(TCP_PORT_KEY).append("=")
                    .append(baseUdpPort - 221).toString();
            String error2 = new StringBuilder().append("Shutting down the system. Add the line ").append(suggestion)
                    .append(" in the camera.properties file and run the program again").toString();
            logger.fatal(error2);
            System.out.println(error2);
            return false;
        }
        udpPortsListPopulated = true;
        return true;
    }

    public static boolean checkArgsIntegrity(int cameraID) {
        int numberOfCameras = Integer.parseInt(System.getProperty(RemoteCameraUtils.NUMBER_OF_CAMERAS_KEY, "0"));
        if (numberOfCameras <= 0) {
            logger.fatal(String.format("Number of cameras defined in camera.properties is %s", numberOfCameras));
            System.out.println(String.format("Number of cameras defined in camera.properties is %s", numberOfCameras));
            logger.fatal("Shutting down the system as at least 1 camera is required...");
            System.out.println("Shutting down the system as at least 1 camera is required...");
            System.exit(1);
        }
        boolean integrityOfProperties = RemoteCameraUtils.ensurePropertiesFileIntegrity(numberOfCameras);
        if (!integrityOfProperties) {
            logger.fatal("Shutting down the system. Make suggested changes to the properties file and restart.");
            System.exit(1);
        }
        int currentUdpPort = 0;
        try {
            currentUdpPort = Integer.parseInt(
                    System.getProperty(new StringBuilder().append(cameraID).append(".").append(RemoteCameraUtils.UDP_PORT_KEY).toString(), "0"));
        } catch (NumberFormatException nfe) {
            currentUdpPort = 0;
        }
        if (currentUdpPort == 0) {
            String error1 = new StringBuilder().append(cameraID).append(".").append(RemoteCameraUtils.UDP_PORT_KEY)
                    .append(" needs to be defined in camera.properties").toString();
            logger.fatal(error1);
            System.out.println(error1);
            String suggestion = new StringBuilder().append(cameraID).append(".").append(RemoteCameraUtils.UDP_PORT_KEY).append("=")
                    .append(RemoteCameraUtils.baseUdpPort + cameraID).toString();
            String error2 = new StringBuilder().append("Shutting down the system. Add the line ").append(suggestion)
                    .append(" in the camera.properties file and run the program again").toString();
            logger.fatal(error2);
            System.out.println(error2);
            System.exit(1);
        }
        return true;
    }

}
