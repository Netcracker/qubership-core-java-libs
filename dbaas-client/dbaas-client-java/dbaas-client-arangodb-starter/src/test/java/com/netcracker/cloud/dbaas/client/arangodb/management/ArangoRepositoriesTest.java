package com.netcracker.cloud.dbaas.client.arangodb.management;


import com.arangodb.springframework.annotation.EnableArangoRepositories;
import org.qubership.cloud.dbaas.client.arangodb.entity.Person;
import org.qubership.cloud.dbaas.client.arangodb.repository.PersonRepository;
import org.qubership.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBConfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestArangoDBConfiguration.class})
@WebAppConfiguration
@EnableArangoRepositories(basePackages = "org.qubership.cloud.dbaas.client.arangodb.repository")
@TestPropertySource(properties = {
        "dbaas.arangodb.dbId=db-test-name-1"
})
public class ArangoRepositoriesTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    ArangoPostProcessor arangoPostProcessor;

    @Test
    public void testCrud() {
        Person person = new Person(FIRST_NAME, LAST_NAME);
        personRepository.save(person);
        Optional<Person> optionalFoundPerson = personRepository.findById(person.getId());
        assertTrue(optionalFoundPerson.isPresent());
        Person foundPerson = optionalFoundPerson.get();
        assertEquals(FIRST_NAME, foundPerson.getFirstName());
        assertEquals(LAST_NAME, foundPerson.getLastName());
        personRepository.deleteById(person.getId());
        optionalFoundPerson = personRepository.findById(person.getId());
        assertFalse(optionalFoundPerson.isPresent());
    }

    @Test
    void testArangoSerde() {
        Assertions.assertNotNull(arangoPostProcessor.dbaasArangoDBClientProperties.getArangoSerde());
    }
}
