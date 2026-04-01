package com.netcracker.cloud.microserviceframework.application;

import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
import com.netcracker.cloud.microserviceframework.TestApplication;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class MicroserviceApplicationContextTest {

    private static final String SPRINGFRAMEWORK_PACKAGE = "org.springframework";
    private ConfigurableApplicationContext context;

    @BeforeAll
    static void init() {
        System.setProperty("spring.cloud.config.enabled", String.valueOf(false));
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void testMandatoryTrackingFilterIsPresentByDefault() {
        context = new MicroserviceApplicationBuilder()
                .withApplicationClass(TestApplication.class)
                .build();
        List<Class<?>> filterClasses = getFilterClasses(context);
        assertThat("One of mandatory filters are absent but it must be always present",
                filterClasses,
                CoreMatchers.hasItems(
                        SpringPreAuthnContextProviderFilter.class,
                        SpringPostAuthnContextProviderFilter.class
                )
        );
    }

    @Test
    void testExcludeSpringPackage() {
        context = new MicroserviceApplicationBuilder()
                .withApplicationClass(TestApplication.class)
                .withFilterPackagesToExclude(SPRINGFRAMEWORK_PACKAGE)
                .build();
        List<Class<?>> filterClasses = getFilterClasses(context);
        assertThat("AnonymousAuthenticationFilter must not be present in result list since its package is excluded from parsing",
                filterClasses,
                Matchers.not(hasItem(AnonymousAuthenticationFilter.class))
        );
    }

    @Test
    void testAnonymousAuthenticationFilterIsPresentEventSpringPackageExcluded() {
        context = new MicroserviceApplicationBuilder()
                .withApplicationClass(TestApplication.class)
                .withFilterPackagesToExclude(SPRINGFRAMEWORK_PACKAGE)
                .withExceptFilterClasses(AnonymousAuthenticationFilter.class)
                .build();
        List<Class<?>> filterClasses = getFilterClasses(context);
        assertThat("AnonymousAuthenticationFilter must be present in result list since it is added as exceptional filter",
                filterClasses,
                hasItem(AnonymousAuthenticationFilter.class)
        );
    }

    private List<Class<?>> getFilterClasses(ConfigurableApplicationContext context) {
        ServletContextInitializerBeans initializers = new ServletContextInitializerBeans(context.getBeanFactory());
        return initializers
                .stream()
                .filter(initializer -> initializer instanceof FilterRegistrationBean)
                .filter(initializer -> ((FilterRegistrationBean<?>) initializer).isEnabled())
                .map(initializer -> ((FilterRegistrationBean) initializer).getFilter().getClass())
                .collect(Collectors.toList());
    }
}
