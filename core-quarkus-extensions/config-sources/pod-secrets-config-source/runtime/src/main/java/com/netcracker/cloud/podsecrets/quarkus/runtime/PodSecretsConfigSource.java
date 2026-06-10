package com.netcracker.cloud.podsecrets.quarkus.runtime;

import com.netcracker.cloud.podsecrets.PodSecretsLoader;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Map;
import java.util.Set;

/**
 * SmallRye / MicroProfile {@link ConfigSource} backed by {@link PodSecretsLoader}.
 *
 * <p>Ordinal {@value PRIORITY} — above EnvConfigSource (300) and SysPropsConfigSource (400),
 * below ConsulConfigSource (500).
 */
public class PodSecretsConfigSource implements ConfigSource {

    public static final int PRIORITY = 450;

    private final PodSecretsLoader loader;

    public PodSecretsConfigSource(PodSecretsLoader loader) {
        this.loader = loader;
    }

    @Override
    public String getName() {
        return "pod-secrets-config-source";
    }

    @Override
    public int getOrdinal() {
        return PRIORITY;
    }

    @Override
    public Map<String, String> getProperties() {
        return loader.getSecrets();
    }

    @Override
    public Set<String> getPropertyNames() {
        return loader.getSecrets().keySet();
    }

    @Override
    public String getValue(String propertyName) {
        return loader.getSecrets().get(propertyName);
    }
}
