# Logging aggregator
This library allows you to add endpoints to your application to manage logging levels.
Logging levels are set through integration with the Consul.

Usage
-----

##### Add the Maven dependency:

```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>logger-manager-spring</artifactId>
        <version>{VERSION}</version>
    </dependency>
```
##### Configuration properties

After this add the necessary properties for the Consul integration.

***First option***. Consul is required. 
(fail-fast property set to true by default)
```yaml
spring:
  cloud:
    consul:
      enabled: ${CONSUL_ENABLED}
      config:
        m2m:
          enabled: true  # use this for getting token via key manager and  auto refresh
        prefixes: config/${NAMESPACE},logging/${NAMESPACE} #Spring Cloud adds the string specified in the spring.application.name property to the prefixes
        enabled: ${CONSUL_ENABLED}
        watch:
          enabled: ${CONSUL_ENABLED}
          delay: 1000
          wait-time: 20
  config:
    import: consul:${CONSUL_URL}
```

When connecting these settings, connection to the Consul is required. If the Consul is unavailable, the application will not work and will throw an error.

***Second option***. Consul is not required.
If you want to use only log management and connecting to the Consul is not necessary for you, use the following settings:

```yaml
spring:
  cloud:
    consul:
      enabled: ${CONSUL_ENABLED}
      config:
        failFast: false #fail-safe on start
        m2m:
          enabled: true  # use this for getting token via key manager and  auto refresh
        prefixes: logging/${NAMESPACE} #Spring Cloud adds the string specified in the spring.application.name property to the prefixes
        enabled: ${CONSUL_ENABLED}
        watch:
          enabled: ${CONSUL_ENABLED}
          delay: 1000
          wait-time: 20
  config:
    import: optional:consul:${CONSUL_URL} # runtime import is optional 
```

##### Endpoints

This library allows you to connect an endpoint for obtaining levels.

| Endpoint                    | Http Method |                                  Description                                   |
|-----------------------------|:-----------:|:------------------------------------------------------------------------------:|
| `/api/logging/v1/levels`    |   `GET`     | Returns the map of all loggers, with information about logger names and levels |

##### LoggingUpdater
This class allows to change logging levels via Consul based on the Spring Cloud library.
This class implements the logic of default values of logging levels in case of deleting properties from the Consul.
