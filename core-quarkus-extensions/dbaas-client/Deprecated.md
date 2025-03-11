# Deprecated

## dbaas-datasource-postgresql

`org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder` and 
`org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.TenantClassifierBuilder` are deprecated and will be removed 
in nearest major library release.

Instead, use `org.qubership.cloud.dbaas.common.classifier.ServiceClassifierBuilder` and 
`org.qubership.cloud.dbaas.common.classifier.TenantClassifierBuilder`

## dbaas-cassandra-client

`org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator#createSession(CassandraDBConnection connectionProperties)` is deprecated
and will be removed in nearest major library release.

Instead use `org.qubership.cloud.quarkus.dbaas.cassandraclient.service.CqlSessionCreator#createSession(CassandraDatabase cassandraDatabase)`
