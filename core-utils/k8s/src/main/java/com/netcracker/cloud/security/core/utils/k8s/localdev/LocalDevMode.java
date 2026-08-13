package com.netcracker.cloud.security.core.utils.k8s.localdev;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocalDevMode {

    public static final String QUARKUS_PROFILE_PROPERTY = "quarkus.profile";

    public static final String SPRING_PROFILES_ACTIVE_PROPERTY = "spring.profiles.active";

    public static final String DEV_PROFILE = "dev";

    public static boolean isEnabled() {
        return hasDevProfile(System.getProperty(QUARKUS_PROFILE_PROPERTY))
                || hasDevProfile(System.getProperty(SPRING_PROFILES_ACTIVE_PROPERTY));
    }

    private static boolean hasDevProfile(String profiles) {
        return Optional.ofNullable(profiles)
                .map(value -> value.split(" *, *"))
                .stream()
                .flatMap(Arrays::stream)
                .anyMatch(DEV_PROFILE::equalsIgnoreCase);
    }
}
