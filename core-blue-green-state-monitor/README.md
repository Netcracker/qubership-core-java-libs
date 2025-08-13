[![Maven build](https://github.com/Netcracker/qubership-core-blue-green-state-monitor/actions/workflows/maven-build.yaml/badge.svg)](https://github.com/Netcracker/qubership-core-blue-green-state-monitor/actions/workflows/maven-build.yaml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-blue-green-state-monitor)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor)
[![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-blue-green-state-monitor)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor)
[![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-blue-green-state-monitor)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor)
[![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-blue-green-state-monitor)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor)
[![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-blue-green-state-monitor)](https://sonarcloud.io/summary/overall?id=Netcracker_qubership-core-blue-green-state-monitor)

# blue-green-state-monitor-java library
This library provides ability to listen for BlueGreen state changes in Consul and acquire/release Global/Microservice locks in Consul

<!-- TOC -->
* [blue-green-state-monitor-java library](#blue-green-state-monitor-java-library)
  * [Plain java](#plain-java)
  * [Spring](#spring)
  * [Blue-Green State Logging](#blue-green-state-logging)
    * [Overview](#overview)
    * [Features](#features)
    * [Quick Start](#quick-start)
    * [Advanced Configuration](#advanced-configuration)
    * [Benefits](#benefits)
    * [Requirements](#requirements)
    * [Troubleshooting](#troubleshooting)
  * [Quarkus](#quarkus)
<!-- TOC -->

## Plain java
See [blue-green-state-monitor-java-plain readme](./blue-green-state-monitor-java/README.md)

## Spring
See [blue-green-state-monitor-java-spring readme](./blue-green-state-monitor-spring/README.md)

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
    <groupId>org.qubership.cloud</groupId>
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
                    converterClass="org.qubership.cloud.bluegreen.spring.log.BGStateConverter"/>

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