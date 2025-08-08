package org.qubership.cloud.context.propagation.quarkus.runtime.interceptor;

import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuarkusResponseContextDataTest {

    @Test
    public void testGetAndSetHeaders() {
        QuarkusResponseContextData quarkusResponseContextData = new QuarkusResponseContextData();
        quarkusResponseContextData.set("header", "value");
        quarkusResponseContextData.set("list_header", Arrays.asList("1", "2", "3"));
        assertEquals(2, quarkusResponseContextData.getResponseHeaders().size());
        assertTrue(quarkusResponseContextData.getResponseHeaders().containsKey("header"));
        assertEquals("value", quarkusResponseContextData.getResponseHeaders().get("header"));
    }

    @Test
    public void testGetAndSetHeaders_addHeadersToMap() {
        QuarkusResponseContextData quarkusResponseContextData = new QuarkusResponseContextData();
        quarkusResponseContextData.set("header", "value");
        quarkusResponseContextData.set("list_header", Arrays.asList("1", "2", "3"));

        MultivaluedMap<String, Object> map = new QuarkusMultivaluedHashMap<>();
        map.putSingle("header", "value2");
        quarkusResponseContextData.addHeadersToMap(map);

        assertEquals("value2", map.getFirst("header"));
        assertEquals("1", map.getFirst("list_header"));
    }
}
