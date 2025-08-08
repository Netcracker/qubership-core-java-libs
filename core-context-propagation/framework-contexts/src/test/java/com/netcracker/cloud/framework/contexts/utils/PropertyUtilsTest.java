package com.netcracker.cloud.framework.contexts.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.netcracker.cloud.framework.contexts.utils.PropertyUtils;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(SystemStubsExtension.class)
public class PropertyUtilsTest {

    @SystemStub
    private EnvironmentVariables environmentVariables = new EnvironmentVariables("TEST_PROP", "env_val");

    @Test
    void testPropertyUtils() {
        final String randomUUID = UUID.randomUUID().toString();
        final String testProperty = "test.prop." + randomUUID;

        System.clearProperty(testProperty);
        assertNull(PropertyUtils.getPropertyOrEnv(testProperty));
        assertEquals("default", PropertyUtils.getPropertyOrEnvOrDefault(testProperty, "TEST_PROP_" + randomUUID, "default"));

        System.setProperty(testProperty, "val");
        assertEquals("val", PropertyUtils.getPropertyOrEnvOrDefault(testProperty, "TEST_PROP_" + randomUUID, "default"));
        assertEquals("val", PropertyUtils.getPropertyOrEnv(testProperty, "TEST_PROP_" + randomUUID));
        assertEquals("val", PropertyUtils.getPropertyOrEnv(testProperty));

        assertEquals("env_val", PropertyUtils.getPropertyOrEnv("TEST_PROP"));
    }
}
