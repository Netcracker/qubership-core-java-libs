package com.netcracker.cloud.dbaas.client.arangodb.management;

import com.arangodb.ArangoDB;
import com.arangodb.serde.ArangoSerde;
import com.netcracker.cloud.dbaas.client.arangodb.configuration.DbaasArangoDBClientProperties;
import com.netcracker.cloud.dbaas.client.arangodb.entity.connection.ArangoConnection;
import com.netcracker.cloud.dbaas.client.arangodb.entity.database.ArangoDatabase;
import com.netcracker.cloud.dbaas.client.management.PostConnectProcessor;
import com.netcracker.cloud.security.core.utils.tls.TlsUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;

@Slf4j
@Order(1)
@AllArgsConstructor
public class ArangoPostProcessor implements PostConnectProcessor<ArangoDatabase> {

    DbaasArangoDBClientProperties dbaasArangoDBClientProperties;

    private static final int ARANGODB_TLS_PORT = 8530;

    public void process(ArangoDatabase database) {
        log.info("Starting creating ArangoOperations for database: {}", database.getName());
        ArangoConnection connectionProperties = database.getConnectionProperties();
        String password = connectionProperties.getPassword();
        ArangoDB.Builder arangoDBBuilder = loadPropertiesFromConfig(new ArangoDB.Builder())
                .host(connectionProperties.getHost(), connectionProperties.getPort())
                .user(connectionProperties.getUsername())
                .password(password);
        ArangoSerde arangoSerde = dbaasArangoDBClientProperties.getArangoSerde();
        if (arangoSerde != null) {
            arangoDBBuilder.serde(arangoSerde);
        }
        if (connectionProperties.isTls()) {
            log.info("Connection to arangodb will be secured");
            arangoDBBuilder
                    .useSsl(true)
                    .sslContext(TlsUtils.getSslContext())
                    .host(connectionProperties.getHost(), ARANGODB_TLS_PORT);
        }

        ArangoDB arangoDB = arangoDBBuilder.build();
        connectionProperties.setArangoDatabase(arangoDB.db(connectionProperties.getDbName()));
        log.info("Finished creating ArangoOperations for database: {}", database.getName());
    }

    @Override
    public Class<ArangoDatabase> getSupportedDatabaseType() {
        return ArangoDatabase.class;
    }

    ArangoDB.Builder loadPropertiesFromConfig(ArangoDB.Builder builder) {
        return builder.loadProperties(dbaasArangoDBClientProperties.getProperties().asArangoConfigProperties());
    }
}
