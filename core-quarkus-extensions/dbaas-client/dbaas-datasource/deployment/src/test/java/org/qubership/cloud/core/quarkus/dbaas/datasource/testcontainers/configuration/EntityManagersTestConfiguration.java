package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.qubership.cloud.core.quarkus.dbaas.datasource.DbaasQuarkusPostgresqlDatasourceBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;

import org.flywaydb.core.Flyway;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.CommonTestUtils.getServiceClassifierBuilder;

@Dependent
public class EntityManagersTestConfiguration {

    public static final String SECONDARY_DATA_SOURCE_NAME = "secondary";

    @Produces
    @Named(SECONDARY_DATA_SOURCE_NAME)
    @DataSource(SECONDARY_DATA_SOURCE_NAME)
    @Alternative
    @Priority(1)
    public AgroalDataSource getSecondaryDataSource(@NotNull DbaaSPostgresDbCreationService dsCreationService) {
        return (AgroalDataSource) new DbaasQuarkusPostgresqlDatasourceBuilder(dsCreationService).newBuilder(getServiceClassifierBuilder("secondary-db"))
                .withFlyway(context -> {
                    Flyway flyway = Flyway.configure()
                            .dataSource(context.getDataSource())
                            .baselineOnMigrate(true)
                            .locations("classpath:db/migration")
                            .load();
                    flyway.migrate();
                }).build();
    }
}
