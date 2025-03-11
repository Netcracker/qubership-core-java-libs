package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.config.DataSourceConfiguration.SERVICE_DATASOURCE;

@ApplicationScoped
public class CoreFlywayStartupConfiguration {

    @Inject
    CoreFlywayConfig coreFlywayConfig;

    @Inject
    CoreFlywayCreator coreFlywayCreator;

    private static final Logger log = Logger.getLogger(CoreFlywayStartupConfiguration.class);

    void onStart(@Observes StartupEvent event, @Named(SERVICE_DATASOURCE) AgroalDataSource serviceDataSource) {
        if (coreFlywayConfig.globalFlywayConfig.cleanAndMigrateAtStart) {
            Flyway flyway = coreFlywayCreator.createFlyway(serviceDataSource, null);
            log.debug("Core flyway: clean and run start time migration");
            flyway.clean();
            flyway.migrate();
        }
    }
}