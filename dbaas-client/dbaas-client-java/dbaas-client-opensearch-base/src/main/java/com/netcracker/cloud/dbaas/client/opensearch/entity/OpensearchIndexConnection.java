package com.netcracker.cloud.dbaas.client.opensearch.entity;

import com.netcracker.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;
import org.opensearch.client.opensearch.OpenSearchClient;

@Data
@ToString(of = {"resourcePrefix", "host", "port"})
public class OpensearchIndexConnection extends DatabaseConnection {

    private String host;
    private int port;
    private String resourcePrefix;

    @Setter(AccessLevel.NONE)
    private OpenSearchClient openSearchClient;

    public void setOpenSearchClient(OpenSearchClient openSearchClient) {
        this.openSearchClient = openSearchClient;
    }

    public void close() throws Exception {
        if (openSearchClient._transport() != null) {
            openSearchClient._transport().close();
        }
    }
}
