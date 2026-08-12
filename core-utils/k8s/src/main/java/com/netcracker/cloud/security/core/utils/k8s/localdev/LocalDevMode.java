package com.netcracker.cloud.security.core.utils.k8s.localdev;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocalDevMode {

    public static final String QUARKUS_PROFILE_PROPERTY = "quarkus.profile";

    public static final String SPRING_PROFILES_ACTIVE_PROPERTY = "spring.profiles.active";

    public static final String DEV_PROFILE = "dev";

    public static boolean isEnabled() {
        if (DEV_PROFILE.equalsIgnoreCase(System.getProperty(QUARKUS_PROFILE_PROPERTY, "-"))) {
            return true;
        }
        return System.getProperty(SPRING_PROFILES_ACTIVE_PROPERTY, "-").contains(DEV_PROFILE);
    }
}
