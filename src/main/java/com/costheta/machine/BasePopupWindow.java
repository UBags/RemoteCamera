package com.costheta.machine;

import com.costheta.image.BasePaneConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;

public abstract class BasePopupWindow implements BasePaneConstants {

    private static final Logger logger = LogManager.getLogger(BasePopupWindow.class);
    protected Stage owner;

    public void process() {

        if (owner == null) {
            // loads JavaFX
            final CountDownLatch latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    new JFXPanel(); // initializes JavaFX environment
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException ie) {

            }
            // then executes processImage() in a separate GUI thread
            Platform.runLater(new Runnable() {
                public void run() {
                    processImage();
                }
            });
        } else {
            processImage();
        }
    }

    protected abstract Object processImage();
}
