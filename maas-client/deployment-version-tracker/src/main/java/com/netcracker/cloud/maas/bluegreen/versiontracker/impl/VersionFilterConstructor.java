package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.model.NamespaceVersion;
import com.netcracker.cloud.bluegreen.api.model.State;
import com.netcracker.cloud.bluegreen.api.model.Version;

import java.util.Optional;
import java.util.function.Predicate;

public class VersionFilterConstructor {
    public static Predicate<String> constructVersionFilter(BlueGreenState bgState) {
        NamespaceVersion currentNsVersion = bgState.getCurrent();
        Optional<NamespaceVersion> siblingNsV = bgState.getSibling();
        State blueGreenState = currentNsVersion.getState();
        if (siblingNsV.isEmpty() || siblingNsV.get().getState() == State.IDLE) {
            return new PrintablePredicate<>(v -> true, "true");
        } else {
            switch (blueGreenState) {
                case ACTIVE -> {
                    Version siblingVersion = siblingNsV.get().getVersion();
                    return new PrintablePredicate<>(v -> !new Version(v).equals(siblingVersion), "!" + siblingVersion);
                }
                case CANDIDATE, LEGACY -> {
                    Version currentVersion = currentNsVersion.getVersion();
                    return new PrintablePredicate<>(v -> new Version(v).equals(currentVersion), currentVersion.toString());
                }
                default -> throw new IllegalStateException("Invalid Blue Green State " + blueGreenState);
            }
        }
    }

    /**
     * Builds a filter over the {@code X-Version-Name} header value (the BG state name: active/candidate/legacy).
     * Mirrors {@link #constructVersionFilter} but matches by state name instead of version number, so it works
     * regardless of the numeric version assigned to the sibling/current namespace.
     */
    public static Predicate<String> constructVersionNameFilter(BlueGreenState bgState) {
        NamespaceVersion currentNsVersion = bgState.getCurrent();
        Optional<NamespaceVersion> siblingNsV = bgState.getSibling();
        State blueGreenState = currentNsVersion.getState();
        if (siblingNsV.isEmpty() || siblingNsV.get().getState() == State.IDLE) {
            return new PrintablePredicate<>(v -> true, "true");
        } else {
            switch (blueGreenState) {
                case ACTIVE -> {
                    String siblingName = siblingNsV.get().getState().getName();
                    return new PrintablePredicate<>(name -> !siblingName.equalsIgnoreCase(name), "!" + siblingName);
                }
                case CANDIDATE, LEGACY -> {
                    String currentName = blueGreenState.getName();
                    return new PrintablePredicate<>(currentName::equalsIgnoreCase, currentName);
                }
                default -> throw new IllegalStateException("Invalid Blue Green State " + blueGreenState);
            }
        }
    }
}
