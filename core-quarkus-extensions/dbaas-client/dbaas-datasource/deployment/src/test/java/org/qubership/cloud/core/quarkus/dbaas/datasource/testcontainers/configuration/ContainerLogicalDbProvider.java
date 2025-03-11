package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.service.PostgresqlLogicalDbProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class ContainerLogicalDbProvider extends PostgresqlLogicalDbProvider {
    @NonNull
    private final PostgreSQLContainer container;

    private final Map<DbaasDbClassifier, PostgresDatabase> createdDatabases = new ConcurrentHashMap<>();

    private PostgresDBConnection constructDbConnection() {
        String username = "dbaas" + UUID.randomUUID().toString().substring(0, 8);
        String password = "dbaas" + UUID.randomUUID().toString().substring(0, 8);
        String role = "dbaas_role" + UUID.randomUUID().toString().substring(0, 8);
        String dbName = username;
        String jdbcUrlWithoutDbName = container.getJdbcUrl().substring(0, container.getJdbcUrl().lastIndexOf("/"));
        String userJdbcUrl = jdbcUrlWithoutDbName + "/" + dbName;

        return new PostgresDBConnection(userJdbcUrl, username, password, role);
    }

    public void forEachCreatedDatabase(BiConsumer<DbaasDbClassifier, PostgresDatabase> operation) {
        createdDatabases.forEach(operation);
    }

    private void createUserAndDatabase(String username, String dbName, String password) {
        log.info("Start creation user={} and it's associated database={}", username, dbName);
        try {
            executeSqlViaPsql(String.format("CREATE USER %s WITH ENCRYPTED PASSWORD '%s'", username, password));
            executeSqlViaPsql(String.format("CREATE DATABASE %s", dbName));
            executeSqlViaPsql(String.format("GRANT ALL PRIVILEGES ON DATABASE %s TO %s", dbName, username));
        } catch(Exception e) {
            log.error("Cannot execute commands in container: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("User={} successfully created with all rights to database={}", username, dbName);
    }

    private void executeSqlViaPsql(String sqlCommand) throws UnsupportedOperationException, IOException, InterruptedException {
        Container.ExecResult result = container.execInContainer("psql", "-U", container.getUsername(), "-c", sqlCommand);
        if (result.getExitCode() != 0) {
            throw new CannotExecutePsqlException(result, sqlCommand);
        }
    }


    @Override
    public PostgresDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig config, String namespace) {
        return createdDatabases.computeIfAbsent(new DbaasDbClassifier(classifier), (clf) -> {
            PostgresDatabase db = new PostgresDatabase();
            db.setClassifier(classifier);
            db.setNamespace(namespace);
            PostgresDBConnection conn = constructDbConnection();
            db.setName(conn.getUsername());
            createUserAndDatabase(conn.getUsername(), db.getName(), conn.getPassword());
            db.setConnectionProperties(conn);
            return db;
        });
    }

    @Override
    public @Nullable PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        PostgresDBConnection postgresDBConnection = constructDbConnection();
        return new PostgresConnectionProperty(postgresDBConnection.getUrl(), postgresDBConnection.getUsername(), postgresDBConnection.getPassword(), postgresDBConnection.getRole(), postgresDBConnection.isTls());
    }

    @Getter
    private static final class CannotExecutePsqlException extends RuntimeException {
        private final Container.ExecResult execResult;
        private final String sqlCommand;

        public CannotExecutePsqlException(Container.ExecResult execResult, String sqlCommand) {
            super("Cannot execute " + sqlCommand + ". ExecResult: " + execResult.toString());
            this.execResult = execResult;
            this.sqlCommand = sqlCommand;
        }
    }
}
