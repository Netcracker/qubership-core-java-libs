package org.qubership.cloud.microserviceframework.testconfig;

import org.qubership.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import org.qubership.cloud.microserviceframework.dataaccess.MongoEvolutionPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;

@TestConfiguration
public class MongoPackagesConfigHolderTestConfiguration {

    @Bean
    public MongoPackagesConfigHolder configHolderMongo(){
        MongoPackagesConfigHolder mongoConfigHolder = MongoPackagesConfigHolder.INSTANCE;
        mongoConfigHolder.setCommonPackage(TEST_MONGO_PACKAGE);
        mongoConfigHolder.setMicroserviceCustomPackage(MS_CUSTOM_PACKAGE);
        mongoConfigHolder.setTenantAwareCustomPackage(TENANT_AWARE_CUSTOM_PACKAGE);
        return mongoConfigHolder;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MongoPackagesConfigHolder configHolderWithEmptyPackages() {
        MongoPackagesConfigHolder mongoConfigHolder = MongoPackagesConfigHolder.INSTANCE;
        mongoConfigHolder.setUsingCustomPackages(true);
        mongoConfigHolder.setCommonPackage(NULL_PACKAGE);
        mongoConfigHolder.setDefaultPackage(NULL_PACKAGE);
        mongoConfigHolder.setMicroserviceCustomPackage(NULL_PACKAGE);
        mongoConfigHolder.setTenantAwareCustomPackage(NULL_PACKAGE);
        return mongoConfigHolder;
    }

    @Bean
    public MongoEvolutionPostProcessor mongoEvolutionPostProcessor(@Autowired Environment environment) {
        return new MongoEvolutionPostProcessor(environment);
    }
}
