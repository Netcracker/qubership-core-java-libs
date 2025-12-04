package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface JDBCConfig {

    /**
     * poolSize
     */
    @WithName("max-size")
    @WithDefault("5")
    Integer poolSize();

    /**
     * minPoolSize
     */
    @WithName("min-size")
    @WithDefault("0")
    Integer minPoolSize();

    /**
     * initPoolSize
     */
    @WithName("initial-size")
    @WithDefault("0")
    Integer initPoolSize();

    /**
     * background-validation-interval
     */
    @WithName("background-validation-interval.seconds")
    @WithDefault("120")
    double datasourceValidationInterval();

    /**
     * idle-removal-interval
     */
    @WithName("idle-removal-interval.seconds")
    @WithDefault("0.5")
    double datasourceIdleValidationTimeout();

    /**
     * reap-timeout
     */
    @WithName("idle-reap-interval.seconds")
    @WithDefault("0.5")
    double datasourceReapTimeout();

    /**
     * acquisition-timeout
     */
    @WithName("acquisition-timeout.seconds")
    @WithDefault("30")
    double datasourceAcquisitionTimeout();

    /**
     * respond-time-to-drop
     */
    @WithName("respond-time-to-drop.seconds")
    @WithDefault("5")
    String datasourceRespondTimeToDrop();

    /**
     * leak-detection-interval
     */
    @WithName("leak-detection-interval.seconds")
    @WithDefault("0")
    double datasourceLeakDetectionInterval();

    /**
     * autocommit
     */
    @WithName("autocommit")
    @WithDefault("true")
    Boolean autoCommit();

    /**
     * flush-on-close
     */
    @WithName("flush-on-close")
    @WithDefault("false")
    Boolean flushOnClose();
}
