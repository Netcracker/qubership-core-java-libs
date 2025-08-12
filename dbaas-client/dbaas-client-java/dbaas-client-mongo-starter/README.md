# DBaaS Client Mongo Starter

DBaaS Client starter for Spring-based applications working with MongoDB. 

This module can be included as a dependency directly to your microservice. 

- [How to use](#how-to-use)
  * [Steps to set up library](#steps-to-set-up-library)
  * [Access to several databases](#access-to-several-databases)
  * [SSL/TLS support](#ssltls-support)

## How to use
### Steps to set up library
1. Add dependencies to your pom.xml: 
  
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-mongo-starter</artifactId>
        <version>${dbaas-client-mongo-starter.version}</version>
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
    Where `${dbaas-client-mongo-starter.version}` is the desired library version. We recommend using the latest release. 
    
    **Important!** Omit tag `version` if you are using qubership-spring-boot-starter-parent or cloud-core-java-bom 
    in such case version of `dbaas-client-mongo-starter` will be resolved automatically. 
2. Configure necessary mongo client options 
3. Put one of the following annotations over one of your configuration file: 
    * `@EnableServiceDbaasMongo` - for working with only a service database 
    * `@EnableTenantDbaasMongo`  - for working with only tenant databases 
    * `@EnableDbaasMongo`        - for working with both service and tenant databases 
    
#### Usage `@EnableServiceDbaasMongo` annotation
When you use this annotation, you can work with only a `service database`. 
This library initializes service specific `MongoDatabaseFactory` bean with `serviceMongoDbFactory` qualifier and MongoTemplate bean with `serviceMongoTemplate` qualifier.  
You need to inject and perform mongo operations using these beans: 

  ```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

  @Autowired
  @Qualifier(SERVICE_MONGO_DB_FACTORY)
  private MongoDatabaseFactory mongoDatabaseFactory;

  @Autowired
  @Qualifier(SERVICE_MONGO_TEMPLATE)
  private MongoTemplate mongoTemplate;
  ```

These beans are with a `primary` qualifiers.

Besides, you can use `spring data mongodb` with a dbaas integration. In order to start using this, you need to put the following annotation:

  ```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.SERVICE_MONGO_TEMPLATE;

@EnableMongoRepositories(mongoTemplateRef = SERVICE_MONGO_TEMPLATE)
  ```

#### Usage `@EnableTenantDbaasMongo` annotation
Using this annotation, you can work with only a `tenant database`. 
This library initializes tenant specific `MongoDatabaseFactory` bean with `tenantMongoDbFactory` qualifier and MongoTemplate bean with `tenantMongoTemplate` qualifier.  
You need to inject and perform mongo operations using these beans: 

  ```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

  @Autowired
  @Qualifier(TENANT_MONGO_DB_FACTORY)
  private MongoDatabaseFactory mongoDatabaseFactory;

  @Autowired
  @Qualifier(TENANT_MONGO_TEMPLATE)
  private MongoTemplate mongoTemplate;
  ```

Besides, you can use `spring data mongodb` with a dbaas integration. In order to start using this, you need to put the following annotation:

  ```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.TENANT_MONGO_TEMPLATE;

@EnableMongoRepositories(mongoTemplateRef = TENANT_MONGO_TEMPLATE)
  ```

#### Usage `@EnableDbaasMongo` annotation
When using `@EnableDbaasMongo` annotation dbaas-client creates tenant-aware and service-aware `MongoTemplate` and `MongoDbFactory` beans with different qualifiers.   
You can inject them in your code using these qualifiers:  
```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

@Autowired
@Qualifier(SERVICE_MONGO_TEMPLATE)
private MongoTemplate serviceMongoTemplate;

@Autowired
@Qualifier(SERVICE_MONGO_DB_FACTORY)
private MongoDbFactory serviceMongoDbFactory;

@Autowired
@Qualifier(TENANT_MONGO_TEMPLATE)
private MongoTemplate tenantMongoTemplate;

@Autowired
@Qualifier(TENANT_MONGO_DB_FACTORY)
private MongoDbFactory tenantMongoDbFactory;
```

Also you can use `spring data mongo` by putting `@EnableMongoRepositories` annotation  and specifying `mongoTemplateRef` parameter. For example:
```java
import static com.netcracker.cloud.dbaas.client.config.DbaasMongoConfiguration.*;

@EnableMongoRepositories(mongoTemplateRef = SERVICE_MONGO_TEMPLATE)
```

To set a prefix to the database name, you need to set next property in application.properties/application.yml file:
```
dbaas.api.mongo.db-prefix=some-prefix
```

Property, that allows to configure connection user role for both service and tenant database. Default is "admin".
```
dbaas.api.mongo.runtime-user-role=some-role
```

You can find more of configuring both tenant-aware and service-aware Spring Data Repositories [in this example](../dbaas-client-sample-tests/mongo-sample-test). 


### Access to several databases
To be able to connect multiple MongoDB databases to one microservice, you need to follow these steps:
1. For connecting to second Mongo database, create next bean:
```java
@Bean(name = {SECOND_SERVICE_MONGO_DB_FACTORY})
public MongoDatabaseFactory serviceMongoDbFactory(DatabasePool databasePool,
                                                    DbaasClassifierFactory dbaasClassifierFactory) {
        Map<String, Object> additionalClassifierFields = new HashMap<>(1);
        additionalClassifierFields.put("dbClassifier", "dbClassifierFieldValue");
        DbaaSClassifierBuilder classifierBuilder = dbaasClassifierFactory.newServiceClassifierBuilder(additionalClassifierFields);
        DatabaseConfig databaseConfig = DatabaseConfig.builder()
            .dbNamePrefix("some-prifix")
            .userRole("admin")
            .build();
        return new DbaasMongoDbFactory(classifierBuilder, databasePool, databaseConfig);
        }
```
2. Specify database classifier (use `withProperty("some-new-property", "some-new-value")` method) and "dbClassifier" field if you need.
3. In `DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix")` you should specify the user role (default is admin role) for declarative database connection and database prefix.
4. Then inject DbaasMongoDbFactory into code and use `getMongoDatabase()` method:

```java
@Qualifier(SECOND_SERVICE_MONGO_DB_FACTORY)
private MongoDatabaseFactory mongoDatabaseFactory;
//...
MongoDatabase db = dbaasMongoDbFactory.getMongoDatabase();
```
#### Getting access to another service's database

For getting connection to another microservice's database, you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### SSL/TLS support

This library supports work with secured connections to mongodb. Connection will be secured if TLS mode is enabled in
mongodb-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://<github link todo>/Databases_Repo/mongodb-operator/-/blob/master/docs/installation_guide.md#tls-parameters

To enable TLS support in physical database redeploy mongodb with mandatory parameters
```yaml
tls.mode=requireTLS;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
tls.generateCerts.enabled=true;
tls.generateCerts.clusterIssuerName=<cluster issuer name>
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

## Sample microservice
Sample microservice using DBaaS Client starter for mongoDB can be found [here](https://<github link todo>/DEMO.Platform_Researches/cloud-core-samples/tree/master/mongo-client-example). 
