package org.qubership.cloud.routesregistration.common.gateway.route;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutesRegistrationDelayProviderTest {

    private RoutesRegistrationDelayProvider routesRegistrationDelayProvider;

    @BeforeEach
    void setUp() {
        routesRegistrationDelayProvider = new RoutesRegistrationDelayProvider();
    }

    @Test
    void testIsRegistrationActive_default() {
        boolean isActiveRegistration = routesRegistrationDelayProvider.isRegistrationActive();
        assertTrue(isActiveRegistration);
    }

    @Test
    void testPauseRegistration() {
        long timeBeforeWait = System.currentTimeMillis();
        routesRegistrationDelayProvider.pauseRegistration();
        assertTrue((System.currentTimeMillis() - timeBeforeWait) >= 1000);
    }

    @Test
    void getProgressiveGatewayTimeoutTest() {
        assertEquals(1000, routesRegistrationDelayProvider.getTimeout());
        assertEquals(2000, routesRegistrationDelayProvider.getTimeout());
        for (int i = 0; i <= 11; i++) {
            routesRegistrationDelayProvider.getTimeout();
        }
        assertEquals(10000, routesRegistrationDelayProvider.getTimeout());
    }
}
