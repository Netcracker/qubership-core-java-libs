# DBaaS Client Cassandra Starter

DBaaS Client starter for Spring-based applications working with cassandra. 

This module can be included as a dependency directly to your microservice. 

- [Requirements](#requirements)
- [How to use](#how-to-use)
  * [Steps to set up library](#steps-to-set-up-library)
  * [Access to several databases](#access-to-several-databases)
  * [Metrics](#metrics)
  * [Migration](#migration)
- [Sample microservice](#sample-microservice)
- [Amazon Keyspaces](#amazon-keyspaces)

## How to use
### Steps to set up library
1. Add dependencies to your pom.xml: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-cassandra-starter</artifactId>
        <version>${dbaas-client-cassandra-starter.version}</version>
    </dependency>
   
   and one of the following dependencies
   -------------------------
   if you use resstemplate:
   
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-resttemplate</artifactId>
        <version>{version}</version>
    </dependency>
   
   -------------------------
   if you use webclient:
   
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-webclient</artifactId>
        <version>{version}</version>
    </dependency>
    ```
    Where `${dbaas-client-cassandra-starter.version}` is the desired library version. 
    
    **Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) or [cloud-core-java-bom](<github link todo>/libs/microservice-dependencies/-/tree/master/) 
    in such case version of `dbaas-client-cassandra-starter` will be resolved automatically.
2. In case autoconfiguration is enabled in your application then add into exclusion class `CassandraAutoConfiguration`: 
    ```java
    @SpringBootApplication(exclude = CassandraAutoConfiguration.class)
    // or 
    // @EnableAutoConfiguration(exclude = CassandraAutoConfiguration.class)
    public class Application {
        public static void main(String[] args) {
            ...
        }
    ``` 
3. Configure necessary cassandra driver options according to [Dbaas Client Configuration - Cassandra Driver Configuration](/docs/dbaas-client-configuration.md)).  
4. Put one of the following annotations over one of your configuration file: 
    * `@EnableServiceDbaasCassandra` - for working with only service database
    * `@EnableTenantDbaasCassandra`  - for working with only tenant database
    * `@EnableDbaasCassandra`       - for working both tenant and service database 
   
#### Usage `@EnableServiceDbaasCassandra` annotation
You should use this annotation when you are working with only a service database and you don't need to initialize and work with a tenant database.   
Using `@EnableServiceDbaasCassandra` annotation dbaas-client creates only service aware `CassandraOperations` bean with a `serviceCassandraTemplate` qualifier. 
You can inject it in your code like this:  

```java
@Autowired
@Qualifier(SERVICE_CASSANDRA_TEMPLATE)
private CassandraOperations cassandraTemplate;
```   

Besides this, you can use `cassandra spring data` with a dbaas solution. For it you should put `@EnableCassandraRepositories` 
and specify `SERVICE_CASSANDRA_TEMPLATE` as cassandraTemplateRef parameter:
```java
@EnableCassandraRepositories(cassandraTemplateRef = SERVICE_CASSANDRA_TEMPLATE)
```

#### Usage `@EnableTenantDbaasCassandra` annotation   
You should use this annotation when you are working with only a tenant database and you don't need to initialize and work with a service database.   
Using `@EnableTenantDbaasCassandra` annotation dbaas-client creates only tenant aware `CassandraOperations` bean with a `tenantCassandraTemplate` qualifier. 
You can inject it in your code like this:  

```java
@Autowired
@Qualifier(TENANT_CASSANDRA_TEMPLATE)
private CassandraOperations cassandraTemplate;
```   

Besides this, you can use `cassandra spring data` with a dbaas solution. For it you should put `@EnableCassandraRepositories` 
and specify `TENANT_CASSANDRA_TEMPLATE` as cassandraTemplateRef parameter:
```java
@EnableCassandraRepositories(cassandraTemplateRef = TENANT_CASSANDRA_TEMPLATE)
```

#### Usage `@EnableDbaasCassandra` annotation   
You should use this annotation when you working with tenant and service databases. This annotation contains `EnableServiceDbaasCassandra` and
`EnableTenantDbaasCassandra`. So, service and tenant related beans will be created with `serviceCassandraTemplate` and`tenantCassandraTemplate` qualifiers.

```java
@Autowired
@Qualifier(SERVICE_CASSANDRA_TEMPLATE)
private CassandraOperations cassandraTemplate;

@Autowired
@Qualifier(TENANT_CASSANDRA_TEMPLATE)
private CassandraOperations cassandraTemplate;
``` 

Pay attention, there is no `primary` bean so you should always include a qualifier. Like it was described in the previous sections, you can 
use `cassandra spring data`. For this you should configure `@EnableTenantDbaasCassandra` with a cassandraTemplateRef and additional parameters.
You can find an example of configuring both tenant-aware and service-aware `Spring Data Repositories` in [Sample microservice](#sample-microservice) section.   

To set a prefix to the database name, you need to set next property in application.properties/application.yml file:
```
dbaas.api.cassandra.db-prefix=some-prefix
```
Property, that allows to configure connection user role for both service and tenant database. Default is "admin".
```
dbaas.api.cassandra.runtime-user-role=some-role
```

### Access to several databases
To be able to connect multiple Cassandra databases to one microservice, you need to follow these steps:
1. For connecting to second Cassandra database, create next bean:
```java
@Bean(name = SECOND_SERVICE_CASSANDRA_TEMPLATE)
    @ConditionalOnMissingBean(name = SERVICE_CASSANDRA_TEMPLATE)
    public CassandraTemplate serviceCassandraTemplate(@Autowired DatabasePool pool,
                                                      DbaasClassifierFactory classifierFactory,
                                                      CassandraConverter converter) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
            .userRole("admin")
            .dbNamePrefix("some-prefix")
            .build();
        return new CassandraTemplate(
                new CassandraDbaaSSessionProxy(pool, classifierFactory.newServiceClassifierBuilder(), databaseConfig), converter);
    }
```
2. Specify database classifier (use `withProperty("some-new-property", "some-new-value")` method for adding additional fields).
3. In `DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix")` you should specify the user role (default is admin role) for declarative database connection and database prefix.
4. Then inject CassandraTemplate bean into code:
```java
@Autowired
@Qualifier(SECOND_SERVICE_CASSANDRA_TEMPLATE)
private CassandraOperations cassandraTemplate;
//...
```

#### Getting access to another service's database

For getting connection to another microservice's database, you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### Metrics

DBaaS provides access to metric configuration properties of the Cassandra driver with Micrometer integration.
Metrics will only work when Micrometer is included in application by using Spring autoconfiguration with `spring-boot-starter-actuator` dependency:
```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
```

Metrics support is enabled by default but require explicit configuration of which metrics you want to export. You can fully disable metrics if needed with the following configuration property:
```
dbaas.cassandra.metrics.enabled=false
```

Session and node Cassandra metrics configuration is stored under the `dbaas.cassandra.metrics.session` and `dbaas.cassandra.metrics.node` keys respectively. They follow the same naming as in reference Cassandra configuration (https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/reference/).

#### Sample metrics configuration
For example to enable `bytes-sent`, `bytes-received`, `connected-nodes`, `cql-requests` session metrics with additional configuration for `cql-requests` can be done with the following configuration in you application.yml file:
```yaml
dbaas.cassandra.metrics:
  enabled: true
  session:
    enabled: [bytes-sent,bytes-received,connected-nodes,cql-requests]
    cql-requests:
      highest-latency : 10s
      lowest-latency : 10ms
      significant-digits : 2
      refresh-interval : 1m
  node:
    enabled: [pool.open-connections,pool.available-streams,pool.in-flight]
```

### Migration

Library provides custom migration implementation that you can use. Please refer to this doc for details: [dbaas-client-cassandra-migration](../dbaas-client-cassandra-migration/README.md)

## Sample microservice
Sample microservice using DBaaS Client starter for cassandra can be found [here](../dbaas-client-sample-tests/cassandra-sample-test).

## Amazon Keyspaces
In case usage of Casandra database managed by AWS, it is required:
1. Register in DBAAS the Casandra as an external database. (see [Registration external logical databases](/docs/external-databases.md))
2. Amazon Keyspaces [requires](https://docs.aws.amazon.com/keyspaces/latest/devguide/using_java_driver.html#java_tutorial.driverconfiguration) the use of Transport Layer Security (TLS). \
   To enable TLS use application properties
```
dbaas.cassandra.ssl=true
dbaas.cassandra.ssl-hostname-validation=false
dbaas.cassandra.lb-slow-replica-avoidance=false
```
3. Working in this mode, the library requires having a truststore with certificate. \
The platform deployer provides the bulk uploading of certificates to truststores. \
Use deployer's property **CERTIFICATES_SECRET** to inject desired certificate into microservice. \
Make sure the certificate is mount into your microservice (see [JVM Certificates in Base Image](https://<github link todo>/centos_base_image#jvm-certificates)).

4. On bootstrapping microservice there is generated truststore with default location and password. \
To change path to truststore and password use application properties:
```
dbaas.cassandra.truststorePath=/path/to/cassandra/truststore
dbaas.cassandra.truststorePassword=password
```

References: \
[Using a Cassandra Java client driver to access Amazon Keyspaces programmatically - Amazon Keyspaces (for Apache Cassandra)](https://docs.aws.amazon.com/keyspaces/latest/devguide/using_java_driver.html) \
[JVM Certificates in Base Image](https://<github link todo>/centos_base_image#jvm-certificates) \
[DataStax Java Driver - SSL](https://docs.datastax.com/en/developer/java-driver/4.13/manual/core/ssl/)

### SSL/TLS support

This library supports work with secured connections to cassandra. Connection will be secured if TLS mode is enabled in
cassandra-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://<github link todo>/Databases_Repo/cassandra-operator/-/blob/master/docs/installation_guide.md#tls-encryption

To enable TLS support in physical database redeploy cassandra with mandatory parameters
```yaml
tls.enabled=true;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
tls.generateCerts.enabled=true;
tls.generateCerts.clusterIssuerName=<cluster issuer name>;
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

