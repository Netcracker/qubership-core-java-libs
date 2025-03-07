package org.qubership.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.test.Person;
import org.qubership.cloud.dbaas.client.test.PersonService;
import org.qubership.cloud.dbaas.client.test.configuration.MongoTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.test.configuration.TestMongoRepositoriesConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ContextConfiguration(classes = {TestMongoRepositoriesConfiguration.class, MongoTestContainerConfiguration.class})
@TestPropertySource(properties = {
        "dbaas.mongo.options.serverSelectionTimeout=100"
})
public class DbaasMongoRepositoriesTest {

    @Autowired
    private PersonService personService;

    @Autowired
    private DbaasClient dbaasClient;

    @Test
    public void testMongoRepository() {
        Person francisco = new Person("Francisco", "d'Anconia");
        personService.addPerson(francisco);
        Assertions.assertEquals(1, personService.findAll().size());
    }
}
