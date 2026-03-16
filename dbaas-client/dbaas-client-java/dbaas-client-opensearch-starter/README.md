# DBaaS Client Opensearch Starter

DBaaS Client starter for Spring-based applications working with opensearch.

This module can be included as a dependency directly to your microservice. 

* [Overview](#overview)
* [How to use](#how-to-use)
    - [Steps to set up library](#steps-to-set-up-library)
    - [Access to several indexes](#access-to-several-indexes)
    - [Metrics](#metrics)
* [Supported methods and Restrictions](#supported-methods-and-restrictions)
* [SSL/TLS support](#ssltls-support)
* [Prefix and delimiter configuration for resources](#prefix-and-delimiter-configuration-for-resources)
* [Opensearch connection properties configuration](#opensearch-connection-properties-configuration)
* [Example normalize method with above configuration](#example-normalize-method-with-above-configuration-)
* [Http customizers](#http-customizers)

## Overview
Since opensearch does not support objects (indices, templates and aliases) isolation we had to create it artificially.
This was done by introducing a unique prefix for any opensearch user. Each user's object will have the same prefix. 
Each user can access only objects with its prefix. 

New user wil be created for each service database and for each tenant database. You don't have to create user by 
yourself - user wil be created automatically with first request.

The next diagram shows how microservice interact with dbaas-aggregator and opensearch.  
![alt text](Overiew diagram.png)

User must add prefix by themselves. Please take a look on section
[Restrictions](#restrictions) because it contains some useful info about creation of some requests and how to work with 
some responses.

## Attention!
Since 5.0.0 version `dbaas-client-opensearch-renovated-starter` has been renamed to `dbaas-client-opensearch-starter`

## How to use
### Steps to set up library
1. Add dependencies to your pom.xml:
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-opensearch-starter</artifactId>
        <version>${dbaas-client-opensearch-starter.version}</version>
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
   Where `${dbaas-client-opensearch-starter.version}` is the desired library version. 
   
   **Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) 
       or [cloud-core-java-bom](<github link todo>/libs/microservice-dependencies/-/tree/master/), 
   in such case version of `dbaas-client-opensearch-starter` will be resolved automatically. 
2. Add one of the following annotations to your main `Application` class or any Spring configuration class:   
    * `@EnableServiceDbaasOpensearch` - for working with only a service database   
    * `@EnableTenantDbaasOpensearch`  - for working with only tenant databases   
    * `@EnableDbaasOpensearch`        - for working with both service and tenant databases 
      
#### Usage `@EnableServiceDbaasOpensearch` annotation
This annotation is intended for working with only `service index`. In order to create and make operations with `service index`
you need to inject the following bean:

```java
import static DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT;

@Autowired
@Qualifier(SERVICE_NATIVE_OPENSEARCH_CLIENT) // to work with service index
private DbaasOpensearchClient serviceClient;
```

#### Usage `@EnableTenantDbaasOpensearch` annotation
This annotation is intended for working with only `tenant index`. In order to create and make operations with `tenant indexes`
you need to inject the following bean:

```java
import static DbaasOpensearchConfiguration.TENANT_NATIVE_OPENSEARCH_CLIENT;

@Autowired
@Qualifier(TENANT_NATIVE_OPENSEARCH_CLIENT) // to work with tenant indexes
private DbaasOpensearchClient tenantClient;
```

#### Usage `@EnableDbaasOpensearch` annotation
You have to use `@EnableDbaasOpensearch` annotation if you need to work as a service as a tenant indexes. 
This annotation creates tenant-aware and service-aware `DbaasOpensearchClient` beans :
```java
import static DbaasOpensearchConfiguration.SERVICE_NATIVE_OPENSEARCH_CLIENT;

@Autowired
@Qualifier(SERVICE_NATIVE_OPENSEARCH_CLIENT) // to work with service index
private DbaasOpensearchClient serviceClient;
```
```java
import static DbaasOpensearchConfiguration.TENANT_NATIVE_OPENSEARCH_CLIENT;

@Autowired
@Qualifier(TENANT_NATIVE_OPENSEARCH_CLIENT) // to work with tenant indexes
private DbaasOpensearchClient tenantClient;
```

---
`DbaasOpensearchClient` bean provides method with native client which get [Java OpenSearchClient](https://opensearch.org/docs/latest/clients/java/)
and some more functions for convenient work:

* `String normalize(String)` - returns full_name with prefix. Have to use this method with each request.
* `String normalize(DatabaseConfig, String)` - returns full_name with prefix with custom configuration. Have to use this method with each request.
* `OpenSearchClient getClient()` - returns `OpenSearchClient`.
* `OpenSearchClient getClient(DatabaseConfig)` - returns `OpenSearchClient` with custom configuration.
* `String getPrefix()` - returns prefix for current user

### Access to several prefixes
To be able to connect multiple Opensearch spaces with different prefixes to one microservice, you need to follow these steps:
1. For creating new Opensearch prefix space, create next bean:
```java
@Bean(name = {SECOND_SERVICE_NATIVE_OPENSEARCH_CLIENT})
    public DbaasOpensearchClient microserviceOpensearchRestClient(DatabasePool databasePool,
                                                                  DbaasClassifierFactory classifierFactory,
                                                                  OpensearchProperties opensearchProperties) {
        DatabaseSettings dbSettings = getDatabaseSettings();
        DatabaseConfig.Builder databaseConfig = DatabaseConfig.builder()
                .userRole(opensearchProperties.getRuntimeUserRole())
                .databaseSettings(dbSettings);
        OpensearchConfig opensearchConfig = new OpensearchConfig(opensearchProperties, opensearchProperties.getService().getDelimiter());
        return new DbaasOpensearchClientImpl(databasePool, classifierFactory.newServiceClassifierBuilder(), databaseConfig, opensearchConfig);
    }
```

2. Specify database classifier (use `withProperty("some-new-property", "some-new-value")` method for adding additional fields).
3. It is also possible to override the user role and dbSettings in DatabaseConfig.class.
4. Then inject DbaasOpensearchClient bean into code for work with new prefix:
```java
@Autowired
@Qualifier(SECOND_SERVICE_NATIVE_OPENSEARCH_CLIENT) // to work with second service index
private DbaasOpensearchClient serviceClient;
```

#### Getting access to another service's objects

For getting connection to another microservice's objects (eg. indices, aliases, templates), you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### Metrics

DBaaS automatically provides metrics for Opensearch client.
Metrics will only work when Micrometer is included in application by using Spring autoconfiguration with `spring-boot-starter-actuator` dependency:
```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
```

Metrics support is enabled by default. You can fully disable metrics if needed with the following configuration property:
```
dbaas.opensearch.metrics.enabled=false
```

#### Requests seconds metric
It is Opensearch client's metric called 'opensearch_client_requests_seconds'.
This metric is intended to record durations (in seconds) of requests from client to Opensearch.
Its metric configuration is stored under the `dbaas.opensearch.metrics.requests-seconds` key.

#### Sample metrics configuration for Opensearch client
For example to enable 'opensearch_client_requests_seconds' metric with the following configuration in your application.yml file:
```yaml
# Properties for Opensearch client metrics
dbaas.opensearch.metrics:
  # Default value: true
  enabled: true
  # Properties for 'opensearch_client_requests_seconds' metric
  requests-seconds:
    # Default value: true
    enabled: true
    # Possible values: SUMMARY or HISTOGRAM. Default value: SUMMARY
    type: SUMMARY
    # Default value: 1ms
    minimum-expected-value: 1ms
    # Default value: 30s
    maximum-expected-value: 30s
    # It matters only if type=SUMMARY
    # Property for list of Double numbers meaning quantiles
    # Default value: empty list
    quantiles: 0.25,0.5,0.75,0.95
    # It matters only if type=SUMMARY
    # Property for creating arbitrary amount of histogram buckets with values from 'minimum-expected-value' to 'maximum-expected-value' 
    # Default value: false
    quantile-histogram: false
    # It matters only if type=HISTOGRAM
    # Property for list of Duration instances meaning expected request durations in buckets
    # Default value: empty list
    histogram-buckets: 100ms,500ms,1000ms,2000ms,5000ms
```

### SSL/TLS support

This library supports work with secured connections to opensearch. Connection will be secured if TLS mode is enabled in
opensearch-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://github.com/Netcracker/qubership-opensearch/blob/main/docs/public/tls.md

To enable TLS support in physical database redeploy opensearch with mandatory parameters
```yaml
global.tls.enabled=true;
opensearch.tls.enabled=true;
dbaasAdapter.tls.enabled=true;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
global.tls.generateCerts.clusterIssuerName=<cluster issuer name>;
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

## Prefix and delimiter configuration for resources

You have an ability to set custom prefix and delimiter in `application.yaml` file to get access to resources created before dbaas integration.

| Property                                 | Description                                                                                                                                                           | Default value           | Status       | Example                                                     |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|--------------|-------------------------------------------------------------|
| `dbaas.api.opensearch.service.prefix`    | Sets custom prefix for service database resource(index, alias, template)                                                                                              | `no default value.`     | since 4.3.0. | `dbaas.api.opensearch.service.prefix=some-service-prefix`   |
| `dbaas.api.opensearch.service.delimiter` | Sets custom delimiter for service database                                                                                                                            | `_`                     | since 4.3.0. | `dbaas.api.opensearch.service.delimiter=-`                  |
| `dbaas.api.opensearch.tenant.prefix`     | Sets custom prefix for all tenants database resource(index, alias, template). Put substring '{tenantId}' in prefix. This substring will be replaced to real tenantId. | `no default value.`     | since 4.3.0. | `dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-some` |
| `dbaas.api.opensearch.tenant.delimiter`  | Sets custom delimiter for all tenants database                                                                                                                        | `_`                     | since 4.3.0. | `dbaas.api.opensearch.tenant.delimiter=--`                  |
| `dbaas.api.opensearch.runtime-user-role` | Allows to configure connection user role for both service and tenant database                                                                                         | `admin`                 | since 5.0.3  | `dbaas.api.opensearch.runtime-user-role=ro`                 |


## Opensearch connection properties configuration

You have an ability to set maxConnTotal and maxConnPerRoute in `application.yaml` file to set these settings in Apache Http Client.

| Property                                 | Description                                 | Default value       | Status       | Example                                  |
|------------------------------------------|---------------------------------------------|---------------------|--------------|------------------------------------------|
| `dbaas.opensearch.max-conn-total`        | Sets custom value for maxConnTotal field    | `no default value.` | since 7.2.0. | `dbaas.opensearch.max-conn-total=50`     |
| `dbaas.opensearch.max-conn-per-route`    | Sets custom value for maxConnPerRoute field | `no default value.` | since 7.2.0. | `dbaas.opensearch.max-conn-per-route=50` |


## Example `normalize` method with above configuration: 

### With serviceClient :
If you have `dbaas.api.opensearch.service.prefix=some-service-prefix` and `dbaas.api.opensearch.service.delimiter=-`

```java
@Autowired
@Qualifier(SERVICE_NATIVE_OPENSEARCH_CLIENT)
private DbaasOpensearchClient serviceClient;

ExistsRequest existsRequest = new ExistsRequest.Builder().index(serviceClient.normalize("uniq_name")).id("1").build(); // serviceClient.normalize() returns full_name like 'some-service-prefix-uniq_name'
boolean exists = serviceClient.getClient().indices().exists(existsRequest).value();
```


### With tenantClient:
If you have `dbaas.api.opensearch.tenant.prefix=tenant-{tenantId}-some`, `dbaas.api.opensearch.tenant.delimiter=--`, and tenantId is = `1234`

```java
@Autowired
@Qualifier(TENANT_NATIVE_OPENSEARCH_CLIENT)
private DbaasOpensearchClient tenantClient;

ExistsRequest existsRequest = new ExistsRequest.Builder().index(tenantClient.normalize("uniq_name")).id("1").build(); // tenantClient.normalize() returns 'tenant-1234-some--uniq_name'
boolean exists = tenantClient.getClient().indices().exists(existsRequest).value();
```

More examples can be found at [Readme.md](../dbaas-client-opensearch-base/README.md#how-to-use-example)

## Http customizers

The DbaasOpensearchClientBuilderCustomizer interface is used to customize various components of the OpenSearch client configuration.
It provides a set of default methods to customize builders used to configure HTTP clients, connection managers, and request configurations in an OpenSearch client setup.

This interface is designed to be implemented by customizers who need to modify the behavior of the OpenSearch client’s HTTP transport and connection settings.

### Available Methods
The interface provides default methods for customizing the following builders:

1. customize(ApacheHttpClient5TransportBuilder builder)

Use this method to modify settings related to the HTTP transport layer.

Example:

```java
@Override
public void customize(ApacheHttpClient5TransportBuilder builder) {
    builder.setDefaultHeaders(...).setChunkedEnabled(true);
}
```

2. customize(HttpAsyncClientBuilder builder)

This method allows you to modify settings related to asynchronous requests.

Example:

```java
@Override
public void customize(HttpAsyncClientBuilder builder) {
    builder.setRetryStrategy(...).disableAuthCaching();
} 
```

3. customize(RequestConfig.Builder builder)

Customizes the RequestConfig.Builder used to configure request-level settings.

Example:

```java
@Override
public void customize(RequestConfig.Builder builder) {
    builder.setConnectTimeout(3000);
    builder.setSocketTimeout(5000);
}
```

4. customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder)

Customizes the PoolingAsyncClientConnectionManagerBuilder used for connection pooling settings.
You can configure the maximum number of connections per route or set other connection pool-specific parameters.

Example:

```java
@Override
public void customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder) {
    connectionManagerBuilder.setMaxConnTotal(200);
    connectionManagerBuilder.setMaxConnPerRoute(50);
}
```

### How to Use
To use the DbaasOpensearchClientBuilderCustomizer, follow these steps:

1. Implement the Interface: 
Create a custom implementation of the DbaasOpensearchClientBuilderCustomizer interface where you can override any of the customize methods. Each method allows you to modify the specific builder passed into it.

Example:

```java
@Component
public class CustomOpensearchClientCustomizer implements DbaasOpensearchClientBuilderCustomizer {

    @Override
    public void customize(ApacheHttpClient5TransportBuilder builder) {
        builder.setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(2000)
                .setSocketTimeout(5000)
                .build());
    }

    @Override
    public void customize(HttpAsyncClientBuilder builder) {
        builder.setRetryStrategy(...).disableAuthCaching();
    }

    @Override
    public void customize(RequestConfig.Builder builder) {
        builder.setConnectTimeout(3000);
    }

    @Override
    public void customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder) {
        connectionManagerBuilder.setMaxConnTotal(300);
        connectionManagerBuilder.setMaxConnPerRoute(150);
    }
}
```

2. Register the Customizer Bean:
Register your customizer as a Spring Bean by annotating it with @Component or manually defining it in a configuration class using @Bean.

Example:
```java
@Configuration
public class OpensearchConfig {

    @Bean
    public DbaasOpensearchClientBuilderCustomizer customOpensearchClientCustomizer() {
        return new CustomOpensearchClientCustomizer();
    }
}
```
