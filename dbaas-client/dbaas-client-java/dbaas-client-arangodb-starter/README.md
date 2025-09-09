# DBaaS Client ArangoDB Starter

DBaaS Client starter for Spring-based applications working with ArangoDB. 

This module can be included as a dependency directly to your microservice. 

- [Requirements](#requirements)
- [How to use](#how-to-use)
  * [Steps to set up library](#steps-to-set-up-library)
  * [ArangoDB creation params](#arangodb-creation-params)
  * [Usage of Spring beans created by library](#usage-of-spring-beans-created-by-library)
    + [Available beans when using `@EnableTenantDbaasArangoDB`](#available-beans-when-using-enabletenantdbaasarangodb)
    + [Available beans when using `@EnableServiceDbaasArangoDB`](#available-beans-when-using-enableservicedbaasarangodb)
    + [ArangoDatabaseProviders](#arangodatabaseproviders)
  * [Adding custom serialization](#adding-custom-serialization)
  * [Access to several databases](#access-to-several-databases)
  * [SSL/TLS support](#ssltls-support)

## Requirements
* Your microservice must use one of the following microservice-framework starters: [microservice-framework-webclient](https://github.com/Netcracker/qubership-core-microservice-framework/blob/main/microservice-framework-webclient/README.md)
or [microservice-framework-resttemplate](https://github.com/Netcracker/qubership-core-microservice-framework/blob/main/microservice-framework-resttemplate/README.md).


## How to use
### Steps to set up library
1. Add dependencies to your pom.xml: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-arangodb-starter</artifactId>
        <version>${dbaas-client-arangodb-starter.version}</version>
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
    Where `${dbaas-client-arangodb-starter.version}` is the desired library version. We recommend to use latest release. 

    **Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter), 
    in such case version of `dbaas-client-arangodb-starter` will be resolved automatically. 
2. Choose one of the annotations: `@EnableServiceDbaasArangoDB` or `@EnableTenantDbaasArangoDB`. 
    
    The difference between them is that `@EnableServiceDbaasArangoDB` only supports service-aware databases, while `@EnableTenantDbaasArangoDB` supports both tenant-aware and service-aware databases.   
    
    Add chosen annotation to your main `Application` class or any Spring configuration class. 
3. Add `@EnableArangoRepositories` annotation in case you need to use Spring Data Repositories. 
4. Configure necessary ArangoDB driver options according to [Dbaas Client Configuration - ArangoDB Driver Configuration](/docs/dbaas-client-configuration.md):

ArangoDB Driver created by default configuration in dbaas-client can be configured with options from https://www.arangodb.com/docs/stable/drivers/java-reference-setup.html using next syntax:

| Property                                      | Description                                                                                                          | Default Value                                  |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| dbaas.arangodb.timeout                        | Sets connect & request timeout (millisecond)                                                                         | `0`                                            |
| dbaas.arangodb.useSsl                         | Use SSL connection                                                                                                   | `false`                                        |
| dbaas.arangodb.chunkSize                      | Sets VelocyStream Chunk content-size (bytes)                                                                         | `30000`                                        |
| dbaas.arangodb.maxConnections                 | Sets max number of connections                                                                                       | `1` for VST, `1` for HTTP/2, `20` for HTTP/1.1 |
| dbaas.arangodb.connectionTtl                  | Sets a TTL (time to life) in milliseconds for connections managed by the internal connection pool                    | `null`, no automatic connection closure        |
| dbaas.arangodb.keepAliveInterval              | Sets keep-alive interval (in seconds) for VST connections                                                            | `null`, no keep-alive probes will be sent      |
| dbaas.arangodb.acquireHostList                | Enables driver feature of acquiring a list of known hosts in a cluster setup or a single server setup with followers | `false`                                        |
| dbaas.arangodb.acquireHostListInterval        | Sets minimum interval in milliseconds for acquiring host list                                                        | `3600000`                                      |
| dbaas.arangodb.loadBalancingStrategy          | Sets load balancing strategy                                                                                         | `NONE`                                         |
| dbaas.arangodb.protocol                       | Configures network protocol used by the driver                                                                       | `HTTP2_JSON`                                          |

### ArangoDB creation params

ArangoDb creation params allow user customize some connection properties. 
They may be set in application.properties/application.yml file. 

| Property                             | Description                                                                   | Default Value |
|--------------------------------------|-------------------------------------------------------------------------------|---------------|
| dbaas.api.arangodb.db-prefix         | Prefix to the database name                                                   | -             |
| dbaas.api.arangodb.runtime-user-role | Allows to configure connection user role for both service and tenant database | admin         |
| dbaas.api.arangodb.retry-attempts    | Number of reconnection retry attempts on connection failure                   | 0             |
| dbaas.api.arangodb.retry-delay       | Delay between retry attempts in milliseconds                                  | 0             |


### Usage of Spring beans created by library
This section describes Spring beans created by library in different configurations. Proceed to one of the following items: 
+ [Available beans when using `@EnableServiceDbaasArangoDB`](#available-beans-when-using-enableservicedbaasarangodb)
+ [Available beans when using `@EnableTenantDbaasArangoDB`](#available-beans-when-using-enabletenantdbaasarangodb)

#### Available beans when using `@EnableServiceDbaasArangoDB`

When using annotation `@EnableServiceDbaasArangoDB` dbaas-client creates only service-aware `ArangoOperations` bean.
You can inject it in your code:
```java
@Autowired
private ArangoOperations arangoOperations;
```

Or use it in Spring Data Repositories:
```java
@EnableServiceDbaasArangoDB
@EnableArangoRepositories(basePackages = "com.netcracker...")
```
Database that is accessed by this bean can be configured in application properties:

| Property                                      | Description                                                                                                        | Default Value                             |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| dbaas.arangodb.dbId                           | additional classifier's field. Setting this field you can chose which arango db will be used in spring integration | `default`                                 |

#### Available beans when using `@EnableTenantDbaasArangoDB`

When using annotation `@EnableTenantDbaasArangoDB` dbaas-client creates both tenant-aware and service-aware `ArangoOperations` beans with different qualifiers.
User may inject and perform arango operations using these beans:
```java
@Autowired
@Qualifier(SERVICE_ARANGO_TEMPLATE)
private ArangoOperations serviceArangoOperations;

@Autowired
@Qualifier(TENANT_ARANGO_TEMPLATE)
private ArangoOperations tenantArangoOperations;
```

Besides, you can use `spring data arangodb` with a dbaas integration. In order to start using this, you need to put the following annotation:

  ```java
@EnableTenantDbaasArangoDB
@EnableArangoRepositories(basePackages = "com.netcracker...")
  ```

Note that service-aware ArangoOperations is primary and will be used by default in repositories unless configured otherwise in `@EnableArangoRepositories` annotation.
To use tenant-aware ArangoOperations user have to provide their own configuration for `repositoryFactoryBeanClass` and use tenant template there.
  ```java
@EnableTenantDbaasArangoDB
@EnableArangoRepositories(basePackages = "com.netcracker...", repositoryFactoryBeanClass = CustomTenantFactoryBean.class)
  ```

Database that is accessed by this bean can be configured in application properties:

| Property                                      | Description                                                                                                        | Default Value                             |
|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| dbaas.arangodb.dbId                           | additional classifier's field. Setting this field you can chose which arango db will be used in spring integration | `default`                                 |

#### ArangoDatabaseProviders

ArangoOperations beans allow users to work with one service or one tenant database. If you need to
work with several service databases belonging to one microservice or with several tenant databases belonging to one tenant
you have to use `ArangoDatabaseProvider` beans.

Provider beans are also managed by `@EnableTenantDbaasArangoDB` and `@EnableServiceDbaasArangoDB` annotations.
* `@EnableServiceDbaasArangoDB` enables only _SERVICE_ARANGODB_PROVIDER_
* `@EnableTenantDbaasArangoDB` enables both _SERVICE_ARANGODB_PROVIDER_ and _TENANT_ARANGODB_PROVIDER_.

You can inject providers in your code using these qualifiers:  
```java
@Autowired
@Qualifier(SERVICE_ARANGODB_PROVIDER)
private ArangoDatabaseProvider arangoMicroserviceDatabaseProvider;
```
```java
@Autowired
@Qualifier(TENANT_ARANGODB_PROVIDER)
private ArangoDatabaseProvider arangoTenantDatabaseProvider;
```

These beans have methods `ArangoDatabase provide()`, `ArangoDatabase provide(String dbId)` and `ArangoDatabase provide(String dbId, DatabaseConfig customDatabaseConfig)`.
They return fully built `ArangoDatabase` instance with credentials from dbaas for specified dbId.
Through `DatabaseConfig` in `ArangoDatabase provide(String dbId, DatabaseConfig customDatabaseConfig)` method, it is possible to specify the required physicalDatabaseId and get or create a database on a specific physical ArangoDB instance.
You can also specify a dbPrefix and userRole through DatabaseConfig.
Use case:
```java
DatabaseConfig customDatabaseConfig = DatabaseConfig.builder()
        .physicalDatabaseId("physical-db-id-1")
        .userRole("admin")
        .dbNamePrefix("db_prefix_1")
        .backupDisabled(true)
        .build();
ArangoDatabase database = arangoDatabaseProvider.provide(DB_NAME, customDatabaseConfig);
```
**In order to get fresh and correct connection to specific database user have to call _provide_ method each time 
before business logic call.** 

### Adding custom serialization

To add custom serialization and override default one to ArangoDB, you can use ArangoSerde. To do this, you need to add the @Primary bean implementating of ArangoSerde interface in configuration class in the following format:
```java
@Bean
@Primary
public ArangoSerde ArangoSerde() {
    ArangoSerde arangoSerde = new ArangoSerde() {
    
        @Override
        public byte[] serialize(Object value) {
          ...
        }

        @Override
        public <T> T deserialize(byte[] content, Class<T> clazz) {
          ...
        }
    }
}
```

For example:
```java
@Bean
@Primary
public ArangoSerde ArangoSerdeCongiguration(ArangoConverter converter) {
    ArangoSerde arangoSerde = new ArangoSerde() {
        private final ObjectMapper mapper;

        {
            mapper = new ObjectMapper();
            SimpleModule module = new SimpleModule("PersonModule");
            module.addDeserializer(Person.class, new PersonDeserializer());
            mapper.registerModule(module);
        }

        @Override
        public byte[] serialize(Object value) {
            try {
                return mapper.writeValueAsBytes(converter.write(value));
            } catch (JsonProcessingException e) {
                throw new MappingException("Exception while serializing.", e);
            }
        }

        @Override
        public <T> T deserialize(byte[] content, Class<T> clazz) {
            try {
                return converter.read(clazz, mapper.readTree(content));
            } catch (IOException e) {
                throw new MappingException("Exception while deserializing.", e);
            }
        }
    };

    return arangoSerde;
}
```

### Access to several databases
To be able to connect multiple ArangoDB databases to one microservice, you need to follow these steps:
1. For connecting to second Arango database, create next bean in configuration class:

```java
    @Bean(SECOND_SERVICE_ARANGODB_PROVIDER)
    public ArangoDatabaseProvider serviceArangoDatabaseProvider(DatabasePool databasePool,
                                                                DbaasClassifierFactory classifierFactory) {
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole("admin")
                .dbNamePrefix("some-prefix")
                .build();
        return new ArangoDatabaseProvider(databasePool, classifierFactory.newServiceClassifierBuilder().withProperty("some-new-property", "some-new-value"), databaseConfig);
    }
```

2. Specify database classifier (use `withProperty("some-new-property", "some-new-value")` method for adding additional fields).
3. In `DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix")` you should specify the user role (default is admin role) for declarative database connection and database prefix.
4. Then inject ArangoDatabaseProvider into code and use one of next `provide` methods of ArangoDatabaseProvider.class:

- `provide(String dbId)`
- `provide()`

```java
@Autowired
@Qualifier(SECOND_SERVICE_ARANGODB_PROVIDER)
private ArangoDatabaseProvider arangoMicroserviceDatabaseProvider;
//...
ArangoDatabase arangoDatabase = arangoMicroserviceDatabaseProvider.provide();
```

#### Getting access to another service's database

For getting connection to another microservice's database, you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### SSL/TLS support

This library supports work with secured connections to arangodb. Connection will be secured if TLS mode is enabled in
arangodb-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://<github link todo>/Databases_Repo/arangodb-operator/-/blob/master/docs/installation.md#tls

To enable TLS support in physical database redeploy arangodb with mandatory parameters
```yaml
tls.enabled=true;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
ISSUER_NAME=<cluster issuer name>;
tls.generateCerts.enabled=true;
tls.generateCerts.clusterIssuerName=<cluster issuer name>;
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

