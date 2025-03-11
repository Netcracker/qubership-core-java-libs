package org.qubership.cloud.quarkus.logging.manager.runtime.consul;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.consul.client.RetryableConsulClient;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.ConfigUpdatedEvent;
import org.qubership.cloud.quarkus.logging.manager.runtime.updater.event.LogUpdateEvent;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import java.util.*;


public class ConsulLoggingConfigWatchFactory {
    public static final String LOGGING_ROOT_NAME = "logging";

    private static final Logger log = Logger.getLogger(ConsulLoggingConfigWatchFactory.class);

    private final TokenStorage tokenStorage;

    private final ConsulClient consulClient;

    ConsulLoggingConfigWatchFactory(ConsulClient consulClient, TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
        this.consulClient = consulClient;
    }

    public void initConsulLoggingWatch(@Observes StartupEvent event, ConsulSourceConfig consulSourceConfig, ConsulLoggingSourceConfig consulLoggingSourceConfig) {
        if (consulLoggingSourceConfig.loggingEnabled()) {
            log.info("Init consul logging watch");
            String namespace = ConfigProvider.getConfig().getValue("cloud.microservice.namespace", String.class);
            String appName = ConfigProvider.getConfig().getValue("cloud.microservice.name", String.class);
            String propertyRoot = LOGGING_ROOT_NAME + "/" + namespace + "/" + appName;
            runConsulLoggingWatcher(propertyRoot, consulSourceConfig, consulLoggingSourceConfig);
        }
    }

    private void runConsulLoggingWatcher(String propertyRoot,
                                         ConsulSourceConfig consulSourceConfig,
                                         ConsulLoggingSourceConfig consulLoggingSourceConfig){
        RetryableConsulClient retryableConsulClient = new RetryableConsulClient(consulClient, tokenStorage);
        Thread watcher = new Thread(() -> {
            Response<List<GetValue>> kvValuesResponse = retryableConsulClient.callConsulWithRetry(propertyRoot, consulLoggingSourceConfig.consulRetryTime());
            List<GetValue> values = kvValuesResponse.getValue();
            if (values == null) {
                values = Collections.emptyList();
            }
            Map<String, String> propertySourceRef;
            try {
                propertySourceRef = kVAsMap(values, propertyRoot);
                fireLogUpdated(propertySourceRef, propertyRoot);
            } catch (Exception ex) {
                log.error(ex);
            }
            log.infov("Start watching for Consul properties at {0}", propertyRoot);
            Long lastIndex = kvValuesResponse.getConsulIndex();
            while (true) {
                try {
                    Response<List<GetValue>> pooledKvValuesResponse = retryableConsulClient.getKVValues(propertyRoot, new QueryParams(consulSourceConfig.waitTime(), lastIndex));
                    lastIndex = pooledKvValuesResponse.getConsulIndex();
                    if (pooledKvValuesResponse.getValue() != null) {
                        Map<String, String> updatedProperties = kVAsMap(pooledKvValuesResponse.getValue(), propertyRoot);
                        log.tracev("Got update at {0} with {1} updated items", propertyRoot, updatedProperties.size());
                        propertySourceRef = updatedProperties;
                        firePropertiesUpdated(propertySourceRef, propertyRoot);
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        });
        watcher.setDaemon(true);
        watcher.setName("consul-logging-watcher-" + propertyRoot);
        watcher.start();
    }


    protected void fireLogUpdated(Map<String, String> properties, String root) {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new LogUpdateEvent(properties, root));
    }

    protected void firePropertiesUpdated(Map<String, String> properties, String root) {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new ConfigUpdatedEvent(properties, root));
    }

    private Map<String, String> kVAsMap(List<GetValue> kvValues, String root) {
        if (kvValues == null || kvValues.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (GetValue val : kvValues) {
            if (val.getValue() == null) {
                continue;
            }
            int prefixSize = root.length();
            if (!root.endsWith("/")) {
                prefixSize += 1;
            }
            String effectiveKey = val.getKey().substring(prefixSize).replace("/", ".");

            result.put(effectiveKey, val.getDecodedValue());
        }
        return result;
    }

}
