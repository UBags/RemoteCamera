package com.costheta.tests;

import com.costheta.camera.imagemessages.ImageRequest;
import com.costheta.camera.imagemessages.ImageResponse;
import com.costheta.camera.processor.ImageProcessor;
import com.costheta.machine.MachiningNode;
import com.costheta.machine.ProcessingResult;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public abstract class CameraProcessor implements ImageProcessor {

    public static final boolean SUCCESSFUL_REQUEST = true;
    public static final boolean UNSUCCESSFUL_REQUEST = false;

    private static final String nameString = "Camera Name : ";
    private static final String idString = "Camera Id : ";
    private static final String semicolon = "; ";

    private MachiningNode parent;
    private final String cameraName;
    private final int cameraId;
    private final int udpPort;
    private final Client client;
    private boolean connected;
    private ImageView imageView;
    private Path imageDirPath;
    private int counter = 1;

    private ProcessingResult result;

    protected CameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        this.cameraName = cameraName;
        this.cameraId = cameraId;
        this.udpPort = udpPort;
        this.client = client;
        this.connected = connected;
        this.imageView = imageView;
        this.imageDirPath = Paths.get(RemoteCamera1.tempImageDirPath.toAbsolutePath().toString(),Integer.toString(cameraId));
        try {
            Files.createDirectories(imageDirPath);
        } catch (Exception e) {

        }
        client.addListener(new Listener() {
            public void connected (Connection connection) {
                result = null;
                client.sendTCP(Integer.valueOf(cameraId));
                connection.setKeepAliveTCP(3600000);
                connection.setTimeout(4000000);
//                try {
//                    client.update(250);
//                } catch (IOException ioe) {
//
//                }
            }

            public void received (Connection connection, Object object) {
                if (!client.isConnected()) {
                    System.out.println("Client not connected. Hence, reconnecting");
                    try {
                        client.reconnect();
                        return;
                    } catch (Exception e) {

                    }
                }
                System.out.println("In Client.received()");
                if (object instanceof ImageResponse) {
                    ImageResponse response = (ImageResponse) object;
                    System.out.println("Received " + response);
                    if (ImageResponse.isEmptyResponse(response)) {
                        // do nothing if an empty response packet has been received
                        return;
                    }
                    BufferedImage image = ImageResponse.getBufferedImage(response);
                    result = CameraProcessor.this.process(image);
                    RemoteCamera1.enableActionButton();
                }
                connection.setKeepAliveTCP(3600000);
                connection.setTimeout(4000000);
            }

            public void disconnected (Connection connection) {
                EventQueue.invokeLater(new Runnable() {
                    public void run () {
                        // Closing the frame calls the close listener which will stop the client's update thread.
                    }
                });
            }
        });
    }

    public boolean requestImage() {
        result = null;
        if (!connected) {
            System.out.println("The camera id " + cameraId + " on " + cameraName + " has not been started. It is recommended that you start the camera on that machine and re-start this program.");
            return UNSUCCESSFUL_REQUEST;
        }
        if (!client.isConnected()) {
            // if the client had earlier connected but is now somehow not connected, then
            // reconnect
            System.out.println("Connection to camera has dropped. Hence, reconnecting to camera " + cameraId);
            try {
                client.reconnect(250);
            } catch (IOException ioe) {
                System.out.println("Could not connect to camera on machine " + cameraId);
                return UNSUCCESSFUL_REQUEST;
            }
        }
        int lengthOfBytesSent = client.sendTCP(ImageRequest.SEND_PICTURE_REQUEST);
//        try {
//            client.update(250);
//        } catch (IOException ioe) {
//
//        }
        if (lengthOfBytesSent == 0) {
            return UNSUCCESSFUL_REQUEST;
        }
        return SUCCESSFUL_REQUEST;
    }

    public String getCameraName() {
        return cameraName;
    }

    public int getCameraId() {
        return cameraId;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(nameString);
        builder.append(cameraName);
        builder.append(semicolon);
        builder.append(idString);
        builder.append(cameraId);
        return builder.toString();
    }

    public final void shutdown() {

        result = null;
        if (!connected) {
            return;
        }
        try {
            client.update(250);
        } catch (IOException ioe) {

        } catch (IllegalStateException ise) {

        }
        requestServerShutdown();

        try {
            client.close(); // will throw ClosedSelectorException as server has been shutdown
        } catch (IllegalStateException ise) {

        }

        try {
            client.dispose();
        } catch (IOException ioe) {

        } catch (IllegalStateException ise) {

        }
        connected = false;
    }

    public final void close() {

        result = null;
        if (!connected) {
            return;
        }
        try {
            client.update(250);
        } catch (IOException ioe) {
            System.out.println(cameraId + " : " + "IOException in client.update()");
        } catch (IllegalStateException ise) {
            System.out.println(cameraId + " : " + "IllegalStateException in client.update()");
        }

        try {
            client.close();
        } catch (IllegalStateException ise) {
            System.out.println(cameraId + " : " + "IllegalStateException in client.close()");
        }

        try {
            client.dispose();
        } catch (IOException ioe) {
            System.out.println(cameraId + " : " + "IOException in client.dispose()");
        } catch (IllegalStateException ise) {
            System.out.println(cameraId + " : " + "IllegalStateException in client.dispose()");
        }
        connected = false;
    }


    private boolean requestServerShutdown() {
        if (!connected) {
            System.out.println("The camera id " + cameraId + " on " + cameraName + " has not been started. It is recommended that you start the camera on that machine and re-start this program.");
            return UNSUCCESSFUL_REQUEST;
        }
        if (!client.isConnected()) {
            // if the client had earlier connected but is now somehow not connected, then
            // reconnect
            try {
                client.reconnect(250);
            } catch (IOException ioe) {
                System.out.println("Could not connect to camera on machine " + cameraId);
                return UNSUCCESSFUL_REQUEST;
            }
        }
        int lengthOfBytesSent = client.sendTCP(ImageRequest.SHUTDOWN_REQUEST);
        if (lengthOfBytesSent == 0) {
            return UNSUCCESSFUL_REQUEST;
        }
        return SUCCESSFUL_REQUEST;
    }

    public void setImage(BufferedImage image) {
        Image nextImage = SwingFXUtils.toFXImage(image,null);
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(160);
        imageView.setFitWidth(240);
        imageView.setImage(nextImage);
    }

    public static final CameraProcessor createLeftCameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        return new LeftCameraProcessor(cameraName, cameraId, imageView, udpPort, client, connected);
    }

    public static final CameraProcessor createRightCameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        return new RightCameraProcessor(cameraName, cameraId, imageView, udpPort, client, connected);
    }

    public static final CameraProcessor createTopCameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        return new TopCameraProcessor(cameraName, cameraId, imageView, udpPort, client, connected);
    }

    public static final CameraProcessor createBottomCameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        return new BottomCameraProcessor(cameraName, cameraId, imageView, udpPort, client, connected);
    }

    public ProcessingResult getResult() {
        return result;
    }

    protected void saveImage(BufferedImage image) {
        try {
            ImageIO.write(image, "jpg", new File(imageDirPath.toAbsolutePath().toString() + File.pathSeparator + counter++ + ".jpg"));
        } catch (IOException ioe) {

        }
    }

    public String getName() {
        return getCameraName();
    }

    public MachiningNode getParent() {
        return parent;
    }

    public void setParent(MachiningNode machiningNode) {
        // sets the node only if the current node is null
        if (parent == null) {
            this.parent = machiningNode;
        }
    }

    public ArrayList<MachiningNode> getChildren() {
        // A processor does not have any children
        // However, we do not want to return null; instead, we return an empty ArrayList
        return new ArrayList<>();
    }


}
