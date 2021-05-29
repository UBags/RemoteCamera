package com.costheta.camera.remote.client;

import com.costheta.camera.imagemessages.ImageBytes;
import com.costheta.camera.imagemessages.ImageRequest;
import com.costheta.camera.imagemessages.ImageResponse;
import com.costheta.camera.remote.RemoteCameraUtils;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class NetworkClientCamera extends GenericClientCamera {

    private static Logger logger = LogManager.getLogger(NetworkClientCamera.class);

    public static final boolean SUCCESSFUL_REQUEST = true;
    public static final boolean UNSUCCESSFUL_REQUEST = false;

    private static final String INVALID_CAMERA_INPUT = "0";
    private static final String serverStart = "Starting camera id ";
    private static final String serverStartFailed = "Could not start camera id ";
    private static final String tcpStart = " on TCP port ";
    private static final String udpStart = " and UDP port ";
    private static final String numberOfCamerasErrorStatement = "The cameraId argument in the command line must be <= the number of cameras specified in cameras.total property in the properties file";

    private int numberOfCameras = 0;

    private int imageCounter = 1;
    private long timeTakenForPicture = 0;
    private String currentImageFileName = null;

    private String serverIP = null;
    private int tcpPort = 54555;
    private int udpPort = 54777;
    private int timeOutMillis = 5000;

    private Client client;
    private volatile boolean connectedAtLeastOnce = false;

    protected NetworkClientCamera(String cameraName, int cameraId, int udpPort, Client client) {
        super(cameraName, cameraId);
        logger.trace("After calling super() in constructor");
        try {
            tcpPort = Integer.parseInt(System.getProperty(RemoteCameraUtils.TCP_PORT_KEY, "54555"));
        } catch (NumberFormatException nfe) {
            tcpPort = 54555;
        }
        this.udpPort = udpPort;
        this.client = client;
        attachListeners(client);
        setActive(true);
        // reconnect client thread
        startKeepAliveThread(client);
    }

    public NetworkClientCamera(String cameraName, int cameraId) {
        super(cameraName, cameraId);
        logger.trace("After calling super() in constructor");
        logger.trace("Calling RemoteCameraUtils.checkArgsIntegrity from cameraId : " + cameraId);
        RemoteCameraUtils.checkArgsIntegrity(cameraID);
        udpPort = 0;
        try {
            udpPort = Integer.parseInt(
                    System.getProperty(new StringBuilder().append(cameraID).append(".").append(RemoteCameraUtils.UDP_PORT_KEY).toString(), "0"));
        } catch (NumberFormatException nfe) {
            // Will not happen as the method checkArgsIntegrity addresses the issue,
            // but still kept for completeness. Actually udpPort = 0 is meaningless
            udpPort = 0;
        }
        try {
            tcpPort = Integer.parseInt(System.getProperty(RemoteCameraUtils.TCP_PORT_KEY, "54555"));
        } catch (NumberFormatException nfe) {
            tcpPort = 54555;
        }
        try {
            timeOutMillis = Integer.parseInt(System.getProperty(RemoteCameraUtils.TIMEOUT_MILLIS_KEY, "5000"));
        } catch (NumberFormatException nfe) {
            timeOutMillis = 5000;
        }
        logger.trace("Time out value set to " + timeOutMillis);

        if (!RemoteCameraUtils.udpPortsDefinedInProperties.contains(udpPort)) {
            System.out.println(udpPortMismatchStatement);
            logger.fatal(udpPortMismatchStatement);
            System.out.println(exitStatement);
            logger.fatal(exitStatement);
            System.exit(1);
        }
        // After port information is got, now create the Client
        client = new Client(16777216, 16777216);
        serverIP = null;
        try {
            InetAddress serverAddress = client.discoverHost(udpPort, timeOutMillis);
            if (serverAddress != null) {
                logger.info("Discovered host at " + serverAddress + " for client " + cameraID);
            } else {
                logger.info("No host discovered for client " + cameraID);
            }
            serverIP = serverAddress.getHostAddress();
            client.start();
            RemoteCameraUtils.register(client);
            // clients[i].connect(timeOutMillis, serverIP, tcpPort, udpPorts[i]);
            client.connect(timeOutMillis, serverIP, tcpPort);
            connectedAtLeastOnce = true; // if this sentence is reached then client has connected
            setActive(true);
            logger.info("Connected to camera number " + (cameraID) + " at IP " + serverIP + " on tcp port " + tcpPort
                    + " and udp port " + udpPort);
        } catch (Exception e) {
            logger.error("Unable to connect to camera number " + cameraID + " on tcp port " + tcpPort
                    + " and udp port " + udpPort);
            System.out.println("Check if camera number " + cameraID + " has been started and is running");
            logger.info("Check if camera number " + cameraID + " has been started and is running");
            System.out.println("Continuing without connecting to camera number " + cameraID);
            logger.info("Continuing without connecting to camera number " + cameraID);
        }
        attachListeners(client);
        // reconnect client thread
        startKeepAliveThread(client);
    }

    public boolean requestImage() {
        logger.debug("Entering requestImage()");
        setCurrentImage(null);
        setNewPictureReceived(false);
//        if (!connected) {
//            System.out.println("The camera id " + cameraId + " on " + cameraName + " has not been started. It is recommended that you start the camera on that machine and re-start this program.");
//            return UNSUCCESSFUL_REQUEST;
//        }
        if (!client.isConnected()) {
            setActive(false);
            // if the client had earlier connected but is now somehow not connected, then
            // reconnect
            logger.info("requestImage() - Connection to camera has dropped. Hence, reconnecting to camera " + getCameraID());
            try {
                client.reconnect(250);
                connectedAtLeastOnce = true;
                setActive(true);
            } catch (IOException ioe) {
                logger.info("requestImage() - Could not connect to camera on machine " + getCameraID());
                logger.info("requestImage() - Exiting requestImage() as client not connected");
                return UNSUCCESSFUL_REQUEST;
            }
        }
        if (!client.isConnected()) {
            setActive(false);
            logger.info("requestImage() - Exiting requestImage() as client not connected");
            return UNSUCCESSFUL_REQUEST;
        }
        setActive(true);
        int lengthOfBytesSent = client.sendTCP(ImageRequest.SEND_PICTURE_REQUEST);
//        try {
//            client.update(250);
//        } catch (IOException ioe) {
//
//        }
        if (lengthOfBytesSent == 0) {
            logger.info("requestImage() - Exiting requestImage() with unsuccessful_request as bytes sent = 0");
            return UNSUCCESSFUL_REQUEST;
        }
        logger.trace("requestImage() - Exiting requestImage() normally");
        return SUCCESSFUL_REQUEST;
    }

    public final void shutdown() {
        logger.info("Entering shutdown()");
        setActive(false);
        setCurrentImage(null);
        setNewPictureReceived(false);
        boolean exceptionRaised = false;
        if (client.isConnected()) {
            logger.info("shutdown() - Client is connected. Just before calling update()");
            try {
                client.update(250);
            } catch (IOException ioe) {
                logger.warn("shutdown() - " + ioe.getStackTrace());
                exceptionRaised = true;
            } catch (IllegalStateException ise) {
                logger.warn("shutdown() - " + ise.getStackTrace());
                exceptionRaised = true;
            }
            logger.info("shutdown() - Just before requesting server shutdown");
            requestServerShutdown();

//            try {
//                client.close(); // could throw ClosedSelectorException as server has shutdown
//            } catch (IllegalStateException ise) {
//                logger.warn("shutdown() - " + ise.getStackTrace());
//                exceptionRaised = true;
//            } catch (Exception e) {
//                logger.warn("shutdown() - " + e.getStackTrace());
//                exceptionRaised = true;
//            }
        } else {
            if (connectedAtLeastOnce) {
                logger.info("In shutdown() - Client connection lost. Just before calling reconnect()");
                try {
                    client.reconnect(250);
                } catch (IOException ioe) {
                    logger.warn("shutdown() - " + ioe.getStackTrace());
                    exceptionRaised = true;
                } catch (IllegalStateException ise) {
                    logger.warn("shutdown() - " + ise.getStackTrace());
                    exceptionRaised = true;
                }
                logger.info("shutdown() - Just before requesting server shutdown");
                if (client.isConnected()) {
                    requestServerShutdown();
                }
            }
        }

        try {
            client.dispose(); // dispose calls close()
        } catch (IOException ioe) {
            logger.warn("shutdown() - " + ioe.getStackTrace());
            exceptionRaised = true;
        } catch (IllegalStateException ise) {
            logger.warn("shutdown() - " + ise.getStackTrace());
            exceptionRaised = true;
        } catch (Exception e) {
            logger.warn("shutdown() - " + e.getStackTrace());
            exceptionRaised = true;
        }
        if (exceptionRaised) {
            logger.info("shutdown() - Exiting shutdown() after exceptions");
        } else {
            logger.info("shutdown() - Exiting shutdown() normally");
        }
    }

    public final void close() {
        logger.warn("Entering close()");
        setActive(false);
        setCurrentImage(null);
        setNewPictureReceived(false);
        if (!client.isConnected()) {
            logger.info("close() - Exiting close() as client is not connected");
            return;
        }
        try {
            client.update(250);
        } catch (IOException ioe) {
            logger.warn("close() - " + getCameraID() + " : " + "IOException in client.update()");
        } catch (IllegalStateException ise) {
            logger.warn("close() - " + getCameraID() + " : " + "IllegalStateException in client.update()");
        } catch (Exception e) {
            logger.warn("close() - " + e.getStackTrace());
        }

//        try {
//            client.close();
//        } catch (IllegalStateException ise) {
//            logger.warn("close() - " + getCameraID() + " : " + "IllegalStateException in client.close()");
//        } catch (Exception e) {
//            logger.warn("close() - " + e.getStackTrace());
//        }

        try {
            client.dispose(); // dispose() calls close()
        } catch (IOException ioe) {
            logger.warn("close() - " + getCameraID() + " : " + "IOException in client.dispose()");
        } catch (IllegalStateException ise) {
            logger.warn("close() - " + getCameraID() + " : " + "IllegalStateException in client.dispose()");
        } catch (Exception e) {
            logger.warn("close() - " + e.getStackTrace());
        }
        logger.trace("Exiting close()");
    }


    private boolean requestServerShutdown() {
        if (!connectedAtLeastOnce) {
            return UNSUCCESSFUL_REQUEST;
        }
        connectedAtLeastOnce = false; // important to do that, else a reconnection will be established by the keepalive thread
        logger.trace("Entering requestServerShutdown()");
//        if (!client.isConnected()) {
//            System.out.println("The camera id " + cameraId + " on " + cameraName + " has not been started. It is recommended that you start the camera on that machine and re-start this program.");
//            return UNSUCCESSFUL_REQUEST;
//        }
        if (!client.isConnected()) {
            // if the client had earlier connected but is now somehow not connected, then
            // reconnect
            try {
                client.reconnect(250);
            } catch (IOException ioe) {
                logger.warn("requestServerShutdown() - Could not connect to camera on machine " + getCameraID());
                logger.debug("requestServerShutdown() - Exiting requestServerShutdown() as client is not connected");
                return UNSUCCESSFUL_REQUEST;
            }
        }
        int lengthOfBytesSent = client.sendTCP(ImageRequest.SHUTDOWN_REQUEST);
        if (lengthOfBytesSent == 0) {
            logger.debug("requestServerShutdown() - Exiting requestServerShutdown() with unsuccessful_request as bytes ent = 0");
            return UNSUCCESSFUL_REQUEST;
        }
        logger.debug("requestServerShutdown() - Exiting requestServerShutdown() with successful_request");
        return SUCCESSFUL_REQUEST;
    }

    @Override
    public boolean connectCamera() {
        logger.trace("Entering connectCamera()");
        setActive(false);
        // setCurrentImage(null);
        // setNewPictureReceived(false);
        if (client.isConnected()) {
            setActive(true);
            logger.debug("connectCamera() - Exiting connectCamera() as client is connected");
            return true;
        }
        try {
            client.reconnect(250);
        } catch (IOException ioe) {
            logger.warn("connectCamera() - Could not connect to camera on machine " + getCameraID());
            logger.debug("connectCamera() - Exiting connectCamera() as client could not connect");
            return UNSUCCESSFUL_REQUEST;
        } catch (IllegalStateException e) {
            logger.warn("connectCamera() - Could not connect to camera on machine " + getCameraID());
            logger.debug("connectCamera() - Exiting connectCamera() as client could not connect");
            return UNSUCCESSFUL_REQUEST;
        }
        return true;
    }

    @Override
    public void shutdownServer() {
        logger.debug("Entering shutdownServer()");
        shutdown();
        logger.debug("Exiting shutdownServer()");
    }

    private void attachListeners(Client client) {
        client.addListener(new Listener() {
            public void connected (Connection connection) {
                logger.debug("connected() - Entering connected() in client " + cameraID + " with connection " + connection);
                connectedAtLeastOnce = true;
                connection.setKeepAliveTCP(3600000);
                connection.setTimeout(4000000);
                setActive(true);
                // setCurrentImage(null);
                // setNewPictureReceived(false);
                int bytesSent = client.sendTCP(Integer.valueOf(cameraID));
                if (bytesSent == 0) {
                    try {
                        client.close();
                        setActive(false);
                    } catch (Exception e) {
                        logger.warn("connected() - Closed the connection as client " + cameraID + " is not connected");
                    }
                    logger.debug("connected() - Exiting connected() as bytesSent = 0");
                    return;
                }
                connection.setKeepAliveTCP(3600000);
                connection.setTimeout(4000000);
            }

            public void received (Connection connection, Object object) {
                Thread pictureUpdate = new Thread() {
                    public void run() {
                        logger.info("Entering received() in client with " + connection);
                        // setCurrentImage(null);
                        // setNewPictureReceived(false);
                        // setActive(true); // since we have received the message,
                        // we must be in active state

                        // =====================
                        // Is this required ? It's unlikely that the client can simultaneously
                        // receive a response and be disconnected
/*

                if (!client.isConnected()) {
                    setActive(false);
                    logger.info("received() - Client not connected. Request for image not sent. Reconnecting...");
                    try {
                        client.reconnect();
                        setActive(true);
                        logger.info("received() - Reconnected. Send request again");
                        return;
                    } catch (Exception e) {
                        logger.warn("received() - " + e.getStackTrace());
                        setActive(false);
                    }
                }
*/
                        // =====================
                        //System.out.println("In Client.received()");
                        // if (isActive) {
                        // logger.info("Entered isActive block");
                        if (object instanceof ImageResponse) {
                            ImageResponse response = (ImageResponse) object;
                            logger.trace("received() - Received an ImageResponse " + response);
                            if (ImageResponse.isEmptyResponse(response)) {
                                setCurrentImage(ImageResponse.EMPTY_IMAGE);
                                setNewPictureReceived(true);
                                // logger.debug("received() - Exiting received() as empty response received. No need to process it");
                                logger.info("received() - Got empty response.");
                                // do nothing if an empty response packet has been received
                                return;
                            }
                            BufferedImage image = ImageResponse.getBufferedImage(response);
                            setCurrentImage(image);
                            setNewPictureReceived(true);
                        } else {
                            if (object instanceof ImageBytes) {
                                ImageBytes response = (ImageBytes) object;
                                logger.trace("received() - Received ImageBytes " + response);
                                if (ImageBytes.isEmptyImageBytes(response)) {
                                    setCurrentImage(ImageBytes.EMPTY_IMAGE);
                                    setNewPictureReceived(true);
                                    // logger.debug("received() - Exiting received() as empty response received. No need to process it");
                                    logger.info("received() - Got empty response.");
                                    // do nothing if an empty response packet has been received
                                    return;
                                }
                                BufferedImage image = ImageBytes.getBufferedImage(response);
                                setCurrentImage(image);
                                setNewPictureReceived(true);
                            } else {
                                logger.info("Object received is neither of type ImageBytes nor ImageResponse");
                            }
                        }
                        connection.setKeepAliveTCP(3600000);
                        connection.setTimeout(4000000);
                        logger.info("Exiting received() in client with " + connection);
                    }
                };
                pictureUpdate.setPriority(Thread.MAX_PRIORITY);
                pictureUpdate.start();
            }

            public void disconnected (Connection connection) {
                logger.debug("Entering disconnected() in client with " + connection);
                setActive(false);
                setNewPictureReceived(false);
                setCurrentImage(null);
                EventQueue.invokeLater(new Runnable() {
                    public void run () {
                        // Closing the frame calls the close listener which will stop the client's update thread.
                    }
                });
            }
        });
    }

    public void startKeepAliveThread(Client client) {
        Thread keepAlive = new Thread() {
            public void run() {
                while (true) {
                    if (connectedAtLeastOnce) {
                        if (!client.isConnected()) {
                            System.out.println("Trying to reconnect");
                            setActive(false);
                            try {
                                client.reconnect(250);
                                connectedAtLeastOnce = true;
                                setActive(true);
                                logger.info("Reconnected client " + cameraID + " as connection had dropped");
                            } catch (IOException ioe) {

                            }
                        }
                        try {
                            TimeUnit time = TimeUnit.SECONDS;
                            time.sleep(3);
                        } catch (InterruptedException ie) {

                        }
                    } else {
                        try {
                            TimeUnit time = TimeUnit.SECONDS;
                            time.sleep(500);
                        } catch (InterruptedException ie) {

                        }
                    }
                }
            }
        };
        keepAlive.start();
    }
}
