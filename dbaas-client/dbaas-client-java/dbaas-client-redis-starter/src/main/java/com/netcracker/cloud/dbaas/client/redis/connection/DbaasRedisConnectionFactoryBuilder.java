package com.netcracker.cloud.dbaas.client.redis.connection;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DbaasRedisConnectionFactoryBuilder {

    private DatabasePool databasePool;

    public Builder newBuilder(DbaaSChainClassifierBuilder classifierBuilder) {
        return new Builder(databasePool, classifierBuilder);
    }

    public static class Builder {
        private DatabasePool databasePool;
        private DbaaSChainClassifierBuilder classifierBuilder;
        private DatabaseConfig databaseConfig;

        private Builder(DatabasePool databasePool, DbaaSChainClassifierBuilder classifierBuilder) {
            this.databasePool = databasePool;
            this.classifierBuilder = classifierBuilder;
        }

        public Builder withDatabaseConfig(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
            return this;
        }

        public DbaasRedisConnectionFactory build() {
            return new DbaasRedisConnectionFactory(databasePool, classifierBuilder, databaseConfig);
        }
    }

}
