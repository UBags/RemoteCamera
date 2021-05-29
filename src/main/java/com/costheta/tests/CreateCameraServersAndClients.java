package com.costheta.tests;

import com.costheta.camera.imagemessages.ImageRequest;
import com.costheta.camera.imagemessages.ImageResponse;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;

public class CreateCameraServersAndClients {

    private static final Logger logger = LogManager.getLogger(CreateCameraServersAndClients.class);

    public static int numberOfCameras = 0;

    private static boolean[] connectedStatus = null;
    private static int clientConnected = -1;

    private static final String NUMBER_OF_CAMERAS = "cameras.total";
    private static final String TCP_PORT_KEY = "tcp.port";
    private static final String UDP_PORT_KEY = ".udp.port";
    private static final String TIMEOUT_MILLIS_KEY = "timeout.millis";

    private static int tcpPort = 54555;
    private static int[] udpPorts = { 54777 };
    private static int timeOutMillis = 5000;
    private static int lastUdpPortDefined = 54776;

    private static final String serverStart = "Starting camera id ";
    private static final String tcpStart = " on TCP port ";
    private static final String udpStart = " and UDP port ";

    private static final String numberOfCamerasErrorStatement = "The cameraId argument in the command line must be <= the number of cameras specified in cameras.total property in the properties file";

    public static Server getServer() {
        return server;
    }

    private static Server server = null;

    public static Client[] getClients() {
        return clients;
    }

    public static Client getClient(int cameraId) {
        if (cameraId < 0) {
            return null;
        }
        if (cameraId >= numberOfCameras) {
            return null;
        }
        if (connectedStatus[cameraId] == false) {
          return null;
        }
        return clients[cameraId];
    }

    private static Client[] clients = null;

    public static boolean[] getConnectedStatus() {
        return connectedStatus;
    }

    public static boolean getConnectedStatus(int cameraId) {
        if (cameraId < 0) {
            return false;
        }
        if (cameraId >= numberOfCameras) {
            return false;
        }
        return connectedStatus[cameraId];

    }


    private CreateCameraServersAndClients() {

    }

    public static void configure() {
        numberOfCameras = Integer.parseInt(System.getProperty(NUMBER_OF_CAMERAS, "0"));
        if (numberOfCameras <= 0) {
            System.out.println(String.format("Number of cameras defined in camera.properties is %s", numberOfCameras));
            System.out.println("Shutting down the system as at least 1 camera is required...");
            System.exit(1);
        }
        udpPorts = new int[numberOfCameras];
        for (int i = 0; i < numberOfCameras; ++i) {
            int currentUdpPort = 0;
            try {
                currentUdpPort = Integer.parseInt(
                        System.getProperty(new StringBuilder().append(i + 1).append(UDP_PORT_KEY).toString(), "0"));
            } catch (NumberFormatException nfe) {
                currentUdpPort = 0;
            }
            if (currentUdpPort == 0) {
                System.out.println(new StringBuilder().append(i + 1).append(UDP_PORT_KEY)
                        .append(" needs to be defined in camera.properties").toString());
                String suggestion = new StringBuilder().append(i + 1).append(UDP_PORT_KEY).append("=")
                        .append(lastUdpPortDefined + 1).toString();
                System.out.println("Shutting down the system. Add the line " + suggestion
                        + " in the camera.properties file and run the program again");
                System.exit(1);
            } else {
                boolean duplicate = false;
                for (int j = 0; j < i; ++j) {
                    if (udpPorts[j] == udpPorts[i]) {
                        duplicate = true;
                    }
                }
                if (duplicate) {
                    System.out.println(new StringBuilder().append(i + 1).append(UDP_PORT_KEY)
                            .append(" needs to be uniquely defined in camera.properties").toString());
                    String suggestion = new StringBuilder().append(i + 1).append(UDP_PORT_KEY).append("=")
                            .append(lastUdpPortDefined + 1).toString();
                    System.out.println("Shutting down the system. Add the line " + suggestion
                            + " in the camera.properties file and run the program again");
                    System.exit(1);
                }
                udpPorts[i] = currentUdpPort;
                lastUdpPortDefined = currentUdpPort;
            }
        }
        try {
            tcpPort = Integer.parseInt(System.getProperty(TCP_PORT_KEY, "54555"));
        } catch (NumberFormatException nfe) {
            tcpPort = 54555;
        }
        try {
            timeOutMillis = Integer.parseInt(System.getProperty(TIMEOUT_MILLIS_KEY, "5000"));
        } catch (NumberFormatException nfe) {
            timeOutMillis = 5000;
        }
        System.out.println("Time out value set to " + timeOutMillis);
        if (RemoteCamera1.server) {
            // start a server on this machine
            if (RemoteCamera1.cameraID > udpPorts.length) {
                System.out.println(RemoteCamera1.serverErrorStatement);
                System.out.println(RemoteCamera1.exitStatement);
                System.exit(0);

            }
            System.out.println(new StringBuilder(serverStart).append(RemoteCamera1.cameraID).append(tcpStart)
                    .append(tcpPort).append(udpStart).append(udpPorts[RemoteCamera1.cameraID - 1]).toString());
            server = new Server(16777216,16777216);
            register(server);
            server.start();
            server.addListener(new Listener() {

                public void connected (Connection connection) {
                    System.out.println("Received a connection");
                    connection.setKeepAliveTCP(3600000);
                    connection.setTimeout(4000000);
//                    NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS
//                    KEPT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
//                    WHAT DOESN'T WORK
//                    try {
//                        server.update(100);
//                    } catch (IOException ioe) {
//
//                    }
                }

                public void received(Connection connection, Object object) {
                    // System.out.println("Received a request");
                    if (object instanceof Integer) {
                        Integer request = (Integer) object;
                        clientConnected = request.intValue();
                        System.out.println("Established connection with CameraProcessor id " + clientConnected);
                        connection.sendTCP(ImageResponse.EMPTY_IMAGE_RESPONSE);
//                        NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS
//                        KEPT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
//                        WHAT DOESN'T WORK
//                        try {
//                            server.update(250);
//                        } catch (IOException ioe) {
//
//                        }
                        return;
                    } else {
                        if (object instanceof ImageRequest) {
                            ImageRequest request = (ImageRequest) object;
                            if (RemoteCamera1.sysoutDebugLevel <= 4) {
                                System.out.println("Received request for " + request);
                            }
                            if (request.isShutdownRequest()) {
                                System.out.println("Received request for shutting down. Hence, shutting down camera " + RemoteCamera1.cameraID);
                                shutdownServer();
                                System.exit(250);
                            }
                            if (request.isImageRequestForNextPicture()) {
                                BufferedImage nextImage = RemoteCamera1.getNextImage();
                                ImageResponse response = (ImageResponse) ImageResponse.getImageResponse(nextImage);
                                connection.sendTCP(response);
//                                NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS
//                                KEPT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
//                                WHAT DOESN'T WORK
//                                try {
//                                    server.update(250);
//                                } catch (IOException ioe) {
//
//                                }
                                return;
                            }
                            // if unknown request
                            connection.sendTCP(ImageResponse.RESPONSE_TO_UNKNOWN_REQUEST);
//                            NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS
//                            KEPT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
//                            WHAT DOESN'T WORK
//                            try {
//                                server.update(250);
//                            } catch (IOException ioe) {
//
//                            }
                        }
                    }
                }
            });
            try {
                server.bind(tcpPort);
                Server broadcastServer = new Server();
                register(broadcastServer);
                broadcastServer.start();
                broadcastServer.bind(0,udpPorts[RemoteCamera1.cameraID - 1]);
            } catch (Exception e) {
                System.out.println("Unable to start server on tcp port " + tcpPort + " and udp port "
                        + udpPorts[RemoteCamera1.cameraID - 1]);
                System.exit(1);
            }
            System.out.println("About to add listener to the camera " + RemoteCamera1.cameraID);

        } else {
            // start the clients
            clients = new Client[udpPorts.length];
            connectedStatus = new boolean[udpPorts.length];
            for (int i = 0; i < clients.length; ++i) {
                clients[i] = new Client(16777216, 16777216);
                String serverIP = null;
                try {
                    InetAddress serverAddress = clients[i].discoverHost(udpPorts[i], timeOutMillis);
                    System.out.println("Discovered host at " + serverAddress);
                    serverIP = serverAddress.getHostAddress();
                    clients[i].start();
                    register(clients[i]);
                    // clients[i].connect(timeOutMillis, serverIP, tcpPort, udpPorts[i]);
                    clients[i].connect(timeOutMillis, serverIP, tcpPort);
                    connectedStatus[i] = true;
                    System.out.println("Connected to camera number " + (i + 1) + " at IP " + serverIP + " on tcp port " + tcpPort
                            + " and udp port " + udpPorts[i]);
                } catch (Exception e) {
                    System.out.println("Unable to connect to camera number " + (i + 1) + " on tcp port " + tcpPort
                            + " and udp port " + udpPorts[i]);
                    System.out.println("Check if camera number " + (i + 1) + " has been started and is running");
                    System.out.println("Continuing without connecting to camera number " + (i+1));
                    connectedStatus[i] = false;
                    // System.exit(1);
                }
            }
            for (int i = 0; i < clients.length; ++i) {
                switch (i) {
                    case 0: RemoteCamera1.createLeftCameraProcessor(udpPorts[i],clients[i],connectedStatus[i]); break;
                    case 1: RemoteCamera1.createRightCameraProcessor(udpPorts[i],clients[i],connectedStatus[i]); break;
                    case 2: RemoteCamera1.createTopCameraProcessor(udpPorts[i],clients[i],connectedStatus[i]); break;
                    case 3: RemoteCamera1.createBottomCameraProcessor(udpPorts[i],clients[i],connectedStatus[i]); break;
                    default: break;
                }
            }
        }

    }

    public static final void shutdownServer() {
        try {
            server.update(1000);
        } catch (IOException ioe) {

        } catch (IllegalStateException ioe) {

        }
        try {
            server.close();
        } catch (IllegalStateException ioe) {

        }

        try {
            server.dispose();
        } catch (IOException ioe) {

        } catch (IllegalStateException ioe) {

        }
    }

    /*
     * private static void checkForServerOrClient() { String isServer =
     * System.getProperty("server", "true"); if ("true".equals(isServer)) { server =
     * true; } else { server = false; } }
     */

    private static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(ImageRequest.class);
        // kryo.register(int[].class);
        kryo.register(byte[].class);
        kryo.register(ImageResponse.class);
    }

}
