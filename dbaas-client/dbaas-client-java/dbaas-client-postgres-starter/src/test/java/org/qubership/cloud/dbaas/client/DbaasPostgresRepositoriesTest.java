package org.qubership.cloud.dbaas.client;

import org.qubership.cloud.dbaas.client.testconfiguration.PostgresTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.testconfiguration.TestPostgresWithRepositoriesConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = {TestPostgresWithRepositoriesConfig.class, PostgresTestContainerConfiguration.class},
        properties = {
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
                "spring.jpa.properties.hibernate.show_sql=true",
                "spring.jpa.properties.hibernate.hbm2ddl.auto=update",
                "dbaas.api.retry.default.template.maxAttempts=1",
                "dbaas.api.retry.default.template.backOffPeriod=0"})
public class DbaasPostgresRepositoriesTest {

    @Autowired
    private PersonService personService;

    @Test
    public void testPostgresRepository() {
        Person francisco = new Person("Francisco", "d'Anconia");
        personService.addPerson(francisco);
        List<Person> all = personService.findAll();
        Assertions.assertEquals(1, all.size());
    }
}
