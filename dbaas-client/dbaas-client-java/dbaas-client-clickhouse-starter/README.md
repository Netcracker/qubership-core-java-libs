# DBaaS Client Clickhouse Starter

DBaaS Client starter for Spring-based applications working with Clickhouse.

This module can be included as a dependency directly to your microservice.

- [Requirements](#requirements)
- [How to use](#how-to-use)
    * [Steps to set up library](#steps-to-set-up-library)
    * [Clickhouse creation params](#clikhouse-creation-params)
    * [Access to several databases](#access-to-several-databases)
    * [SSL/TLS support](#ssltls-support)
    * [Datasource builder](#dbaas-datasource-builder)
        * [Usage](#usage)


## How to use
### Steps to set up library
1. Add dependencies to your pom.xml:
```xml
 <dependency>
     <groupId>com.netcracker.cloud</groupId>
     <artifactId>dbaas-client-clickhouse-starter</artifactId>
     <version>${dbaas-client-clickhouse-starter.version}</version>
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

Where `${dbaas-client-clickhouse-starter.version}` is the desired library version. We recommend using the latest release.

**Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter),
in such case version of `dbaas-client-clickhouse-starter` will be resolved automatically.

3. Add one of the following annotations to your main `Application` class or any Spring configuration class:
    * `@EnableServiceDbaasClickhouse` - for working with only a service database
    * `@EnableTenantDbaasClickhouse`  - for working with only tenant databases
    * `@EnableDbaasClickhouse`        - for working with both service and tenant databases

These annotations allow you to create service and tenant [ClickHouseDataSource](https://clickhouse.com/docs/en/integrations/java#connect-to-clickhouse-1)

#### Usage `@EnableServiceDbaasClickhouse` annotation
This annotation creates `only service clickhouse datasource` with `serviceClickhouseDatasource` qualifier. You can inject this clickhouse datasource by the following way:
```java
import static DbaasClickhouseConfiguration.SERVICE_CLICKHOUSE_DATASOURCE;

@Autowired
@Qualifier(SERVICE_CLICKHOUSE_DATASOURCE)
private DataSource serviceDatasource;
```

#### Usage `@EnableTenantDbaasClickhouse` annotation
This annotation creates `only tenant clickhouse datasource` with a `tenantClickhouseDatasource` qualifier. You can inject this clickhouse datasource by the following way:
```java
import static DbaasClickhouseConfiguration.TENANT_CLICKHOUSE_DATASOURCE;

@Autowired
@Qualifier(TENANT_CLICKHOUSE_DATASOURCE)
private DataSource dbaasClickhouseDataSource;
```

#### Usage `@EnableDbaasClickhouse` annotation
This annotation allows to work with service and tenant databases at the same microservice. The annotation brings configurations that build `service and tenant` specific
clickhouse datasources.  
Service clickhouse datasource has `serviceClickhouseDatasource` qualifier and tenant one has `tenantClickhouseDatasource` qualifier. You may inject them by the following way:

```java


@Autowired
@Qualifier(SERVICE_CLICKHOUSE_DATASOURCE)
private DataSource serviceDatasource;

@Autowired
@Qualifier(TENANT_CLICKHOUSE_DATASOURCE)
private DataSource tenantDatasource;
```

### Clickhouse creation params

To set a prefix to the database name, you need to set next property in application.properties/application.yml file:
```
dbaas.api.clickhouse.db-prefix=some-prefix
```
Property, that allows to configure connection user role for both service and tenant database. Default is "admin".
```
dbaas.api.clickhouse.runtime-user-role=some-role
```

### Access to several databases
To be able to connect multiple Clickhouse databases to one microservice, you need to follow these steps:
1. To connect to another service database, you need to use `DbaasClickhouseDatasourceBuilder` in configuration class:
```java
@Autowired
private DbaasClickhouseDatasourceBuilder dbaasDatasourceBuilder;

@Autowired
private DbaasClassifierFactory classifierFactory;

public void customDatasource() {
        DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix").build();
        Properties properties = new Properties(); //This properties will be used for ClickHouseDataSource creating
        properties.put("key", "value");
        DataSource dataSource = dbaasDatasourceBuilder.newBuilder(classifierFactory.newServiceClassifierBuilder().withProperty("some-new-property", "some-new-value"))
        .withDiscriminator("discriminatorValue")
        .withDatabaseConfig(databaseConfig)
        .withDatasourceProperties(properties)
        .build();
}
```
2. In `classifierFactory.newServiceClassifierBuilder().withProperty("some-new-property", "some-new-value")` specify new classifier field and value. For example, it's may be "dbId" field. A new classifier is needed to distinguish between databases.
3. In `DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix")` you should specify the user role (default is admin role) for declarative database connection and database prefix and database settings based on service database.

After these steps you can get the connection to second database.
More details about the `DbaasClickhouseDatasourceBuilder` are described [here](#dbaas-datasource-builder)

#### Getting access to another service's database

For getting connection to another microservice's database, you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### SSL/TLS support

This library supports work with secured connections to clickhouse. Connection will be secured if TLS mode is enabled in
clickhouse-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://<github link todo>/Databases_Repo/clickhouse-operator/-/blob/master/docs/public/installation.md#tls

To enable TLS support in physical database redeploy clickhouse with mandatory parameters
```yaml
tls.enabled=true;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
ISSUER_NAME=<cluster issuer name>;
tls.certificateSecretName=pg-cert
tls.generateCerts.enabled=true
tls.generateCerts.clusterIssuerName=<cluster issuer name>
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

### DbaaS datasource builder

`dbaas-client-clickhouse-starter` module provides `DbaasClickhouseDatasourceBuilder` bean that provides a convenient and
flexible way to configure and create a Clickhouse datasource with various options that is integrated with DbaaS.
It is especially useful for applications where you need to manage database connections efficiently or build clickhouse datasources with different classifiers.  


By using the following annotations: `@EnableServiceDbaasClickhouse`, `@EnableTenantDbaasClickhouse`,
and `@EnableDbaasClickhouse`,
the dbaas-client library provides the `DbaasClickhouseDatasourceBuilder` bean along with other default datasources.
If you don't intend to use the default service and tenant datasources, you can choose the
dedicated `@EnableDbaasClickhouseDatasourceBuilder`
annotation to create the DbaasClickhouseDatasourceBuilder bean and build your own datasources.

#### Usage

To use this class, you need to inject instance of `DbaasClickhouseDatasourceBuilder` and use the `Builder` class to
set various configuration options for the datasource. After configuring the datasource, you can call the `build()`
method to obtain a fully configured `DataSource` instance.

```java
@Autowired
private DbaasClickhouseDatasourceBuilder dbaasDatasourceBuilder;

public void customDatasource() {
    DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("ro").build();
    Properties properties = new Properties(); //This properties will be used for ClickHouseDataSource creating
    properties.put("key", "value");
    DataSource dataSource = dbaasDatasourceBuilder.newBuilder(classifierBuilder)
            .withDiscriminator("discriminatorValue")
            .withDatabaseConfig(databaseConfig)
            .withDatasourceProperties(properties)
            .build();
}

```

#### Methods

These methods allow you to configure and customize the behavior of the datasource created by the `DbaasClickhouseDatasourceBuilder`.

- `newBuilder(DbaaSChainClassifierBuilder classifierBuilder)`: Create a new builder instance with the specified `DbaaSChainClassifierBuilder` and return it.
  The most appropriate implementations are `ServiceDbaaSClassifierBuilder` and `TenantDbaaSClassifierBuilder`.
  `DbaaSChainClassifierBuilder` provides `withProperty` and `withCustomKey` methods that allow you to pass custom classifier keys.

- `withDatabaseConfig(DatabaseConfig databaseConfig)`: Set the `DatabaseConfig` instance for the datasource.
  `DatabaseConfig` allows you to configure properties related to the database, such as `dbNamePrefix`, `databaseSettings`, and `physicalDatabaseId`.

- `withDatasourceProperties(Properties properties)`: Set additional datasource properties for the datasource. These properties are used to configure ClickHouseDataSource connection. 
 If you have questions about how to configure a specific parameter, you should refer to the official documentation and code.
 You also can paste these properties through `dbaas.clickhouse.datasourse_properties=key1=value1;key2=value2`
- `withDiscriminator(String discriminator)`: Set the discriminator value for the dbaas pool cache. This field is an additional field in the databasePool cache.
  By default, the discriminator is constructed using userRole. If you provide your own value, it will override the default value and be used in the DbaaS cache.

- `build()`: Create and return a fully configured `DataSource` instance. This method creates a fully configured datasource based on the settings provided through the other methods.


## Logical database provider
dbaas-client-clickhouse-starter supports work with DB created by client side(not from dbaas-aggregator). You can read more info [here]https://github.com/Netcracker/qubership-dbaas-client/blob/main/dbaas-client-java/dbaas-client-core/README.md#logical-database-provider-feature).
