package com.netcracker.cloud.dbaas.common.mountedsecret;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.dbaas.client.DbaasConst;
import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.MountedSecretSource;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.SecretMetadata;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * CDI-visible extension of dbaas-client-base's {@link MountedSecretSource} — the descriptor index,
 * throttled re-scan, and eviction logic live there. This class only adds the synthetic-response
 * builder that the per-driver {@code LogicalDbProvider}s use to turn resolved connection
 * properties into their typed database.
 */
@Singleton
public class MountedSecretConnectionSource extends MountedSecretSource {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public MountedSecretConnectionSource() {
    }

    public MountedSecretConnectionSource(String basePath) {
        super(basePath);
    }

    /**
     * Builds the typed database from a mounted Secret (synthetic-response): assemble a map mirroring
     * the dbaas REST response and convert it to {@code databaseClass} with the same deserialization
     * semantics as the REST path. No provisioning and no REST call happen here.
     */
    public <D extends AbstractDatabase<?>> D buildDatabase(Class<D> databaseClass,
                                                           Map<String, Object> classifier,
                                                           Resolved resolved) {
        SecretMetadata meta = resolved.metadata();
        Map<String, Object> synthetic = new HashMap<>();
        synthetic.put("classifier", meta.getClassifier() != null ? meta.getClassifier() : classifier);
        synthetic.put("connectionProperties", resolved.connectionProperties());

        String name = meta.getName() != null ? meta.getName() : asString(resolved.connectionProperties().get("name"));
        if (name != null) {
            synthetic.put("name", name);
        }
        String dbNamespace = meta.getNamespace() != null ? meta.getNamespace() : asString(classifier.get(DbaasConst.NAMESPACE));
        if (dbNamespace != null) {
            synthetic.put("namespace", dbNamespace);
        }
        if (meta.getSettings() != null) {
            synthetic.put("settings", meta.getSettings());
        }
        return MAPPER.convertValue(synthetic, databaseClass);
    }

    private static String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}
