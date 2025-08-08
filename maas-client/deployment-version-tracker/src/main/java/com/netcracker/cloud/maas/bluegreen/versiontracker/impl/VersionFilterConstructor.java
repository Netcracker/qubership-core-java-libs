package org.qubership.cloud.maas.bluegreen.versiontracker.impl;

import org.qubership.cloud.bluegreen.api.model.BlueGreenState;
import org.qubership.cloud.bluegreen.api.model.NamespaceVersion;
import org.qubership.cloud.bluegreen.api.model.State;
import org.qubership.cloud.bluegreen.api.model.Version;

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
}
