package org.qubership.cloud.quarkus.dbaas.mongoclient.service;

import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;

public interface MongoClientCreation {
    MongoDatabase getOrCreateMongoDatabase(DbaasDbClassifier classifier);
}
