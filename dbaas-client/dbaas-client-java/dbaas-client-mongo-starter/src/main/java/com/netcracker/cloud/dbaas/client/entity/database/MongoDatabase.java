package com.netcracker.cloud.dbaas.client.entity.database;

import com.netcracker.cloud.dbaas.client.entity.connection.MongoDBConnection;

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
