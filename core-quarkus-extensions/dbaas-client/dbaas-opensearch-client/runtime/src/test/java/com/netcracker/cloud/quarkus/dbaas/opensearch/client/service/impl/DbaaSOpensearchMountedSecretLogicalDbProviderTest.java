package com.netcracker.cloud.quarkus.dbaas.opensearch.client.service.impl;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.client.opensearch.entity.OpensearchIndex;
import com.netcracker.cloud.dbaas.common.mountedsecret.MountedSecretConnectionSource;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the file-backed opensearch provider: a mounted Secret is turned into a typed
 * {@link OpensearchIndex} via the synthetic-response helper (no REST), and a miss returns null so
 * the provider chain falls through to the agent provider.
 */
class DbaaSOpensearchMountedSecretLogicalDbProviderTest {

    private static final String NS = "team-a";

    private SortedMap<String, Object> classifier() {
        SortedMap<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("namespace", NS);
        c.put("scope", "service");
        return c;
    }

    private DbaaSOpensearchMountedSecretLogicalDbProvider providerFor(Path root) {
        return new DbaaSOpensearchMountedSecretLogicalDbProvider(new MountedSecretConnectionSource(root.toString()));
    }

    @Test
    void buildsOpensearchIndexFromMountedSecret(@TempDir Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("opensearch"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"svc\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"opensearch\",\"name\":\"app_db\",\"namespace\":\"" + NS + "\"}");
        Files.writeString(d.resolve("connectionProperties.json"),
                "{\"username\":\"app_user\",\"password\":\"secret\"}");

        OpensearchIndex db = providerFor(root).provide(classifier(), DatabaseConfig.builder().build(), NS);

        assertNotNull(db);
        assertEquals("app_db", db.getName());
        assertEquals(NS, db.getNamespace());
        assertNotNull(db.getConnectionProperties());
    }

    @Test
    void returnsNullWhenNothingMounted(@TempDir Path root) {
        assertNull(providerFor(root).provide(classifier(), DatabaseConfig.builder().build(), NS));
    }

    @Test
    void orderSitsJustBeforeTheAggregatorProvider(@TempDir Path root) {
        // After any user-supplied providers (default order 0), before the agent provider (MAX_VALUE).
        assertEquals(Integer.MAX_VALUE - 1, providerFor(root).order());
    }

    @Test
    void nullConfigMatchesDescriptorWithoutUserRole(@TempDir Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("no-role"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"svc\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"opensearch\"}");
        Files.writeString(d.resolve("connectionProperties.json"), "{}");

        // A null DatabaseConfig means "no requested role", which matches a descriptor without userRole.
        assertNotNull(providerFor(root).provide(classifier(), null, NS));
    }

    @Test
    void connectionPropertyLookupIsNotImplemented(@TempDir Path root) {
        // The mounted-secret provider only serves whole databases; the property-level
        // lookup of the parent contract is intentionally unsupported.
        assertThrows(NotImplementedException.class,
                () -> providerFor(root).provideConnectionProperty(classifier(), DatabaseConfig.builder().build()));
    }
}
