package com.netcracker.cloud.dbaas.common.config;

import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;

import java.util.Optional;

public interface DbaasApiPropertiesConfig {

    /**
     * Property with user role for outgoing requests.
     */
    Optional<String> runtimeUserRole();

    /**
     * Property with database name prefix.
     */
    Optional<String> dbPrefix();

    default DbaasApiProperties getDbaaseApiProperties() {
        DbaasApiProperties dbaasApiProperties = new DbaasApiProperties();
        dbaasApiProperties.setRuntimeUserRole(runtimeUserRole().orElse(null));
        dbaasApiProperties.setDbPrefix(dbPrefix().orElse(null));
        return dbaasApiProperties;
    }
}
