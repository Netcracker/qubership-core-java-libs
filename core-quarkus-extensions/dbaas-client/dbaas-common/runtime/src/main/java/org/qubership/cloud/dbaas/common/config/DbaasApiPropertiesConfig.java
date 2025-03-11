package org.qubership.cloud.dbaas.common.config;

import org.qubership.cloud.dbaas.client.entity.DbaasApiProperties;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import lombok.Getter;

import java.util.Optional;

@ConfigGroup
@Getter
public class DbaasApiPropertiesConfig {

    /**
     * Property with user role for outgoing requests.
     */
    @ConfigItem
    public Optional<String> runtimeUserRole;

    /**
     * Property with database name prefix.
     */
    @ConfigItem
    public Optional<String> dbPrefix;

    public DbaasApiProperties getDbaaseApiProperties() {
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        dbaasApiProperties.setRuntimeUserRole(runtimeUserRole.orElse(null));
        dbaasApiProperties.setDbPrefix(dbPrefix.orElse(null));
        return dbaasApiProperties;
    }
}
