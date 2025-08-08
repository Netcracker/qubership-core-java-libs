package com.netcracker.cloud.mongoevolution;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.mongoevolution.java.AbstractMongoEvolution;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeEntry;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.AbstractEnvironment;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = MongoTestConfiguration.class)
class SpringMongoEvolutionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringMongoEvolutionTest.class);
    private static ConnectionSearchKey connectionSearchKeyDefault = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.DEFAULT_DB_NAME);
    private static ConnectionSearchKey connectionSearchKeyError = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.ERROR_DEFAULT_DB_NAME);
    private static final long EXPECTED_MAX_ANNOTATION_VERSION_PROFILED = 10;
    private static long testStartTimestamp;
    private final boolean in_progress = false;
    private final long version = 1;


    SpringMongoEvolution springMongoEvolution;
    SpringMongoEvolution springMongoEvolutionError;

    @Autowired
    private MongoClient fakeMongoClient;

    MongoCollection<Document> mongoCollection;
    Document testDoc = null;

    @Autowired
    AbstractEnvironment springEnvironment;

    private SpringDBManagerEntity springDBManagerEntity = new SpringDBManagerEntity();
    private Map<String, Object> classNamesAndBeans = new HashMap<>();
    private List<String> tenantChangeLogsPackages;
    private List<String> serviceChangeLogsPackages;
    private String strChangeLogsPackages;

    private static long getCurrentTimeInSeconds() {
        return currentTimeMillis() / 1000;
    }

    private SpringMongoEvolution createStandardMongoDbSchemaEvolution() {
        return new SpringMongoEvolution(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
    }

    private SpringMongoEvolution createErrorMongoDbSchemaEvolution() {
        return new SpringMongoEvolution(fakeMongoClient, TestConstants.ERROR_DB_NAME, connectionSearchKeyError);
    }

    @BeforeAll
    static void setUpBeforeClass() {
        testStartTimestamp = getCurrentTimeInSeconds();
    }

    @BeforeEach
    void setUp() throws Exception {
        springMongoEvolution = createStandardMongoDbSchemaEvolution();
        springMongoEvolutionError = createErrorMongoDbSchemaEvolution();
        mongoCollection = fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(SpringMongoEvolution.TRACKER_COLLECTION);
        testDoc = springMongoEvolution.createTrackerCollectionRecord(testStartTimestamp, testStartTimestamp, in_progress, version);
        classNamesAndBeans.put(springEnvironment.getClass().getName(), springEnvironment);
        tenantChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_TENANT_PACKAGE, TestConstants.CHANGELOGS_SCAN_PROFILE_PACKAGE);
        serviceChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_SERVICE_PACKAGE);
        strChangeLogsPackages = TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE;

        /* clear changelog caches*/
        Method m = AnnotationProcessor.class.getDeclaredMethod("clearCache");
        m.setAccessible(true);
        m.invoke(null);

        springEnvironment.setActiveProfiles(TestConstants.DEFAULT_PROFILE);
    }

    @AfterEach
    void setDown() {
        if (mongoCollection != null) {
            mongoCollection.drop();
        }
        fakeMongoClient.getDatabase(TestConstants.DB_NAME).drop();
    }

    @Test
    void getMaxChangeLogVersion_withProfile() throws Exception {
        springEnvironment.setActiveProfiles(TestConstants.TEST_PROFILE);
        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        processor.applyChanges(0);
        long parsedVersion = processor.getMaxChangeLogVersion();
        assertEquals(EXPECTED_MAX_ANNOTATION_VERSION_PROFILED, parsedVersion);
    }

    @Test
    void evolve() throws Exception {
        springMongoEvolution.evolve(TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        assertEquals(1, (int) mongoCollection.countDocuments());
    }

    @Test
    void evolve_classNamesAndBeans() throws Exception {
        springEnvironment.setActiveProfiles(TestConstants.TEST_PROFILE_2);
        springMongoEvolution.evolve(TestConstants.CHANGELOGS_SCAN_PROFILE_PACKAGE, springEnvironment, classNamesAndBeans);
        assertEquals(1, fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    void evolve_changeLogsScanPackag_list_environment() throws Exception {
        springMongoEvolution.evolve(tenantChangeLogsPackages, springEnvironment);
        assertEquals(2, (int) fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    void evolve_changeLogsScanPackag_str_environment() throws Exception {
        springMongoEvolution.evolve(strChangeLogsPackages, springEnvironment);
        assertEquals(1, (int) fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    void evolveTenantDataBase() throws Exception {
        springMongoEvolution.evolve(tenantChangeLogsPackages, springEnvironment, classNamesAndBeans);
        assertEquals(2, (int) fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    void evolveServiceDataBase() throws Exception {
        springMongoEvolution.evolve(serviceChangeLogsPackages, springEnvironment, classNamesAndBeans);
        assertEquals(2, (int) fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    void evolveErrorDataBase() {
        try {
            springMongoEvolutionError.evolve(TestConstants.CHANGELOGS_SCAN_ERROR_PACKAGE, springEnvironment, classNamesAndBeans);
        } catch (Exception e) {
            assertEquals("error", e.getCause().getCause().getCause().getMessage());
        }
        assertEquals(1, (int) fakeMongoClient.getDatabase(TestConstants.ERROR_DB_NAME).getCollection(TestConstants.COLLECTION_NAME).countDocuments());
    }

    @Test
    void isDatabaseUpdateLockAliveTest() {
        Document doc1 = new Document()
                .append(SpringMongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int) getCurrentTimeInSeconds(), 0));
        mongoCollection.insertOne(doc1);
        assertTrue(springMongoEvolution.isDatabaseUpdateLockAlive());
        // deleteOne() replaced because mongo-java-server has bug with remove object
        mongoCollection.deleteOne(Filters.eq("_id", doc1.get("_id")));

        Document doc2 = new Document()
                .append(SpringMongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int) (getCurrentTimeInSeconds() - springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask() / 1000), 0));
        mongoCollection.insertOne(doc2);
        assertFalse(springMongoEvolution.isDatabaseUpdateLockAlive());
        mongoCollection.deleteOne(doc2);
    }

    @Test
    void applyChangesTest() {
        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        try {
            processor.applyChanges(3);
            assertNull(fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first());

            long before = getCurrentTimeInSeconds();
            processor.applyChanges(1);
            long after = getCurrentTimeInSeconds();
            Document first = fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first();
            System.out.println("Document: " + first);
            long lastUpdate = ((BsonTimestamp) first.get("timestamp")).getTime();
            assertTrue(before <= lastUpdate && lastUpdate <= after);
        } catch (Exception e) {
            LOGGER.error("Execution error: {}", e);
            fail();
        }
    }

    @Test
    void constructorSignatureAnnotationProcessorTest() {
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages, springEnvironment);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages, springEnvironment, classNamesAndBeans);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, serviceChangeLogsPackages, springEnvironment, springDBManagerEntity, classNamesAndBeans);
    }

    @Test
    void testConstructorForDefaultFields() {
        SpringMongoEvolution springMongoEvolution = createStandardMongoDbSchemaEvolution();
        assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, springMongoEvolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), springMongoEvolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    void testConstructorForCustomFields() {
        int customWaitTimeSecondsForUpdate = 45;
        int customWaitTimeMillisecWithinUpdate = 15;
        long customWaitTimeForUpdateStatusTask = 6000;
        long customDelayStatusTask = 45;

        SpringMongoEvolution springMongoEvolution = new SpringMongoEvolution(
                fakeMongoClient,
                "testdb",
                connectionSearchKeyDefault,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        assertEquals(customWaitTimeSecondsForUpdate, springMongoEvolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeMillisecWithinUpdate), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeForUpdateStatusTask), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(customDelayStatusTask), springMongoEvolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    void testCustomConstructorWithNegativeValues() {
        long customWaitTimeSecondsForUpdate = -45;
        long customWaitTimeMillisecWithinUpdate = -15;
        long customWaitTimeForUpdateStatusTask = -60000;
        long customDelayStatusTask = -45;
        SpringMongoEvolution springMongoEvolution = new SpringMongoEvolution(
                fakeMongoClient,
                "testdb",
                connectionSearchKeyDefault,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, springMongoEvolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), springMongoEvolution.getDelayTimeMillisecStatusTask());
    }
}
