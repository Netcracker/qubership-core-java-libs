package com.netcracker.cloud.quarkus.dbaas.cassandraclient.service;

import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;

public interface CassandraClientCreation {
    CassandraDatabase getOrCreateCassandraDatabase(DbaasDbClassifier classifier);
}
