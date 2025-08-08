package org.qubership.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import org.qubership.cloud.mongoevolution.java.AbstractMongoEvolution;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpringMongoEvolution extends AbstractMongoEvolution {

    private MongoClient client;
    private String dbName;
    private ConnectionSearchKey connectionSearchKey;

    public SpringMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        super(client, dbName, connectionSearchKey);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }

    // Constructor with custom configurable parameters
    public SpringMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                                long waitTimeSecondsForUpdate, long waitTimeMillisecWithinUpdate,
                                long waitTimeForUpdateStatusTask, long delayStatusTask) {
        super(client, dbName, connectionSearchKey, waitTimeSecondsForUpdate, waitTimeMillisecWithinUpdate,
                waitTimeForUpdateStatusTask, delayStatusTask);
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
    }

    public void evolve(String changeLogsScanPackage, Environment environment) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), environment);
    }

    public void evolve(String changeLogsScanPackage, Environment environment, SpringDBManagerEntity springDBManagerEntity) throws Exception {
        evolve(Collections.singletonList(changeLogsScanPackage), environment, springDBManagerEntity);
    }

    public void evolve(String changeLogsScanBasePackage, Environment environment, Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(Collections.singletonList(changeLogsScanBasePackage), environment, classNamesAndBeans);
    }

    public void evolve(String changeLogsScanBasePackage, Environment environment, SpringDBManagerEntity springDBManagerEntity, Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(Collections.singletonList(changeLogsScanBasePackage), environment, springDBManagerEntity, classNamesAndBeans);
    }

    public void evolve(List<String> changeLogsScanPackages, Environment environment) throws Exception {
        evolve(changeLogsScanPackages, environment, null, null);
    }

    public void evolve(List<String> changeLogsScanPackages, Environment environment, SpringDBManagerEntity springDBManagerEntity) throws Exception {
        evolve(changeLogsScanPackages, environment, springDBManagerEntity, null);
    }

    public void evolve(List<String> changeLogsScanPackages, Environment environment, Map<String, Object> classNamesAndBeans) throws Exception {
        evolve(changeLogsScanPackages, environment, null, classNamesAndBeans);
    }

    public void evolve(List<String> changeLogsScanPackages, Environment environment, SpringDBManagerEntity springDBManagerEntity, Map<String, Object> classNamesAndBeans) throws Exception {
        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(client, dbName, connectionSearchKey, changeLogsScanPackages, environment, new SpringDBManagerEntity(), classNamesAndBeans);
        doEvolve(processor);
    }
}
