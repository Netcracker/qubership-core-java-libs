package com.netcracker.cloud.core.quarkus.dbaas.datasource.service.impl;

import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.dbaas.common.mountedsecret.MountedSecretConnectionSource;
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
 * Verifies the file-backed postgres provider: a mounted Secret is turned into a typed
 * {@link PostgresDatabase} via the synthetic-response helper (no REST), and a miss returns null so
 * the provider chain falls through to the agent provider.
 */
class DbaaSPgMountedSecretLogicalDbProviderTest {

    private static final String NS = "team-a";

    private SortedMap<String, Object> classifier() {
        SortedMap<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("namespace", NS);
        c.put("scope", "service");
        return c;
    }

    private DbaaSPgMountedSecretLogicalDbProvider providerFor(Path root) {
        return new DbaaSPgMountedSecretLogicalDbProvider(new MountedSecretConnectionSource(root.toString()));
    }

    @Test
    void buildsPostgresDatabaseFromMountedSecret(@TempDir Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("postgres"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"svc\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"postgresql\",\"name\":\"app_db\",\"namespace\":\"" + NS + "\"}");
        Files.writeString(d.resolve("connectionProperties.json"),
                "{\"url\":\"jdbc:postgresql://pg/app\",\"username\":\"app_user\",\"password\":\"secret\","
                        + "\"role\":\"admin\",\"host\":\"pg\",\"roHost\":\"pg-ro\"}");

        PostgresDatabase db = providerFor(root).provide(classifier(), DatabaseConfig.builder().build(), NS);

        assertNotNull(db);
        assertEquals("jdbc:postgresql://pg/app", db.getConnectionProperties().getUrl());
        assertEquals("app_user", db.getConnectionProperties().getUsername());
        assertEquals("secret", db.getConnectionProperties().getPassword());
        assertEquals("admin", db.getConnectionProperties().getRole());
        assertEquals("pg", db.getConnectionProperties().getHost());
        assertEquals("pg-ro", db.getConnectionProperties().getRoHost());
        assertEquals("app_db", db.getName());
        assertEquals(NS, db.getNamespace());
    }

    @Test
    void returnsNullWhenNothingMounted(@TempDir Path root) {
        // empty mount directory → miss → null → chain falls through to the agent provider (REST)
        assertNull(providerFor(root).provide(classifier(), DatabaseConfig.builder().build(), NS));
    }
}
