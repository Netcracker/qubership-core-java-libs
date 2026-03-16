package com.netcracker.cloud.quarkus.dbaas.mongoclient.service;

import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;

public interface MongoClientCreation {
    MongoDatabase getOrCreateMongoDatabase(DbaasDbClassifier classifier);
}
