package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.security.core.utils.k8s.M2MAuthMode;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * The addresses to talk to the DBaaS API with, resolved for an {@link M2MAuthMode}.
 *
 * @param url      where the client sends its requests
 * @param agentUrl where requests carrying a legacy token go
 */
@Slf4j
public record DbaasApiAddress(String url, String agentUrl) {

    public static final String DEFAULT_AGENT_ADDRESS = "http://dbaas-agent:8080";
    public static final String AGENT_ADDRESS_PROPERTY = "dbaas.api.address";
    public static final String AGENT_ADDRESS_ENV = "DBAAS_API_ADDRESS";
    public static final String DBAAS_ADDRESS_PROPERTY = "api.dbaas.address";
    public static final String DBAAS_ADDRESS_ENV = "API_DBAAS_ADDRESS";

    /**
     * Takes both addresses from the configuration.
     *
     * @throws IllegalStateException if the M2M auth mode is k8s and no DBaaS address is configured
     */
    public static DbaasApiAddress resolve(M2MAuthMode mode) {
        return resolve(mode, null, null);
    }

    /**
     * @param mode         decides which of the addresses the client sends its requests to
     * @param agentAddress dbaas-agent address; when null or blank it is taken from the
     *                     {@value #AGENT_ADDRESS_PROPERTY} property or the {@value #AGENT_ADDRESS_ENV} env, and that
     *                     falls back to {@value #DEFAULT_AGENT_ADDRESS}
     * @param dbaasAddress DBaaS address; when null or blank it is taken from the
     *                     {@value #DBAAS_ADDRESS_PROPERTY} property or the {@value #DBAAS_ADDRESS_ENV} env
     * @throws IllegalStateException if {@code mode} is k8s and no DBaaS address is given or configured
     */
    public static DbaasApiAddress resolve(M2MAuthMode mode, String agentAddress, String dbaasAddress) {
        String agent = nonBlank(agentAddress).or(DbaasApiAddress::configuredAgentAddress).orElse(DEFAULT_AGENT_ADDRESS);
        Optional<String> direct = nonBlank(dbaasAddress).or(DbaasApiAddress::configuredDbaasAddress);
        return new DbaasApiAddress(switch (mode) {
            case LEGACY -> agent;
            case HYBRID -> direct.orElseGet(() -> {
                log.warn("DBaaS address is not available, falling back to dbaas-agent. Specify '{}' property or '{}' env to DBaaS url",
                        DBAAS_ADDRESS_PROPERTY, DBAAS_ADDRESS_ENV);
                return agent;
            });
            case K8S -> direct.orElseThrow(() -> new IllegalStateException(DBAAS_ADDRESS_PROPERTY + " is not set: with "
                    + M2MAuthMode.M2M_AUTH_MODE_ENV + "=k8s the client sends requests directly to DBaaS, set "
                    + DBAAS_ADDRESS_PROPERTY + " or " + DBAAS_ADDRESS_ENV + " to the DBaaS URL"));
        }, agent);
    }

    private static Optional<String> configuredAgentAddress() {
        return configured(AGENT_ADDRESS_PROPERTY, AGENT_ADDRESS_ENV);
    }

    private static Optional<String> configuredDbaasAddress() {
        return configured(DBAAS_ADDRESS_PROPERTY, DBAAS_ADDRESS_ENV);
    }

    private static Optional<String> configured(String property, String env) {
        return nonBlank(System.getProperty(property)).or(() -> nonBlank(System.getenv(env)));
    }

    private static Optional<String> nonBlank(String candidate) {
        return Optional.ofNullable(candidate).filter(value -> !value.isBlank());
    }
}
