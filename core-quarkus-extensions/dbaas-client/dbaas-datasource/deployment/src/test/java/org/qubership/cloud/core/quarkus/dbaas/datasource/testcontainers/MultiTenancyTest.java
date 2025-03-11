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
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.FIRST_TENANT_ID;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.MICROSERVICE_CLASSIFIER_BUILDER;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.SECOND_TENANT_ID;
import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.TENANT_CLASSIFIER_BUILDER;

@SkipOnDemand
@QuarkusTestResource(PostgresqlContainerResource.class)
@QuarkusTest
public class MultiTenancyTest {

    @Inject
    PersonService personService;

    @Inject
    DbaaSPostgresDbCreationService creationService;

    @Inject
    ContainerLogicalDbProvider logicalDbProvider;

    @BeforeAll
    public static void initContext() {
        ContextManager.register(Collections.singletonList(new TenantProvider()));
    }

    @AfterEach
    public void cleanup() {
        TestUtils.cleanupTenantDatabases(logicalDbProvider, personService);
    }

    @Test
    public void tenantsDataExistsSeparatelyUsingRepository() {
        String firstTenantUniqParam = "first-tenant-uniq-param";
        String secondTenantUniqParam = "second-tenant-uniq-param";

        TenantContext.set(FIRST_TENANT_ID);
        Person firstTenantEntity = new Person();
        firstTenantEntity.setFirstName(firstTenantUniqParam);

        personService.save(firstTenantEntity);
        Person savedFirstTenantEntity = personService.findAll().get(0);

        TenantContext.set(SECOND_TENANT_ID);
        Person secondTenantEntity = new Person();
        secondTenantEntity.setFirstName(secondTenantUniqParam);

        personService.save(secondTenantEntity);
        Person savedSecondTenantEntity = personService.findAll().get(0);

        Assertions.assertNotEquals(savedSecondTenantEntity.getFirstName(), savedFirstTenantEntity.getFirstName());
    }

    @Test
    public void tenantAndServiceDatabasesMustHaveDifferentConnectionUrls() throws SQLException {
        DbaasDbClassifier serviceClassifier = MICROSERVICE_CLASSIFIER_BUILDER.build();

        TenantContext.set(FIRST_TENANT_ID);
        DbaasDbClassifier firstTenantClassifier = TENANT_CLASSIFIER_BUILDER.build();

        TenantContext.set(SECOND_TENANT_ID);
        DbaasDbClassifier secondTenantClassifier = TENANT_CLASSIFIER_BUILDER.build();

        Assertions.assertNotEquals(serviceClassifier, firstTenantClassifier);
        Assertions.assertNotEquals(serviceClassifier, secondTenantClassifier);
        Assertions.assertNotEquals(firstTenantClassifier, secondTenantClassifier);

        PostgresDatabase serviceDb = creationService.getOrCreatePostgresDatabase(serviceClassifier);
        PostgresDatabase firstTenantDb = creationService.getOrCreatePostgresDatabase(firstTenantClassifier);
        PostgresDatabase secondTenantDb = creationService.getOrCreatePostgresDatabase(secondTenantClassifier);

        Assertions.assertNotEquals(serviceDb, firstTenantDb);
        Assertions.assertNotEquals(serviceDb, secondTenantDb);
        Assertions.assertNotEquals(firstTenantDb, secondTenantDb);

        try (java.sql.Connection serviceDbConnection = serviceDb.getConnectionProperties().getDataSource().getConnection()) {
            try (java.sql.Connection firstTenantDbConnection = firstTenantDb.getConnectionProperties().getDataSource().getConnection()) {
                try (java.sql.Connection secondTenantDbConnection = secondTenantDb.getConnectionProperties().getDataSource().getConnection()) {
                    assertConnectionsNotEqual(serviceDbConnection, firstTenantDbConnection);
                    assertConnectionsNotEqual(serviceDbConnection, secondTenantDbConnection);
                    assertConnectionsNotEqual(firstTenantDbConnection, secondTenantDbConnection);
                }
            }
        }
    }

    private void assertConnectionsNotEqual(java.sql.Connection left, java.sql.Connection right) throws SQLException {
        Assertions.assertNotEquals(left.getMetaData().getUserName(), right.getMetaData().getURL());
        Assertions.assertNotEquals(left.getMetaData().getURL(), right.getMetaData().getURL());
    }
}
