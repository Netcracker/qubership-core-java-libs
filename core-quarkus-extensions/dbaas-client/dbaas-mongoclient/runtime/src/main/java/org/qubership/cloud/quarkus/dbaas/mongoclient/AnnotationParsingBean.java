package org.qubership.cloud.quarkus.dbaas.mongoclient;

import lombok.Data;

import java.util.List;

@Data
public class AnnotationParsingBean {
    List<String> serviceDatabases;
    List<String> tenantDatabases;

    public AnnotationParsingBean(List<String> serviceDatabases, List<String> tenantDatabases) {
        System.setProperty("quarkus.mongodb.database", "database");
        this.serviceDatabases = serviceDatabases;
        this.tenantDatabases = tenantDatabases;
    }

}
