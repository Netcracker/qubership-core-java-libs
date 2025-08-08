package com.netcracker.cloud.dbaas.client;


import com.netcracker.cloud.dbaas.client.entity.PhysicalDatabases;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.DatabaseType;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasException;
import com.netcracker.cloud.dbaas.client.exceptions.DbaasUnavailableException;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Abstracts interaction with dbaas service
 */
public interface DbaasClient {

    /**
     * Creates logical database with provided type and classifier in particular
     * namespace with random dbName, username and password.
     * <p>
     * Logical database would be created in a default physical database cluster,
     * if database with same classifier and type was not created earlier. In that case
     * method would return already created database.
     * <p>
     * Exception may be thrown in case if supplied physical database identifier differs
     * from the one used during first request of database or during registration of database in
     * DbaaS aggregator in case if migration of database is not supported.
     *databaseConfig –
     * @param type               the type of the database supported by dbaas
     * @param namespace          the namespace the db will be created in
     * @param classifier         the map of parameters used to uniquely identify the database
     * @param databaseConfig     the object with some database parameters for database creation such as
     *                           dbNamePrefix, backupDisabled, databaseSettings, physicalDatabaseId
     * @param <T>                the java type of the Database connectionProperties field
     * @param <D>                the java type of the Database entity
     * @return Database entity
     * @throws DbaasException            thrown in case any error happens not related to dbaas' unavailability
     * @throws DbaasUnavailableException thrown when dbaas gets unavailable
     */
    <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(final DatabaseType<T, D> type,
                                                             final String namespace,
                                                             final Map<String, Object> classifier,
                                                             final DatabaseConfig databaseConfig)
            throws DbaasException, DbaasUnavailableException;

    /**
     * Creates Database with provided type and classifier in particular namespace with random dbName, username and password
     *
     * @param type       the type of the database supported by dbaas
     * @param namespace  the namespace the db will be created in
     * @param classifier the map of parameters used to uniquely identify the database
     * @param <T>        the java type of the Database connectionProperties field
     * @param <D>        the java type of the Database entity
     * @return Database entity
     * @throws DbaasException            thrown in case any error happens not related to dbaas' unavailability
     * @throws DbaasUnavailableException thrown when dbaas gets unavailable
     */
     <T, D extends AbstractDatabase<T>> D getOrCreateDatabase(final DatabaseType<T, D> type,
                                                             final String namespace,
                                                             final Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException;


    /**
     * Searches for the database by its type, namespace and classifier in particular namespace.
     * <p>
     * This API does not create database if it does not exist
     *
     * @param type       the type of the database supported by dbaas
     * @param namespace  the namespace the database will be created in
     * @param classifier the map of parameters used to uniquely identify the database
     * @param userRole   indicates which user's connection properties must be in returned object
     * @param <T>        the java type of the Database connectionProperties field
     * @param <D>        the java type of the Database entity
     * @return Database entity if Database has been created early, otherwise API returns NULL
     * @throws DbaasException                thrown in case any error happens not related to dbaas' unavailability
     * @throws DbaasUnavailableException     thrown when dbaas gets unavailable
     */

    @Nullable
     <T, D extends AbstractDatabase<T>> D getDatabase(final DatabaseType<T, D> type,
                                                     final String namespace,
                                                     final String userRole,
                                                     final Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException;

    /**
     * Searches for the database by its type, namespace and classifier in particular namespace.
     * <p>
     * This API does not create database if it does not exist
     *
     * @param type       the type of the database supported by dbaas
     * @param namespace  the namespace the database will be created in
     * @param userRole   indicates which user's connection properties must be in returned object
     * @param classifier the map of parameters used to uniquely identify the database
     * @param <T>        the java type of the Database connectionProperties field
     * @param <D>        the java type of the Database entity
     * @return Database connection if Database has been created early, otherwise API returns NULL
     * @throws DbaasException                thrown in case any error happens not related to dbaas' unavailability
     * @throws DbaasUnavailableException     thrown when dbaas gets unavailable
     */

    @Nullable
     <T, D extends AbstractDatabase<T>> T getConnection(final DatabaseType<T, D> type,
                                                                                  final String namespace,
                                                                                  final String userRole,
                                                                                  final Map<String, Object> classifier) throws DbaasException, DbaasUnavailableException;

    PhysicalDatabases getPhysicalDatabases(final String type)
            throws DbaasException, DbaasUnavailableException;
}
