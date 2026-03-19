package com.netcracker.cloud.microserviceframework.application;

import org.junit.jupiter.api.Test;
import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.restclient.webclient.MicroserviceWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = {TestApplicationOnWebClient.class},
        properties = {
                "cloud.microservice.name=test-service"
        },
        webEnvironment = RANDOM_PORT)
class BaseApplicationOnWebClientTest {

    @Autowired
    ApplicationContext context;

    @Test
    void testDbaasRestClientInitialized() {
        assertThat("Bean 'dbaasRestClient' is missing in context",
                context.getBean("dbaasRestClient") instanceof MicroserviceWebClient);
    }

    @Test
    void testDbaasClientInitialized() {
        assertThat("Bean 'dbaasClient' is missing in context",
                context.getBean("dbaasClient") instanceof DbaasClient);
    }
}
