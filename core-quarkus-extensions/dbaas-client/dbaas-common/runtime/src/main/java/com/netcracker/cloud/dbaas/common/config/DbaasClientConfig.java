package com.netcracker.cloud.dbaas.common.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

import java.util.Optional;

@ConfigMapping(prefix = "quarkus.dbaas.api")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DbaasClientConfig {

    String DEFAULT_DBAAS_AGENT_ADDRESS = "http://dbaas-agent:8080";

    /**
     * dbaas agent url
     */
    @WithName("agent.url")
    Optional<String> dbaasAgentUrl();

    /**
     * dbaas url.
     */
    @WithName("aggregator.address")
    Optional<String> dbaasUrl();

    /**
     * dbaas aggregator username
     */
    @WithName("aggregator.username")
    Optional<String> dbaasUsername();

    /**
     * dbaas aggregator password
     */
    @WithName("aggregator.password")
    Optional<String> dbaasPassword();
}
