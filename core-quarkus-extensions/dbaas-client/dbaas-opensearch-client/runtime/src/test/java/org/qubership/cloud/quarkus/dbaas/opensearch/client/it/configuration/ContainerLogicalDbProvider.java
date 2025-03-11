package org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration;

import org.qubership.cloud.dbaas.client.management.DatabaseConfig;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration.OpensearchContainerResource.OPENSEARCH_PORT;

@Slf4j
@RequiredArgsConstructor
public class ContainerLogicalDbProvider extends OpensearchLogicalDbProvider {

    public static String TEST_PREFIX = "test";
    public static String TEST_INDEX = "opensearch_index";
    public static String ADMIN_ROLE = "admin";
    public static String TEST_FULL_INDEX_NAME = TEST_PREFIX + "_" + TEST_INDEX;
    public static String TEST_ALIAS = "openserch_alias";

    public static String TEST_TEMPLATE = "opensearch_template";

    @NonNull
    private final GenericContainer container;

    private final Map<DbaasDbClassifier, OpensearchIndex> createdDatabases = new ConcurrentHashMap<>();


    private OpensearchIndexConnection constructDbConnection(OpensearchIndex db, String tenantId) {
        String username = "admin";
        String password = "admin";
        String dbName = TEST_INDEX;
        db.setName(dbName);
        OpensearchIndexConnection connection = new OpensearchIndexConnection();
        String httpHostAddress = container.getHost() + ":" + container.getMappedPort(OPENSEARCH_PORT);
        String host = container.getHost();
        Integer port = container.getMappedPort(OPENSEARCH_PORT);
        connection.setHost(host);
        connection.setPort(port);
        connection.setUrl("http://" + httpHostAddress);
        String prefix = tenantId != null ? tenantId : TEST_PREFIX;
        connection.setResourcePrefix(prefix);
        connection.setUsername(username);
        connection.setPassword(password);

        return connection;
    }


    @Override
    public @Nullable OpensearchConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        String username = "admin";
        String password = "admin";
        String httpHostAddress = container.getHost() + ":" + container.getMappedPort(OPENSEARCH_PORT);
        String host = container.getHost();
        Integer port = container.getMappedPort(OPENSEARCH_PORT);
        String tenantId = (String) classifier.get("tenantId");
        String prefix = params.getDbNamePrefix() != null ? params.getDbNamePrefix() : (tenantId != null ? tenantId : TEST_PREFIX);
        String role = params.getUserRole() != null ? params.getUserRole() : ADMIN_ROLE;

        OpensearchConnectionProperty connection = new OpensearchConnectionProperty(
                "http://" + httpHostAddress,
                username,
                password,
                host,
                prefix,
                port
        );
        connection.setRole(role);

        return connection;
    }

}
