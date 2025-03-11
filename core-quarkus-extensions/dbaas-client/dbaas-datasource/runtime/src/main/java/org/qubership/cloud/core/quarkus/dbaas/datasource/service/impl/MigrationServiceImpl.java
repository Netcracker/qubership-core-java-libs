package org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl;

import org.qubership.cloud.core.quarkus.dbaas.datasource.config.flyway.CoreFlywayCreator;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.MigrationService;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;

@ApplicationScoped
public class MigrationServiceImpl implements MigrationService {

    private static final Logger log = Logger.getLogger(MigrationServiceImpl.class);

    @Inject
    CoreFlywayCreator coreFlywayCreator;

    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public int migrate(DataSource dataSource) {
        return migrate(dataSource, null);
    }

    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public int migrate(DataSource dataSource, @Nullable String logicalDbName) {
        String dsName = StringUtil.isNullOrEmpty(logicalDbName) ? "default datasource" : logicalDbName;
        log.debug("Core flyway: run run time migration for " + dsName);
        Flyway flyway = coreFlywayCreator.createFlyway(dataSource, logicalDbName);
        return flyway.migrate().migrationsExecuted;
    }
}
