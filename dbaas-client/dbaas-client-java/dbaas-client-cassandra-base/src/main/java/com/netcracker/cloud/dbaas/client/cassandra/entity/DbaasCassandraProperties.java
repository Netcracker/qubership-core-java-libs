package com.netcracker.cloud.dbaas.client.cassandra.entity;

import lombok.Data;

@Data
public class DbaasCassandraProperties {
    private boolean ssl;
    private int requestTimeoutMs;
    private String truststorePath;
    private String truststorePassword;
    private Boolean sslHostnameValidation;
    private Boolean lbSlowReplicaAvoidance;
    private Boolean resolveContactPoints;
    private boolean usePreparedStatements = true;
    private DbaasCassandraMetricsProperties metrics = new DbaasCassandraMetricsProperties();
}
