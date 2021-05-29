package com.costheta.tesseract;

import com.costheta.machine.BaseCamera;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CosThetaTesseractHandlePool extends GenericObjectPool<PoolHandle> {

    private static final Logger logger = LogManager.getLogger(CosThetaTesseractHandlePool.class);

    private CosThetaTesseractHandlePool costhetaTesseractPool = null;
    public static final GenericObjectPoolConfig<PoolHandle> singletonConfig = new GenericObjectPoolConfig<PoolHandle>();
    static {
        singletonConfig.setMaxIdle(1);
        singletonConfig.setMaxTotal(1);
        singletonConfig.setLifo(true);
        singletonConfig.setMinIdle(1);
        // singletonConfig.setTestOnBorrow(true);
        singletonConfig.setTestOnReturn(true);
    }

    public static final GenericObjectPoolConfig<PoolHandle> defaultConfig = new GenericObjectPoolConfig<PoolHandle>();
    static {
        defaultConfig.setMaxIdle(10);
        defaultConfig.setMaxTotal(25);
        defaultConfig.setLifo(false);
        defaultConfig.setMinIdle(5);
        // defaultConfig.setTestOnBorrow(true);
        defaultConfig.setTestOnReturn(true);
    }

    public static final GenericObjectPoolConfig<PoolHandle> oneMachineConfig = new GenericObjectPoolConfig<PoolHandle>();
    static {
        oneMachineConfig.setMaxIdle(30);
        oneMachineConfig.setMaxTotal(50);
        oneMachineConfig.setLifo(false);
        oneMachineConfig.setMinIdle(25);
        // oneMachineConfig.setMaxIdle(1);
        // oneMachineConfig.setMaxTotal(1);
        // oneMachineConfig.setLifo(false);
        // oneMachineConfig.setMinIdle(0);

        // defaultConfig.setTestOnBorrow(true);
        oneMachineConfig.setTestOnReturn(true);
    }

    public static final GenericObjectPoolConfig<PoolHandle> smallPoolConfig = new GenericObjectPoolConfig<PoolHandle>();
    static {
        smallPoolConfig.setMaxIdle(10);
        smallPoolConfig.setMaxTotal(15);
        smallPoolConfig.setLifo(false);
        smallPoolConfig.setMinIdle(9);
        // oneMachineConfig.setMaxIdle(1);
        // oneMachineConfig.setMaxTotal(1);
        // oneMachineConfig.setLifo(false);
        // oneMachineConfig.setMinIdle(0);

        // defaultConfig.setTestOnBorrow(true);
        smallPoolConfig.setTestOnReturn(true);
    }

    public CosThetaTesseractHandlePool getPool() {
        return this.costhetaTesseractPool;
    }

    public void createPool(PooledObjectFactory<PoolHandle> factory, int processInstance,
                           int debugLevel) {
        this.costhetaTesseractPool = new CosThetaTesseractHandlePool(factory, processInstance, debugLevel);
    }

    public void createPool(PooledObjectFactory<PoolHandle> factory, GenericObjectPoolConfig<PoolHandle> config,
                           int processInstance, int debugLevel) {
        this.costhetaTesseractPool = new CosThetaTesseractHandlePool(factory, config, processInstance,
                debugLevel);
    }

    private int processInstance;
    private int debugLevel;

    /**
     * Constructor.
     *
     * It uses the default configuration for pool provided by apache-commons-pool2.
     *
     * @param factory
     */
    public CosThetaTesseractHandlePool(PooledObjectFactory<PoolHandle> factory, int processInstance,
                                       int debugLevel) {
        this(factory, defaultConfig, processInstance, debugLevel);
    }

    /**
     * Constructor.
     *
     * This can be used to have full control over the pool using configuration
     * object.
     *
     * @param factory
     * @param config
     */
    public CosThetaTesseractHandlePool(PooledObjectFactory<PoolHandle> factory,
                                       GenericObjectPoolConfig<PoolHandle> config, int processInstance, int debugLevel) {
        super(factory, config);
        this.processInstance = processInstance;
        this.debugLevel = debugLevel;
        try {
            this.preparePool();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.debugLevel <= 4) {
            System.out.println(
                    "Created a CosThetaTesseractHandlePool of size " + ((this.getNumIdle() >= 0 ? this.getNumIdle() : 0)
                            + (this.getNumActive() >= 0 ? this.getNumActive() : 0)));
        }
    }

    /**
     * @return the processInstance
     */
    public int getProcessInstance() {
        return this.processInstance;
    }

    public static final void initialise() {

    }

}
