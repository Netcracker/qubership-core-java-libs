package org.qubership.cloud.mongoevolution;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.qubership.cloud.mongoevolution.java.AbstractMongoEvolution;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.annotation.ChangeEntry;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest({AbstractMongoEvolution.class})
@ContextConfiguration(classes = MongoTestConfiguration.class)
public class SpringMongoEvolutionTest {

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

    private static long getCurrentTimeInSeconds(){
        return currentTimeMillis()/1000;
    }

    private SpringMongoEvolution createStandardMongoDbSchemaEvolution() {
        return new SpringMongoEvolution(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault);
    }

    private SpringMongoEvolution createErrorMongoDbSchemaEvolution() {
        return new SpringMongoEvolution(fakeMongoClient, TestConstants.ERROR_DB_NAME, connectionSearchKeyError);
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        testStartTimestamp = getCurrentTimeInSeconds();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        springMongoEvolution = createStandardMongoDbSchemaEvolution();
        springMongoEvolutionError = createErrorMongoDbSchemaEvolution();
        mongoCollection = fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(SpringMongoEvolution.TRACKER_COLLECTION);
        testDoc = springMongoEvolution.createTrackerCollectionRecord(testStartTimestamp, testStartTimestamp, in_progress, version);
        classNamesAndBeans.put(springEnvironment.getClass().getName(), springEnvironment);
        tenantChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_TENANT_PACKAGE, TestConstants.CHANGELOGS_SCAN_PROFILE_PACKAGE);
        serviceChangeLogsPackages = Arrays.asList(TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE, TestConstants.CHANGELOGS_SCAN_SERVICE_PACKAGE);
        strChangeLogsPackages = TestConstants.CHANGELOGS_SCAN_BASE_PACKAGE;

        /* $currentDate command with $type=timestamp is not supported by mongo-java-server
         * so we make it mock & $set command
         */
        PowerMockito.mockStatic(AbstractMongoEvolution.class);
        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

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
            }
        }).when(AbstractMongoEvolution.class, "updateFieldWithMongoCurrentDate", any(), any(), any());

        /* clear changelog caches*/
        Field field = AnnotationProcessor.class.getDeclaredField("changeEntriesCache");
        field.setAccessible(true);

        final Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, new ConcurrentHashMap<>());

        springEnvironment.setActiveProfiles(TestConstants.DEFAULT_PROFILE);
    }

    @After
    public void setDown(){
        if(mongoCollection != null) {
            mongoCollection.drop();
        }
        fakeMongoClient.getDatabase(TestConstants.DB_NAME).drop();
    }

    @Test
    public void getMaxChangeLogVersion_withProfile() throws Exception {
        springEnvironment.setActiveProfiles(TestConstants.TEST_PROFILE);
        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        processor.applyChanges(0);
        long parsedVersion = processor.getMaxChangeLogVersion();
        Assert.assertEquals(EXPECTED_MAX_ANNOTATION_VERSION_PROFILED, parsedVersion);
    }

    @Test
    public void evolve() throws Exception {
        springMongoEvolution.evolve(TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        Assert.assertEquals(1, (int)mongoCollection.countDocuments());
    }

    @Test
    public void evolve_classNamesAndBeans() throws Exception {
        springEnvironment.setActiveProfiles(TestConstants.TEST_PROFILE_2);
        springMongoEvolution.evolve(TestConstants.CHANGELOGS_SCAN_PROFILE_PACKAGE, springEnvironment, classNamesAndBeans);
        Assert.assertEquals(1, fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }


    @Test
    public void evolve_changeLogsScanPackag_list_environment() throws Exception {
        springMongoEvolution.evolve(tenantChangeLogsPackages, springEnvironment);
        Assert.assertEquals(2, (int)fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolve_changeLogsScanPackag_str_environment() throws Exception {
        springMongoEvolution.evolve(strChangeLogsPackages, springEnvironment);
        Assert.assertEquals(1, (int)fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveTenantDataBase() throws Exception {
        springMongoEvolution.evolve(tenantChangeLogsPackages, springEnvironment, classNamesAndBeans);
        Assert.assertEquals(2, (int)fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveServiceDataBase() throws Exception {
        springMongoEvolution.evolve(serviceChangeLogsPackages, springEnvironment, classNamesAndBeans);
        Assert.assertEquals(2, (int)fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(TestConstants.PACKAGE_COLLECTION_NAME).countDocuments());
    }

    @Test
    public void evolveErrorDataBase() throws Exception {
        try {
            springMongoEvolutionError.evolve(TestConstants.CHANGELOGS_SCAN_ERROR_PACKAGE, springEnvironment, classNamesAndBeans);
        }
        catch (Exception e){
            Assert.assertEquals(e.getCause().getCause().getCause().getMessage(), "error");
        }
        Assert.assertEquals(1, (int)fakeMongoClient.getDatabase(TestConstants.ERROR_DB_NAME).getCollection(TestConstants.COLLECTION_NAME).countDocuments());
    }

    @Test
    public void isDatabaseUpdateLockAliveTest() {
        Document doc1 = new Document()
                .append(SpringMongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int)getCurrentTimeInSeconds(), 0));
        mongoCollection.insertOne(doc1);
        Assert.assertTrue(springMongoEvolution.isDatabaseUpdateLockAlive());
        // deleteOne() replaced because mongo-java-server has bug with remove object
        mongoCollection.deleteOne(Filters.eq("_id", doc1.get("_id")));

        Document doc2 = new Document()
                .append(SpringMongoEvolution.TRACKER_KEY_UPDATE_LAST, new BsonTimestamp((int) (getCurrentTimeInSeconds() - springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask() / 1000 ), 0));
        mongoCollection.insertOne(doc2);
        Assert.assertFalse(springMongoEvolution.isDatabaseUpdateLockAlive());
        mongoCollection.deleteOne(doc2);
    }

    @Test
    public void applyChangesTest(){
        AnnotationProcessor processor = new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME,
                connectionSearchKeyDefault, TestConstants.CHANGELOGS_SCAN_PACKAGE, springEnvironment, classNamesAndBeans);
        try {
            processor.applyChanges(3);
            Assert.assertNull(fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first());

            long before = getCurrentTimeInSeconds();
            processor.applyChanges(1);
            long after = getCurrentTimeInSeconds();
            Document first = fakeMongoClient.getDatabase(TestConstants.DB_NAME).getCollection(ChangeEntry.CHANGELOG_COLLECTION).find().first();
            System.out.println("Document: " + first);
            long lastUpdate =  ((BsonTimestamp) first.get("timestamp")).getTime();
            Assert.assertTrue(before <= lastUpdate && lastUpdate <= after);
        } catch (Exception e){
            LOGGER.error("Execution error: {}", e);
            Assert.fail();
        }
    }


    @Test
    public void constructorSignatureAnnotationProcessorTest(){
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages, springEnvironment);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, strChangeLogsPackages, springEnvironment, classNamesAndBeans);
        new SpringMongoEvolutionProcessor(fakeMongoClient, TestConstants.DB_NAME, connectionSearchKeyDefault, serviceChangeLogsPackages, springEnvironment, springDBManagerEntity, classNamesAndBeans);
    }

    @Test
    public void testConstructorForDefaultFields(){
        SpringMongoEvolution springMongoEvolution = createStandardMongoDbSchemaEvolution();
        Assert.assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, springMongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), springMongoEvolution.getDelayTimeMillisecStatusTask());

    }
    @Test
    public void testConstructorForCustomFields(){
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

        Assert.assertEquals(customWaitTimeSecondsForUpdate, springMongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeMillisecWithinUpdate), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeForUpdateStatusTask), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customDelayStatusTask), springMongoEvolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    public void testCustomConstructorWithNegativeValues() {
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

        Assert.assertEquals(springMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, springMongoEvolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(springMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), springMongoEvolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(springMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), springMongoEvolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(springMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), springMongoEvolution.getDelayTimeMillisecStatusTask());
    }

}