package org.qubership.cloud.dbaas.client.arangodb.management;

import com.arangodb.springframework.annotation.EnableArangoRepositories;
import org.qubership.cloud.dbaas.client.arangodb.entity.Person;
import org.qubership.cloud.dbaas.client.arangodb.repository.PersonRepository;
import org.qubership.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBConfiguration;
import org.qubership.cloud.dbaas.client.arangodb.test.configuration.TestArangoDBContainer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestArangoDBConfiguration.class})
@WebAppConfiguration
@EnableArangoRepositories(basePackages = "org.qubership.cloud.dbaas.client.arangodb.repository")
@TestPropertySource(properties = {
        "dbaas.arangodb.dbId=db-test-name-1"
})
public class DbaasArangoRepositoriesStabilityTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @Autowired
    PersonRepository personRepository;

    @Test
    void testReconnect() {
        assertDoesNotThrow(() -> checkCrudOperations());

        TestArangoDBContainer.getInstance().stop();
        while (TestArangoDBContainer.getInstance().isRunning()) {
        }

        assertThrows(Exception.class, () -> checkCrudOperations());
        TestArangoDBContainer.getInstance().start();

        assertDoesNotThrow(() -> checkCrudOperations());
    }

    private void checkCrudOperations() {
        Person person = new Person(FIRST_NAME, LAST_NAME);
        personRepository.save(person);
        personRepository.findById(person.getId());
        personRepository.deleteById(person.getId());
    }
}
