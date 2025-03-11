package org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database;

import org.qubership.cloud.dbaas.client.entity.database.AbstractDatabase;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MongoDatabase extends AbstractDatabase<MongoDBConnection> {
}
