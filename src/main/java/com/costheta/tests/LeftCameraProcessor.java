package com.costheta.tests;

import com.costheta.machine.ProcessingResult;
import com.esotericsoftware.kryonet.Client;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;

public class LeftCameraProcessor extends CameraProcessor {

    protected LeftCameraProcessor(String cameraName, int cameraId, ImageView imageView, int udpPort, Client client, boolean connected) {
        super(cameraName, cameraId, imageView, udpPort, client, connected);
    }

    @Override
    public ProcessingResult process(BufferedImage image) {
        setImage(image);
        saveImage(image);
        return ProcessingResult.EMPTY_PROCESSING_RESULT;
    }

}
