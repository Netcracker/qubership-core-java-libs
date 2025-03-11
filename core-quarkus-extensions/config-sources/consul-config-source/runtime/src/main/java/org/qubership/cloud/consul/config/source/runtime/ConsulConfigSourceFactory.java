package org.qubership.cloud.consul.config.source.runtime;

import org.qubership.cloud.consul.provider.common.TokenStorage;
import org.qubership.cloud.quarkus.consul.client.ConsulClient;
import org.qubership.cloud.quarkus.consul.client.ConsulSourceConfig;
import org.qubership.cloud.quarkus.consul.client.RetryableConsulClient;
import org.qubership.cloud.quarkus.consul.client.model.GetValue;
import org.qubership.cloud.quarkus.consul.client.http.QueryParams;
import org.qubership.cloud.quarkus.consul.client.http.Response;
import io.quarkus.arc.Arc;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import jakarta.enterprise.event.Event;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConsulConfigSourceFactory implements ConfigSourceFactory.ConfigurableConfigSourceFactory<ConsulSourceConfig> {
    public static final String CONFIG_ROOT_NAME = "config";
    public static final String BASE_CONFIG_SOURCE_NAME = "consul-config-source";

    private static final Logger log = Logger.getLogger(ConsulConfigSourceFactory.class);

    private final TokenStorage tokenStorage;

    private final ConsulClient consulClient;

    ConsulConfigSourceFactory(ConsulClient consulClient, TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
        this.consulClient = consulClient;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ConfigSourceContext configSourceContext, ConsulSourceConfig consulSourceConfig) {
        if (consulSourceConfig.enabled()) {
            Optional<List<String>> propertiesRoot = consulSourceConfig.propertiesRoot();
            if (propertiesRoot.isEmpty()) {
                String namespace = ConfigProvider.getConfig().getValue("cloud.microservice.namespace", String.class);
                String appName = ConfigProvider.getConfig().getValue("cloud.microservice.name", String.class);
                List<String> propertiesRootList = new ArrayList<>(2);
                propertiesRootList.add(CONFIG_ROOT_NAME + "/" + namespace + "/application");
                propertiesRootList.add(CONFIG_ROOT_NAME + "/" + namespace + "/" + appName);
                propertiesRoot = Optional.of(propertiesRootList);
            }
            return reverse(propertiesRoot.get().stream())
                    .map((String propertyRoot) -> createConsulConfigSource(propertyRoot, consulSourceConfig))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static <T> Stream<T> reverse(Stream<T> stream) {
        LinkedList<T> stack = new LinkedList<>();
        stream.forEach(stack::push);
        return stack.stream();
    }

    private ConsulConfigSource createConsulConfigSource(String propertyRoot, ConsulSourceConfig consulSourceConfig) {
        Response<List<GetValue>> kvValuesResponse = consulClient.getKVValues(propertyRoot, tokenStorage.get());
        List<GetValue> values = kvValuesResponse.getValue();
        if (values == null) {
            values = Collections.emptyList();
        }
        AtomicReference<Map<String, String>> propertySourceRef = new AtomicReference<>(kVAsMap(values, propertyRoot));
        RetryableConsulClient retryableConsulClient = new RetryableConsulClient(consulClient, tokenStorage);
        Thread watcher = new Thread(() -> {
            log.infov("Start watching for Consul properties at {0}", propertyRoot);
            Long lastIndex = kvValuesResponse.getConsulIndex();
            while (true) {
                try {
                    Response<List<GetValue>> pooledKvValuesResponse = retryableConsulClient.getKVValues(propertyRoot, new QueryParams(consulSourceConfig.waitTime(), lastIndex));
                    lastIndex = pooledKvValuesResponse.getConsulIndex();
                    if (pooledKvValuesResponse.getValue() != null) {
                        Map<String, String> updatedProperties = kVAsMap(pooledKvValuesResponse.getValue(), propertyRoot);
                        log.infov("Got update at {0} with {1} updated items", propertyRoot, updatedProperties.size());
                        propertySourceRef.set(updatedProperties);
                        firePropertiesUpdated();
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        });
        watcher.setDaemon(true);
        watcher.setName("consul-config-properties-watcher-" + propertyRoot);
        watcher.start();
        return new ConsulConfigSource(BASE_CONFIG_SOURCE_NAME + "-" + propertyRoot, propertySourceRef);
    }

    protected void firePropertiesUpdated() {
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fire(new ConfigUpdatedEvent());
    }

    private Map<String, String> kVAsMap(List<GetValue> kvValues, String root) {
        if (kvValues == null || kvValues.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new HashMap<>();
        for (GetValue val : kvValues) {
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
