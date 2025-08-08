package org.qubership.cloud.microserviceframework.config;

import org.qubership.cloud.microserviceframework.config.annotation.EnableCustomChangeLogsPackages;
import org.qubership.cloud.microserviceframework.dataaccess.MongoEvolutionPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

@Slf4j
@Configuration
public class MongoEvolutionConfiguration {

    public static final String MICROSERVICE_MONGO_EVOLUTION_DATA_BEANS = "microservice.mongo.evolution.data.beans";

    @Value("${microservice.mongo.evolution.package:org.qubership.cloud}")
    private String mongoEvolutionPackage;

    @Value("${microservice.mongo.evolution.package.common:#{null}}")
    private String mongoEvolutionPackageCommon;

    @Value("${application.common.package:org.qubership.cloud}")
    private String applicationPackage;

    @Bean
    public MongoPackagesConfigHolder mongoPackagesConfigHolder() {
        MongoPackagesConfigHolder configHolder = MongoPackagesConfigHolder.getInstance();
        configHolder.setDefaultPackage(mongoEvolutionPackage);
        configHolder.setCommonPackage(mongoEvolutionPackageCommon);
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(EnableCustomChangeLogsPackages.class));

        Set<BeanDefinition> definitions = provider.findCandidateComponents(applicationPackage);
        BeanDefinition definition = definitions.stream().findFirst().orElse(null);

        if (definition != null) {
            String className = definition.getBeanClassName();
            Class<?> annotatedClass = null;
            try {
                annotatedClass = Class.forName(className);
            } catch (Exception e) {
                log.error(e.toString());
            }

            if (annotatedClass != null) {
                configHolder.setUsingCustomPackages(true);
                EnableCustomChangeLogsPackages annotation = annotatedClass.getAnnotation(EnableCustomChangeLogsPackages.class);
                if (!annotation.microserviceChangeLogsPackage().equals("")){
                    configHolder.setMicroserviceCustomPackage(annotation.microserviceChangeLogsPackage());
                }
                if (!annotation.tenantAwareChangeLogsPackage().equals("")){
                    configHolder.setTenantAwareCustomPackage(annotation.tenantAwareChangeLogsPackage());
                }
            }
        }
        return configHolder;
    }

    @Bean
    public MongoEvolutionPostProcessor mongoEvolutionPostProcessor(@Autowired Environment environment) {
        return new MongoEvolutionPostProcessor(environment);
    }

}
