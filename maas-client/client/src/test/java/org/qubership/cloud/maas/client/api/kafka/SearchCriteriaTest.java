package org.qubership.cloud.maas.client.api.kafka;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchCriteriaTest {
    @Test
    void testSerialization() throws Exception {
        var sc = SearchCriteria.builder().namespace("core-dev").build();
        var mapper = new ObjectMapper();
        assertEquals("{\"namespace\":\"core-dev\"}", mapper.writeValueAsString(sc));
    }

    @Test
    void testToString() {
        assertEquals("SearchCriteria(topic=abc, namespace=null, instance=null)", 
        SearchCriteria.builder().topic("abc").build().toString());
    }
}
