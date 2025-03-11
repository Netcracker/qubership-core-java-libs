package org.qubership.cloud.dbaas.common.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Optional;

@ConfigRoot(name = "dbaas.api", phase = ConfigPhase.RUN_TIME)
public class DbaasClientConfig {
    public final static String DEFAULT_DBAAS_AGENT_ADDRESS = "http://dbaas-agent:8080";

    /**
     * dbaas agent url
     */
    @ConfigItem(name = "agent.url")
    public Optional<String> dbaasAgentUrl;

    /**
     * dbaas url.
     */
    @ConfigItem(name = "aggregator.address")
    public Optional<String> dbaasUrl;

    /**
     * dbaas aggregator username
     */
    @ConfigItem(name = "aggregator.username")
    public Optional<String> dbaasUsername;

    /**
     * dbaas aggregator password
     */
    @ConfigItem(name = "aggregator.password")
    public Optional<String> dbaasPassword;
}
