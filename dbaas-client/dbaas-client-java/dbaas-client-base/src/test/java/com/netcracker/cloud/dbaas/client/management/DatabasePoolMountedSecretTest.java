package com.netcracker.cloud.dbaas.client.management;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.entity.test.TestDBConnection;
import com.netcracker.cloud.dbaas.client.entity.test.TestDBType;
import com.netcracker.cloud.dbaas.client.entity.test.TestDatabase;
import com.netcracker.cloud.dbaas.client.service.mountedsecret.MountedSecretSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static com.netcracker.cloud.dbaas.client.DbaasConst.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies the source-first behaviour added to {@link DatabasePool#createDatabase}: when a matching
 * Secret is mounted, the typed database is built from it (synthetic-response) and dbaas REST is never
 * called; with nothing mounted the pool falls back to REST exactly as before.
 */
class DatabasePoolMountedSecretTest {

    private static final String MS_NAME = "test-ms";
    private static final String NS = "team-a";

    private final DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

    // DatabasePool's only constructors take the (deprecated, for-removal) DatabaseDefinitionHandler,
    // which the REST-fallback path still invokes, so a stub is required here.
    @SuppressWarnings({"deprecation", "removal"})
    private DatabasePool pool() {
        return new DatabasePool(dbaasClient, MS_NAME, NS,
                Collections.emptyList(), Mockito.mock(DatabaseDefinitionHandler.class));
    }

    private DbaasDbClassifier classifier() {
        return new DbaasDbClassifier.Builder()
                .withProperty(SCOPE, SERVICE)
                .withProperty(NAMESPACE, NS)
                .withProperty(MICROSERVICE_NAME, MS_NAME)
                .build();
    }

    private void writeSecret(Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("postgres"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"" + MS_NAME + "\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"testdb\",\"name\":\"app_db\",\"namespace\":\"" + NS + "\"}");
        Files.writeString(d.resolve("connectionProperties.json"),
                "{\"url\":\"jdbc:testdb://pg/app\",\"username\":\"app_user\",\"password\":\"secret\",\"name\":\"app_db\"}");
    }

    @Test
    void buildsDatabaseFromMountedSecretWithoutCallingRest(@TempDir Path root) throws IOException {
        writeSecret(root);
        DatabasePool pool = pool();
        pool.setMountedSecretSource(new MountedSecretSource(root.toString()));

        TestDatabase db = pool.getOrCreateDatabase(TestDBType.INSTANCE, classifier(), DatabaseConfig.builder().build());

        assertNotNull(db);
        TestDBConnection conn = db.getConnectionProperties();
        assertEquals("jdbc:testdb://pg/app", conn.getUrl());
        assertEquals("app_user", conn.getUsername());
        assertEquals("secret", conn.getPassword());
        assertEquals(MS_NAME, db.getClassifier().get(MICROSERVICE_NAME));
        assertEquals("app_db", db.getName());

        verify(dbaasClient, never()).getOrCreateDatabase(any(), any(), any(), any());
    }

    @Test
    void buildsDatabaseFromMinimalMetadataUsingFallbacks(@TempDir Path root) throws IOException {
        // metadata without top-level name/namespace and connectionProperties without "name":
        // exercises the synthetic-response fallbacks (name from props, namespace from classifier,
        // settings copied through).
        Path d = Files.createDirectories(root.resolve("postgres"));
        Files.writeString(d.resolve("metadata.json"),
                "{\"classifier\":{\"microserviceName\":\"" + MS_NAME + "\",\"namespace\":\"" + NS + "\",\"scope\":\"service\"},"
                        + "\"type\":\"testdb\",\"settings\":{\"region\":\"eu\"}}");
        Files.writeString(d.resolve("connectionProperties.json"),
                "{\"url\":\"jdbc:testdb://pg/app\",\"username\":\"u\",\"password\":\"p\"}");

        DatabasePool pool = pool();
        pool.setMountedSecretSource(new MountedSecretSource(root.toString()));

        TestDatabase db = pool.getOrCreateDatabase(TestDBType.INSTANCE, classifier(), DatabaseConfig.builder().build());

        assertNotNull(db);
        assertEquals("jdbc:testdb://pg/app", db.getConnectionProperties().getUrl());
        assertEquals(NS, db.getNamespace(), "namespace falls back to the classifier namespace");
        assertNull(db.getName(), "name is absent in both metadata and connectionProperties");
        assertNotNull(db.getSettings());
        assertEquals("eu", db.getSettings().get("region"));
        verify(dbaasClient, never()).getOrCreateDatabase(any(), any(), any(), any());
    }

    @Test
    void fallsBackToRestWhenNothingMounted(@TempDir Path root) {
        // empty mount directory → no index → miss → REST
        DatabasePool pool = pool();
        pool.setMountedSecretSource(new MountedSecretSource(root.toString()));

        TestDatabase fromRest = new TestDatabase();
        fromRest.setNamespace(NS);
        Mockito.when(dbaasClient.getOrCreateDatabase(any(), any(), any(), any())).thenReturn(fromRest);

        TestDatabase db = pool.getOrCreateDatabase(TestDBType.INSTANCE, classifier(), DatabaseConfig.builder().build());

        assertSame(fromRest, db);
        verify(dbaasClient).getOrCreateDatabase(any(), any(), any(), any());
    }
}
