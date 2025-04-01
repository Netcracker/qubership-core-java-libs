package org.qubership.cloud.microserviceframework.application;

import org.qubership.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
import org.qubership.cloud.microserviceframework.TestApplication;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class MicroserviceApplicationContextTest {

    private static final String SPRINGFRAMEWORK_PACKAGE = "org.springframework";
    private MicroserviceApplicationContext context;

    @BeforeClass
    public static void init() {
        System.setProperty("spring.cloud.config.enabled", String.valueOf(false));
    }

    @After
    public void tearDown() {
        context.close();
    }

    @Test
    public void testMandatoryTrackingFilterIsPresentByDefault() {
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
    public void testExcludeSpringPackage() {
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
    public void testAnonymousAuthenticationFilterIsPresentEventSpringPackageExcluded() {
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

    private List<Class<?>> getFilterClasses(MicroserviceApplicationContext context) {
        return context.getServletContextInitializerBeans()
                .stream()
                .filter(initializer -> initializer instanceof FilterRegistrationBean)
                .map(initializer -> ((FilterRegistrationBean) initializer).getFilter().getClass())
                .collect(Collectors.toList());
    }
}