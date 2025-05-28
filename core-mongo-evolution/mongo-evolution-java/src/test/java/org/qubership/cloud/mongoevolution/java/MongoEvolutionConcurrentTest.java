package org.qubership.cloud.mongoevolution.java;

import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MongoEvolutionConcurrentTest extends MongoServerConfiguration {

    private static final int THREAD_COUNT_EVOLVE = 3;

    private final ConnectionSearchKey connectionSearchKeyDefault = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.DEFAULT_DB_NAME);

    private MongoCollection<Document> mongoCollection;
    private MongoEvolution mongoEvolution;

    @BeforeEach
    void before() {
        mongoEvolution = new MongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
        mongoCollection = mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(MongoEvolution.TRACKER_COLLECTION);
    }

    @Test
    void evolveConcurrent_emptyMongoEvolutionCollection() {
        try {
            assertEquals(0, (int) mongoCollection.countDocuments());
            evolveConcurrent();
        } catch (Exception e) {
            log.error("Execution error: {}", e);
            fail();
        }
    }

    @Test
    void evolveConcurrent_NotEmptyMongoEvolutionCollection() {
        try {
            assertFalse(mongoEvolution.isUpdateInProgress());
            assertEquals(1, (int) mongoCollection.countDocuments());
            evolveConcurrent();
        } catch (Exception e) {
            log.error("Execution error: {}", e);
            fail();
        }
    }

    private void evolveConcurrent() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT_EVOLVE);
        List<Future<Long>> futures = executorService.invokeAll(spawnThreads());
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }
        futures.forEach(result -> {
            try {
                assertEquals(Long.valueOf(1L), result.get());
            } catch (Exception ex) {
                fail();
            }
        });
    }

    private Collection<Callable<Long>> spawnThreads() {
        Collection<Callable<Long>> threads = new HashSet<>();
        for (int i = 0; i < THREAD_COUNT_EVOLVE; i++) {
            threads.add(() -> {
                try {
                    new MongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault)
                            .evolve(TestConstants.CHANGELOGS_SCAN_PACKAGE);

                    return mongoCollection.countDocuments();
                } catch (Exception e) {
                    log.error("Thread failed: {}", e);
                    return 0L;
                }
            });
        }
        return threads;
    }
}
