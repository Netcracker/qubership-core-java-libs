package com.netcracker.cloud.quarkus.dbaas.mongoclient.service.impl;

import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.common.mountedsecret.MountedSecretConnectionSource;
import com.netcracker.cloud.quarkus.dbaas.mongoclient.entity.database.MongoDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the file-backed mongo provider: a mounted Secret is turned into a typed
 * {@link MongoDatabase} via the synthetic-response helper (no REST), and a miss returns null so the
 * provider chain falls through to the agent provider.
 */
class DbaaSMongoMountedSecretLogicalDbProviderTest {

    private static final String NS = "team-a";

    private SortedMap<String, Object> classifier() {
        SortedMap<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("namespace", NS);
        c.put("scope", "service");
        return c;
    }

    private DbaaSMongoMountedSecretLogicalDbProvider providerFor(Path root) {
        return new DbaaSMongoMountedSecretLogicalDbProvider(new MountedSecretConnectionSource(root.toString()));
    }

    @Test
    void buildsMongoDatabaseFromMountedSecret(@TempDir Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("mongodb"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"svc\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"mongodb\",\"name\":\"app_db\",\"namespace\":\"" + NS + "\"}");
        Files.writeString(d.resolve("connectionProperties.json"),
                "{\"url\":\"mongodb://mongo-host:27017/app_db\",\"username\":\"app_user\",\"password\":\"secret\","
                        + "\"authDbName\":\"admin\"}");

        MongoDatabase db = providerFor(root).provide(classifier(), DatabaseConfig.builder().build(), NS);

        assertNotNull(db);
        assertEquals("app_db", db.getName());
        assertEquals(NS, db.getNamespace());
        assertNotNull(db.getConnectionProperties());
        assertEquals("admin", db.getConnectionProperties().getAuthDbName());
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
                        + "\"type\":\"mongodb\"}");
        Files.writeString(d.resolve("connectionProperties.json"), "{}");

        // A null DatabaseConfig means "no requested role", which matches a descriptor without userRole.
        assertNotNull(providerFor(root).provide(classifier(), null, NS));
    }
}
