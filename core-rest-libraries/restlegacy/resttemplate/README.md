# resttemplate

* [Overview](#overview)
* [How to use](#how-to-use)
    - [Prerequisite](#prerequisite)
    - [Add the Maven dependency](#add-the-maven-dependency)
    - [Enable resttemplate configuration](#enable-resttemplate-configuration)
    - [Autowire resttemplate beans](#autowire-resttemplate-beans)
* [RestTemplate customizer and provider](#resttemplate-customizer-and-provider)
* [Configuration properties](#configuration-properties)
* [Example micrometer integration](#example-micrometer-integration)

# Overview

Resttemplate module offers resttemplate client for making secured request. 
`JWT` token is automatically inserted to this client during outgoing request so you do not need to think about a needed bearer token
when you want to make a `m2m` or `user` REST request.

# How to use

## Prerequisite

The library relies on the ```RestTemplateBuilder``` bean provided by Spring Boot. 
Therefore, it is essential to ensure that your application provides this bean from the ```RestTemplateAutoConfiguration``` 
in the ```spring-boot-autoconfigure``` module. Subsequently, you can enable RestTemplate Micrometer metrics by incorporating the ```spring-boot-starter-actuator``` module.


## Add the Maven dependency

```xml
    <dependency>
        <artifactId>resttemplate</artifactId>
        <groupId>org.qubership.cloud</groupId>
        <version>{VERSION}</version>
    </dependency>
```

## Enable resttemplate configuration

To enable resttemplate and for loading necessary configuration 
you should put the `@EnableFrameworkRestTemplate` annotation on the your `@Configuration` class.
For example:
```java
    @Configuration
    @EnableFrameworkRestTemplate
    public class Application {
        ...
    }
```
Since 5.0.0 `@EnableFrameworkRestTemplate` use behavior of `@EnableAlternativeRestTemplate`
This resttemplate use HttpComponentsClientHttpRequestFactory which support `PATCH` requests.

## Autowire resttemplate beans

For m2m resttemplate injection you should specify `m2mRestTemplate` qualifier:

 ```java
    @Autowired("m2mRestTemplate")
    private RestTemplate restTemplate;
```

For `user` resttemplate injection specify `restTemplate` qualifier:

```java
    @Autowired("restTemplate")
    private RestTemplate restTemplate;
```

Also, you have an ability to get ConnectionManager and use it :

```java
    @Autowired
    @Qualifier("coreConnectionManager")
    HttpClientConnectionManager httpClientConnectionManager;
```
# RestTemplate customizer and provider

**Customizers**

RestTemplate supports few customizers `PoolingHttpClientConnectionManagerCustomizer` and `RequestFactoryProvider`

1. ConnectionManagerCustomizer
This customizer allows you to override or add additional configurations to `PoolingHttpClientConnectionManager`.
To use it you need to implement the `PoolingHttpClientConnectionManagerCustomizer` interface and override the `customize` method.
`PoolingHttpClientConnectionManagerCustomizer` supports ordering. The implementation with the highest order value will be executed last.
Example:
```java
@Component
public class PoolingHttpClientConnectionManagerCustomizerImpl implements PoolingHttpClientConnectionManagerCustomizer {
    @Override
    public void customize(PoolingHttpClientConnectionManager connectionManager) {
        connectionManager.setMaxTotal(13);
        connectionManager.setDefaultMaxPerRoute(9);
    }
}
```

2. RequestFactoryProvider
This customizer allows you to override configurations or change the implementing `ClientHttpRequestFactory` class.
To use it you need to implement the `RequestFactoryProvider` interface and override the `provide` method.
`RequestFactoryProvider` supports ordering. The implementation with the highest order value will be executed last.
Example:
```java
@Component
public class RequestFactoryProviderImpl implements RequestFactoryProvider {

    @Override
    public ClientHttpRequestFactory provide(HttpClientBuilder httpClientBuilder) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClientBuilder.build());
        return new BufferingClientHttpRequestFactory(requestFactory);
    }
}
```



# Configuration properties

| Property                                       | Description                                           | Default value | Status |
|------------------------------------------------|-------------------------------------------------------|---------------|--------|
| connection.readTimeout                         | configure read timeout. Milliseconds                  | 60000         |        |
| connection.connectTimeout                      | configure connect timeout. Milliseconds               | 60000         |        |
| connection.connectionRequestTimeout            | configure request timeout. Milliseconds               | 60000         |        |
| core.connection.manager.maxConnectionsTotal    | configure max connection to request factory           | -             |        |
| core.connection.manager.maxConnectionsPerRoute | configure max connection per route to request factory | -             |        |

# Example micrometer integration

**Pay attention!**

Resttemplate doesn't bring micrometer dependency. You have to include micrometer dependency by yourself.
Also, you  have to add the bellow configuration.

```java
@Configuration
@Slf4j
public class HttpPoolMetricConfiguration {
    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private Map<String, PoolingHttpClientConnectionManager> connectionManagerMap;

    @EventListener(ApplicationReadyEvent.class)
    public void bindMetrics() {
        connectionManagerMap.forEach((key, value) ->
                new PoolingHttpClientConnectionManagerMetricsBinder(value, key).bindTo(meterRegistry)
        );

    }
}
```
