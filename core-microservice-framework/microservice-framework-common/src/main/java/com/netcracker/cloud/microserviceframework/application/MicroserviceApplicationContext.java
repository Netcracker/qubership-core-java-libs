package com.netcracker.cloud.microserviceframework.application;

import jakarta.servlet.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Collection;

;

/**
 * Application context which remove unnecessary filters from application filter chain
 */
public class MicroserviceApplicationContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceApplicationContext.class);

    private Collection<String> filterPackagesToExclude;
    private Collection<Class<? extends Filter>> filterClasses;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    public MicroserviceApplicationContext(Collection<String> filterPackagesToExclude, Collection<Class<? extends Filter>> filterClasses) {
        this.filterPackagesToExclude = filterPackagesToExclude;
        this.filterClasses = filterClasses;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void listFiltersOnRefresh() {
        ServletContextInitializerBeans initializers = new ServletContextInitializerBeans(applicationContext.getBeanFactory());
        initializers.forEach(initializer -> {
            if (initializer instanceof FilterRegistrationBean<?> filterRegistrationBean) {
                if (!filterClasses.contains(filterRegistrationBean.getFilter().getClass())) {
                    String className = filterRegistrationBean.getClass().getName();
                    boolean match = filterPackagesToExclude.stream().anyMatch(className::startsWith);
                    if (match) {
                        filterRegistrationBean.setEnabled(false);
                        LOGGER.info("Filter={} will not be added to an application filter chain since its package is in exclude package list", className);
                    }
                }
            }
        });
    }
}

