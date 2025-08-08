package com.netcracker.cloud.dbaas.client.opensearch;

import org.qubership.cloud.dbaas.client.entity.database.DatabaseSettings;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.opensearch.config.OpensearchConfig;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDBType;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchDatabaseSettings;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Objects;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;

@Slf4j
@AllArgsConstructor
public class DbaasOpensearchClientImpl extends AbstractDbaasOpensearchClient {

    private final DatabasePool databasePool;
    private final DbaaSClassifierBuilder dbaaSClassifierBuilder;
    private final DatabaseConfig.Builder databaseConfigBuilder;
    private final OpensearchConfig opensearchConfig;

    @Override
    public String getDelimiter() {
        return opensearchConfig.getDelimiter();
    }

    @Override
    public OpensearchIndexConnection getOrCreateIndex() {
        return getOrCreateIndex(databaseConfigBuilder.build());
    }

    @SneakyThrows
    @Override
    public OpensearchIndexConnection getOrCreateIndex(DatabaseConfig databaseConfig) {
        log.trace("get or creat opensearch connection with databaseConfig={}", databaseConfig);
        Objects.requireNonNull(databaseConfig, "databaseConfig can't be null");

        DbaasDbClassifier dbaasDbClassifier = dbaaSClassifierBuilder.build();

        databaseConfig.setDbNamePrefix(getDatabasePrefix(databaseConfig, dbaasDbClassifier));
        if (databaseConfig.getUserRole() == null && opensearchConfig.getOpensearchProperties() != null
                && opensearchConfig.getOpensearchProperties().getRuntimeUserRole() != null) {
            databaseConfig.setUserRole(opensearchConfig.getOpensearchProperties().getRuntimeUserRole());
        }
        log.trace("opensearch connection prefix = {}", databaseConfig.getDbNamePrefix());

        DatabaseType<OpensearchIndexConnection, OpensearchIndex> type = OpensearchDBType.INSTANCE;
        databaseConfig.setDatabaseSettings(updateDatabaseSettings(databaseConfig.getDatabaseSettings()));

        return withPasswordCheck(
                () -> databasePool.getOrCreateDatabase(type, dbaasDbClassifier, databaseConfig)
                        .getConnectionProperties(),
                () -> databasePool.removeCachedDatabase(type, dbaasDbClassifier)
        );
    }

    private String getDatabasePrefix(DatabaseConfig databaseConfig, DbaasDbClassifier dbaasDbClassifier) {
        if (opensearchConfig.getOpensearchProperties() != null && databaseConfig.getDbNamePrefix() == null) {
            if (opensearchConfig.getOpensearchProperties().getTenant().getPrefix() != null && dbaasDbClassifier.asMap().get(SCOPE) == TENANT) {
                return opensearchConfig.getOpensearchProperties().getTenant().getPrefix()
                        .replace("{tenantId}", (String) dbaasDbClassifier.asMap().get(TENANT_ID));

            } else if (opensearchConfig.getOpensearchProperties().getService().getPrefix() != null) {
                return opensearchConfig.getOpensearchProperties().getService().getPrefix();
            }
        }
        return databaseConfig.getDbNamePrefix();
    }

    private OpensearchDatabaseSettings updateDatabaseSettings(DatabaseSettings databaseSettings) {
        OpensearchDatabaseSettings settings = (OpensearchDatabaseSettings) databaseSettings;
        if (settings == null) {
            settings = new OpensearchDatabaseSettings();
        }
        settings.setResourcePrefix(true);
        settings.setCreateOnly(Collections.singletonList("user"));
        return settings;
    }
}
