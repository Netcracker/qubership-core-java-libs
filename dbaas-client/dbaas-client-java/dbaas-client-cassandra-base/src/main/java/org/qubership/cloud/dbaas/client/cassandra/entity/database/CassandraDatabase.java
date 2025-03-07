package org.qubership.cloud.dbaas.client.cassandra.entity.database;

import org.qubership.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CassandraDatabase extends AbstractDatabase<CassandraDBConnection> {
}
