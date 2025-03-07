package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;

public interface DatabaseDefinitionProcessor<D extends AbstractDatabase<?>> extends SupportedDatabaseType<D>  {

    /**
     * @param database to perform additional post processing for
     */
    void process(D database);
}