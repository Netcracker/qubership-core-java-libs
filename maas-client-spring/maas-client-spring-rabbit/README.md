# maas-client-spring

You need to put `@EnableDynamicQueueBindings` in your application.
This annotation includes configuration classes: `DynamicQueueBindingManagerConfiguration.class`, `DeploymentVersionTrackerConfiguration.class`, `MaasRabbitLocalDevConfig.class`
For local dev purposes you can put `maas.local-dev.enabled:true` property, then `DeploymentVersionTrackerConfiguration` bean will be substituted by local one.

Also you need to have an M2MManager bean, for example from annotation `@EnableM2MManager`.
Versioned exchange is created using VersionedBinding class and marked as `@Bean`.

Application configuration class:

```java     
import org.qubership.cloud.maas.spring.rabbit.annotation.VersionedBinding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitTestConfiguration {


    @Bean
    Queue myQueue() {
        return new Queue("my-queue-test-1");
    }

    //versioned business exchange is created by MaaS declaration, not manually.
    @Bean
    VersionedBinding myBinding() {
        return new VersionedBinding("my-exchange-test", "my-queue-test", "", null);
    }

}

```
