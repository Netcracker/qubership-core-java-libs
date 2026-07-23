package com.netcracker.cloud.dbaas.common.mountedsecret;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.MountedSecretSource;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.SecretMetadata;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the synthetic-response builder only — the index, re-scan, and eviction logic lives in
 * dbaas-client-base and is tested by its own {@code MountedSecretSourceTest}.
 */
class MountedSecretConnectionSourceTest {

    public static class TestConnectionProperties {
        public String url;
    }

    public static class TestDatabase extends AbstractDatabase<TestConnectionProperties> {
    }

    private final MountedSecretConnectionSource source = new MountedSecretConnectionSource();

    private static Map<String, Object> classifier(boolean withNamespace) {
        Map<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("scope", "service");
        if (withNamespace) {
            c.put("namespace", "ns-from-classifier");
        }
        return c;
    }

    @Test
    void metadataFieldsWinOverArgumentsAndProperties() {
        SecretMetadata meta = new SecretMetadata();
        meta.setClassifier(Map.of("microserviceName", "meta-svc", "scope", "service"));
        meta.setName("db-from-meta");
        meta.setNamespace("ns-from-meta");
        meta.setSettings(Map.of("ttl", 5));
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", "db-from-props"), meta);

        TestDatabase db = source.buildDatabase(TestDatabase.class, classifier(true), resolved);

        assertEquals("db-from-meta", db.getName());
        assertEquals("ns-from-meta", db.getNamespace());
        assertEquals("meta-svc", db.getClassifier().get("microserviceName"));
        assertEquals(Map.of("ttl", 5), db.getSettings());
        assertEquals("u", db.getConnectionProperties().url);
    }

    @Test
    void emptyMetadataFallsBackToArgumentsAndProperties() {
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", "db-from-props"), new SecretMetadata());

        TestDatabase db = source.buildDatabase(TestDatabase.class, classifier(true), resolved);

        assertEquals("db-from-props", db.getName());
        assertEquals("ns-from-classifier", db.getNamespace());
        assertEquals("svc", db.getClassifier().get("microserviceName"));
        assertNull(db.getSettings());
    }

    @Test
    void nonStringNameAndAbsentNamespaceAreLeftUnset() {
        // A non-string "name" connection property is not a database name, and with no namespace in
        // the metadata or the classifier the field simply stays empty.
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", 42), new SecretMetadata());

        TestDatabase db = source.buildDatabase(TestDatabase.class, classifier(false), resolved);

        assertNull(db.getName());
        assertNull(db.getNamespace());
    }
}
