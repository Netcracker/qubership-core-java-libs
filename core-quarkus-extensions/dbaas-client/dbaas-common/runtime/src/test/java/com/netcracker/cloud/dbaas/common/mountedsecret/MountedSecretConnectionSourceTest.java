package com.netcracker.cloud.dbaas.common.mountedsecret;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountedSecretConnectionSourceTest {

    @TempDir
    Path root;

    // ── helpers ───────────────────────────────────────────────────────────────

    private void writeSecret(String dir, String metadataJson, String connPropsJson) throws IOException {
        Path d = Files.createDirectories(root.resolve(dir));
        if (metadataJson != null) {
            Files.writeString(d.resolve(MountedSecretConnectionSource.METADATA_FILE), metadataJson);
        }
        if (connPropsJson != null) {
            Files.writeString(d.resolve(MountedSecretConnectionSource.CONNECTION_PROPERTIES_FILE), connPropsJson);
        }
    }

    private MountedSecretConnectionSource source() {
        return new MountedSecretConnectionSource(root.toString());
    }

    private Map<String, Object> classifier(String ns) {
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

    private static final String SERVICE_CLASSIFIER =
            "{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\"}";

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void serviceScopeHitReturnsPropsAndMetadata() throws IOException {
        writeSecret("s1",
                metadata(SERVICE_CLASSIFIER, "postgresql", null),
                "{\"host\":\"pg\",\"port\":5432,\"username\":\"u\",\"password\":\"p\"}");

        Optional<MountedSecretConnectionSource.Resolved> r =
                source().resolve(classifier("team-a"), "postgresql", null);

        assertTrue(r.isPresent());
        assertEquals("pg", r.get().connectionProperties().get("host"));
        assertEquals("postgresql", r.get().metadata().getType());
        assertEquals("app_db", r.get().metadata().getName());
    }

    @Test
    void missOnDifferentClassifierFallsThrough() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");

        assertTrue(source().resolve(classifier("other-ns"), "postgresql", null).isEmpty());
        assertTrue(source().resolve(classifier("team-a"), "mongodb", null).isEmpty());
    }

    @Test
    void readsConnectionPropertiesFreshEveryCall() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"password\":\"v1\"}");
        MountedSecretConnectionSource src = source();

        assertEquals("v1", src.resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("password"));

        // rotate the password on disk — the next resolve must read it fresh (no caching).
        Files.writeString(root.resolve("s1").resolve(MountedSecretConnectionSource.CONNECTION_PROPERTIES_FILE), "{\"password\":\"v2\"}");
        assertEquals("v2", src.resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("password"));
    }

    @Test
    void readsMetadataFreshEveryCall() throws IOException {
        // name n1; classifier/type/role unchanged so the index key stays the same.
        writeSecret("s1",
                "{\"classifier\":" + SERVICE_CLASSIFIER + ",\"type\":\"postgresql\",\"name\":\"n1\",\"namespace\":\"team-a\"}",
                "{\"host\":\"pg\"}");
        MountedSecretConnectionSource src = source();
        assertEquals("n1", src.resolve(classifier("team-a"), "postgresql", null).get().metadata().getName());

        // change a non-key descriptor field on disk — the next resolve must reflect it (fresh metadata, not cached).
        Files.writeString(root.resolve("s1").resolve(MountedSecretConnectionSource.METADATA_FILE),
                "{\"classifier\":" + SERVICE_CLASSIFIER + ",\"type\":\"postgresql\",\"name\":\"n2\",\"namespace\":\"team-a\"}");
        assertEquals("n2", src.resolve(classifier("team-a"), "postgresql", null).get().metadata().getName());
    }

    @Test
    void metadataChangedInPlaceIsEvictedAndNoLongerServesOldClassifier() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretConnectionSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent());

        // The descriptor's classifier changes in place (different microserviceName -> different key).
        String movedClassifier = "{\"microserviceName\":\"other\",\"scope\":\"service\",\"namespace\":\"team-a\"}";
        Files.writeString(root.resolve("s1").resolve(MountedSecretConnectionSource.METADATA_FILE),
                metadata(movedClassifier, "postgresql", null));

        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty(),
                "a descriptor that changed classifier in place must be evicted, not served under the old key");
    }

    @Test
    void missingMetadataSkipsDirectory() throws IOException {
        writeSecret("s1", null, "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    @Test
    void corruptMetadataSkipsDirectory() throws IOException {
        writeSecret("s1", "{not-json", "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    @Test
    void incompleteMetadataMissingTypeSkipsDirectory() throws IOException {
        writeSecret("s1", "{\"classifier\":" + SERVICE_CLASSIFIER + "}", "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    @Test
    void missingConnectionPropertiesIsAMiss() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), null);
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    @Test
    void corruptConnectionPropertiesIsAMiss() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{not-json");
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    @Test
    void duplicateKeyResolvesDeterministicallyByLowestDirName() throws IOException {
        String md = metadata(SERVICE_CLASSIFIER, "postgresql", null);
        // Written out of order on purpose; the lowest directory name ("a") must win regardless.
        writeSecret("b", md, "{\"host\":\"from-b\"}");
        writeSecret("a", md, "{\"host\":\"from-a\"}");

        assertEquals("from-a",
                source().resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("host"));
        // Stable across separate index builds (i.e. restarts/re-scans).
        assertEquals("from-a",
                source().resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("host"));
    }

    @Test
    void nonDirectoryEntriesAreIgnored() throws IOException {
        Files.writeString(root.resolve("stray-file.txt"), "not a secret");
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        assertTrue(source().resolve(classifier("team-a"), "postgresql", null).isPresent());
    }

    @Test
    void rescanOnMissPicksUpNewlyAddedSecret() throws IOException {
        // Duration.ZERO disables the throttle so the miss triggers an immediate re-scan.
        MountedSecretConnectionSource src = new MountedSecretConnectionSource(root.toString(), Duration.ZERO);
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty(), "index starts empty");

        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent(),
                "a re-scan on miss must pick up a secret added after construction");
    }

    @Test
    void roleMatchingIsExactAndEmptyMatchesUnset() throws IOException {
        writeSecret("admin", metadata(SERVICE_CLASSIFIER, "postgresql", "admin"), "{\"role\":\"admin\"}");
        MountedSecretConnectionSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "admin").isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "ro").isEmpty());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "  admin  ").isPresent(), "role is trimmed");
    }

    @Test
    void emptyRoleMatchesDescriptorWithUnsetUserRole() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretConnectionSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "").isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "admin").isEmpty());
    }

    @Test
    void matchIsKeyOrderScopeCaseAndEmptyFieldInsensitive() throws IOException {
        writeSecret("s1",
                metadata("{\"namespace\":\"team-a\",\"scope\":\"service\",\"microserviceName\":\"svc\",\"customKeys\":{\"logicalDbName\":\"x\"}}", "postgresql", null),
                "{\"host\":\"pg\"}");
        MountedSecretConnectionSource src = source();

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("scope", "SERVICE");
        Map<String, Object> ck = new LinkedHashMap<>();
        ck.put("logicalDbName", "x");
        req.put("customKeys", ck);
        req.put("tenantId", "");
        req.put("namespace", "team-a");
        req.put("microserviceName", "svc");

        assertTrue(src.resolve(req, "POSTGRESQL", null).isPresent(),
                "canonical match must ignore key order, scope case, type case and empty namespace/tenantId");
    }

    @Test
    void absentMountPathYieldsNoIndexAndAllMisses() {
        MountedSecretConnectionSource src = new MountedSecretConnectionSource(root.resolve("does-not-exist").toString());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }
}
