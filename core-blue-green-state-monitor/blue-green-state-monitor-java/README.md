# blue-green-state-monitor-java library
This library provides ability to listen for BlueGreen state changes in Consul and acquire/release Global/Microservice locks in Consul

<!-- TOC -->
* [blue-green-state-monitor-java library](#blue-green-state-monitor-java-library)
  * [Listen for BlueGreen state changes or get current BlueGreen state](#listen-for-bluegreen-state-changes-or-get-current-bluegreen-state)
    * [Subscribe/Unsubscribe usage example:](#subscribeunsubscribe-usage-example)
    * [Get current state usage example:](#get-current-state-usage-example)
  * [Global Mutex](#global-mutex)
    * [Lock/Unlock usage example:](#lockunlock-usage-example)
  * [Microservice Mutex](#microservice-mutex)
    * [Lock/Unlock usage example:](#lockunlock-usage-example-1)
<!-- TOC -->

## Listen for BlueGreen state changes or get current BlueGreen state

### Subscribe/Unsubscribe usage example:
~~~ java 
import model.api.com.netcracker.cloud.bluegreen.BlueGreenState;
import service.impl.com.netcracker.cloud.bluegreen.ConsulBlueGreenStatePublisher;

import java.util.function.Consumer;
import java.util.function.Supplier;

class BGStatePublisherDemo {
    void demo(Supplier<String> consulTokenSupplier) {
        try (ConsulBlueGreenStatePublisher bgStatePublisher = new ConsulBlueGreenStatePublisher(consulTokenSupplier)) {
            Consumer<BlueGreenState> subscriber = state -> {
                System.out.printf("New state = %s\n", state);
            };
            bgStatePublisher.subscribe(subscriber);
            //... do some work

            // unsubscribe if subscriber is not needed anymore
            bgStatePublisher.unsubscribe(subscriber);
            //... do other tings
        }
    }
}
~~~

### Get current state usage example:
~~~ java 
import model.api.com.netcracker.cloud.bluegreen.BlueGreenState;
import service.impl.com.netcracker.cloud.bluegreen.ConsulBlueGreenStatePublisher;

import java.util.function.Supplier;

class BGStateGetDemo {
    void demo(Supplier<String> consulTokenSupplier) {
        try (ConsulBlueGreenStatePublisher bgStatePublisher = new ConsulBlueGreenStatePublisher(consulTokenSupplier)) {
            BlueGreenState blueGreenState = bgStatePublisher.getBlueGreenState();
        }
    }
}
~~~

## MicroserviceMutexService usage example:
Deprecated. Mutexes were not approved by the committee.
~~~ java 
import service.api.com.netcracker.cloud.bluegreen.MicroserviceMutexService;
import service.impl.com.netcracker.cloud.bluegreen.ConsulMicroserviceMutexService;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

@Slf4j
public class MicroserviceMutexServiceDemo {

    public void demo(Supplier<String> consulTokenSupplier) {
        try (ConsulMicroserviceMutexService microserviceMutexService = ConsulMicroserviceMutexService(consulTokenSupplier)) {
          boolean lockAcquired = microserviceMutexService.tryLock(Duration.ofSeconds(30), "test reason");
          log.info("Locked {}", lockAcquired);
                  
          microserviceMutexService.unlock();
          
          boolean locked = microserviceMutexService.isLocked();
          log.info("Locked {}", locked);

        }
    }

}
~~~
