package org.qubership.cloud.springcloud.config.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.quarkus.runtime.LaunchMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PropertyManagerTest {

    private PropertyManager pm;
    private ConfigServerClientImpl csClient;
    private CloudEnv expectedEnv;
    private ObjectMapper mapper;

    private static final VarHandle MODIFIERS;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        System.setProperty("cloud.microservice.name", "test-application");
        io.quarkus.runtime.LaunchMode.set(LaunchMode.NORMAL);
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

        expectedEnv = mapper.readValue(Constants.CS_RESPONSE_STRING, CloudEnv.class);

        csClient = mock(ConfigServerClientImpl.class);
        doNothing().when(csClient).putProperties(any());
        pm = spy(new PropertyManager());
        doReturn(csClient).when(pm).getClient();
        doReturn(expectedEnv).when(csClient).getProperties();
    }

    @Test
    public void getPropertiesTest() {
        Map<String, String> properties = expectedEnv.getPropertySources().get(0).getSource();

        Assertions.assertEquals(properties, pm.getProperties());
        Assertions.assertEquals(properties, pm.getProperties());
        verify(csClient, Mockito.times(1)).getProperties();
    }

    @Test
    public void putPropertiesTest() {
        Map<String, String> properties = new HashMap<>();
        properties.put("var1", "value1");
        properties.put("var2", "value2");

        pm.putProperties(properties);
        Assertions.assertEquals(properties, pm.getProperties());
        verify(csClient, Mockito.times(0)).getProperties();
    }
}

