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
<!-- TOC -->

## BlueGreenStatePublisher TokenStorage dependency:

There are 2 implementations which provide TokenStorage bean required for BlueGreenStatePublisher bean:

1. consul-config-provider-spring-webclient
~~~ xml
        <dependency>
            <groupId>org.qubership.cloud</groupId>
            <artifactId>consul-config-provider-spring-webclient</artifactId>
        </dependency> 
~~~
2. consul-config-provider-spring-resttemplate
~~~ xml
        <dependency>
            <groupId>org.qubership.cloud</groupId>
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
import model.api.org.qubership.cloud.bluegreen.BlueGreenState;
import service.api.org.qubership.cloud.bluegreen.BlueGreenStatePublisher;
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
import model.api.org.qubership.cloud.bluegreen.BlueGreenState;
import service.api.org.qubership.cloud.bluegreen.BlueGreenStatePublisher;
import config.spring.org.qubership.cloud.bluegreen.EnableBlueGreenStatePublisher;
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
import service.api.org.qubership.cloud.bluegreen.MicroserviceMutexService;
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
import service.api.org.qubership.cloud.bluegreen.MicroserviceMutexService;
import config.spring.org.qubership.cloud.bluegreen.BlueGreenMicroserviceMutexConfiguration;
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