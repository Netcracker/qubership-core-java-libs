package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.framework.contexts.tenant.TenantProvider;
import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.ContainerLogicalDbProvider;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.SkipOnDemand;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.service.PersonService;
import org.qubership.cloud.dbaas.client.entity.database.DatasourceConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.sql.DataSource;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.getServiceClassifier;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.FIRST_TENANT_ID;

@SkipOnDemand
@QuarkusTestResource(PostgresqlContainerResource.class)
@QuarkusTest
public class TransactionsTest {
    @Inject
    PersonService personService;

    @Inject
    ContainerLogicalDbProvider logicalDbProvider;

    @Inject
    DbaaSPostgresDbCreationService creationService;


    private static final RuntimeException exception = new RuntimeException("Exception happened while transaction in progress");

    @BeforeAll
    public static void initContext() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @AfterEach
    public void cleanup() {
        TestUtils.cleanupTenantDatabases(logicalDbProvider, personService);
    }

    @Test
    public void rollbackSuccessAfterRuntimeException() {
        TenantContext.set(FIRST_TENANT_ID);
        Person person = new Person();

        Function<Person, Person> applier = c -> {
            throw exception;
        };

        boolean exceptionThrown = false;
        try {
            personService.saveThenApply(person, applier);
        } catch (RuntimeException e) {
            exceptionThrown = true;
            Assertions.assertEquals(exception, e);
            Assertions.assertEquals(0, personService.findAll().size());
        }
        Assertions.assertTrue(exceptionThrown);
    }

    @Test
    public void rollbackAffectsOnlyLastTransaction() {
        String firstPersonFirstName = "first-person-first-name";
        String secondPersonFirstName = "second-person-first-name";

        TenantContext.set(FIRST_TENANT_ID);
        Person firstPerson = new Person();
        firstPerson.setFirstName(firstPersonFirstName);

        Function<Person, Person> applier = c -> {
            throw exception;
        };

        boolean exceptionThrown = false;

        personService.save(firstPerson);
        List<Person> savedPersons = personService.findAll();
        Assertions.assertEquals(1, savedPersons.size());
        Assertions.assertEquals(firstPersonFirstName, savedPersons.get(0).getFirstName());

        Person secondPerson = new Person();
        secondPerson.setFirstName(secondPersonFirstName);
        try {
            personService.saveThenApply(secondPerson, applier);
        } catch (Exception e) {
            exceptionThrown = true;
            Assertions.assertEquals(exception, e);
            savedPersons = personService.findAll();
            Assertions.assertEquals(1, savedPersons.size());
            Assertions.assertEquals(firstPersonFirstName, savedPersons.get(0).getFirstName());
        }
        Assertions.assertTrue(exceptionThrown);
    }

    @Test
    void testConnectionIsNotSharedWithinTransaction() throws SQLException {
        DatasourceConnectorSettings connectorSettings = DatasourceConnectorSettings.builder()
                .flywayRunner(context -> {
                    Flyway flyway = Flyway.configure()
                            .dataSource(context.getDataSource())
                            .baselineOnMigrate(true)
                            .locations("classpath:db/migration")
                            .load();
                    flyway.migrate();
                }).build();
        PostgresDatabase firstDb = creationService.getOrCreatePostgresDatabase(getServiceClassifier("first"), connectorSettings, null);
        PostgresDatabase secondDb = creationService.getOrCreatePostgresDatabase(getServiceClassifier("second"), connectorSettings, null);

        DataSource firstDatasource = firstDb.getConnectionProperties().getDataSource();
        DataSource secondDatasource = secondDb.getConnectionProperties().getDataSource();

        Assertions.assertNotEquals(firstDatasource, secondDatasource);
        firstDatasource.getConnection().createStatement().execute("INSERT into persons(first_name, last_name) VALUES ('first', 'person')");
        secondDatasource.getConnection().createStatement().execute("INSERT into persons(first_name, last_name) VALUES ('second', 'person')");

        QuarkusTransaction.requiringNew().run(() -> {
            try {
                ResultSet firstResultSet = firstDatasource.getConnection().createStatement().executeQuery("SELECT first_name from persons where last_name = 'person'");
                Assertions.assertTrue(firstResultSet.next());
                Assertions.assertEquals("first", firstResultSet.getString(1));
                Assertions.assertFalse(firstResultSet.next());

                ResultSet secondResultSet = secondDatasource.getConnection().createStatement().executeQuery("SELECT first_name from persons where last_name = 'person'");
                Assertions.assertTrue(secondResultSet.next());
                Assertions.assertEquals("second", secondResultSet.getString(1));
                Assertions.assertFalse(secondResultSet.next());
            } catch (SQLException e) {
                Assertions.fail(e);
            }
        });
    }

}
