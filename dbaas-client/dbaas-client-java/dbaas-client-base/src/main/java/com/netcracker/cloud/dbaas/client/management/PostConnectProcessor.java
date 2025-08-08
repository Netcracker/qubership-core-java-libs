package com.netcracker.cloud.dbaas.client.management;


import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;

/**
 * PostProcessor to perform additional work for particular type of databases
 *
 * @param <T> the type of the database this processor is applicable to
 */
public interface PostConnectProcessor<T extends AbstractDatabase> extends SupportedDatabaseType<T> {

    /**
     * @param database to perform additional post processing
     */
    void process(T database);

}
