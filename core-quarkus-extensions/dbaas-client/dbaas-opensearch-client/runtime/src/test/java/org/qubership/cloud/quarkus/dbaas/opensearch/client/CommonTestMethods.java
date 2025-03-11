package org.qubership.cloud.quarkus.dbaas.opensearch.client;

import org.qubership.cloud.dbaas.client.opensearch.entity.OpensearchIndexConnection;
import org.qubership.cloud.dbaas.client.opensearch.service.OpensearchLogicalDbProvider;

public class CommonTestMethods {
    public static OpensearchIndexConnection prepareOpensearchConnection() {
        OpensearchIndexConnection opensearchIndexConnection = new OpensearchIndexConnection();
        opensearchIndexConnection.setPort(9200);
        opensearchIndexConnection.setUsername("test_user");
        opensearchIndexConnection.setPassword("test_password");
        opensearchIndexConnection.setResourcePrefix("test_prefix");
        return opensearchIndexConnection;
    }

    public static OpensearchLogicalDbProvider.OpensearchConnectionProperty prepareOpensearchConnectionProperty() {
        OpensearchLogicalDbProvider.OpensearchConnectionProperty opensearchIndexConnection =
                new OpensearchLogicalDbProvider.OpensearchConnectionProperty(
                        null,
                        "test_user",
                        "test_password",
                        null,
                        null,
                        9200
                );
        opensearchIndexConnection.setRole("admin");
        return opensearchIndexConnection;
    }


}
