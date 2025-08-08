package com.netcracker.cloud.dbaas.client.entity.database;


import org.qubership.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PostgresDatabase extends AbstractDatabase<PostgresDBConnection> {
}
