package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;

public interface DatabaseClientCreator<D extends AbstractDatabase<?>, T extends AbstractConnectorSettings> extends SupportedDatabaseType<D> {
    void create(D database);
    void create(D database, T settings);
}
