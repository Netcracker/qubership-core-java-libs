package com.netcracker.cloud.quarkus.dbaas.mongoclient.service;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;

import java.util.SortedMap;

/**
 * SPI for resolving a {@link MongoDatabase} for a given classifier. Implementations are CDI beans
 * consulted as an ordered chain by {@code MongoClientCreationImpl}: they are sorted by
 * {@link #order()} ascending and the first non-null {@link #provide} result wins. The framework
 * ships two providers — the file-backed mounted-secret provider
 * ({@code DbaaSMongoMountedSecretLogicalDbProvider}, {@code order()=Integer.MAX_VALUE - 1}) and the
 * dbaas-aggregator provider ({@code DbaaSMongoLogicalDbProvider}, {@code order()=Integer.MAX_VALUE}).
 * A microservice may register its own provider with a lower {@code order()} to take precedence.
 *
 * <p>Unlike the SQL/opensearch drivers this is a standalone provider rather than a
 * {@code LogicalDbProvider<C, D>}: mongo resolves a whole {@link MongoDatabase} (the dbaas client
 * returns the full database) instead of a connection property that is then assembled, so the
 * connection-property seam of {@code LogicalDbProvider} does not apply here.
 */
public abstract class MongoLogicalDbProvider {

    protected static final String TYPE = "mongodb";

    /**
     * @return the resolved database, or {@code null} to let the chain fall through to the next provider.
     */
    public abstract MongoDatabase provide(SortedMap<String, Object> classifier, DatabaseConfig params, String namespace);

    public int order() {
        return 0;
    }
}
