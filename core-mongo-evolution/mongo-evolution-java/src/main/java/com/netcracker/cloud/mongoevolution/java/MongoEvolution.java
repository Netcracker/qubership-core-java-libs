package com.netcracker.cloud.mongoevolution.java;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.annotation.DBManagerEntity;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class MongoEvolution extends AbstractMongoEvolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoEvolution.class);

    private MongoClient client;
    private String dbName;
    private ConnectionSearchKey connectionSearchKey;

    public MongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        super(client, dbName, connectionSearchKey);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }

    // Constructor with custom configurable parameters
    public MongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                          long waitTimeSecondsForUpdate, long waitTimeMillisecWithinUpdate,
                          long waitTimeForUpdateStatusTask, long delayStatusTask) {
        super(client, dbName, connectionSearchKey, waitTimeSecondsForUpdate, waitTimeMillisecWithinUpdate,
                waitTimeForUpdateStatusTask, delayStatusTask);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }


    public void evolve(String changeLogsScanPackage) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage));
    }

    public void evolve(String changeLogsScanPackage, DBManagerEntity dbManagerEntity) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), dbManagerEntity);
    }

    public void evolve(List<String> changeLogsScanPackages) throws Exception {
        evolve(changeLogsScanPackages, null);
    }

    public void evolve(List<String> changeLogsScanPackages, DBManagerEntity dbManagerEntity) throws Exception {
        AnnotationProcessor processor = new AnnotationProcessor(client, dbName, connectionSearchKey, changeLogsScanPackages, dbManagerEntity);
        doEvolve(processor);
    }
}
