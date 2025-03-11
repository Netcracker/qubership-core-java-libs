package org.qubership.cloud.quarkus.consul.client.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryParamsTest {

    @Test
    void testToUrlParameters_withValues() {
        QueryParams queryParams = new QueryParams(10, 100);
        queryParams.setToken("test-token");

        var params = queryParams.toUrlParameters();

        assertTrue(params.contains("wait=10s"));
        assertTrue(params.contains("index=100"));
        assertTrue(params.contains("token=test-token"));
        assertTrue(params.contains("recurse"));
    }

    @Test
    void testToUrlParameters_empty() {
        QueryParams queryParams = new QueryParams(-1, -1);
        var params = queryParams.toUrlParameters();

        assertTrue(params.contains("recurse"));
        assertFalse(params.contains("wait="));
        assertFalse(params.contains("index="));
    }
}

