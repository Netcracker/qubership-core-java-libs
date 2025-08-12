package com.netcracker.cloud.microserviceframework.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import jakarta.servlet.Filter;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

;

/**
 * Application context which remove unnecessary filters from application filter chain
 */
public class MicroserviceApplicationContext extends
    AnnotationConfigServletWebServerApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceApplicationContext.class);

    @NotNull
    private final Collection<String> filterPackagesToExclude;
    @NotNull
    private final Collection<Class<? extends Filter>> filterClasses;

    public MicroserviceApplicationContext() {
        this.filterPackagesToExclude = new HashSet<>();
        this.filterClasses = new HashSet<>();
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        if (environment instanceof MicroserviceApplicationEnvironment) {
            MicroserviceApplicationEnvironment applicationEnvironment = (MicroserviceApplicationEnvironment) environment;
            filterClasses.addAll(applicationEnvironment.getFilterClasses());
            filterPackagesToExclude.addAll(applicationEnvironment.getFilterPackagesToExclude());
        }
        super.setEnvironment(environment);
    }

    @Override
    protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
        LOGGER.info("List of packages to skip from instantiating filters: {}", filterPackagesToExclude);
        LOGGER.info("List of filter classes to instantiate even if package is in skip list: {}", filterClasses);
        return super.getServletContextInitializerBeans()
                .stream()
                .filter(this::filterFilterRegistrationBean)
                .collect(Collectors.toList());
    }

    private boolean filterFilterRegistrationBean(ServletContextInitializer initializer) {
        if (initializer instanceof FilterRegistrationBean) {
            FilterRegistrationBean bean = (FilterRegistrationBean) initializer;
            Class<? extends Filter> clazz = bean.getFilter().getClass();
            if (filterClasses.contains(clazz)) {
                // filter class is added as exception
                return true;
            } else {
                // check if filter class is in package to exclude
                String className = clazz.getName();
                boolean match = filterPackagesToExclude.stream().anyMatch(className::startsWith);
                if (match) {
                    LOGGER.info("Filter={} will not be added to an application filter chain since its package is in exclude package list", className);
                }
                return !match;
            }
        } else {
            return true;
        }
    }
}

