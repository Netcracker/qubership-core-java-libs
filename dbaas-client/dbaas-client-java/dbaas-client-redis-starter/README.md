# DBaaS Client Redis Starter

DBaaS Client starter for Spring-based applications working with Redis. 

This module can be included as a dependency directly to your microservice. 

- [How to use](#how-to-use)
  * [Steps to set up library](#steps-to-set-up-library)
  * [Redis creation params](#redis-creation-params)
  * [Usage of Spring beans created by library](#usage-of-spring-beans-created-by-library)
    + [Available beans when using `@EnableServiceDbaasRedis`](#available-beans-when-using-enableservicedbaasredis)
    + [Available beans when using `@EnableTenantDbaasRedis`](#available-beans-when-using-enabletenantdbaasredis)
  * [Usage of Spring Data Redis Repositories](#usage-of-spring-data-redis-repositories)
  * [DBaaS Redis Connection Factory builder](#dbaas-redis-connection-factory-builder)

## How to use
### Steps to set up library
1. Add dependencies to your pom.xml: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-redis-starter</artifactId>
        <version>${dbaas-client-redis-starter.version}</version>
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
    Where `${dbaas-client-redis-starter.version}` is the desired library version. 
 
    **Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](<github link todo>/libs/springboot-starter), 
    in such case version of `dbaas-client-redis-starter` will be resolved automatically. 
2. Put one of the following annotations over one of your configuration file: 
    * `@EnableServiceDbaasRedis` - for working with only a service database 
    * `@EnableTenantDbaasRedis`  - for working with only tenant databases 
    * `@EnableDbaasRedis`        - for working with both service and tenant databases 

3. Add `@EnableDbaasRedisRepositories` annotation in case you need to use Spring Data Repositories - it will provide 
4. Configure necessary Redis connection according to the [Dbaas Client Configuration - Redis Connection Configuration](/docs/dbaas-client-configuration.md):

Following properties can be used to configure Redis Connection:

| Property                                       | Description                                                                                                                                                        | Default Value |
|------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| dbaas.redis.timeout                            | Read timeout.                                                                                                                                                      | `2000`        |
| dbaas.redis.connectTimeout                     | Connection timeout.                                                                                                                                                | `2000`        |
| dbaas.redis.clientName                         | Client name to be set on connections with CLIENT SETNAME.                                                                                                          | `0`           |
| dbaas.redis.ssl.enabled                        | Whether to enable SSL support.                                                                                                                                     | `false`       |
| dbaas.redis.jedis.pool.maxActive               | Maximum number of connections that can be allocated by the pool at a given time. Use a negative value for no limit.                                                | `8`           |
| dbaas.redis.jedis.pool.maxIdle                 | Maximum number of "idle" connections in the pool. Use a negative value to indicate an unlimited number of idle connections.                                        | `8`           |
| dbaas.redis.jedis.pool.minIdle                 | Target for the minimum number of idle connections to maintain in the pool. This setting only has an effect if both it and time between eviction runs are positive. | `0`           |
| dbaas.redis.jedis.pool.timeBetweenEvictionRuns | Time between runs of the idle object evictor thread. When positive, the idle object evictor thread starts, otherwise no idle object eviction is performed.         | `-1`          |
| dbaas.redis.jedis.pool.maxWait                 | Maximum amount of time a connection allocation should block before throwing an exception when the pool is exhausted. Use a negative value to block indefinitely.   | `-1`          |

### Redis creation params

Redis creation params allow user customize some connection properties. 
They may be set in application.properties/application.yml file. 

| Property                             | Description                                                                   | Default Value |
|--------------------------------------|-------------------------------------------------------------------------------|---------------|
| dbaas.api.redis.db-prefix            | Prefix to the database name                                                   | -             |


### Usage of Spring beans created by library
This section describes Spring beans created by library in different configurations. Proceed to one of the following items: 
+ [Available beans when using `@EnableServiceDbaasRedis`](#available-beans-when-using-enableservicedbaasredis)
+ [Available beans when using `@EnableTenantDbaasRedis`](#available-beans-when-using-enabletenantdbaasredis)
+ [Available beans when using `@EnableDbaasRedisRepositories`](#available-beans-when-using-enabledbaasredisrepositories)

#### Available beans when using `@EnableServiceDbaasRedis`

Annotation `@EnableServiceDbaasRedis` provides following service-aware beans that can be injected in your code:

```java
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static com.netcracker.cloud.dbaas.client.redis.configuration.ServiceDbaasRedisConfiguration.SERVICE_DBAAS_REDIS_CONNECTION_FACTORY;
import static com.netcracker.cloud.dbaas.client.redis.configuration.ServiceDbaasRedisConfiguration.SERVICE_DBAAS_REDIS_TEMPLATE;

@Autowired
@Qualifier(SERVICE_DBAAS_REDIS_CONNECTION_FACTORY)
private RedisConnectionFactory serviceRedisConnectionFactory;

@Autowired
@Qualifier(SERVICE_DBAAS_REDIS_TEMPLATE)
private RedisTemplate<String, Object> serviceRedisTemplate;
```

#### Available beans when using `@EnableTenantDbaasRedis`

Annotation `@EnableTenantDbaasRedis` provides following tenant-aware beans that can be injected in your code:

```java
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static com.netcracker.cloud.dbaas.client.redis.configuration.TenantDbaasRedisConfiguration.TENANT_DBAAS_REDIS_CONNECTION_FACTORY;
import static com.netcracker.cloud.dbaas.client.redis.configuration.TenantDbaasRedisConfiguration.TENANT_DBAAS_REDIS_TEMPLATE;

@Autowired
@Qualifier(TENANT_DBAAS_REDIS_CONNECTION_FACTORY)
private RedisConnectionFactory tenantRedisConnectionFactory;

@Autowired
@Qualifier(TENANT_DBAAS_REDIS_TEMPLATE)
private RedisTemplate<String, Object> tenantRedisTemplate;
```

### Usage of Spring Data Redis Repositories

To use Spring Data Redis Repositories with DBaaS client you have 2 options: 

 1. Use default RedisTemplate bean that is provided by DBaaS client. 
 2. Create your own RedisTemplate that suits your needs.

#### Default RedisTemplate provided by DBaaS client.

Annotation @EnableDbaasRedisRepositories provides `RedisTemplate<String, Object>` bean with default name `redisTemplate` that will be automatically used in spring-data redis repositories.
If you only enabled service or tenant databases then it will use them. If you have both enabled then you can choose which databases will be used with the following configuration property:

| Property                            | Description                                                                                                                          | Default Value |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|---------------|
| dbaas.redis.primary-bean            | Specify whether to use service databases or tenant databases in default RedisTemplate bean. Available values: `service` or `tenant`. | `service`     |

#### Creating custom RedisTemplate bean.

RedisTemplate only needs RedisConnectionFactory to be specified after creation. You can either autowire one of the existing DBaaS connection factories 
or use [DBaaS Redis Connection Factory builder](#dbaas-redis-connection-factory-builder) to build one:

```java
@Bean("customRedisTemplate")
public RedisTemplate<Key, Value> customRedisTemplate(@Qualifier(SERVICE_DBAAS_REDIS_CONNECTION_FACTORY) RedisConnectionFactory connectionFactory) {
    RedisTemplate<Key, Value> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(connectionFactory);
    return redisTemplate;
}
```
Where `Key` and `Value` are your desired type parameters:
 * Key - the Redis key type against which the template works (usually a String)
 * Value - the Redis value type against which the template works 

Then you can use this RedisTemplate in your repository configuration:
```java
@EnableRedisRepositories(basePackages = "path-to-your-repository-classes", redisTemplateRef = "customRedisTemplate")
```

### DBaaS Redis Connection Factory builder

`dbaas-client-redis-starter` module provides `DbaasRedisConnectionFactoryBuilder` bean that provides a convenient and 
flexible way to configure and create a Redis Connection Factory with various options that is integrated with DbaaS. 
It is especially useful for applications where you need to manage database connections efficiently or build Redis Connection Factories with different classifiers.  
DBaaS uses Jedis as connection factory implementation.

By using the following annotations: `@EnableServiceDbaasRedis`, `@EnableTenantDbaasRedis` and `@EnableDbaasRedis` the dbaas-client library 
provides the `DbaasRedisConnectionFactoryBuilder` bean along with the other connection factories.

#### Usage

To use this class, you need to inject instance of `DbaasRedisConnectionFactoryBuilder` and use the `Builder` class to
set various configuration options for the datasource. After configuring the datasource, you can call the `build()`
method to obtain a fully configured `DbaasRedisConnectionFactory` instance.

```java
@Autowired
private DbaasRedisConnectionFactoryBuilder dbaasRedisConnectionFactoryBuilder;

public void customConnectionFactory() {
    DatabaseConfig databaseConfig = DatabaseConfig.builder().dbNamePrefix("prefix").build();
    RedisConnectionFactory redisConnectionFactory = dbaasRedisConnectionFactoryBuilder.newBuilder(classifierBuilder)
            .withDatabaseConfig(databaseConfig)
            .build();
}
```

#### Methods

These methods allow you to configure and customize the behavior of the datasource created by the `DbaasRedisConnectionFactoryBuilder`.

- `newBuilder(DbaaSChainClassifierBuilder classifierBuilder)`: Create a new builder instance with the specified `DbaaSChainClassifierBuilder` and return it. 
The most appropriate implementations are `ServiceDbaaSClassifierBuilder` and `TenantDbaaSClassifierBuilder`. 
`DbaaSChainClassifierBuilder` provides `withProperty` and `withCustomKey` methods that allow you to pass custom classifier keys.

- `withDatabaseConfig(DatabaseConfig databaseConfig)`: Set the `DatabaseConfig` instance for the datasource. 
`DatabaseConfig` allows you to configure properties related to the database, such as `dbNamePrefix`, `databaseSettings`, and `physicalDatabaseId`.


