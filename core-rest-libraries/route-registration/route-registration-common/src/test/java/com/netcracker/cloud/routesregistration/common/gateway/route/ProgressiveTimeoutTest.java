package com.netcracker.cloud.routesregistration.common.gateway.route;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressiveTimeoutTest {

    private static final int RETRY_DELAY_MILLIS = 1000;

    @Test
    void progressiveTimeoutConstructorTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS, 1, 10, 1);
        assertEquals(1, progressiveTimeout.getStartMultiplier());
        assertEquals(10, progressiveTimeout.getEndMultiplier());
        assertEquals(1, progressiveTimeout.getMultiplierStep());
        assertEquals(RETRY_DELAY_MILLIS, progressiveTimeout.getBaseTimeout());
    }

    @Test
    void progressiveTimeoutConstructorWithIllegalArgumentExceptionTest() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressiveTimeout(RETRY_DELAY_MILLIS, 100, 10, 1));
    }

    @Test
    void newTimeoutValueTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS, 1, 10, 1);
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
        assertEquals(2_000, progressiveTimeout.nextTimeoutValue());
    }

    @Test
    void resetTest() {
        ProgressiveTimeout progressiveTimeout = new ProgressiveTimeout(RETRY_DELAY_MILLIS, 1, 10, 1);
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
        progressiveTimeout.reset();
        assertEquals(1_000, progressiveTimeout.nextTimeoutValue());
    }
}
