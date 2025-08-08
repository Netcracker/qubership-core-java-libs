package com.netcracker.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.AbstractEnvironment;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MongoTestConfiguration.class)
@Slf4j
class SpringMongoEvolutionConcurrentTest {

    private static final int THREAD_COUNT_EVOLVE = 3;

    private final ConnectionSearchKey connectionSearchKeyDefault = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.DEFAULT_DB_NAME);

    private final MongoServer mongoServer = new MongoServer(new MemoryBackend());

    private MongoCollection<Document> mongoCollection;
    private SpringMongoEvolution springMongoEvolution;
    @Autowired
    private AbstractEnvironment springEnvironment;
    private Map<String, Object> classNamesAndBeans = new HashMap<>();
    private MongoClient mongoClient;

    @BeforeEach
    void beforeTest() {
        mongoServer.bind();
        mongoClient = MongoClients.create("mongodb:/" + mongoServer.getLocalAddress());
        springMongoEvolution = new SpringMongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
        mongoCollection = mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(SpringMongoEvolution.TRACKER_COLLECTION);
        classNamesAndBeans.put(springEnvironment.getClass().getName(), springEnvironment);
    }

    @AfterEach
    void afterTest() {
        mongoServer.shutdown();
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
            assertFalse(springMongoEvolution.isUpdateInProgress());
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
                    new SpringMongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault)
                            .evolve(TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);

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
