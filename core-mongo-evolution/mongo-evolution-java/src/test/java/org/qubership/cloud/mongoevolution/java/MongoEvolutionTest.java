package org.qubership.cloud.mongoevolution.java;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeEntry;
import org.qubership.cloud.mongoevolution.java.annotation.DBManagerEntity;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractMongoEvolution.class})
public class MongoEvolutionTest extends MongoServerConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoEvolutionTest.class);
    private static ConnectionSearchKey connectionSearchKeyDefault = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.DEFAULT_DB_NAME);
    private static ConnectionSearchKey connectionSearchKeyError = new ConnectionSearchKey(TestConstants.TENANT_ID, TestConstants.ERROR_DEFAULT_DB_NAME);
    private static final long EXPECTED_MAX_ANNOTATION_VERSION_PROFILED = 10;
    private static final long EXPECTED_MAX_ANNOTATION_VERSION = 3;
    private static long testStartTimestamp;
    private final boolean in_progress = false;
    private final long version = 1;


    MongoEvolution mongoEvolution;
    MongoEvolution mongoEvolutionError;


    MongoCollection<Document> mongoCollection;
    Document testDoc = null;

    private DBManagerEntity dbManagerEntity = new DBManagerEntity();
    private List<String> tenantChangeLogsPackages;
    private List<String> serviceChangeLogsPackages;
    private String strChangeLogsPackages;

    private static long getCurrentTimeInSeconds(){
        return currentTimeMillis()/1000;
    }

    private MongoEvolution createStandardMongoDbSchemaEvolution() {
        return new MongoEvolution(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
    }

    private MongoEvolution createErrorMongoDbSchemaEvolution() {
        return new MongoEvolution(mongoClient, TestConstants.ERROR_DB_NAME, connectionSearchKeyError);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        testStartTimestamp = getCurrentTimeInSeconds();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void prepareTest() throws Exception {
        mongoEvolution = createStandardMongoDbSchemaEvolution();
        mongoEvolutionError = createErrorMongoDbSchemaEvolution();
        mongoCollection = mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(MongoEvolution.TRACKER_COLLECTION);
        testDoc = mongoEvolution.createTrackerCollectionRecord(testStartTimestamp, testStartTimestamp, in_progress, version);
        tenantChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_TENANT_PACKAGE);
        serviceChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_SERVICE_PACKAGE);
        strChangeLogsPackages = TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE;

        /* $currentDate command with $type=timestamp is not supported by mongo-java-server
         * so we make it mock & $set command
         */
        try (MockedStatic<AbstractMongoEvolution> evolution = mockStatic(AbstractMongoEvolution.class)) {
            evolution.when(() -> AbstractMongoEvolution.updateFieldWithMongoCurrentDate(any(), any(), any())).thenAnswer((Answer<Void>) invocation -> {

                MongoCollection<Document> collection = (MongoCollection<Document>) invocation.getArguments()[0];
                String fieldName = (String) invocation.getArguments()[1];
                BasicDBObject query = (BasicDBObject) invocation.getArguments()[2];
                if (null == query) {
                    query = new BasicDBObject();
                }

                BasicDBObject update = new BasicDBObject("$set",
                        new BasicDBObject(fieldName, new BsonTimestamp((int) getCurrentTimeInSeconds(), 1)));

                collection.updateOne(query, update, new UpdateOptions());

                return null;
            });
        }

        /* clear changelog caches*/
        Method m = AnnotationProcessor.class.getDeclaredMethod("clearCache");
        m.setAccessible(true);
        m.invoke(null);
    }

    @After
    public void setDown(){
        if(mongoCollection != null) {
            mongoCollection.drop();
        }
        mongoClient.getDatabase(TestConstants.DB_NAME).drop();
    }

    @Test
    public void createTrackerCollectionRecord() throws Exception {
        Assert.assertEquals(testStartTimestamp, testDoc.get(MongoEvolution.TRACKER_KEY_UPDATE_START));
        Assert.assertEquals(testStartTimestamp, testDoc.get(MongoEvolution.TRACKER_KEY_UPDATE_END));
        Assert.assertEquals(in_progress, testDoc.get(MongoEvolution.TRACKER_IN_PROGRESS));
        Assert.assertEquals(version, testDoc.get(MongoEvolution.TRACKER_CURRENT_VERSION));
    }

    @Test
    public void insertUpdateFlag(){
        mongoCollection.insertOne(testDoc);
        boolean updateInProgress = true;
        boolean result = mongoEvolution.insertUpdateFlag(mongoCollection, version+1, updateInProgress);
        Assert.assertTrue(result);
    }

    @Test
    public void isUpdateInProgress() throws Exception {
        mongoCollection.insertOne(testDoc);
        mongoEvolution.isUpdateInProgress();
        Assert.assertEquals(in_progress, testDoc.get(MongoEvolution.TRACKER_IN_PROGRESS));
    }

    @Test
    public void getDbCurrentVersion() throws Exception {
        mongoCollection.insertOne(testDoc);
        long version = mongoEvolution.getDbCurrentVersion();
        Assert.assertEquals(testDoc.get(MongoEvolution.TRACKER_CURRENT_VERSION), version);
    }

    @Test
    public void getMaxChangeLogVersion() throws Exception {
        AnnotationProcessor processor = new AnnotationProcessor(mongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE);
        long parsedVersion = processor.getMaxChangeLogVersion();
        Assert.assertEquals(EXPECTED_MAX_ANNOTATION_VERSION, parsedVersion);
    }

    @Test
    public void evolve() throws Exception {
        mongoEvolution.evolve(TestConstants.CHANGELOGS_SCAN_PACKAGE);
        Assert.assertEquals(1, (int) mongoCollection.countDocuments());
    }



    @Test
    public void evolve_changeLogsScanPackage() throws Exception {
        mongoEvolution.evolve(strChangeLogsPackages);
        Assert.assertEquals(1, (int) mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveTenantDataBase() throws Exception {
        mongoEvolution.evolve(tenantChangeLogsPackages);
        Assert.assertEquals(2, (int) mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveServiceDataBase() throws Exception {
        mongoEvolution.evolve(serviceChangeLogsPackages);
        Assert.assertEquals(2, (int) mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveErrorDataBase() throws Exception {
        try {
            mongoEvolutionError.evolve(TestConstants.CHANGELOGS_SCAN_ERROR_PACKAGE);
        }
        catch (Exception e){
            Assert.assertEquals(e.getCause().getCause().getCause().getMessage(), "error");
        }
        Assert.assertEquals(1, (int) mongoClient.getDatabase(TestConstants.ERROR_DB_NAME).getCollection(TestConstants.COLLECTION_NAME).countDocuments());
    }

    @Test
    public void isDatabaseUpdateLockAliveTest() {
        Document doc1 = new Document()
                .append(MongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int)getCurrentTimeInSeconds(), 0));
        mongoCollection.insertOne(doc1);
        Assert.assertTrue(mongoEvolution.isDatabaseUpdateLockAlive());
        mongoCollection.deleteOne(Filters.eq("_id", doc1.get("_id")));
        Document doc2 = new Document()
                .append(MongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int) (getCurrentTimeInSeconds() - mongoEvolution.getWaitTimeMillisecForUpdateStatusTask() / 1000 ), 0));
        mongoCollection.insertOne(doc2);
        Assert.assertFalse(mongoEvolution.isDatabaseUpdateLockAlive());
        mongoCollection.deleteOne(doc2);
    }

    @Test
    public void applyChangesTest(){
        AnnotationProcessor processor = new AnnotationProcessor(mongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE);
        try {
            processor.applyChanges(3);
            Assert.assertNull(mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first());

            long before = getCurrentTimeInSeconds();
            processor.applyChanges(1);
            long after = getCurrentTimeInSeconds();
            long lastUpdate = ((BsonTimestamp) mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first().get("timestamp")).getTime();
            Assert.assertTrue(before <= lastUpdate && lastUpdate <= after);
        } catch (Exception e){
            LOGGER.error("Execution error: {}", e);
            Assert.fail();
        }
    }

    @Test
    public void saveEntryInChangeLogTest(){
        try {
            ChangeEntry entry1 = new ChangeEntry(1, 1, 1,
                    (ChangeClassTest.class.getDeclaredMethod("someChangeOne", null)), new ChangeClassTest());
            entry1.saveEntryInChangeLog(mongoClient.getDatabase(TestConstants.DB_NAME));
            Assert.assertNotNull(mongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first());
        }  catch (Exception e){
            LOGGER.error("Execution error: {}", e);
            Assert.fail();
        }
    }

    @Test
    public void updateFieldWithMongoCurrentDateTest(){
        Document dtest = new Document().append("TestField1", "Test Field 1").append("TestimeField",  getCurrentTimeInSeconds() - 8);
        mongoCollection.insertOne(dtest);
        long before = getCurrentTimeInSeconds();
        MongoEvolution.updateFieldWithMongoCurrentDate(mongoCollection, "TestimeField", null);
        long after = getCurrentTimeInSeconds();
        long updtime = ((BsonTimestamp) mongoCollection.find().first().get("TestimeField")).getTime();
        Assert.assertTrue(( updtime>= before && updtime <= after));
    }

    @Test
    public void constructorSignatureAnnotationProcessorTest() {
        new AnnotationProcessor(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages);
        new AnnotationProcessor(mongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, serviceChangeLogsPackages, dbManagerEntity);
    }

    @Test
    public void getChangeClassAndMethodNameTest(){
        try {
            ChangeEntry entry = new ChangeEntry(1, 1, 1,
                    (ChangeClassTest.class.getDeclaredMethod("someChangeOne", null)), new ChangeClassTest());
            Assert.assertEquals("org.qubership.cloud.mongoevolution.java.ChangeClassTest", entry.getChangeClassName());
            Assert.assertEquals("someChangeOne", entry.getChangeMethodName());

        } catch (Exception e) {
            LOGGER.error("Execution error: {}", e);
            Assert.fail();
        }

    }
    @Test
    public void testConstructorForDefaultFields(){
        MongoEvolution mongoEvolution = createStandardMongoDbSchemaEvolution();
        Assert.assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, mongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), mongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), mongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), mongoEvolution.getDelayTimeMillisecStatusTask());

    }
    @Test
    public void testConstructorForCustomFields(){
        int customWaitTimeSecondsForUpdate = 45;
        int customWaitTimeMillisecWithinUpdate = 15;
        long customWaitTimeForUpdateStatusTask = 6000;
        long customDelayStatusTask = 45;

        MongoEvolution mongoEvolution = new MongoEvolution(
                mongoClient,
                "testdb",
                connectionSearchKeyDefault,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        Assert.assertEquals(customWaitTimeSecondsForUpdate, mongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeMillisecWithinUpdate), mongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeForUpdateStatusTask), mongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customDelayStatusTask), mongoEvolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    public void testCustomConstructorWithNegativeValues() {
        long customWaitTimeSecondsForUpdate = -45;
        long customWaitTimeMillisecWithinUpdate = -15;
        long customWaitTimeForUpdateStatusTask = -60000;
        long customDelayStatusTask = -45;
        MongoEvolution mongoEvolution = new MongoEvolution(
                mongoClient,
                "testdb",
                connectionSearchKeyDefault,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        Assert.assertEquals(mongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, mongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(mongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), mongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(mongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), mongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(mongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), mongoEvolution.getDelayTimeMillisecStatusTask());
    }

}