package org.qubership.cloud.quarkus.dbaas.cassandraclient.service.impl;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.cassandra.service.CassandraLogicalDbProvider;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.common.postprocessor.PostConnectProcessorManager;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraDbConfiguration;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.config.properties.CassandraProperties;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CassandraClientCreation;
import org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class CassandraClientCreationImpl implements CassandraClientCreation {
    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @Inject
    CassandraProperties cassandraProperties;
    @Inject
    Instance<CassandraLogicalDbProvider> dbProviders;
    @Inject
    CqlSessionCreator cqlSessionCreator;
    @Inject
    PostConnectProcessorManager<CassandraDatabase> postConnectProcessorManager;

    private final Map<DbaasDbClassifier, CassandraDatabase> cassandraDbMap = new ConcurrentHashMap<>();

    @Override
    public CassandraDatabase getOrCreateCassandraDatabase(DbaasDbClassifier classifier) {
        log.trace("Create new cassandra database for {}", classifier);
        return cassandraDbMap.computeIfAbsent(classifier, this::createCassandraDatabase);
    }

    private CassandraDatabase createCassandraDatabase(DbaasDbClassifier dbaasDbClassifier) {
        Map<String, Object> classifier = dbaasDbClassifier.asMap();
        log.debug("Create new CassandraClient for {}", classifier);

        CassandraDatabase db = getDatabase(classifier);
        log.debug("Connection: " + db.getConnectionProperties());

        log.debug("Starting the initialization of CassandraClient for database with classifier: {}", db.getClassifier());
        CassandraDBConnection connectionProperties = db.getConnectionProperties();

        CqlSession session = cqlSessionCreator.createSession(db);

        log.info("Created cassandra client: {}. Contact points: {}.", session, connectionProperties.getContactPoints());
        connectionProperties.setSession(session);
        postConnectProcessorManager.applyPostProcessors(db);
        return db;
    }

    private CassandraDatabase getDatabase(Map<String, Object> dbClassifier) {
        TreeMap<String, Object> classifier = new TreeMap<>(dbClassifier);
        DatabaseConfig config = getDatabaseConfig(classifier);
        for (CassandraLogicalDbProvider logicalDbProvider : sortProviders(dbProviders)) {
            CassandraDatabase database = logicalDbProvider.provide(classifier, config, namespace);
            if (database != null) {
                if (database.getConnectionProperties() == null) {
                    throw new IllegalStateException("Provider: " + logicalDbProvider + "have provided postgresql database " +
                            "but connection properties is null");
                }
                return database;
            }
        }
        throw new IllegalStateException("Not one of the providers: " + dbProviders + " could provide a logical Cassandra database");
    }

    private DatabaseConfig getDatabaseConfig(Map<String, Object> classifier) {
        log.debug("Create DbParameters for database with classifier {}", classifier);
        String tenantId = (String) classifier.get("tenantId");
        CassandraDbConfiguration dbConfiguration = cassandraProperties.getCassandraDbCreationConfig().getCassandraDbConfiguration(tenantId);
        DatabaseConfig.Builder config = DatabaseConfig.builder();
        if (dbConfiguration != null) {
            config.physicalDatabaseId(dbConfiguration.getPhysicalDatabaseId().orElse(null));
        }
        config.dbNamePrefix(cassandraProperties.getCassandraDbCreationConfig().
                dbaasApiPropertiesConfig.getDbaaseApiProperties().getDbPrefix());
        config.userRole(cassandraProperties.getCassandraDbCreationConfig().
                dbaasApiPropertiesConfig.getDbaaseApiProperties().getRuntimeUserRole());
        return config.build();
    }

    private List<CassandraLogicalDbProvider> sortProviders(Instance<CassandraLogicalDbProvider> dbProviders) {
        return dbProviders.stream().sorted(Comparator.comparingInt(CassandraLogicalDbProvider::order)).collect(Collectors.toList());
    }
}
