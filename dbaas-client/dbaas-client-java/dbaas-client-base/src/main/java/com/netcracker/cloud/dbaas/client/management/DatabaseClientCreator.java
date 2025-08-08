package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;

public interface DatabaseClientCreator<D extends AbstractDatabase<?>, T extends AbstractConnectorSettings> extends SupportedDatabaseType<D> {
    void create(D database);
    void create(D database, T settings);
}
