# Logging Manager Extension

The **Logging Manager** Extension for Quarkus provides two main functionalities:

1. An endpoint to retrieve the current logging levels of all application loggers.
2. Automatic logging level updates based on properties stored in Consul. 

## Integration with Cockpit
This library is intended to be used with Cockpit for managing logging levels through a UI.
It allows developers to easily view and adjust logger settings directly from the Cockpit interface.

# Usage

First of all in order to use this extension, you should add the maven dependency:

```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>log-manager-deployment</artifactId>
            <version>${version}</version>
        </dependency>
``` 

where a version can be found in an artifactory by the link: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/log-manager-deployment/

Then, fill the config parameters. Configuration information and typical default parameters are described in the consul-client module: https://git.qubership.org/PROD.Platform.Cloud_Core/libs/cloud-core-quarkus-extensions/-/blob/main/config-sources/consul-client/README.md#consul-client

## Additional configuration properties
| Property name                                  | Description                                                                      | Default value |                                        
|------------------------------------------------|----------------------------------------------------------------------------------|---------------|
| quarkus.consul-logger-watcher.logging-enabled   | Enable logging configuration approach with Consul (bool)                         | true          |
| quarkus.consul-logger-watcher.consul-retry-time | The time after which requests to the consul will be resent if it's unavailable   | 20000         |

## Functionality

### 1. Endpoints for log management

This endpoint provides a map of all loggers along with their current logging levels. It can be used for diagnostics and monitoring of the logging settings in your application.

| Endpoint                    | Http Method |                                  Description                                   |
|-----------------------------|:-----------:|:------------------------------------------------------------------------------:|
| `/api/logging/v1/levels`    |   `GET`     | Returns the map of all loggers, with information about logger names and levels |

### 2. Logger level auto update

Log manager provides ability to update Logger level based on properties from Consul.
This extension observes Consul properties updates event and changes appropriate Logger. 
Consul logger config property looks like usually Quarkus logger property.
For example:

```properties
logging.level.root=TRACE //root log level
logging.level.com.example.cloud=DEBUG //log level for category "com.example.cloud"
```

Logger levels will be retrieved from Consul at:
http://consul-server:8500/v1/kv/logging/test-namespace/test-app/logging/level/root
http://consul-server:8500/v1/kv/logging/test-namespace/test-app/logging/level/com/example

```java
Logger myLogger = Logger.getLogger("com.example");
//myLogger.getLevel() will be equals the value of the level set in the Сonsul
```
