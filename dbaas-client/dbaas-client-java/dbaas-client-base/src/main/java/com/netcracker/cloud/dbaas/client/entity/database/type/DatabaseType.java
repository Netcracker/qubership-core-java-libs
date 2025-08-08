package com.netcracker.cloud.dbaas.client.entity.database.type;


import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * The class used to invoke the API of {@link org.qubership.cloud.dbaas.client.DbaasClient}
 * which can operate with any type of database known to dbaas running behind this client
 *
 * usage example:
 *
 * <pre>{@code
 *  DatabaseType<Map<String, Object>, Database> oracleType = new DatabaseType<>("oracle", Database.class);
 *  Database oracleDb = dbaasClient.createDatabase(oracleType, namespace, classifier);
 *  Map<String, Object> oracleConnection = dbaasClient.getConnection(oracleType, namespace, classifier);
 *  dbaasClient.deleteDatabase(oracleDb);
 *  }</pre>
 *
 * @param <T> the type of the connectionProperties field of the D type.
 *           This type will be used while parsing responses from dbaas
 * @param <D> the type of the database entity used during parsing responses from dbaas
 */

@EqualsAndHashCode
@ToString
public class DatabaseType<T, D extends AbstractDatabase<T>> {

    private String name;
    private Class<? extends D> databaseClass;

    public DatabaseType(String name, Class<? extends D> databaseClass) {
        this.name = name;
        this.databaseClass = databaseClass;
    }

    public String getName() {
        return name;
    }

    public Class<? extends D> getDatabaseClass() {
        return this.databaseClass;
    }

}
