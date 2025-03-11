package org.qubership.cloud.springcloud.config.source;

import io.quarkus.runtime.LaunchMode;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PropertyManager {
    private static final String DEFAULT_CONFIG_SERVER_URL = "http://config-server:8080";
    private static final long serialVersionUID = 3127679154588598693L;
    private static Logger LOGGER = LoggerFactory.getLogger(PropertyManager.class);
    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    Map<String, String> properties;
    private AtomicReference<ConfigServerClient> configServerClientHolder;


    // default modifier used to simplify testing
    PropertyManager() {
        configServerClientHolder = new AtomicReference<>();
        properties = new ConcurrentHashMap<>();
    }

    public static PropertyManager getInstance() {
        return PropertyManagerHolder.holder;
    }

    public Map<String, String> getProperties() {
        if (io.quarkus.runtime.LaunchMode.current() == LaunchMode.TEST)
            return Collections.emptyMap();
        if (properties.isEmpty()) {
            ConfigServerClient client = getClient();
            if (client != null) {
                properties = client.getProperties().getPropertySources().iterator().next().getSource();
            }
        }
        return properties;
    }

    public void putProperties(Map<String, String> properties) {
        if (io.quarkus.runtime.LaunchMode.current() == LaunchMode.TEST)
            return;
        ConfigServerClient client = getClient();
        if (client != null) {
            client.putProperties(properties);
            this.properties.putAll(properties);
        } else {
            LOGGER.warn("Can't put properties to config-server. Client is not configured.");
        }
    }

    protected ConfigServerClient getClient() {
        UUID sessionId = UUID.randomUUID();
        ConfigServerClient client = null;
        rwl.readLock().lock();
        boolean initMode = false;
        try {
            client = configServerClientHolder.get();
            if (client == null) {
                rwl.readLock().unlock();
                rwl.writeLock().lock();
                try {
                    client = configServerClientHolder.get();
                    LOGGER.debug("[{}]CSGetClient={}", sessionId, client);
                    if (client == null) {
                        initMode = true;
                        LOGGER.debug("[{}]Init PropertyManager Start", sessionId);
                        client = ConfigServerClientInitStub.getEmptyConfigServerClient();
                        LOGGER.debug("[{}]CSClient={}", sessionId, client);
                        configServerClientHolder.set(client);
                        rwl.readLock().lock();
                        final Config cfg = ConfigProvider.getConfig();

                        Optional<Boolean> isStub = cfg.getOptionalValue("org.qubership.cloud.springcloud.config.source.stub", Boolean.class);
                        if (!isStub.orElse(false)) {
                            Optional<String> urlOptional = cfg.getOptionalValue("org.qubership.cloud.springcloud.config.source.ConfigServerClient/mp-rest/url", String.class);
                            String url = urlOptional.orElse(DEFAULT_CONFIG_SERVER_URL);
                            LOGGER.debug("[{}]Going to create ConfigServer client with URL={}", sessionId, url);
                            try {
                                client = new ConfigServerClientImpl(url);
                                properties = client.getProperties().getPropertySources().iterator().next().getSource();
                            } catch (Throwable e) {
                                LOGGER.error("Error creating client", e);
                            }
                            LOGGER.debug("[{}]Constructed Client {}", sessionId, client);
                            configServerClientHolder.set(client);
                        }

                        initMode = false;
                        LOGGER.debug("[{}]Init PropertyManager End", sessionId);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error in CS:", e);

                } finally {
                    rwl.writeLock().unlock();
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error:", e);
        } finally {
            if (initMode) {
                LOGGER.debug("[{}]Set ClientHolder to null", sessionId);
                configServerClientHolder.set(null);
            }
            rwl.readLock().unlock();
        }
        return client;
    }

    private static class PropertyManagerHolder {
        public static final PropertyManager holder = new PropertyManager();
    }
}