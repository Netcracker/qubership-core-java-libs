package org.qubership.cloud.configserver.common.configuration;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.configserver.common.sample.ApplicationTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomConfigServerDataLoaderTest extends ApplicationTests {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    BootstrapStateEventListeners.ContextRefreshedEventListener contextRefreshedEventListener;

    @Autowired
    BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener environmentChangedEventListener;

    @Test
    void loaderInterceptorRegistered_AndContextRefreshHappenedOnce() {
        // Most assertions performed in BootstrapStateEventListeners.ContextRefreshedEventListener and in
        // BootstrapStateEventListeners.ApplicationStartingEventListener
        assertEquals(contextRefreshedEventListener.maxAmountOfRefreshes, contextRefreshedEventListener.getNumberOfRefreshes());
    }

    @Test
    void loaderInterceptorNotVanishAfterRefreshEvent_AndRefreshHappenedOnce() {
        eventPublisher.publishEvent(new RefreshEvent(this, "RefreshEvent",
                "RefreshEvent to see if LoaderInterceptor stays in BootstrapContext"));
        // Most assertions performed in BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener and in
        // BootstrapStateEventListeners.ApplicationStartingEventListener
        assertEquals(environmentChangedEventListener.maxAmountOfRefreshes, environmentChangedEventListener.getNumberOfRefreshes());
    }
}
