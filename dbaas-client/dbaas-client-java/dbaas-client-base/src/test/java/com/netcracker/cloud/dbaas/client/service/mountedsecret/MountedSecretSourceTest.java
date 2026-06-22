package com.netcracker.cloud.dbaas.client.service.mountedsecret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class MountedSecretSourceTest {

    @TempDir
    Path root;

    // ── helpers ───────────────────────────────────────────────────────────────

    private void writeSecret(String dir, String metadataJson, String connPropsJson) throws IOException {
        Path d = Files.createDirectories(root.resolve(dir));
        if (metadataJson != null) {
            Files.writeString(d.resolve(MountedSecretSource.METADATA_FILE), metadataJson);
        }
        if (connPropsJson != null) {
            Files.writeString(d.resolve(MountedSecretSource.CONNECTION_PROPERTIES_FILE), connPropsJson);
        }
    }

    private MountedSecretSource source() {
        return new MountedSecretSource(root.toString());
    }

    private Map<String, Object> classifier(String ns, String role) {
        Map<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("scope", "service");
        c.put("namespace", ns);
        return c;
    }

    private String metadata(String classifierJson, String type, String userRole) {
        String role = userRole == null ? "" : "\"userRole\":\"" + userRole + "\",";
        return "{\"classifier\":" + classifierJson + ",\"type\":\"" + type + "\"," + role
                + "\"name\":\"app_db\",\"namespace\":\"team-a\"}";
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void serviceScopeHitReturnsPropsAndMetadata() throws IOException {
        writeSecret("s1",
                metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", null),
                "{\"host\":\"pg\",\"port\":5432,\"username\":\"u\",\"password\":\"p\"}");

        Optional<MountedSecretSource.Resolved> r =
                source().resolve(classifier("team-a", null), "postgresql", null);

        assertTrue(r.isPresent());
        assertEquals("pg", r.get().connectionProperties().get("host"));
        assertEquals("postgresql", r.get().metadata().getType());
        assertEquals("app_db", r.get().metadata().getName());
    }

    @Test
    void missOnDifferentClassifierFallsThrough() throws IOException {
        writeSecret("s1",
                metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", null),
                "{\"host\":\"pg\"}");

        assertTrue(source().resolve(classifier("other-ns", null), "postgresql", null).isEmpty());
        assertTrue(source().resolve(classifier("team-a", null), "mongodb", null).isEmpty());
    }

    @Test
    void readsConnectionPropertiesFreshEveryCall() throws IOException {
        String md = metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", null);
        writeSecret("s1", md, "{\"password\":\"v1\"}");
        MountedSecretSource src = source();

        assertEquals("v1", src.resolve(classifier("team-a", null), "postgresql", null).get().connectionProperties().get("password"));

        // rotate the password on disk — the next resolve must read it fresh (no caching).
        Files.writeString(root.resolve("s1").resolve(MountedSecretSource.CONNECTION_PROPERTIES_FILE), "{\"password\":\"v2\"}");
        assertEquals("v2", src.resolve(classifier("team-a", null), "postgresql", null).get().connectionProperties().get("password"));
    }

    @Test
    void missingMetadataSkipsDirectory() throws IOException {
        writeSecret("s1", null, "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a", null), "postgresql", null).isEmpty());
    }

    @Test
    void corruptMetadataSkipsDirectory() throws IOException {
        writeSecret("s1", "{not-json", "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a", null), "postgresql", null).isEmpty());
    }

    @Test
    void missingConnectionPropertiesIsAMiss() throws IOException {
        writeSecret("s1",
                metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", null),
                null);
        assertTrue(source().resolve(classifier("team-a", null), "postgresql", null).isEmpty());
    }

    @Test
    void roleMatchingIsExactAndEmptyMatchesUnset() throws IOException {
        // descriptor with userRole=admin
        writeSecret("admin",
                metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", "admin"),
                "{\"role\":\"admin\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", "admin").isPresent());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", "ro").isEmpty());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", null).isEmpty());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", "  admin  ").isPresent(), "role is trimmed");
    }

    @Test
    void emptyRoleMatchesDescriptorWithUnsetUserRole() throws IOException {
        writeSecret("s1",
                metadata("{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}", "postgresql", null),
                "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", null).isPresent());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", "").isPresent());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", "admin").isEmpty());
    }

    @Test
    void matchIsKeyOrderScopeCaseAndEmptyFieldInsensitive() throws IOException {
        // descriptor: scope lowercase "service", nested customKeys, no tenantId
        writeSecret("s1",
                metadata("{\"namespace\":\"team-a\",\"scope\":\"service\",\"microserviceName\":\"svc\",\"customKeys\":{\"logicalDbName\":\"x\"}}", "postgresql", null),
                "{\"host\":\"pg\"}");
        MountedSecretSource src = source();

        // request: different key order, scope upper-case, customKeys present, an explicit empty tenantId
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("scope", "SERVICE");
        Map<String, Object> ck = new LinkedHashMap<>();
        ck.put("logicalDbName", "x");
        req.put("customKeys", ck);
        req.put("tenantId", ""); // empty top-level tenantId is omitted by canonicalization
        req.put("namespace", "team-a");
        req.put("microserviceName", "svc");

        assertTrue(src.resolve(req, "POSTGRESQL", null).isPresent(),
                "canonical match must ignore key order, scope case, type case and empty namespace/tenantId");
    }

    @Test
    void absentMountPathYieldsNoIndexAndAllMisses() {
        MountedSecretSource src = new MountedSecretSource(root.resolve("does-not-exist").toString());
        assertTrue(src.resolve(classifier("team-a", null), "postgresql", null).isEmpty());
    }
}
