package org.qubership.cloud.routesregistration.common.gateway.route;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProgressiveTimeoutTest {

    private static final int RETRY_DELAY_MILLIS = 1000;

    @Test
    public void progressiveTimeoutConstructorTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS,1, 10, 1);
        assertEquals(1, progressiveTimeout.getStartMultiplier());
        assertEquals(10, progressiveTimeout.getEndMultiplier());
        assertEquals(1, progressiveTimeout.getMultiplierStep());
        assertEquals(RETRY_DELAY_MILLIS, progressiveTimeout.getBaseTimeout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void progressiveTimeoutConstructorWithIllegalArgumentExceptionTest() {
        new ProgressiveTimeout(RETRY_DELAY_MILLIS,100, 10, 1);
    }

    @Test
    public void newTimeoutValueTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS,1, 10, 1);
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
        assertEquals(2_000, progressiveTimeout.nextTimeoutValue());
    }

    @Test
    public void resetTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS,1, 10, 1);
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
        progressiveTimeout.reset();
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
    }
}
