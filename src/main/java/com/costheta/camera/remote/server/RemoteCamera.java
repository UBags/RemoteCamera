package com.costheta.camera.remote.server;

import com.costheta.camera.imagemessages.ImageBytes;
import com.costheta.camera.imagemessages.ImageRequest;
import com.costheta.camera.remote.RemoteCameraUtils;
import com.costheta.camera.remote.image.DefaultImages;
import com.costheta.image.utils.ImageUtils;
import com.costheta.machine.BaseCamera;
import com.costheta.utils.GeneralUtils;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

public class RemoteCamera extends BaseCamera {

    private static Logger logger = LogManager.getLogger(RemoteCamera.class);
    private static final String SAVE_IMAGES_KEY = "save.original.server";

    private static final String CAPRICA_PICAM_KEY = "use.capricapicam";
    private static final String INVALID_CAMERA_INPUT = "0";
    private static final String serverStart = "Starting camera id ";
    private static final String serverStartFailed = "Could not start camera id ";
    private static final String tcpStart = " on TCP port ";
    private static final String udpStart = " and UDP port ";
    private static final String numberOfCamerasErrorStatement = "The cameraId argument in the command line must be <= the number of cameras specified in cameras.total property in the properties file";

//    private int width = 4056;
//    private int height = 3040;

    private int width = 2028;
    private int height = 1520;

    private boolean saveImage = false;

    private int pictureType = BaseCamera.JPEG;

    private int numberOfCameras = 0;
    public int cameraID = 0;

    private int imageCounter = 1;
    private long timeTakenForPicture = 0;
    private String currentImageFileName = null;

    private int tcpPort = 54555;
    private int udpPort = 54777;
    private int timeOutMillis = 5000;

    private Server server = null;
    public Server getServer() {
        return server;
    }

    private int clientConnected = 0;
    public int getClientConnected() {
        return clientConnected;
    }

    public static void main(String[] args) throws Exception {
        // GeneralUtils.rerouteLog(new String[]{"server"});
        GeneralUtils.updateLogger("RemoteCameraLog", "logs/server.log", "com.costheta");
        logger.debug("Entering main()");
        RemoteCamera remoteCamera = new RemoteCamera();
        remoteCamera.initialise(args);
        remoteCamera.createImageSubdirectory();
        String cameraIDInput = System.getProperty(CAMERA_ID_KEY,"0");
        if (INVALID_CAMERA_INPUT.equals(cameraIDInput)) {
            logger.fatal("Cannot start as valid camera id is NOT provided.");
            logger.fatal(generalCommandLine);
            logger.fatal(separatorLine);
            logger.fatal(commandLineFormat);
            logger.fatal(separatorLine);
            logger.fatal(exampleCommandStatement);
            logger.fatal(separatorLine);
            logger.fatal(serverIdStatement);
            logger.fatal(separatorLine);
            logger.fatal(exitStatement);

            System.out.println(generalCommandLine);
            System.out.println(separatorLine);
            System.out.println(commandLineFormat);
            System.out.println(separatorLine);
            System.out.println(exampleCommandStatement);
            System.out.println(separatorLine);
            System.out.println(serverIdStatement);
            System.out.println(separatorLine);
            System.out.println(exitStatement);

            System.exit(1);
        }
        // No fear of NumberFormatException as BaseApplication.initialise() has already handled this issue
        remoteCamera.cameraID = Integer.parseInt(cameraIDInput);
        System.out.println("Attempting to start process as the provider of images from camera no " + remoteCamera.cameraID);
        remoteCamera.startCamera();
        String saveIms = System.getProperty(SAVE_IMAGES_KEY, "false");
        remoteCamera.saveImage = Boolean.parseBoolean(saveIms);
        System.out.println("Started process as the provider of images from camera no " + remoteCamera.cameraID);
        System.out.println("Image output directory = " + remoteCamera.imageDirectoryPath.toAbsolutePath().toString());
    }

    private int clickNextPicture() {
        logger.debug("Entering clickNextPicture()");
        Instant startTime = Instant.now();
        timeTakenForPicture = 0;
        int execValue = -1;
        if (isWindows(isWindows) == 1) {
            logger.debug("Clicking next picture in Windows.");
            return clickNextPictureInWindows();
        }
        Path newImagePath = Paths.get(imageDirectory, cameraID + "-" + imageCounter++ + ".jpg").toAbsolutePath();
        currentImageFileName = newImagePath.toString();
        Process cameraProcess = null;
        String commandString = System.getProperty("cameracommand", "sudo raspistill --width 2028 -height 1520 --nopreview -t 20 --quality 100 -e jpg -th none");
        logger.debug("Fetched canned camera command as " + commandString);
        commandString = new StringBuffer(commandString).append(" -o ").append(currentImageFileName).toString();
        logger.debug("Final camera command is " + commandString);
        String[] command = commandString.split(" ");

        try {
            cameraProcess = new ProcessBuilder(command).start();
            execValue = cameraProcess.waitFor();
            cameraProcess.destroy();
            timeTakenForPicture += Duration.between(startTime, Instant.now()).toMillis();
            logger.debug("Time taken to click picture = " + timeTakenForPicture);
            if (execValue == 0) {
                logger.info("Image taken successfully");
            } else {
                logger.warn("Image not taken. Investigate.");
            }
        } catch (Exception e) {
            logger.warn("Image not taken. Investigate.");
        }
        logger.debug("Exiting clickNextPicture with return value of " + execValue);
        return execValue;
    }

    private int clickNextPictureInWindows() {
        logger.debug("Entering clickNextPictureInWindows()");
        logger.debug("Not clicked any picture in Windows.");
        return -1;
    }

    public BufferedImage getNextImage() {
        logger.debug("Entering getNextImage()");
        if (System.getProperty(CAPRICA_PICAM_KEY, "false").equals("true")) {
            logger.debug("Trying to take picture using Capricam library");
            return RaspberryCamera.takePicture(width, height, pictureType);
        }
        System.out.println("Trying to take picture using Raspistill command");
        int execValue = clickNextPicture();
        if (execValue == -1) {
            return DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        BufferedImage imageClicked = null;
        try {
            imageClicked = ImageIO.read(new File(currentImageFileName));
        } catch (Exception e) {
            logger.debug("Exiting getNextImage() with default image as file could not be read from directory");
            return DefaultImages.EMPTY_BUFFERED_IMAGE;
        }
        return imageClicked;
    }

    public byte[] getNextImageBytes() {
        logger.debug("Entering getNextImageBytes()");
        if (System.getProperty(CAPRICA_PICAM_KEY, "false").equals("true")) {
            logger.debug("Trying to take picture using Capricam library");
            return RaspberryCamera.getPictureBytes(width, height, pictureType);
        }
        System.out.println("Trying to take picture using Raspistill command");
        int execValue = clickNextPicture();
        if (execValue == -1) {
            return DefaultImages.EMPTY_BYTES;
        }
        Path path = Paths.get(currentImageFileName);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException ioe) {
            return DefaultImages.EMPTY_BYTES;
        }
        if ((data == null) || (data.length == 0)) {
            return DefaultImages.EMPTY_BYTES;
        }
        return data;
    }

    public final String getCameraConnectedStatus() {
        logger.debug("Entering getCameraConnectedStatus()");
        return "";
    }

    private final void startCamera() {
        logger.trace("Entering startCamera()");
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
        logger.debug("Time out value set to " + timeOutMillis);

        if (!RemoteCameraUtils.udpPortsDefinedInProperties.contains(udpPort)) {
            System.out.println(udpPortMismatchStatement);
            logger.fatal(udpPortMismatchStatement);
            System.out.println(exitStatement);
            logger.fatal(exitStatement);
            System.exit(1);
        }

        // start a server on this machine
        logger.info(new StringBuilder(serverStart).append(cameraID).append(tcpStart)
                .append(tcpPort).append(udpStart).append(udpPort).toString());
        server = new Server(16777216, 16777216);
        RemoteCameraUtils.register(server);
        server.start();
        server.addListener(new Listener() {
            public void connected(Connection connection) {
                logger.info("Entering connected() on server with " + connection);
                connection.setKeepAliveTCP(3600000);
                connection.setTimeout(4000000);
//                        NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS.
//                        STILL KEPT IT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
//                        WHAT DOESN'T WORK
//                    try {
//                        server.update(100);
//                    } catch (IOException ioe) {
//
//                    }
            }

            public void received(Connection connection, Object object) {
                logger.info("Entering received() in server with " + connection);
                logger.debug("Object received is " + object);
                // System.out.println("Received a request");
                if (object instanceof Integer) {
                    logger.debug("Processing an Integer object in server");
                    Integer request = (Integer) object;
                    clientConnected = request.intValue();
                    logger.debug("Established connection with CameraProcessor id " + clientConnected);
                    // connection.sendTCP(ImageResponse.EMPTY_IMAGE_RESPONSE);
                    //=============
                    // Is this needed ? What is there is no response to unknown requests ?
                    // connection.sendTCP(ImageBytes.RESPONSE_TO_UNKNOWN_REQUEST);
                    //==============
                    //  NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS.
                    //  STILL KEPT IT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
                    //  WHAT DOESN'T WORK
                    //  try {
                    //      server.update(250);
                    //  } catch (IOException ioe) {
                    //  }
                    return;
                }
                if (object instanceof ImageRequest) {
                    logger.debug("Processing a RemoteImageRequest object in server");
                    ImageRequest request = (ImageRequest) object;
                    logger.debug("Received request for " + request);
                    if (request.isShutdownRequest()) {
                        logger.debug("Received request for shutting down. Hence, shutting down camera " + cameraID);
                        try {
                            shutdownServer();
                        } catch (Exception e) {

                        }
                        System.exit(0);
                    }
                    if (request.isImageRequestForNextPicture()) {
                        // BufferedImage nextImage = getNextImage();
                        long startTime = System.currentTimeMillis();
                        byte[] picBytes = getNextImageBytes();
                        new Thread() {
                            public void run() {
                                if (saveImage) {
                                    BufferedImage image = ImageUtils.getBufferedImage(picBytes);
                                    setCurrentImage(image);
                                    saveImage();
                                }
                            }
                        }.start();

                        logger.info("received() - got bytes of length " + picBytes.length + " after taking the picture");
                        // ImageResponse response = (ImageResponse) ImageResponse.getImageResponse(nextImage);
                        ImageBytes response = new ImageBytes(picBytes);
                        int bytesSent = connection.sendTCP(response);
                        logger.info("Sent ImageBytes response of length " + bytesSent + " in " + (System.currentTimeMillis() - startTime) + " ms from receiving the request.");
                        //  NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS.
                        //  STILL KEPT IT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
                        //  WHAT DOESN'T WORK
                        //  try {
                        //  server.update(250);
                        //  } catch (IOException ioe) {
                        //  }
                        return;
                    }
                }
                // if unknown request, do nothing
                // connection.sendTCP(ImageResponse.RESPONSE_TO_UNKNOWN_REQUEST);
                // connection.sendTCP(ImageBytes.RESPONSE_TO_UNKNOWN_REQUEST);
                //  NOT NEEDED, AS THE SERVER HANGS BECAUSE OF THIS
                //  KEPT, AS THIS COMMENT IS IMPORTANT TO UNDERSTAND
                //  WHAT DOESN'T WORK
                //  try {
                //      server.update(250);
                //  } catch (IOException ioe) {
                //
                //  }
                logger.debug("Request wasn't an instance of ImageRequest object");
            }
        });
        try {
            server.bind(tcpPort);
        } catch (IOException ioe) {
            logger.fatal(new StringBuilder(serverStartFailed).append(cameraID).append(tcpStart)
                    .append(tcpPort).append(udpStart).append(udpPort).toString());
            System.exit(1);
        }
        // start another server on udp port
        Server broadcastServer = new Server();
        RemoteCameraUtils.register(broadcastServer);
        broadcastServer.start();
        try {
            broadcastServer.bind(0, udpPort);
        } catch (Exception e) {
            logger.fatal(new StringBuilder(serverStartFailed).append(cameraID).append(tcpStart)
                    .append(tcpPort).append(udpStart).append(udpPort).toString());
            System.exit(1);
        }
        logger.info("Added listener to the camera " + cameraID);
    }

    protected final void shutdownServer() {
        logger.debug("Entering shutdownServer()");
        if (System.getProperty(CAPRICA_PICAM_KEY, "false").equals("true")) {
            System.out.println("Trying to close Capricam library");
            RaspberryCamera.close();
        }
        try {
            server.update(1000);
        } catch (IOException ioe) {
            logger.warn(ioe.getStackTrace());
        } catch (IllegalStateException ise) {
            logger.warn(ise.getStackTrace());
        } catch (Exception e) {
            logger.warn(e.getStackTrace());
        }
        try {
            server.close();
            server.dispose();
        } catch (IOException ioe) {
            logger.warn(ioe.getStackTrace());
        } catch (IllegalStateException ise) {
            logger.warn(ise.getStackTrace());
        } catch (Exception e) {
            logger.warn(e.getStackTrace());
        }
        logger.debug("Exiting shutdownServer() normally");
        logger.debug("Shutting down application at camera " + cameraID);
        System.exit(0);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getPictureType() {
        return pictureType;
    }

    public void setPictureType(int pictureType) {
        if (pictureType == BaseCamera.PNG) {
            this.pictureType = pictureType;
        } else {
            this.pictureType = BaseCamera.JPEG;
        }
    }

    public void saveImage() {
        if (currentImage!= null) {
            saveImage(currentImage, imageDirectory + "/" + imageCounter++ + "-original.jpg");
        }
    }

}
