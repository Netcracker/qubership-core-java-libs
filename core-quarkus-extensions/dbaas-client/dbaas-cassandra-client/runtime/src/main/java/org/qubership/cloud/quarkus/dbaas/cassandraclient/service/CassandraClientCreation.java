package org.qubership.cloud.quarkus.dbaas.cassandraclient.service;

import org.qubership.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

public interface CassandraClientCreation {
    CassandraDatabase getOrCreateCassandraDatabase(DbaasDbClassifier classifier);
}
