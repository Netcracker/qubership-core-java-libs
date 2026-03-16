[![Maven build](https://github.com/Netcracker/qubership-core-blue-green-state-monitor-quarkus/actions/workflows/maven-build.yaml/badge.svg)](https://github.com/Netcracker/qubership-core-blue-green-state-monitor-quarkus/actions/workflows/maven-build.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-blue-green-state-monitor-quarkus)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor-quarkus)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-blue-green-state-monitor-quarkus)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor-quarkus)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-blue-green-state-monitor-quarkus)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor-quarkus)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-blue-green-state-monitor-quarkus)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor-quarkus)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-blue-green-state-monitor-quarkus)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor-quarkus)

# blue-green-state-monitor-quarkus library
Provides configuration with BlueGreenStatePublisher Quarkus bean

<!-- TOC -->
* [blue-green-state-monitor-quarkus library](#blue-green-state-monitor-quarkus-library)
  * [BlueGreenStatePublisher usage example:](#bluegreenstatepublisher-usage-example)
    * [To disable inclusion of BlueGreenStatePublisher bean into quarkus build](#to-disable-inclusion-of-bluegreenstatepublisher-bean-into-quarkus-build)
  * [MicroserviceMutexService usage example:](#microservicemutexservice-usage-example)
    * [To disable inclusion of MicroserviceMutexService bean into quarkus build](#to-disable-inclusion-of-microservicemutexservice-bean-into-quarkus-build)
  * [InMemory implementations for dev purposes:](#inmemory-implementations-for-dev-purposes)
* [Blue-Green State Logging](#blue-green-state-logging)
  * [Overview](#overview)
  * [What's New](#whats-new)
  * [How It Works](#how-it-works)
  * [Usage](#usage)
    * [Basic Log Configuration](#basic-log-configuration)
    * [Example Log Output](#example-log-output)
  * [Troubleshooting](#troubleshooting)
    * [No Blue-Green State in Logs](#no-blue-green-state-in-logs)
    * [Warning Messages](#warning-messages)
  * [Migration from Previous Versions](#migration-from-previous-versions)
  * [Security Considerations](#security-considerations)
<!-- TOC -->

## BlueGreenStatePublisher usage example:
Specify the following required properties in the application.properties:
~~~ properties
consul.url
cloud.microservice.namespace
~~~
Example:
~~~ properties
consul.url=${CONSUL_URL:} 
cloud.microservice.namespace=${NAMESAPCE:}
~~~

~~~ java 
import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
@ApplicationScoped
public class BGStatePublisherQuarkusDemo {

    @Inject
    BlueGreenStatePublisher blueGreenStatePublisher;

    public void getStateDemo() {
        BlueGreenState blueGreenState = blueGreenStatePublisher.getBlueGreenState();
        log.info("Current BG state: {}", blueGreenState);
    }

    public void subscribeDemo() {
        blueGreenStatePublisher.subscribe(newState -> log.info("Received new BG state: {}", newState));
    }

    public void unsubscribeDemo(Consumer<BlueGreenState> subscriber) {
        blueGreenStatePublisher.unsubscribe(subscriber);
    }
}
~~~

### To disable inclusion of BlueGreenStatePublisher bean into quarkus build
Specify the following property in the application.properties:
~~~ properties
blue-green.state-publisher.enabled=false
~~~

## MicroserviceMutexService usage example:
Required:
~~~ properties
consul.url
cloud.microservice.namespace
cloud.microservice.name
~~~
Optional. If not specified, pod name will be equal to the hostname of the machine the java process is running on:
~~~ properties
pod.name
~~~
Example:
~~~ properties
consul.url=${CONSUL_URL:}
cloud.microservice.namespace=${NAMESAPCE:}
cloud.microservice.name=${SERVICE_NAME:}
~~~

~~~ java 
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import lombok.extern.slf4j.Slf4j;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

@Slf4j
@ApplicationScoped
public class MicroserviceMutexServiceDemo {

    @Inject
    MicroserviceMutexService microserviceMutexService;

    public void demo() {
        boolean lockAcquired = microserviceMutexService.tryLock(Duration.ofSeconds(30), "test-lock", "test reason");
        log.info("Locked {}", lockAcquired);
        
        microserviceMutexService.unlock("test-lock");
        
        boolean locked = microserviceMutexService.isLocked("test-lock");
        log.info("Locked {}", locked);
    }
}
~~~

### To disable inclusion of MicroserviceMutexService bean into quarkus build
Specify the following property in the application.yml:
~~~ properties
blue-green.microservice-mutex-service.enabled=false
~~~

## InMemory implementations for dev purposes:
To use InMemory implementations of beans you need 
1) to disable transitive dependency bean configuration for m2m consulTokenStorage
2) switch to InMemoryConfig in build time
To achieve that - specify the following build-time property in the application.properties:
~~~ properties
blue-green.state-monitor.dev.enabled=true
quarkus.consul-source-config.m2m.enabled=false
~~~


# Blue-Green State Logging

## Overview

Starting with version 1.2.0, the Blue-Green State Monitor Quarkus extension automatically provides access to the current Blue-Green deployment state through a system property. This enables you to include the deployment state in your application logs.

## What's New

The extension now automatically:
- Subscribes to Blue-Green state changes at application startup
- Updates a system property `BG_STATE` with the current deployment state name: active, candidate, legacy
- Provides real-time state value updates during blue-green lifecycle 

## How It Works

When your Quarkus application starts up, the `BGStateSubscriberConfiguration` component:

1. **Automatic Registration**: The component is automatically registered when BlueGreenStatePublisher is enabled
2. **State Subscription**: Subscribes to Blue-Green state change events from the publisher
3. **System Property Updates**: Whenever the deployment state changes, it updates the `BG_STATE` system property
4. **Error Handling**: Gracefully handles cases where the publisher is unavailable

## Usage

### Basic Log Configuration

The most common use case is to include the Blue-Green state in your application logs. Add the deployment state to your log format in `application.properties`:

```properties
# Include Blue-Green state in console logs
quarkus.log.console.format=[%d{yyyy-MM-dd'T'HH:mm:ss.SSS}][%-5p] [bg_state:%#{BG_STATE:\-}] %s%e%n

# Include Blue-Green state in file logs
quarkus.log.file.format=[%d{yyyy-MM-dd'T'HH:mm:ss.SSS}][%-5p] [bg_state:%#{BG_STATE:\-}] %s%e%n
```

### Example Log Output

With the configuration above, your logs will look like:

```
[2025-01-15T10:30:45.123][INFO ] [bg_state:candidate] Application started successfully
[2025-01-15T10:31:00.456][INFO ] [bg_state:active] Processing user request
[2025-01-15T10:35:12.789][INFO ] [bg_state:legacy] Blue-Green switch detected
[2025-01-15T10:35:15.012][INFO ] [bg_state:legacy] Continuing processing on green environment
```


## Troubleshooting

### No Blue-Green State in Logs

If you don't see the Blue-Green state in your logs:

1. **Check BlueGreenStatePublisher Configuration**: Ensure you have the required Consul configuration or development mode enabled
2. **Verify Log Format**: Make sure you've added the `%#{BG_STATE:-}` token to your log format
3. **Check Application Logs**: Look for these startup messages:
   ```
   INFO  Subscribe to BlueGreenState change event => store BlueGreenState to the 'BG_STATE' System property
   ```

### Warning Messages

You might see these warning messages in certain scenarios:

- `Cannot get BlueGreenStatePublisher bean -> skip subscription`: BlueGreenStatePublisher is not configured or disabled
- `Cannot subscribe to BlueGreenStatePublisher for propagate Blue Green State to system properties`: Subscription failed, system property will not be updated

These warnings are informational and don't affect the normal operation of your application.

## Migration from Previous Versions

If you're upgrading from version 1.1.x:

1. **No Breaking Changes**: This feature is automatically enabled when BlueGreenStatePublisher is available
2. **Update Log Configuration**: Add the Blue-Green state token to your existing log formats
3. **Version Update**: Update your dependency version to 1.2.0 or later

## Security Considerations

- The Blue-Green state information is not sensitive and can be safely included in logs
- No additional security configuration is required for this feature
