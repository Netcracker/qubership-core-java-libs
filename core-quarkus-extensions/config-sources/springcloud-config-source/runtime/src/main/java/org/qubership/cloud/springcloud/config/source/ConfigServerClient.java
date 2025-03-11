package org.qubership.cloud.springcloud.config.source;

import java.util.Map;

public interface ConfigServerClient {

    CloudEnv getProperties();

    void putProperties(Map<String, String> properties);

}
