package com.netcracker.cloud.routesregistration.common.gateway.route;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Class to activate/pause routes registration process
 */
@Slf4j
public class RoutesRegistrationDelayProvider {
    private static final int RETRY_DELAY_MILLIS = 1000;

    private final RegistrationStatus registrationStatus = new RegistrationStatus();
    private final ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS, 1, 10, 1);

    @Data
    private static class RegistrationStatus {
        private boolean isAvailable = true;
    }

    @Deprecated
    public void setProperties() {}

    public RoutesRegistrationDelayProvider() {
    }

    public long getTimeout() {
        return progressiveTimeout.nextTimeoutValue();
    }

    @Deprecated
    public long getGatewayTimeout(GatewayNameKey gatewayKey) {
        return getTimeout();
    }

    public boolean isRegistrationActive() {
        synchronized (registrationStatus) {
            return registrationStatus.isAvailable();
        }
    }

    @Deprecated
    public boolean isRegistrationActive(GatewayNameKey key) {
        return isRegistrationActive();
    }

    public void pauseRegistration() {
        synchronized (registrationStatus) {
            log.debug("Routes registration is paused");
            long timeout = getTimeout();
            waitTimeout(timeout);
        }
    }

    @Deprecated
    public void pauseRegistration(GatewayNameKey key) {
        pauseRegistration();
    }

    public void activateRegistration() {
        synchronized (registrationStatus) {
            if(!registrationStatus.isAvailable()) {
                registrationStatus.setAvailable(true);
                progressiveTimeout.reset();
                log.debug("Routes registration is activated");
            }
        }
    }

    @Deprecated
    public void activateRegistration(GatewayNameKey key) {
        activateRegistration();
    }

    private void waitTimeout(long timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException ie) {
            log.error("Routes registration Thread error", ie);
        }
    }
}
