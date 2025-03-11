package org.qubership.cloud.springcloud.config.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.springcloud.config.source.configuration.InjectWireMock;
import org.qubership.cloud.springcloud.config.source.configuration.WiremockConfiguration;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.mockito.Mockito.spy;

@QuarkusTest
@QuarkusTestResource(WiremockConfiguration.class)
public class ConfigServerClientImplTest {

    @InjectWireMock
    WireMockServer wireMockServer;

    private ObjectMapper mapper;
    ConfigServerClientImpl csClient;

    @BeforeEach
    public void beforeTest() throws Exception {
        System.setProperty("cloud.microservice.name", "test-application");
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        csClient = spy(new ConfigServerClientImpl(wireMockServer.baseUrl()));
    }

    @Test
    public void getPropertiesTest() throws Exception {
        CloudEnv expectedEnv = mapper.readValue(Constants.CS_RESPONSE_STRING, CloudEnv.class);
        CloudEnv actualEnv = csClient.getProperties();
        Assertions.assertEquals(expectedEnv, actualEnv);
    }


    @Test
    public void putPropertiesTest() {
        Map<String, String> properties = new HashMap<>();
        properties.put("var1", "value1");
        properties.put("var2", "value2");

        csClient.putProperties(properties);
    }
}

