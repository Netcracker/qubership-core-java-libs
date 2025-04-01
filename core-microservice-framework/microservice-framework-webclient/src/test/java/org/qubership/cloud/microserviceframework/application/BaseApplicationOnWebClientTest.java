package org.qubership.cloud.microserviceframework.application;

import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.restclient.webclient.MicroserviceWebClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestApplicationOnWebClient.class},
        properties = {
                "cloud.microservice.name=test-service"
        },
        webEnvironment = RANDOM_PORT)
public class BaseApplicationOnWebClientTest {

    @Autowired
    ApplicationContext context;

    @Test
    public void testDbaasRestClientInitialized() {
        assertThat("Bean 'dbaasRestClient' is missing in context",
                context.getBean("dbaasRestClient") instanceof MicroserviceWebClient);
    }

    @Test
    public void testDbaasClientInitialized() {
        assertThat("Bean 'dbaasClient' is missing in context",
                context.getBean("dbaasClient") instanceof DbaasClient);
    }
}
