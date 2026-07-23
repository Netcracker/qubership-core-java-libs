package com.netcracker.cloud.dbaas.client.service.mountedsecret;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
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

        Optional<MountedSecretSource.Resolved> r =
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
        MountedSecretSource src = source();

        assertEquals("v1", src.resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("password"));

        // rotate the password on disk — the next resolve must read it fresh (no caching).
        Files.writeString(root.resolve("s1").resolve(MountedSecretSource.CONNECTION_PROPERTIES_FILE), "{\"password\":\"v2\"}");
        assertEquals("v2", src.resolve(classifier("team-a"), "postgresql", null).get().connectionProperties().get("password"));
    }

    @Test
    void readsMetadataFreshEveryCall() throws IOException {
        // name n1; classifier/type/role unchanged so the index key stays the same.
        writeSecret("s1",
                "{\"classifier\":" + SERVICE_CLASSIFIER + ",\"type\":\"postgresql\",\"name\":\"n1\",\"namespace\":\"team-a\"}",
                "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertEquals("n1", src.resolve(classifier("team-a"), "postgresql", null).get().metadata().getName());

        // change a non-key descriptor field on disk — the next resolve must reflect it (fresh metadata, not cached).
        Files.writeString(root.resolve("s1").resolve(MountedSecretSource.METADATA_FILE),
                "{\"classifier\":" + SERVICE_CLASSIFIER + ",\"type\":\"postgresql\",\"name\":\"n2\",\"namespace\":\"team-a\"}");
        assertEquals("n2", src.resolve(classifier("team-a"), "postgresql", null).get().metadata().getName());
    }

    @Test
    void metadataChangedInPlaceIsEvictedAndNoLongerServesOldClassifier() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent());

        // The descriptor's classifier changes in place (different microserviceName -> different key).
        String movedClassifier = "{\"microserviceName\":\"other\",\"scope\":\"service\",\"namespace\":\"team-a\"}";
        Files.writeString(root.resolve("s1").resolve(MountedSecretSource.METADATA_FILE),
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
        // valid JSON but no `type` → not indexable
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
        // two directories canonicalize to the same (classifier, type, role) key
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
        MountedSecretSource src = new MountedSecretSource(root.toString(), Duration.ZERO);
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty(), "index starts empty");

        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent(),
                "a re-scan on miss must pick up a secret added after construction");
    }

    @Test
    void roleMatchingIsExactAndEmptyMatchesUnset() throws IOException {
        // descriptor with userRole=admin
        writeSecret("admin",
                metadata(SERVICE_CLASSIFIER, "postgresql", "admin"),
                "{\"role\":\"admin\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "admin").isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "ro").isEmpty());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "  admin  ").isPresent(), "role is trimmed");
    }

    @Test
    void emptyRoleMatchesDescriptorWithUnsetUserRole() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "").isPresent());
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "admin").isEmpty());
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
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty());
    }

    // ── negative matching: tenant / role / extra-key (the "tricky" misses) ──────

    private Map<String, Object> tenantClassifier(String ns, String tenantId) {
        Map<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("scope", "tenant");
        c.put("namespace", ns);
        c.put("tenantId", tenantId);
        return c;
    }

    private static final String TENANT_CLASSIFIER_ACME =
            "{\"microserviceName\":\"svc\",\"scope\":\"tenant\",\"namespace\":\"team-a\",\"tenantId\":\"acme\"}";

    @Test
    void serviceSecretDoesNotMatchTenantRequest() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isPresent(), "service request hits the service secret");
        assertTrue(src.resolve(tenantClassifier("team-a", "acme"), "postgresql", null).isEmpty(),
                "a tenant request must not be served by a service-scope secret");
    }

    @Test
    void differentTenantIdMisses() throws IOException {
        writeSecret("s1", metadata(TENANT_CLASSIFIER_ACME, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(tenantClassifier("team-a", "acme"), "postgresql", null).isPresent(), "same tenantId hits");
        assertTrue(src.resolve(tenantClassifier("team-a", "globex"), "postgresql", null).isEmpty(), "a different tenantId misses");
    }

    @Test
    void roleMatchingIsCaseSensitive() throws IOException {
        writeSecret("s1", metadata(SERVICE_CLASSIFIER, "postgresql", "admin"), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "admin").isPresent(), "exact-case role hits");
        assertTrue(src.resolve(classifier("team-a"), "postgresql", "Admin").isEmpty(),
                "role is case-sensitive (unlike type): 'Admin' must not match 'admin'");
    }

    @Test
    void extraTopLevelClassifierKeyOnOneSideMisses() throws IOException {
        String withExtra = "{\"microserviceName\":\"svc\",\"scope\":\"service\",\"namespace\":\"team-a\",\"logicalDbName\":\"reports\"}";
        writeSecret("s1", metadata(withExtra, "postgresql", null), "{\"host\":\"pg\"}");
        MountedSecretSource src = source();
        assertTrue(src.resolve(classifier("team-a"), "postgresql", null).isEmpty(),
                "an extra top-level identity key on only the descriptor side must diverge the canonical key (silent-miss guard)");
        Map<String, Object> withKey = classifier("team-a");
        withKey.put("logicalDbName", "reports");
        assertTrue(src.resolve(withKey, "postgresql", null).isPresent(), "the same extra key on both sides hits");
    }

    // ── buildDatabase (synthetic-response) ───────────────────────────────────

    public static class TestConnectionProperties {
        public String url;
    }

    public static class TestDatabase extends AbstractDatabase<TestConnectionProperties> {
    }

    private static Map<String, Object> buildClassifier(boolean withNamespace) {
        Map<String, Object> c = new TreeMap<>();
        c.put("microserviceName", "svc");
        c.put("scope", "service");
        if (withNamespace) {
            c.put("namespace", "ns-from-classifier");
        }
        return c;
    }

    @Test
    void buildDatabaseMetadataFieldsWinOverArgumentsAndProperties() {
        SecretMetadata meta = new SecretMetadata();
        meta.setClassifier(Map.of("microserviceName", "meta-svc", "scope", "service"));
        meta.setName("db-from-meta");
        meta.setNamespace("ns-from-meta");
        meta.setSettings(Map.of("ttl", 5));
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", "db-from-props"), meta);

        TestDatabase db = new MountedSecretSource().buildDatabase(TestDatabase.class, buildClassifier(true), resolved);

        assertEquals("db-from-meta", db.getName());
        assertEquals("ns-from-meta", db.getNamespace());
        assertEquals("meta-svc", db.getClassifier().get("microserviceName"));
        assertEquals(Map.of("ttl", 5), db.getSettings());
        assertEquals("u", db.getConnectionProperties().url);
    }

    @Test
    void buildDatabaseEmptyMetadataFallsBackToArgumentsAndProperties() {
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", "db-from-props"), new SecretMetadata());

        TestDatabase db = new MountedSecretSource().buildDatabase(TestDatabase.class, buildClassifier(true), resolved);

        assertEquals("db-from-props", db.getName());
        assertEquals("ns-from-classifier", db.getNamespace());
        assertEquals("svc", db.getClassifier().get("microserviceName"));
        assertNull(db.getSettings());
    }

    @Test
    void buildDatabaseNonStringNameAndAbsentNamespaceAreLeftUnset() {
        // A non-string "name" connection property is not a database name, and with no namespace in
        // the metadata or the classifier the field simply stays empty.
        MountedSecretSource.Resolved resolved =
                new MountedSecretSource.Resolved(Map.of("url", "u", "name", 42), new SecretMetadata());

        TestDatabase db = new MountedSecretSource().buildDatabase(TestDatabase.class, buildClassifier(false), resolved);

        assertNull(db.getName());
        assertNull(db.getNamespace());
    }
}
