package com.netcracker.cloud.maas.bluegreen.versiontracker.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This test fails if {@link TrackingVersionFilter} suddenly changed
 */
public class TrackingVersionFilterApiTest {

    @Test
    void testApi() {
        var tvf = new TrackingVersionFilter() {
            @Override
            public boolean test(String v) {
                return false;
            }

        };
        assertFalse(tvf.test("1"));
    }
}
