# blue-green-state-monitor-quarkus library
Provides configuration with BlueGreenStatePublisher Quarkus bean

<!-- TOC -->
* [blue-green-state-monitor-quarkus library](#blue-green-state-monitor-quarkus-library)
  * [BlueGreenStatePublisher usage example:](#bluegreenstatepublisher-usage-example)
    * [To disable inclusion of BlueGreenStatePublisher bean into quarkus build](#to-disable-inclusion-of-bluegreenstatepublisher-bean-into-quarkus-build)
  * [MicroserviceMutexService usage example:](#microservicemutexservice-usage-example)
    * [To disable inclusion of MicroserviceMutexService bean into quarkus build](#to-disable-inclusion-of-microservicemutexservice-bean-into-quarkus-build)
  * [InMemory implementations for dev purposes:](#inmemory-implementations-for-dev-purposes)
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
import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
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
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
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