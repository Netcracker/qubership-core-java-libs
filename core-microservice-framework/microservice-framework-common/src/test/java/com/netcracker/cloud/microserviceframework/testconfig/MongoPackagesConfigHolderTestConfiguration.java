package com.netcracker.cloud.microserviceframework.testconfig;

import com.netcracker.cloud.microserviceframework.config.MongoPackagesConfigHolder;
import com.netcracker.cloud.microserviceframework.dataaccess.MongoEvolutionPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

import static com.netcracker.cloud.microserviceframework.testconfig.TestConstants.*;

@Configuration
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
