# Deprecated

## dbaas-datasource-postgresql

`com.netcracker.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder` and 
`com.netcracker.cloud.core.quarkus.dbaas.datasource.classifier.TenantClassifierBuilder` are deprecated and will be removed 
in nearest major library release.

Instead, use `com.netcracker.cloud.dbaas.common.classifier.ServiceClassifierBuilder` and 
`com.netcracker.cloud.dbaas.common.classifier.TenantClassifierBuilder`

## dbaas-cassandra-client

`com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator#createSession(CassandraDBConnection connectionProperties)` is deprecated
and will be removed in nearest major library release.

Instead use `com.netcracker.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator#createSession(CassandraDatabase cassandraDatabase)`
