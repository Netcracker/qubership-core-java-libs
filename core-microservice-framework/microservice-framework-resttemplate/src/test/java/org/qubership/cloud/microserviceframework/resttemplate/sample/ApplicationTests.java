package org.qubership.cloud.microserviceframework.resttemplate.sample;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.qubership.cloud.microserviceframework.resttemplate.sample.configuration.RouteConfig;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.GatewayNameKey;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestApplication.class, RouteConfig.class},
        properties = {
                "cloud.microservice.name=test-service"
        },
        webEnvironment = RANDOM_PORT)
public class ApplicationTests {

    @Autowired
    ControlPlaneClient controlPlaneClient;

    @LocalServerPort
    private int port;

    @Test
    public void configServerTest() {
        String res = new TestRestTemplate().getForObject(
                "http://localhost:" + this.port + "/test", String.class);
        Assert.assertEquals("test_value", res);
    }

    @Test
    public void controlPlaneTest() {
        log.info("controlPlaneTest started");
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.PUBLIC)));
                    Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.PRIVATE)));
                    Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.INTERNAL)));
                });
        log.info("controlPlaneTest finished");
    }

    @RequiredArgsConstructor
    private static class RegistrationRequestMatcher implements ArgumentMatcher<RegistrationRequest> {
        private final GatewayNameKey gatewayName;

        @Override
        public boolean matches(RegistrationRequest argument) {
            Object payload = argument.getPayload();
            if (payload instanceof RouteConfigurationRequestV3) {
                return ((RouteConfigurationRequestV3) payload).getGateways().get(0).equals(gatewayName.toGatewayName());
            }
            return false;
        }
    }
}
