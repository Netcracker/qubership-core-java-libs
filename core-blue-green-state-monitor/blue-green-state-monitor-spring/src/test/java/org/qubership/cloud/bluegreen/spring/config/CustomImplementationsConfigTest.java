package org.qubership.cloud.bluegreen.spring.config;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.bluegreen.api.service.GlobalMutexService;
import org.qubership.cloud.bluegreen.api.service.MicroserviceMutexService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest(classes = {
        CustomImplementationsConfigTest.CustomConfig.class,
        InMemoryConfig.class,
        BlueGreenGlobalMutexConfiguration.class,
        BlueGreenMicroserviceMutexConfiguration.class,
        BlueGreenStatePublisherConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CustomImplementationsConfigTest {

    @Autowired
    BlueGreenStatePublisher blueGreenStatePublisher;
    @Autowired
    GlobalMutexService globalMutexService;
    @Autowired
    MicroserviceMutexService microserviceMutexService;

    @Test
    void testBlueGreenStatePublisherBean() {
        Assertions.assertNotNull(blueGreenStatePublisher);
        Assertions.assertTrue(blueGreenStatePublisher instanceof CustomBlueGreenStatePublisher);
    }

    @Test
    void testGlobalMutexServiceBean() {
        Assertions.assertNotNull(globalMutexService);
        Assertions.assertTrue(globalMutexService instanceof CustomGlobalMutexService);
    }

    @Test
    void testMicroserviceMutexServiceBean() {
        Assertions.assertNotNull(microserviceMutexService);
        Assertions.assertTrue(microserviceMutexService instanceof CustomMicroserviceMutexService);
    }

    static class CustomConfig {
        @Bean
        BlueGreenStatePublisher blueGreenStatePublisher() {
            return Mockito.mock(CustomBlueGreenStatePublisher.class);
        }

        @Bean
        GlobalMutexService globalMutexService() {
            return Mockito.mock(CustomGlobalMutexService.class);
        }

        @Bean
        MicroserviceMutexService microserviceMutexService() {
            return Mockito.mock(CustomMicroserviceMutexService.class);
        }
    }

    static abstract class CustomBlueGreenStatePublisher implements BlueGreenStatePublisher {
    }
    static abstract class CustomGlobalMutexService implements GlobalMutexService {
    }
    static abstract class CustomMicroserviceMutexService implements MicroserviceMutexService {
    }

}
