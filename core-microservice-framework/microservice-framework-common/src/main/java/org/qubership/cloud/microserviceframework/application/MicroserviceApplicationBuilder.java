package org.qubership.cloud.microserviceframework.application;

import org.jetbrains.annotations.NotNull;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;

import jakarta.servlet.Filter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Microservice core Spring application builder which is used as a wrapper on {@link SpringApplicationBuilder} to allow
 * microservices specify custom parameters
 */
public final class MicroserviceApplicationBuilder {
    @NotNull
    private final Collection<String> filterPackagesToExclude;
    @NotNull
    private final Collection<Class<? extends Filter>> filterClasses;
    @NotNull
    private final Collection<String> arguments;
    private Class<?> applicationClass;

    public MicroserviceApplicationBuilder() {
        this.filterPackagesToExclude = new HashSet<>();
        this.filterPackagesToExclude.add("org.qubership");
        this.filterClasses = new HashSet<>();
        this.filterClasses.add(SpringPreAuthnContextProviderFilter.class);
        this.filterClasses.add(SpringPostAuthnContextProviderFilter.class);
        this.arguments = new HashSet<>();
    }

    /**
     * Specify an application class which will be used to create an application
     *
     * @param applicationClass application class
     * @return current builder
     */
    public final MicroserviceApplicationBuilder withApplicationClass(@NotNull Class<?> applicationClass) {
        this.applicationClass = applicationClass;
        return this;
    }

    /**
     * Specify the command line arguments which will be used to create an application
     *
     * @param arguments set of arguments provided in command line
     * @return current builder
     */
    public final MicroserviceApplicationBuilder withArguments(@NotNull String... arguments) {
        this.arguments.addAll(Arrays.asList(arguments));
        return this;
    }

    /**
     * Specify packages which will be ignored during application construction and filters from these packages will not
     * be added to a application filter chain provided
     *
     * @param filterPackagesToExclude set of packages to skip
     * @return current builder
     */
    public final MicroserviceApplicationBuilder withFilterPackagesToExclude(@NotNull String... filterPackagesToExclude) {
        this.filterPackagesToExclude.addAll(Arrays.asList(filterPackagesToExclude));
        return this;
    }

    /**
     * Specify filter classes which will be instantiated during application construction even if a package for this
     * class is in the exclude package list
     *
     * @param filterClasses set of classes for filter to instantiate
     * @return current builder
     * @see MicroserviceApplicationBuilder#withExceptFilterClasses(Class[])
     */
    @SafeVarargs
    public final MicroserviceApplicationBuilder withExceptFilterClasses(@NotNull Class<? extends Filter>... filterClasses) {
        this.filterClasses.addAll(Arrays.asList(filterClasses));
        return this;
    }

    /**
     * Construct and run application based on specified parameters
     *
     * @return context
     */
    public MicroserviceApplicationContext build() {
        return (MicroserviceApplicationContext) new SpringApplicationBuilder(applicationClass)
                .contextFactory(ApplicationContextFactory.ofContextClass(MicroserviceApplicationContext.class))
                .bannerMode(Banner.Mode.OFF)
                .environment(new MicroserviceApplicationEnvironment(filterPackagesToExclude, filterClasses))
                .run(arguments.toArray(new String[0]));
    }
}

