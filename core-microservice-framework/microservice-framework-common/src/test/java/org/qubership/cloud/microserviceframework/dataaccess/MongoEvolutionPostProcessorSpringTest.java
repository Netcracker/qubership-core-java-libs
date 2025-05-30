package org.qubership.cloud.microserviceframework.dataaccess;

import org.qubership.cloud.dbaas.client.entity.database.MongoDatabase;
import org.qubership.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import org.qubership.cloud.microserviceframework.testconfig.MongoTestConfiguration;
import org.qubership.cloud.microserviceframework.testconfig.MongoPackagesConfigHolderTestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MongoPackagesConfigHolderTestConfiguration.class,
        MongoTestConfiguration.class
})
@TestPropertySource(properties = "mongo-evolution.auth.enabled=false")
public class MongoEvolutionPostProcessorSpringTest {

    @Autowired
    private MongoEvolutionPostProcessor mongoEvolutionPostProcessor;

    @Autowired
    @Qualifier("configHolderWithEmptyPackages")
    private MongoPackagesConfigHolder configHolderWithEmptyPackages;

    @Autowired
    private MongoDatabase mongoDatabase;

    @Test
    public void mongoEvolutionPostProcessorTestWithoutEmptyPackages() {
        assertDoesNotThrow(() -> mongoEvolutionPostProcessor.process(mongoDatabase));
    }

    @Test
    public void getSupportedDatabaseTypeTest() {
        assertEquals(mongoDatabase.getClass(), mongoEvolutionPostProcessor.getSupportedDatabaseType());
    }

    @Test
    public void mongoEvolutionPostProcessorTestWithEmptyPackages() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        changeBean();
        reInit();
        assertDoesNotThrow(() -> mongoEvolutionPostProcessor.process(mongoDatabase));
    }

    private void changeBean() throws IllegalAccessException, NoSuchFieldException {
        Field field = mongoEvolutionPostProcessor.getClass().getDeclaredField("configHolderMongo");
        field.setAccessible(true);
        field.set(mongoEvolutionPostProcessor,configHolderWithEmptyPackages);
    }

    private void reInit() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = mongoEvolutionPostProcessor.getClass().getDeclaredMethod("init");
        method.setAccessible(true);
        method.invoke(mongoEvolutionPostProcessor);
    }
}
