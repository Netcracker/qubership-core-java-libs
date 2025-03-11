package org.qubership.cloud.disableapi.quarkus.annotations.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class DisabledProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "disabled-profile";
    }
}
