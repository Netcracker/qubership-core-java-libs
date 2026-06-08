package com.netcracker.cloud.podsecrets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class PodSecretsLoaderTest {

    @TempDir
    Path dir;

    private PodSecretsLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
    }

    @Test
    void getValue_allThreeForms() throws Exception {
        Files.writeString(dir.resolve("db_password"), "secret123\n");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));

        assertThat(l.getValue("db_password")).isEqualTo("secret123");
        assertThat(l.getValue("DB_PASSWORD")).isEqualTo("secret123");
        assertThat(l.getValue("db.password")).isEqualTo("secret123");
    }

    @Test
    void getValue_trailingNewlineStripped() throws Exception {
        Files.writeString(dir.resolve("api_token"), "tok\n");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.getValue("api_token")).isEqualTo("tok");
    }

    @Test
    void getValue_emptyFile_returnsEmptyString() throws Exception {
        Files.writeString(dir.resolve("empty_key"), "");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.getValue("empty_key")).isEqualTo("");
    }

    @Test
    void getValue_missingKey_returnsNull() throws Exception {
        Files.writeString(dir.resolve("existing"), "val");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.getValue("nonexistent")).isNull();
    }

    // ---- propertyNames ------------------------------------------------------

    @Test
    void propertyNames_containsAllThreeForms() throws Exception {
        Files.writeString(dir.resolve("db_password"), "x");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        Set<String> names = l.propertyNames();
        assertThat(names).contains("db_password", "DB_PASSWORD", "db.password");
    }

    // ---- TTL caching --------------------------------------------------------

    @Test
    void getValue_cachedBeforeTtl() throws Exception {
        Files.writeString(dir.resolve("k"), "v1");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofSeconds(60)));
        assertThat(l.getValue("k")).isEqualTo("v1");

        // overwrite file — should NOT be visible yet
        Files.writeString(dir.resolve("k"), "v2");
        assertThat(l.getValue("k")).isEqualTo("v1");
    }

    @Test
    void getValue_reloadAfterTtl() throws Exception {
        Files.writeString(dir.resolve("k"), "v1");
        Duration shortTtl = Duration.ofMillis(50);
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, shortTtl));
        assertThat(l.getValue("k")).isEqualTo("v1");

        Thread.sleep(100);
        Files.writeString(dir.resolve("k"), "v2");
        assertThat(l.getValue("k")).isEqualTo("v2");
    }

    @Test
    void getValue_disappearsAfterDeletion() throws Exception {
        Path f = Files.writeString(dir.resolve("k"), "v");
        Duration shortTtl = Duration.ofMillis(50);
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, shortTtl));
        assertThat(l.getValue("k")).isEqualTo("v");

        Thread.sleep(100);
        Files.delete(f);
        assertThat(l.getValue("k")).isNull();
    }

    @Test
    void getValue_newKeyAppearsAfterTtl() throws Exception {
        Duration shortTtl = Duration.ofMillis(50);
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, shortTtl));
        assertThat(l.getValue("new_key")).isNull();

        Thread.sleep(100);
        Files.writeString(dir.resolve("new_key"), "new_val");
        assertThat(l.getValue("new_key")).isEqualTo("new_val");
    }

    // ---- missing directory --------------------------------------------------

    @Test
    void missingDirectory_emptySnapshot_noThrow() {
        Path missing = dir.resolve("nonexistent");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(missing, Duration.ofMinutes(10)));
        assertThat(l.snapshot()).isEmpty();
        assertThat(l.isAvailable()).isFalse();
    }

    // ---- isAvailable --------------------------------------------------------

    @Test
    void isAvailable_trueWhenFilesExist() throws Exception {
        Files.writeString(dir.resolve("key"), "val");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
        assertThat(l.isAvailable()).isTrue();
    }

    @Test
    void isAvailable_falseWhenEmpty() {
        assertThat(loader.isAvailable()).isFalse();
    }

    // ---- log safety ---------------------------------------------------------

    @Test
    void logs_doNotContainSecretValues() throws Exception {
        Files.writeString(dir.resolve("my_secret"), "super-sensitive-value");

        // Redirect stdout/stderr (slf4j-simple outputs to System.err)
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        PrintStream old = System.err;
        System.setErr(new PrintStream(buf));
        try {
            PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMinutes(10)));
            l.snapshot();
        } finally {
            System.setErr(old);
        }
        assertThat(buf.toString()).doesNotContain("super-sensitive-value");
    }

    // ---- concurrency --------------------------------------------------------

    @Test
    void concurrentAccess_doesNotThrow() throws Exception {
        Files.writeString(dir.resolve("c"), "val");
        PodSecretsLoader l = new PodSecretsLoader(PodSecretsLoaderConfig.of(dir, Duration.ofMillis(1)));

        int threads = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> l.getValue("c")));
        }
        pool.shutdown();
        for (Future<?> f : futures) {
            f.get(); // throws if exception occurred
        }
    }
}
