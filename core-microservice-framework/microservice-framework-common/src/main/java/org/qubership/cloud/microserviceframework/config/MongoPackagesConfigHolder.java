package org.qubership.cloud.microserviceframework.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Singleton to hold mongo evolution change logs packages configuration.
 */
@ToString
public enum MongoPackagesConfigHolder {
    INSTANCE;

    /**
     * Guaranteed to always return the same instance of singleton
     * {@code MongoPackagesConfigHolder}.
     *
     * @return config holder instance
     */
    public static MongoPackagesConfigHolder getInstance() {
        return INSTANCE;
    }

    /**
     *  Flag that shows if mongo evolution needs to use custom
     *  change log packages from this config holder. Flag can
     *  be set with {@link MongoPackagesConfigHolder#setUsingCustomPackages(boolean)},
     *  but it's also being set automatically when
     *  {@link MongoPackagesConfigHolder#setTenantAwareCustomPackage} or
     *  {@link MongoPackagesConfigHolder#setMicroserviceCustomPackage(String)}
     *  method is called.
     *
     *  @param usingCustomPackages value for {@code usingCustomPackages} flag.
     *  @return {@code true} if custom packages are used, {@code false} otherwise.
     */
    @Getter
    @Setter
    private boolean usingCustomPackages = false;
    @Getter
    private String tenantAwareCustomPackage;
    @Getter
    private String microserviceCustomPackage;
    @Getter
    @Setter
    private String commonPackage;
    @Getter
    @Setter
    private String defaultPackage;

    /**
     * Sets tenant aware custom package name,
     * also sets {@code usingCustomPackages} flag to true.
     *
     * @param tenantAwareCustomPackage custom tenant aware change logs package name.
     */
    public void setTenantAwareCustomPackage(String tenantAwareCustomPackage) {
        this.tenantAwareCustomPackage = tenantAwareCustomPackage;
        usingCustomPackages = true;
    }

    /**
     * Sets microservice custom package name,
     * also sets {@code usingCustomPackages} flag to true.
     *
     * @param microserviceCustomPackage custom microservice change logs package name'.
     */
    public void setMicroserviceCustomPackage(String microserviceCustomPackage) {
        this.microserviceCustomPackage = microserviceCustomPackage;
        usingCustomPackages = true;
    }
}
