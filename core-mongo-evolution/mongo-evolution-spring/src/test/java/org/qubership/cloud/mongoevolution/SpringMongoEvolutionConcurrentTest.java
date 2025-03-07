package org.qubership.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.*;

@RunWith(SpringRunner.class)
@Slf4j
public class SpringMongoEvolutionConcurrentTest{

    private static final int THREAD_COUNT_EVOLVE = 3;

    @Rule
    public final JavaMongoServerExternalResource resource = new JavaMongoServerExternalResource();

    @Rule
    public final TestRule timeout = Timeout.millis(120000);
    private final ConnectionSearchKey connectionSearchKeyDefault = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.DEFAULT_DB_NAME);

    private MongoCollection<Document> mongoCollection;
    private SpringMongoEvolution springMongoEvolution;
    @Autowired
    private AbstractEnvironment springEnvironment;
    private Map<String, Object> classNamesAndBeans = new HashMap<>();
    private MongoClient mongoClient;

    @Before
    public void beforeTest() throws Exception {
        mongoClient = MongoClients.create("mongodb:/" + resource.getMongoServer().getLocalAddress());
        springMongoEvolution = new SpringMongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
        mongoCollection = mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(SpringMongoEvolution.TRACKER_COLLECTION);
        classNamesAndBeans.put(springEnvironment.getClass().getName(), springEnvironment);
    }

    @Test
    public void evolveConcurrent_emptyMongoEvolutionCollection() throws InterruptedException {

        try {
            Assert.assertEquals(0, (int)mongoCollection.countDocuments());
            evolveConcurrent();
        }catch (Exception e) {
            log.error("Execution error: {}", e);
            Assert.fail();
        }
    }

    @Test
    public void evolveConcurrent_NotEmptyMongoEvolutionCollection() throws InterruptedException {
        try {
            Assert.assertFalse(springMongoEvolution.isUpdateInProgress());
            Assert.assertEquals(1, (int)mongoCollection.countDocuments());
            evolveConcurrent();
        }catch (Exception e) {
            log.error("Execution error: {}", e);
            Assert.fail();
        }
    }

    private void evolveConcurrent() throws Exception{
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT_EVOLVE);
        List<Future<Long>> futures = executorService.invokeAll(spawnThreads());
        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
        }
        futures.stream().forEach(result -> {
            try {
                Assert.assertEquals(Long.valueOf(1L), result.get());
            } catch (Exception ex) {
                Assert.fail();
            }
        });
    }

    private Collection<Callable<Long>> spawnThreads() {
        Collection<Callable<Long>> threads = new HashSet<>();
        for(int i=0; i < THREAD_COUNT_EVOLVE; i++) {
            threads.add(() ->{
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