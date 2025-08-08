package org.qubership.cloud.dbaas.client.entity.database.type;

public class PhysicalDbType {

    private PhysicalDbType() {
    }

    public static final String POSTGRESQL = "postgresql";
    public static final String CASSANDRA = "cassandra";
    public static final String MONGODB = "mongodb";
    public static final String ARANGODB = "arangodb";
    public static final String OPENSEARCH = "opensearch";
    public static final String REDIS = "redis";
    public static final String CLICKHOUSE = "clickhouse";

}
