package org.qubership.cloud.maas.client.impl.apiversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiVersionTest {

    @Test
    void compareEquals() {
        assertTrue(new ApiVersion(1, 2).isCompatible(new ApiVersion(1, 2)));
    }

    @Test
    void compareMajors() {
        assertFalse(new ApiVersion(4, 2).isCompatible(new ApiVersion(1, 2)));
    }

    @Test
    void compareMinors() {
        assertTrue(new ApiVersion(1, 2).isCompatible(new ApiVersion(1, 8)));
    }

    @Test
    void isVersionCompatible() {
        ApiVersion requested = new ApiVersion(2,7);
        ApiVersion received = new ApiVersion(2,10);

        assertTrue(requested.isCompatible(received));
    }
}