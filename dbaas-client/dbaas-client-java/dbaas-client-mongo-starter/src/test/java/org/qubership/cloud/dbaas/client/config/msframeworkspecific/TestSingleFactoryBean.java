package org.qubership.cloud.dbaas.client.config.msframeworkspecific;

import org.qubership.cloud.dbaas.client.config.EnableDbaasMongo;
import org.qubership.cloud.dbaas.client.config.msframeworkspecific.testconfig.TestMongoDbConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@EnableDbaasMongo
@SpringBootTest(properties = {"cloud.microservice.namespace=default", "cloud.microservice.name=mongo-app"})
@EnableAutoConfiguration
@ContextConfiguration(classes = TestMongoDbConfiguration.class)
public class TestSingleFactoryBean {

    @Test
    public void contextLoads() {
    }

}
