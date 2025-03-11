package org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway;

import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.util.List;

@ApplicationScoped
public class CoreFlywayCreator {

    private final List<FlywayConfigurationCustomizer> conifguration;

    public CoreFlywayCreator(@All List<FlywayConfigurationCustomizer> flywayConfigurationCustomizers) {
        this.conifguration = flywayConfigurationCustomizers;
    }

    public Flyway createFlyway(DataSource dataSource) {
        return createFlyway(dataSource, null);
    }

    public Flyway createFlyway(DataSource dataSource, @Nullable String logicalDbName) {
        FluentConfiguration configure = Flyway.configure();
        conifguration.forEach(v -> v.customize(configure, logicalDbName));
        configure.dataSource(dataSource);
        return configure.load();
    }
}
