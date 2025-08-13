# blue-green-state-monitor-spring library
Provides configuration with BlueGreenStatePublisher Spring Boot bean

<!-- TOC -->
* [blue-green-state-monitor-spring library](#blue-green-state-monitor-spring-library)
  * [BlueGreenStatePublisher TokenStorage dependency:](#bluegreenstatepublisher-tokenstorage-dependency)
  * [BlueGreenStatePublisher](#bluegreenstatepublisher)
    * [Usage with enabled AutoConfiguration example:](#usage-with-enabled-autoconfiguration-example)
    * [Usage with disabled AutoConfiguration example:](#usage-with-disabled-autoconfiguration-example)
    * [To disable instantiation of BlueGreenStatePublisher bean](#to-disable-instantiation-of-bluegreenstatepublisher-bean-)
  * [MicroserviceMutexService:](#microservicemutexservice)
    * [Usage with enabled AutoConfiguration example](#usage-with-enabled-autoconfiguration-example-1)
    * [Usage with disabled AutoConfiguration example:](#usage-with-disabled-autoconfiguration-example-1)
    * [To disable instantiation of MicroserviceMutexService bean](#to-disable-instantiation-of-microservicemutexservice-bean)
  * [InMemory implementations for dev purposes:](#inmemory-implementations-for-dev-purposes)
  * [Blue-Green State Logging](#blue-green-state-logging)
    * [Overview](#overview)
    * [Features](#features)
    * [Quick Start](#quick-start)
    * [Advanced Configuration](#advanced-configuration)
    * [Benefits](#benefits)
    * [Requirements](#requirements)
    * [Troubleshooting](#troubleshooting)
<!-- TOC -->

## BlueGreenStatePublisher TokenStorage dependency:

There are 2 implementations which provide TokenStorage bean required for BlueGreenStatePublisher bean:

1. consul-config-provider-spring-webclient
~~~ xml
        <dependency>
            <groupId>com.netcracker.cloud</groupId>
            <artifactId>consul-config-provider-spring-webclient</artifactId>
        </dependency> 
~~~
2. consul-config-provider-spring-resttemplate
~~~ xml
        <dependency>
            <groupId>com.netcracker.cloud</groupId>
            <artifactId>consul-config-provider-spring-resttemplate</artifactId>
        </dependency>
~~~

Include one of these dependencies in your SpringBoot application or provide your own implementation

## BlueGreenStatePublisher
Required properties:
~~~ properties
consul.url
cloud.microservice.namespace
~~~
By default properties will be resolved from the corresponding env variables:
~~~ properties
CONSUL_URL
NAMESPACE
SERVICE_NAME
~~~

### Usage with enabled AutoConfiguration example:
~~~ java 
import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Consumer;

@Slf4j
@Configuration
@EnableAutoConfiguration
public class BGStatePublisherSpringDemo {

    @Autowired
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

### Usage with disabled AutoConfiguration example:
~~~ java 
import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import com.netcracker.cloud.bluegreen.spring.config.EnableBlueGreenStatePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Consumer;

@Slf4j
@Configuration
@EnableBlueGreenStatePublisher
public class BGStatePublisherSpringDemo {

    @Autowired
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

### To disable instantiation of BlueGreenStatePublisher bean 
Specify the following property in the application.yml:
~~~ yaml
blue-green.state-publisher.enabled: false
~~~

## MicroserviceMutexService:
Required properties:
~~~ properties
consul.url
cloud.microservice.namespace
cloud.microservice.name
~~~
Optional. If not specified and there is no POD_NAME env variable, pod name will be equal to the hostname of the machine the java process is running on:
~~~ properties
pod.name
~~~
By default properties will be resolved from the corresponding env variables:
~~~ properties
CONSUL_URL
NAMESPACE
SERVICE_NAME
POD_NAME
~~~
### Usage with enabled AutoConfiguration example
~~~ java 
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@Slf4j
@Configuration
public class MicroserviceMutexServiceDemo {

    @Autowired
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

### Usage with disabled AutoConfiguration example:
~~~ java 
import com.netcracker.cloud.bluegreen.api.service.MicroserviceMutexService;
import com.netcracker.cloud.bluegreen.spring.config.BlueGreenMicroserviceMutexConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

@Slf4j
@Configuration
@Import(BlueGreenMicroserviceMutexConfiguration.class)
public class MicroserviceMutexServiceDemo {

    @Autowired
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

### To disable instantiation of MicroserviceMutexService bean
Specify the following property in the application.yml:
~~~ yaml
blue-green.microservice-mutex-service.enabled: false
~~~

## InMemory implementations for dev purposes:
To use InMemory implementations of beans specify the following property in the application.yml:
~~~ yaml
blue-green.state-monitor.dev.enabled: true
~~~

## Blue-Green State Logging

### Overview
The Blue-Green State Monitor library now includes automatic logging integration that injects the current Blue-Green state of your namespace into log messages. This feature helps with debugging, monitoring, and understanding which deployment slot (blue/green) is currently active when analyzing application logs.

### Features
- **Automatic State Injection**: Automatically includes the current Blue-Green state (e.g., "active", "standby") in log messages
- **Zero Configuration**: Works out of the box with Spring Boot auto-configuration  
- **Logback Integration**: Seamless integration with Logback logging framework
- **Fallback Handling**: Shows "-" when state cannot be determined (e.g., during startup)

### Quick Start

#### 1. Add Dependency
The logging feature is included in the Spring module:

```xml
<dependency>
    <groupId>com.netcracker.cloud</groupId>
    <artifactId>blue-green-state-monitor-spring</artifactId>
    <version>1.2.0</version> <!-- or higher -->
</dependency>
```

#### 2. Configure Logback
Add the custom converter to your `logback.xml` or `logback-spring.xml`:

```xml
<configuration>
    <!-- Register the Blue-Green state converter -->
    <conversionRule conversionWord="bg_state" 
                    converterClass="com.netcracker.cloud.bluegreen.spring.log.BGStateConverter"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- Include %bg_state in your log pattern -->
            <pattern>%d{HH:mm:ss.SSS} %-5level [%thread] %logger bg_state:%bg_state - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

#### 3. Example Output
With the above configuration, your logs will automatically include the Blue-Green state:

```
14:30:15.123 INFO  [main] com.example.MyService bg_state:active - Processing user request
14:30:16.456 INFO  [main] com.example.MyService bg_state:active - Operation completed successfully
```

### Advanced Configuration

#### Custom Log Patterns
You can use the `%bg_state` conversion word anywhere in your log pattern:

```xml
<!-- Simple format -->
<pattern>%d [%bg_state] %logger - %msg%n</pattern>

<!-- Detailed format with thread and level -->
<pattern>%d{ISO8601} [%-5level] [%thread] [bg_state:%bg_state] %logger{36} - %msg%n</pattern>

<!-- JSON format (when using structured logging) -->
<pattern>{"timestamp":"%d","level":"%level","thread":"%thread","bg_state":"%bg_state","logger":"%logger","message":"%msg"}%n</pattern>
```

#### State Values
The `%bg_state` converter will output:
- `active` - When the namespace is in the active Blue-Green state
- `standby` - When the namespace is in the standby Blue-Green state  
- `-` - When the state cannot be determined (e.g., during application startup, configuration issues)

### Benefits

1. **Enhanced Debugging**: Quickly identify which deployment slot was active when issues occurred
2. **Deployment Tracking**: Monitor Blue-Green deployment transitions in real-time through logs
3. **Troubleshooting**: Correlate application behavior with deployment states
4. **Compliance**: Maintain audit trails of which environment version handled specific requests

### Requirements
- Spring Boot application with Blue-Green State Monitor configured
- Logback as the logging framework (default in Spring Boot)
- Properly configured Blue-Green state monitoring (see Spring module documentation)

### Troubleshooting

**Issue**: Logs show `bg_state:-` instead of actual state
- **Cause**: Blue-Green state publisher not properly initialized or configured
- **Solution**: Verify your Blue-Green monitoring configuration and ensure the application can connect to Consul

**Issue**: `%bg_state` appears literally in logs
- **Cause**: Conversion rule not properly registered in logback configuration
- **Solution**: Ensure the `<conversionRule>` is correctly defined in your logback.xml
