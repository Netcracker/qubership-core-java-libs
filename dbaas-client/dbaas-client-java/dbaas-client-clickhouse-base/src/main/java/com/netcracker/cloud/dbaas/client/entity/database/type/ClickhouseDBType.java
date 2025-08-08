package com.netcracker.cloud.dbaas.client.entity.database.type;

import com.netcracker.cloud.dbaas.client.entity.connection.ClickhouseConnection;
import com.netcracker.cloud.dbaas.client.entity.database.ClickhouseDatabase;

import static com.netcracker.cloud.dbaas.client.entity.database.type.PhysicalDbType.CLICKHOUSE;

public class ClickhouseDBType extends DatabaseType<ClickhouseConnection, ClickhouseDatabase> {

    public static final ClickhouseDBType INSTANCE = new ClickhouseDBType(ClickhouseDatabase.class);

    private ClickhouseDBType(Class<? extends ClickhouseDatabase> databaseClass) {
        super(CLICKHOUSE, databaseClass);
    }

}
