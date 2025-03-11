package org.qubership.cloud.quarkus.dbaas.mongoclient.config;

import org.qubership.cloud.quarkus.dbaas.mongoclient.DbaaSMongoClient;
import org.qubership.cloud.quarkus.dbaas.mongoclient.classifier.ServiceClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.mongoclient.classifier.TenantClassifierBuilder;
import org.qubership.cloud.quarkus.dbaas.mongoclient.service.MongoClientCreation;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class MongoClientConfiguration {
    public static final String SERVICE_MONGO_CLIENT = "serviceMongoClient";
    public static final String TENANT_MONGO_CLIENT = "tenantMongoClient";

    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String namespace;

    @ConfigProperty(name = "quarkus.dbaas.mongodb.db-classifier", defaultValue = "default")
    String dbClassifierField;

    @Produces
    @Named(SERVICE_MONGO_CLIENT)
    @DefaultBean
    public DbaaSMongoClient getDbaaSServiceMongoClient(@NotNull MongoClientCreation mongoClientCreation) {
        return new DbaaSMongoClient(new ServiceClassifierBuilder(getInitialClassifierMap()), mongoClientCreation);
    }

    @Produces
    @Named(TENANT_MONGO_CLIENT)
    @DefaultBean
    public DbaaSMongoClient getDbaaSTenantMongoClient(@NotNull MongoClientCreation mongoClientCreation) {
        return new DbaaSMongoClient(new TenantClassifierBuilder(getInitialClassifierMap()), mongoClientCreation);
    }

    private Map<String, Object> getInitialClassifierMap() {
        Map<String, Object> params = new HashMap<>();
        params.put("microserviceName", microserviceName);
        params.put("namespace", namespace);
        params.put("dbClassifier", dbClassifierField);
        return params;
    }

}
