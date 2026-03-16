package com.netcracker.cloud.consul.provider.spring.common.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.netcracker.cloud.consul.provider.spring.common.config.ConsulM2MConfigDataLocationResolver.args;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsulM2MConfigDataLocationResolverTest {

    @Test
    void getPropsOrEnvsMust() {
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args(""), args("")));
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("not.exists"), args("")));
        assertThrows(IllegalArgumentException.class, () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args(""), args("NOT_EXISTS")));

        System.setProperty("my.property", "property-value");
        String val = ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("my.property"), args(""));
        assertEquals("property-value", val);
        System.clearProperty("my.property");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("first.property", "second.property"), args("ENV_FIRST_PROPERTY", "ENV_SECOND_PROPERTY")));
        assertEquals("Missing required prop(s): [first.property, second.property] or env(s): [ENV_FIRST_PROPERTY, ENV_SECOND_PROPERTY]", ex.getMessage());
    }

    @Test
    void getPropsOrEnvsMust_envs() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        String firstEnvKey = entry.getKey();
        String firstEnvVal = entry.getValue();

        Assumptions.assumeFalse(firstEnvKey.isBlank());

        String val = ConsulM2MConfigDataLocationResolver.getPropsOrEnvsMust(args("not.exists"), args(firstEnvKey));
        assertEquals(firstEnvVal, val);
    }
}
