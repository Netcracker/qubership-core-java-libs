package com.netcracker.cloud.maas.client.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.impl.Env;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.qubership.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClassifierTest {
    @BeforeEach
    public void setup() {
        System.clearProperty(Env.PROP_NAMESPACE);
    }
    @AfterEach
    public void tearDown() {
        System.clearProperty(Env.PROP_NAMESPACE);
    }

    @Test
    public void testConstructor() {
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            assertEquals(
                    new HashMap<String, String>() {{
                        put(Classifier.NAME, "bc");
                        put(Classifier.NAMESPACE, "core-dev");
                    }},
                    new Classifier("bc").toMap());
        });
    }

    @Test
    public void testConstructorWithOpts() {
        Classifier classifier = new Classifier("bc", Classifier.NAMESPACE, "test-namespace", Classifier.TENANT_ID, "test-tenant-id");
        assertEquals("bc", classifier.getName());
        assertEquals("test-namespace", classifier.getNamespace());
        assertEquals(Optional.of("test-tenant-id"), classifier.getTenantId());
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            var original = new Classifier("bc", "a", "b");

            ObjectMapper om = new ObjectMapper();
            String json = om.writeValueAsString(original);
            assertEquals("{\"name\":\"bc\",\"namespace\":\"core-dev\"}", json);

            var restored = om.readValue(json, Classifier.class);
            assertEquals(original, restored);
        });
    }

    @Test
    public void testDeserialize() throws Exception {
        ObjectMapper om = new ObjectMapper();
        Classifier classifier = om.readValue("{\"name\":\"bc\",\"a\":\"b\",\"namespace\":\"core-dev\"}", Classifier.class);
        assertEquals(new Classifier("bc", Classifier.NAMESPACE, "core-dev"), classifier);
    }

    @Test
    public void testDeserializeWrong_NoName() throws Exception {
        ObjectMapper om = new ObjectMapper();
        assertThrows(IllegalArgumentException.class, () -> om.readValue("{}", Classifier.class));
    }

    @Test
    public void testDeserializeWrong_NoNamespace() throws Exception {
        ObjectMapper om = new ObjectMapper();
        assertThrows(IllegalArgumentException.class, () -> om.readValue("{\"name\": \"bc\"}", Classifier.class));
    }

    @Test
    public void testWrongArgs() {
        assertThrows(IllegalArgumentException.class, () -> new Classifier("äbc", "b"));
    }

    @Test
    public void testToString() {
        withProp(Env.PROP_NAMESPACE, "core-dev", () -> {
            assertEquals("Classifier{name=foo, namespace=core-dev}", new Classifier("foo").toString());
        });
    }

    @Test
    public void testPredefinedNamespace() {
        // classifier should be able to allow insert custom namespace value
        // also test that this execution doesn't require PROP_NAMESPACE env property set
        assertEquals("abc", new Classifier("foo", Classifier.NAMESPACE, "abc").getNamespace());
    }
}
