package org.qubership.cloud.disableapi.quarkus.annotations.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class AnnotationsProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "annotations-profile";
    }
}
