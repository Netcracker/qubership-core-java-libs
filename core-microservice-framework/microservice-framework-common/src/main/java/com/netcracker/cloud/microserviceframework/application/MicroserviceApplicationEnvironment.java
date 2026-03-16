package com.netcracker.cloud.microserviceframework.application;

import org.springframework.core.env.StandardEnvironment;

import jakarta.servlet.Filter;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * Application environment to provide additional parameters for {@link
 * MicroserviceApplicationContext}
 */
public class MicroserviceApplicationEnvironment extends StandardEnvironment {
    @NotNull
    private final Collection<String> filterPackagesToExclude;
    @NotNull
    private final Collection<Class<? extends Filter>> filterClasses;

    public MicroserviceApplicationEnvironment(@NotNull Collection<String> filterPackagesToExclude,
                                              @NotNull Collection<Class<? extends Filter>> filterClasses) {
        this.filterPackagesToExclude = filterPackagesToExclude;
        this.filterClasses = filterClasses;
    }

    @NotNull
    public Collection<String> getFilterPackagesToExclude() {
        return filterPackagesToExclude;
    }

    @NotNull
    public Collection<Class<? extends Filter>> getFilterClasses() {
        return filterClasses;
    }
}

