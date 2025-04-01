package org.qubership.cloud.microserviceframework.webclient.sample;

import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.qubership.cloud.microserviceframework.webclient.sample.configuration.RouteConfig;
import org.qubership.cloud.routesregistration.common.gateway.route.ControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.GatewayNameKey;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.RouteConfigurationRequestV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

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
        WebTestClient webClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
        EntityExchangeResult<String> a = webClient.get().uri("/test").exchange().expectBody(String.class).returnResult();
        Assert.assertEquals("test_value", a.getResponseBody());
    }

    @Test
    public void controlPlaneTest() {
        Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.PUBLIC)));
        Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.PRIVATE)));
        Mockito.verify(controlPlaneClient, Mockito.times(1)).sendRequest(Mockito.argThat(new RegistrationRequestMatcher(GatewayNameKey.INTERNAL)));
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
