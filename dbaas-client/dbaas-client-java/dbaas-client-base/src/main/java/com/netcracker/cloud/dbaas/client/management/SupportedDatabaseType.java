package com.netcracker.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;

public interface SupportedDatabaseType<T extends AbstractDatabase> {
    /**
     * @return the Class of the database this processor is applicable to
     */
    Class<T> getSupportedDatabaseType();
}
