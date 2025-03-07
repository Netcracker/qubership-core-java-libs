package org.qubership.cloud.configserver.common.configuration;

import org.qubership.cloud.configserver.common.sample.ApplicationTests;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEventPublisher;

public class CustomConfigServerDataLoaderTest extends ApplicationTests {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    BootstrapStateEventListeners.ContextRefreshedEventListener contextRefreshedEventListener;

    @Autowired
    BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener environmentChangedEventListener;

    @Test
    public void loaderInterceptorRegistered_AndContextRefreshHappenedOnce() {
        // Most assertions performed in BootstrapStateEventListeners.ContextRefreshedEventListener and in
        // BootstrapStateEventListeners.ApplicationStartingEventListener
        Assert.assertEquals(contextRefreshedEventListener.maxAmountOfRefreshes, contextRefreshedEventListener.getNumberOfRefreshes());
    }

    @Test
    public void loaderInterceptorNotVanishAfterRefreshEvent_AndRefreshHappenedOnce() {
        eventPublisher.publishEvent(new RefreshEvent(this, "RefreshEvent",
                "RefreshEvent to see if LoaderInterceptor stays in BootstrapContext"));
        // Most assertions performed in BootstrapStateEventListeners.EnvironmentChangedAfterRefreshEventListener and in
        // BootstrapStateEventListeners.ApplicationStartingEventListener
        Assert.assertEquals(environmentChangedEventListener.maxAmountOfRefreshes, environmentChangedEventListener.getNumberOfRefreshes());
    }

}
