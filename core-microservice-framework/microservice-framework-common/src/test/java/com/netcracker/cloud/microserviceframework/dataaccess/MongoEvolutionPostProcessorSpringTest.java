package com.netcracker.cloud.microserviceframework.dataaccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import org.qubership.cloud.microserviceframework.testconfig.MongoPackagesConfigHolderTestConfiguration;
import org.qubership.cloud.microserviceframework.testconfig.MongoTestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MongoTestConfiguration.class, MongoPackagesConfigHolderTestConfiguration.class})
@TestPropertySource(properties = "mongo-evolution.auth.enabled=false")
class MongoEvolutionPostProcessorSpringTest {

    @Autowired
    private MongoEvolutionPostProcessor mongoEvolutionPostProcessor;

    @Autowired
    @Qualifier("configHolderWithEmptyPackages")
    private MongoPackagesConfigHolder configHolderWithEmptyPackages;

    @Autowired
    private MongoDatabase mongoDatabase;

    @Test
    void mongoEvolutionPostProcessorTestWithoutEmptyPackages() {
        assertDoesNotThrow(() -> mongoEvolutionPostProcessor.process(mongoDatabase));
    }

    @Test
    void getSupportedDatabaseTypeTest() {
        assertEquals(mongoDatabase.getClass(), mongoEvolutionPostProcessor.getSupportedDatabaseType());
    }

    @Test
    void mongoEvolutionPostProcessorTestWithEmptyPackages() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        changeBean();
        reInit();
        assertDoesNotThrow(() -> mongoEvolutionPostProcessor.process(mongoDatabase));
    }

    private void changeBean() throws IllegalAccessException, NoSuchFieldException {
        Field field = mongoEvolutionPostProcessor.getClass().getDeclaredField("configHolderMongo");
        field.setAccessible(true);
        field.set(mongoEvolutionPostProcessor, configHolderWithEmptyPackages);
    }

    private void reInit() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = mongoEvolutionPostProcessor.getClass().getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(mongoEvolutionPostProcessor);
    }
}
