package org.qubership.cloud.routesregistration.common.gateway.route;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RoutesRegistrationDelayProviderTest {

    private RoutesRegistrationDelayProvider routesRegistrationDelayProvider;

    @Before
    public void setUp() {
        routesRegistrationDelayProvider = new RoutesRegistrationDelayProvider();
    }

    @Test
    public void testIsRegistrationActive_default() {
        boolean isActiveRegistration = routesRegistrationDelayProvider.isRegistrationActive();
        assertTrue(isActiveRegistration);
    }

    @Test
    public void testPauseRegistration() {
        long timeBeforeWait = System.currentTimeMillis();
        routesRegistrationDelayProvider.pauseRegistration();
        Assert.assertTrue((System.currentTimeMillis() - timeBeforeWait) >= 1000);
    }

    @Test
    public void getProgressiveGatewayTimeoutTest() {
        assertEquals(1000, routesRegistrationDelayProvider.getTimeout());
        assertEquals(2000, routesRegistrationDelayProvider.getTimeout());
        for (int i = 0; i <= 11; i++) {
            routesRegistrationDelayProvider.getTimeout();
        }
        assertEquals(10000, routesRegistrationDelayProvider.getTimeout());
    }
}
