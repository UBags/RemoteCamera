package com.costheta.tesseract;

import com.costheta.machine.BaseCamera;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CosThetaTesseractHandle implements PoolHandle {

    private static final Logger logger = LogManager.getLogger(CosThetaTesseractHandle.class);

    // public static final String language = "eng+hin";
    private CosThetaTesseract handle;
    private int processInstance;
    private int debugLevel;
    private boolean useAutoOSD;

    public CosThetaTesseractHandle(boolean useAutoOSD, int processInstance, int debugLevel) {
        this.useAutoOSD = useAutoOSD;
        this.processInstance = processInstance;
        this.debugLevel = debugLevel;
        this.handle = new CosThetaTesseract(useAutoOSD, debugLevel);
        if (this.debugLevel <= 4) {
            System.out.println("Created a CosThetaTesseractHandle");
        }
    }

    public CosThetaTesseractHandle(int processInstance, int debugLevel) {
        this(false, processInstance, debugLevel);
    }

    @Override
    public Object handle(Object image) throws Exception {
        return null;
    }

    @Override
    public boolean isValid() {
        return this.handle.isValid();
    }

    @Override
    public boolean release() {
        // return this.handle.release();
        // NOTHING NEEDS TO BE DONE HERE AS THE CosThetaTesseract OBJECT TAKES CARE OF
        // RELEASING RESOURCES OF ITS OWN VOLITION AFTER FINISHING OCR, THEREBY READYING
        // IT FOR REUSE
        return true;
    }

    @Override
    public boolean destroy() {
        return this.handle.destroy();
    }

    public CosThetaTesseract getHandle() {
        return this.handle;
    }

    public int getProcessInstance() {
        return this.processInstance;
    }

}
