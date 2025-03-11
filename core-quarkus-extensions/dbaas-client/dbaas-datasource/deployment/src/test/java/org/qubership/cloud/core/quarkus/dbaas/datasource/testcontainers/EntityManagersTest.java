package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.SkipOnDemand;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.secondary.PersonSecondary;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SkipOnDemand
@QuarkusTestResource(PostgresqlContainerResource.class)
@QuarkusTest
@TestProfile(EntityManagersTest.EntityManagersTestProfile.class)
class EntityManagersTest {

    @Inject
    @PersistenceUnit("secondary")
    EntityManager secondaryEntityManager;

    @Inject
    EntityManager defaultEntityManager;

    @Test
    void testSeparateEntityManagers() {
        QuarkusTransaction.requiringNew().run(() -> {
            Person firstPerson = new Person();
            firstPerson.setFirstName("first");
            defaultEntityManager.persist(firstPerson);

            PersonSecondary secondPerson = new PersonSecondary();
            secondPerson.setFirstName("second");
            secondaryEntityManager.persist(secondPerson);

            List<Person> persons = defaultEntityManager.createQuery("from Person").getResultList();
            assertEquals(1, persons.size());
            assertEquals("first", persons.get(0).getFirstName());

            List<PersonSecondary> secondaryPersons = secondaryEntityManager.createQuery("from PersonSecondary").getResultList();
            assertEquals(1, secondaryPersons.size());
            assertEquals("second", secondaryPersons.get(0).getFirstName());
        });
    }

    @NoArgsConstructor
    protected static final class EntityManagersTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "em";
        }
    }
}
