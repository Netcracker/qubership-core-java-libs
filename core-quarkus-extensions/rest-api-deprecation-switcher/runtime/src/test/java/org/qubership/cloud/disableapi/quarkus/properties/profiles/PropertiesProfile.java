package org.qubership.cloud.disableapi.quarkus.properties.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class PropertiesProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "properties-profile";
    }
}
