package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class JDBCConfig {
    /**
     * poolSize
     */
    @ConfigItem(name = "max-size", defaultValue = "5")
    public Integer poolSize;

    /**
     * minPoolSize
     */
    @ConfigItem(name = "min-size", defaultValue = "0")
    public Integer minPoolSize;

    /**
     * initPoolSize
     */
    @ConfigItem(name = "initial-size", defaultValue = "0")
    public Integer initPoolSize;

    /**
     * background-validation-interval
     */
    @ConfigItem(name = "background-validation-interval.seconds", defaultValue = "120")
    public double datasourceValidationInterval;

    // set IdleValidationTimeout to 500 ms to make Agroal datasource to validate connection before each getConnection() call
    // to make sure that connection is checked if it's closed
    /**
     * idle-removal-interval
     */
    @ConfigItem(name = "idle-removal-interval.seconds", defaultValue = "0.5")
    public double datasourceIdleValidationTimeout;

    /**
     * reap-timeout
     */
    @ConfigItem(name = "idle-reap-interval.seconds", defaultValue = "0.5")
    public double datasourceReapTimeout;

    /**
     * acquisition-timeout
     */
    @ConfigItem(name = "acquisition-timeout.seconds", defaultValue = "30")
    public double datasourceAcquisitionTimeout;

    /**
     * respond-time-to-drop
     */
    @ConfigItem(name = "respond-time-to-drop.seconds", defaultValue = "5")
    public String datasourceRespondTimeToDrop;

    /**
     * leak-detection-interval
     */
    @ConfigItem(name = "leak-detection-interval.seconds", defaultValue = "0")
    public double datasourceLeakDetectionInterval;

    /**
     * autocommit
     */
    @ConfigItem(name = "autocommit", defaultValue = "true")
    public Boolean autoCommit;

    /**
     * flush-on-close
     */
    @ConfigItem(name = "flush-on-close", defaultValue = "false")
    public Boolean flushOnClose;

}
