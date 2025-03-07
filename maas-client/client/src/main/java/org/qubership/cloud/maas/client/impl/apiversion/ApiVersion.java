package org.qubership.cloud.maas.client.impl.apiversion;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
public class ApiVersion {
    int major;
    int minor;

    /**
     * Tests whether this version is compatible to given as argument

     * @param o version test compatibility with
     * @return compatibility test result
     */
    public boolean isCompatible(@NotNull ApiVersion o) {
        if (major < o.major) {
            return true;
        } else if (major > o.major) {
            return false;
        }

        // major is equals, need to compare minors to make final decision
        return minor <= o.minor;
    }
}