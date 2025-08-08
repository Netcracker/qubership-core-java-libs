package com.netcracker.cloud.microserviceframework.dataaccess;

import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import java.util.Optional;

/**
 * Microservice information provider with local development support.
 */
@Slf4j
public class LocalMSInfoProvider implements MSInfoProvider {
    private static final String LOCALDEV_NAMESPACE_ENV = "LOCALDEV_NAMESPACE";
    private static final String ATTACH_TO_CLOUD_DB_ENV = "attachToCloudDB";
    private static final String SANDBOX_DBAAS_NAMESPACE_ENV = "SANDBOX_DBAAS_NAMESPACE";

    private static final String sandboxDbaasNamespace = getEnvVariable(SANDBOX_DBAAS_NAMESPACE_ENV);
    private static final boolean attachToCloudDB = "true".equalsIgnoreCase(getEnvVariable(ATTACH_TO_CLOUD_DB_ENV));
    private static final String localDevNamespace = attachToCloudDB ? null : getEnvVariable(LOCALDEV_NAMESPACE_ENV);

    @PostConstruct
    private void resolveNamespace() {
        if (attachToCloudDB && sandboxDbaasNamespace != null) {
            namespace = sandboxDbaasNamespace;
        } else {
            if ((Optional.ofNullable(cloudNamespace).filter(s -> !s.trim().isEmpty()).isPresent()) && !("unknown".equalsIgnoreCase(cloudNamespace)) && !("default".equalsIgnoreCase(cloudNamespace))) {
                namespace = cloudNamespace;
            }
        }
    }

    @Getter
    @Value("${cloud.microservice.name:#{null}}")
    private String microserviceName;

    @Value("${cloud.microservice.namespace}")
    private String cloudNamespace;

    private String namespace;

    @Override
    public String getNamespace() {
        log.debug("dbaas-client is started with namespace: {}", namespace);
        return namespace;
    }

    @Override
    public String getLocalDevNamespace() {
        return localDevNamespace;
    }

    public static String provideLocalDevNamespace() {
        return localDevNamespace;
    }

    private static String getEnvVariable(String key) {
        String value = System.getProperty(key, System.getenv(key));
        return StringUtils.isEmpty(value) ? null : value;
    }
}
