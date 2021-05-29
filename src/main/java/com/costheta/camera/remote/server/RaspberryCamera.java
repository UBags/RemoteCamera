package com.costheta.camera.remote.server;

import com.costheta.machine.BaseCamera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.co.caprica.picam.*;
import uk.co.caprica.picam.enums.AutomaticWhiteBalanceMode;
import uk.co.caprica.picam.enums.Encoding;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import static uk.co.caprica.picam.CameraConfiguration.cameraConfiguration;
import static uk.co.caprica.picam.PicamNativeLibrary.installTempLibrary;
import static uk.co.caprica.picam.app.Environment.dumpEnvironment;

public class RaspberryCamera {

    private static Logger logger = LogManager.getLogger(RaspberryCamera.class);

    public static final BufferedImage takePicture(int width, int height, int PIC_TYPE) {
        logger.trace("Entered takePicture()");
        synchronized (lock) {
            Camera camera = null;
            try {
                camera = createCamera(width, height, PIC_TYPE);
                logger.trace("After createCamera()");
            } catch (NativeLibraryException nle) {
                logger.error("In NativeLibraryException. Returning EMPTY_IMAGE");
                return EMPTY_IMAGE;
            }
            if (camera == null) {
                logger.error("Camera is null. Returning EMPTY IMAGE");
                return EMPTY_IMAGE;
            }

            PictureCaptureHandler<?> pictureCaptureHandler = new ByteArrayPictureCaptureHandler();
            long time = System.currentTimeMillis();
            try {

                if (count == 1) {
                    aCamera.takePicture(pictureCaptureHandler, 1000);
                    logger.info("Took picture in " + (System.currentTimeMillis() - time) + "ms");
                } else {
                    aCamera.takePicture(pictureCaptureHandler, 50);
                    logger.info("Took picture in " + (System.currentTimeMillis() - time) + "ms");
                }
                if (count <= 1) {
                    ++count;
                }
            } catch (CaptureFailedException e) {
                logger.warn("Capture failed, reopening camera: " + e.getMessage());
                aCamera.close(); // free resources
                count = 1; // reset count
                boolean openedAgain = aCamera.open();
                if (!openedAgain) {
                    logger.error("Failed to reopen the camera. Serious error");
                    aCamera.close();
                    count = 1;
                    return EMPTY_IMAGE;
                } else {
                    return takePictureTake2();
                }
            }
            BufferedImage picCaptured = null;
            try {
                byte[] picBytes = ((ByteArrayPictureCaptureHandler) pictureCaptureHandler).result();
                logger.trace("Got picture bytes of length " + picBytes.length);
                picCaptured = ImageIO.read(new ByteArrayInputStream(picBytes));
                logger.trace("Made BufferedImage");
                logger.info("Got picture byte[] in " + (System.currentTimeMillis() - time) + "ms");
            } catch (IOException ioe) {
                logger.error("Error in converting pic bytes to a Buffered Image " + ioe.getStackTrace());
                return EMPTY_IMAGE;
            } catch (Exception e) {
                logger.error("Error in converting pic bytes to a Buffered Image " + e.getStackTrace());
                return EMPTY_IMAGE;
            }
            return picCaptured;
        }
    }

    public static void close() {
        if (aCamera != null) {
            aCamera.close();
            aCamera = null;
            count = 1;
        }
    }

    public static boolean isRaspberryPi() {
        if (isRaspberryPi != null) {
            logger.info("Raspberry check already done");
            return isRaspberryPi;
        }
        StringBuilder stringBuilder = getInfoByExecuteCommandLinux("cat /proc/device-tree/model");
        isRaspberryPi = stringBuilder.toString().toLowerCase().contains("raspberry");
        logger.info("Raspberry check is " + isRaspberryPi);
        return isRaspberryPi;
    }

    private static Camera aCamera;

    private static Object lock = new Object();
    private static String currentCameraConfigString = null;
    private static BufferedImage EMPTY_IMAGE = new BufferedImage(50,50,BufferedImage.TYPE_INT_ARGB);
    private static byte[] EMPTY_BYTES;
    private static int count = 1;

    static {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(EMPTY_IMAGE, "jpg", baos);
            EMPTY_BYTES = baos.toByteArray();
        } catch (Exception e) {

        }

    }

    private static final BufferedImage takePictureTake2() {
        PictureCaptureHandler<?> pictureCaptureHandler = new ByteArrayPictureCaptureHandler();
        try {
            if (count == 1) {
                aCamera.takePicture(pictureCaptureHandler, 1000);
            } else {
                aCamera.takePicture(pictureCaptureHandler, 50);
            }
            if (count <= 1) {
                ++count;
            }
        } catch (CaptureFailedException e) {
            logger.error("Capture failed again with reopened camera " + e.getMessage());
            close();
            return EMPTY_IMAGE;
        }
        BufferedImage picCaptured = null;
        try {
            byte[] picBytes = ((ByteArrayPictureCaptureHandler) pictureCaptureHandler).result();
            picCaptured = ImageIO.read(new ByteArrayInputStream(picBytes));
        } catch (IOException ioe) {
            logger.error("Error in converting pic bytes to a Buffered Image " + ioe.getStackTrace());
            return EMPTY_IMAGE;
        } catch (Exception e) {
            logger.error("Error in converting pic bytes to a Buffered Image " + e.getStackTrace());
            return EMPTY_IMAGE;
        }
        return picCaptured;
    }

    private static Camera createCamera(int width, int height, int PIC_TYPE) throws NativeLibraryException {

        if(!isRaspberryPi()) {
            logger.error("We are not in a Raspberry Pi. No camera created");
            return null;
        }

        String configString = getConfigurationString(width, height, PIC_TYPE);
        logger.trace("Created configuration");
        if (configString.equals(currentCameraConfigString)) {
            logger.trace("We already have a camera with that configuration. Camera object is " + aCamera);
            return aCamera;
        }

        currentCameraConfigString = null; // invalidate the current camera config string
        logger.trace("Need to create a new camera with configuration " + configString);
        if (aCamera != null) {
            close();
        }
        dumpEnvironment();
        logger.trace("Dumped environment");
        installTempLibrary();
        logger.debug("Installed picam library");

        CameraConfiguration cameraConfiguration = cameraConfiguration()
                .width(width)
                .height(height)
                .automaticWhiteBalance(AutomaticWhiteBalanceMode.AUTO)
                .quality(100)
                .captureTimeout(3000)
//              .brightness(50)
//              .contrast(-30)
//              .saturation(80)
//              .sharpness(100)
//              .stabiliseVideo()
//              .shutterSpeed(10)
//              .iso(4)
//              .exposureMode(ExposureMode.FIREWORKS)
//              .exposureMeteringMode(ExposureMeteringMode.BACKLIT)
//              .exposureCompensation(5)
//              .dynamicRangeCompressionStrength(DynamicRangeCompressionStrength.MAX)
//              .automaticWhiteBalance(AutomaticWhiteBalanceMode.FLUORESCENT)
//              .imageEffect(ImageEffect.SKETCH)
//              .flipHorizontally()
//              .flipVertically()
//              .rotation(rotation)
//              .crop(0.25f, 0.25f, 0.5f, 0.5f)
                ;

        logger.trace("Created camera configuration");
        if (PIC_TYPE == BaseCamera.JPEG) {
            cameraConfiguration = cameraConfiguration.encoding(Encoding.JPEG);
        } else {
            if (PIC_TYPE == BaseCamera.PNG) {
                cameraConfiguration = cameraConfiguration.encoding(Encoding.PNG);
            } else {
                cameraConfiguration = cameraConfiguration.encoding(Encoding.JPEG);
            }
        }

        try {
            logger.trace("About to create camera");
            aCamera = new Camera(cameraConfiguration);
            logger.debug("Created camera with configuration " + cameraConfiguration);
            currentCameraConfigString = configString;
        }
        catch (CameraException e) {
            logger.error("Could not create camera");
            e.printStackTrace();
            return null;
        }
        return aCamera;
    }

    private static String getConfigurationString(int width, int height, int PIC_TYPE) {
        return new StringBuilder().append(width).append("x").append(height).append("x").append(PIC_TYPE).toString();
    }

    private static Boolean isRaspberryPi = null;

    private static StringBuilder getInfoByExecuteCommandLinux(String command){
        Process pb = null;
        StringBuilder stringBuilderResult = new StringBuilder();
        try {
            pb = new ProcessBuilder("bash", "-c", command).start();
        } catch (IOException ioe) {
            return stringBuilderResult;
        }
        BufferedReader reader=new BufferedReader(new InputStreamReader(pb.getInputStream()));
        String line;

        try {
            while((line = reader.readLine()) != null) {
                stringBuilderResult.append(line).append(" ");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return stringBuilderResult;
    }

    public static final byte[] getPictureBytes(int width, int height, int PIC_TYPE) {
        logger.trace("Entered getPictureBytes()");
        synchronized (lock) {
            Camera camera = null;
            try {
                camera = createCamera(width, height, PIC_TYPE);
                logger.trace("After createCamera()");
            } catch (NativeLibraryException nle) {
                logger.error("In NativeLibraryException. Returning EMPTY_IMAGE");
                return EMPTY_BYTES;
            }
            if (camera == null) {
                logger.error("Camera is null. Returning EMPTY IMAGE");
                return EMPTY_BYTES;
            }

            PictureCaptureHandler<?> pictureCaptureHandler = new ByteArrayPictureCaptureHandler();
            long time = System.currentTimeMillis();
            try {

                if (count == 1) {
                    aCamera.takePicture(pictureCaptureHandler, 1000);
                    logger.debug("Took picture in " + (System.currentTimeMillis() - time) + "ms");
                } else {
                    aCamera.takePicture(pictureCaptureHandler, 50);
                    logger.debug("Took picture in " + (System.currentTimeMillis() - time) + "ms");
                }
                if (count <= 1) {
                    ++count;
                }
            } catch (CaptureFailedException e) {
                logger.warn("Capture failed, reopening camera: " + e.getMessage());
                aCamera.close(); // free resources
                count = 1; // reset count
                boolean openedAgain = aCamera.open();
                if (!openedAgain) {
                    logger.error("Failed to reopen the camera. Serious error");
                    aCamera.close();
                    count = 1;
                    return EMPTY_BYTES;
                } else {
                    return getPictureBytesTake2();
                }
            }
            byte[] picBytes = ((ByteArrayPictureCaptureHandler) pictureCaptureHandler).result();
            logger.trace("Got picture bytes of length " + picBytes.length);
            logger.info("Got picture byte[" + picBytes.length +"] in " + (System.currentTimeMillis() - time) + "ms");
            return picBytes;
        }
    }

    private static final byte[] getPictureBytesTake2() {
        logger.trace("Entered getPicturesBytesTake2");
        PictureCaptureHandler<?> pictureCaptureHandler = new ByteArrayPictureCaptureHandler();
        try {
            if (count == 1) {
                aCamera.takePicture(pictureCaptureHandler, 1000);
            } else {
                aCamera.takePicture(pictureCaptureHandler, 50);
            }
            if (count <= 1) {
                ++count;
            }
        } catch (CaptureFailedException e) {
            logger.error("Capture failed again with reopened camera " + e.getMessage());
            close();
            return EMPTY_BYTES;
        }
        byte[] picBytes = ((ByteArrayPictureCaptureHandler) pictureCaptureHandler).result();
        return picBytes;
    }

}

