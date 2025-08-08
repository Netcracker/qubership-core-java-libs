package com.netcracker.cloud.dbaas.client.it.postgresql;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.management.PostConnectProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Slf4j
@Component
public class DataInitializePostConnectProcessor implements PostConnectProcessor<PostgresDatabase> {
    @Override
    public void process(PostgresDatabase postgresDatabase)  {
        try {
        postgresDatabase.getConnectionProperties().getDataSource().getConnection().prepareStatement(
                "CREATE TABLE IF NOT EXISTS Customer (id int8 not null, firstName varchar(255) not null, lastName varchar(255) not null, primary key (id))"
        ).execute();
            postgresDatabase.getConnectionProperties().getDataSource().getConnection().prepareStatement(
                    "create sequence customer_seq start with 1 increment by 50"
            ).execute();

            log.debug("Customer table has been created");
        } catch (SQLException sqlExp) {
            log.error(sqlExp.getMessage());
        }
    }

    @Override
    public Class<PostgresDatabase> getSupportedDatabaseType() {
        return PostgresDatabase.class;
    }
}

