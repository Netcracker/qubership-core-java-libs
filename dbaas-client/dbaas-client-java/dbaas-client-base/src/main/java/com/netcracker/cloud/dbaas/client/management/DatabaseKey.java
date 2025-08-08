package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.DatabaseType;
import lombok.Value;

import java.util.Map;

/**
 * Key that describes database, consists of the database type and the database classifier.
 * This key is used in caches inside {@link DatabasePool} implementations.
 *
 * @param <T> the database connection properties type.
 * @param <D> the database type.
 */
@Value
public class DatabaseKey <T, D extends AbstractDatabase<T>> {
    private DatabaseType<T, D> dbType;
    private Map<String, Object> classifier;
    private String discriminator;
}
