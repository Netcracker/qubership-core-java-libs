package com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.connection.MongoDBConnection;
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
