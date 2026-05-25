package com.netcracker.cloud.dbaas.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringDbaasApiPropertiesTest {

    @Test
    void testGetAddress() {
        SpringDbaasApiProperties properties = new SpringDbaasApiProperties();
        
        // Test non-k8s path
        ReflectionTestUtils.setField(properties, "k8sEnabled", false);
        ReflectionTestUtils.setField(properties, "dbaasAgentAddress", Optional.of("http://custom"));
        assertEquals("http://custom", properties.getAddress());
        
        ReflectionTestUtils.setField(properties, "dbaasAgentAddress", Optional.empty());
        assertEquals("http://dbaas-agent:8080", properties.getAddress());
        
        // Test k8s path
        ReflectionTestUtils.setField(properties, "k8sEnabled", true);
        ReflectionTestUtils.setField(properties, "dbaasAddress", Optional.of("http://k8s-url"));
        assertEquals("http://k8s-url", properties.getAddress());
        
        ReflectionTestUtils.setField(properties, "dbaasAddress", Optional.empty());
        assertEquals("http://dbaas-agent:8080", properties.getAddress());
    }
}
