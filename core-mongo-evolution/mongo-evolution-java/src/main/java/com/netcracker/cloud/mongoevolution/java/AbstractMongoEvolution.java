package com.netcracker.cloud.mongoevolution.java;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import org.qubership.cloud.mongoevolution.java.annotation.AnnotationProcessor;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

public class AbstractMongoEvolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMongoEvolution.class);

    private static final long INITIAL_VERSION = 0;
    public static final String TRACKER_COLLECTION = "_schema_evolution";
    public static final String TRACKER_KEY_UPDATE_START = "startTime";
    public static final String TRACKER_KEY_UPDATE_END = "endTime";
    public static final String TRACKER_KEY_UPDATE_LAST = "lastUpdateTime";
    public static final String TRACKER_IN_PROGRESS = "inProgress";
    public static final String TRACKER_CURRENT_VERSION = "currentVersion";

    public static final long DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE = 30;
    public static final long DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE = 1;
    public static final long DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK = 300;
    public static final long DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK = 30;

    private static final int ERR_CODE_MONGO_DUPLICATE_KEY = 11000;/*E11000 duplicate key error index*/
    private final MongoClient client;
    private final String dbName;
    private final ConnectionSearchKey connectionSearchKey;
    private final MongoDatabase database;

    private MongoDbUpdateStatusTask statusTask = null;

    // Configurable parameters
    private long waitTimeSecondsForUpdate;
    private long waitTimeMillisecWithinUpdate;
    private long waitTimeMillisecForUpdateStatusTask;
    private long delayTimeMillisecStatusTask;

    public long getWaitTimeSecondsForUpdate() {
        return waitTimeSecondsForUpdate;
    }

    public long getWaitTimeMillisecWithinUpdate() {
        return waitTimeMillisecWithinUpdate;
    }

    public long getWaitTimeMillisecForUpdateStatusTask() {
        return waitTimeMillisecForUpdateStatusTask;
    }

    public long getDelayTimeMillisecStatusTask() {
        return delayTimeMillisecStatusTask;
    }

    protected AbstractMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey) {
        this(client, dbName, connectionSearchKey,
                DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE,
                DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE,
                DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK,
                DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK);
    }

    protected AbstractMongoEvolution(MongoClient client, String dbName, ConnectionSearchKey connectionSearchKey,
                                     long waitTimeSecondsForUpdate, long waitTimeSecondsWithinUpdate,
                                     long waitTimeSecondsForUpdateStatusTask, long delayTimeSecondsStatusTask) {
        this.client = client;
        this.dbName = dbName;
        this.connectionSearchKey = connectionSearchKey;
        this.database = client.getDatabase(dbName);
        this.waitTimeSecondsForUpdate = waitTimeSecondsForUpdate > 0 ? waitTimeSecondsForUpdate : DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE;
        this.waitTimeMillisecWithinUpdate = waitTimeSecondsWithinUpdate > 0 ? TimeUnit.SECONDS.toMillis(waitTimeSecondsWithinUpdate) : TimeUnit.SECONDS.toMillis(DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE);
        this.waitTimeMillisecForUpdateStatusTask = waitTimeSecondsForUpdateStatusTask > 0 ? TimeUnit.SECONDS.toMillis(waitTimeSecondsForUpdateStatusTask) : TimeUnit.SECONDS.toMillis(DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK);
        this.delayTimeMillisecStatusTask = delayTimeSecondsStatusTask > 0 ? TimeUnit.SECONDS.toMillis(delayTimeSecondsStatusTask) : TimeUnit.SECONDS.toMillis(DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK);
    }

    protected void doEvolve(AnnotationProcessor processor) throws Exception {
        Timer updateStatusTask = startMongoDbUpdateStatusTask();
        Long currentVersion = null;
        try {
            while (isUpdateInProgress()) {
                Thread.sleep(TimeUnit.SECONDS.toMillis(waitTimeSecondsForUpdate));
            }

            Long expectedVersion = processor.getMaxChangeLogVersion();
            LOGGER.debug("Mongo evolution expected version:{}", expectedVersion);
            boolean finishUpdate = false;

            while (!finishUpdate) {

                currentVersion = getDbCurrentVersion();
                LOGGER.debug("Mongo evolution current version:{}", currentVersion);
                if (expectedVersion > currentVersion) {
                    statusTask.setVersionBeforeUpdate(currentVersion);
                    MongoCollection<Document> updatesTracker = client.getDatabase(dbName).getCollection(TRACKER_COLLECTION);
                    /* insert current DB version at start */
                    boolean startUpdate = insertUpdateFlag(updatesTracker, currentVersion, true);
                    if (startUpdate) {
                        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_START, null);
                        updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_LAST, null);
                        processor.applyChanges(currentVersion);

                        /* insert expected version at finish */
                        finishUpdate = insertUpdateFlag(updatesTracker, expectedVersion, false);
                        if (finishUpdate) {
                            updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_END, null);
                        }
                    } else {
                        try {
                            Thread.sleep(waitTimeMillisecWithinUpdate);
                        } catch (InterruptedException e) {
                            LOGGER.error("executeChangeLogUpdate Thread was interrupted: {}", e);
                        }
                    }
                } else {
                    finishUpdate = true;
                }
            }
        } catch (Exception e) {
            LOGGER.info("Exception during evolve. Try to set currentVersion and status InProgress=false");
            try {
                MongoCollection<Document> updatesTracker = client.getDatabase(dbName).getCollection(TRACKER_COLLECTION);
                Long expectedVersion = getDbCurrentVersion();
                if (currentVersion != null) {
                    if (expectedVersion > currentVersion) {
                        insertUpdateFlag(updatesTracker, expectedVersion, false);
                    } else {
                        insertUpdateFlag(updatesTracker, currentVersion, false);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Cant't update InProgress status for db {}.", dbName, ex);
            } finally {
                throw new Exception("executeChangeLogUpdate Exception during update.", e);
            }
        } catch (Throwable t) {
            LOGGER.error("Error: ", t);
        } finally {
            updateStatusTask.cancel();
        }
    }

    private Timer startMongoDbUpdateStatusTask() {
        Timer updateStatusTimer = new Timer();
        statusTask = new MongoDbUpdateStatusTask(updateStatusTimer, this, database);
        updateStatusTimer.schedule(statusTask,
                delayTimeMillisecStatusTask, delayTimeMillisecStatusTask);
        return updateStatusTimer;
    }

    public static void updateFieldWithMongoCurrentDate(MongoCollection<Document> collection, String fieldName, BasicDBObject query) {
        if (null == query) {
            query = new BasicDBObject();
        }
        BasicDBObject update = new BasicDBObject("$currentDate",
                new BasicDBObject(fieldName, new BasicDBObject("$type", "timestamp"))
        );

        collection.updateOne(query, update, new UpdateOptions());
    }

    public Document createTrackerCollectionRecord(long dateStart, long dateEnd, boolean in_progress, long version) {
        return new Document()
                .append(TRACKER_KEY_UPDATE_START, dateStart)
                .append(TRACKER_IN_PROGRESS, in_progress)
                .append(TRACKER_KEY_UPDATE_END, dateEnd)
                .append(TRACKER_CURRENT_VERSION, version)
                .append(TRACKER_KEY_UPDATE_LAST, dateStart);
    }

    public boolean isDatabaseUpdateLockAlive() {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
        long lastUpdateStatusTimeMillis = 1000L * (((BsonTimestamp) updatesTracker.find().
                first().get(TRACKER_KEY_UPDATE_LAST))
                .getTime());
        long currentTimeMillis = currentTimeMillis();
        long millisecDiff = currentTimeMillis - lastUpdateStatusTimeMillis;
        return millisecDiff <= waitTimeMillisecForUpdateStatusTask;
    }

    public boolean isUpdateInProgress() throws Exception {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
        try {
            FindIterable<Document> docs = updatesTracker.find();
            Document doc;
            if (!docs.iterator().hasNext()) {
                long currentTime = currentTimeMillis();
                doc = createTrackerCollectionRecord(currentTime, currentTime, false, INITIAL_VERSION);

                updatesTracker.createIndex(new Document(TRACKER_IN_PROGRESS, 1), new IndexOptions().unique(true));
                updatesTracker.insertOne(doc);

                updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_START, null);
                updateFieldWithMongoCurrentDate(updatesTracker, TRACKER_KEY_UPDATE_END, null);
            } else {
                doc = docs.first();
            }

            return (boolean) doc.get(TRACKER_IN_PROGRESS);
        } catch (MongoWriteException mongoEx) {
            if (mongoEx.getError().getCode() == ERR_CODE_MONGO_DUPLICATE_KEY) {
                LOGGER.debug("getUpdateStatus failed due to DOCUMENT concurrent insertion: {}", mongoEx);
                return true;
            } else {
                throw mongoEx;
            }
        } catch (MongoCommandException mongoEx) {
            if (mongoEx.getErrorCode() == ERR_CODE_MONGO_DUPLICATE_KEY) {
                LOGGER.debug("getUpdateStatus failed due to INDEX concurrent insertion in collection: {}", mongoEx);
                return true;
            } else {
                throw mongoEx;
            }
        } catch (Exception e) {
            LOGGER.error("getUpdateStatus failed: {}", e);
            throw e;
        }
    }

    boolean insertUpdateFlag(MongoCollection<Document> collection, Long expectedVersion, boolean updateInProgress) {

        try {
            long currentTime = currentTimeMillis();
            Document newDoc = new Document()
                    .append(TRACKER_IN_PROGRESS, updateInProgress);
            if (expectedVersion != null) {
                newDoc.append(TRACKER_CURRENT_VERSION, expectedVersion);
            }
            if (updateInProgress) {
                newDoc.append(TRACKER_KEY_UPDATE_START, currentTime);
            } else {
                newDoc.append(TRACKER_KEY_UPDATE_END, currentTime);
            }

            Document previousDoc = collection.findOneAndUpdate(Filters.eq(TRACKER_IN_PROGRESS, !updateInProgress),
                    new Document("$set", newDoc));

            return (null == previousDoc) ? false : true;
        } catch (MongoCommandException mongoEx) {
            if (mongoEx.getErrorCode() == ERR_CODE_MONGO_DUPLICATE_KEY) {
                LOGGER.debug("insertUpdateFlag failed due to INDEX concurrent insertion in collection: {}", mongoEx);
                return false;
            } else {
                throw mongoEx;
            }
        } catch (Exception e) {
            LOGGER.error("getUpdateStatus failed: {}", e);
            throw e;
        }
    }


    public Long getDbCurrentVersion() {
        MongoCollection<Document> updatesTracker = database.getCollection(TRACKER_COLLECTION);
        Document doc = updatesTracker.find().first();
        return (Long) doc.get(TRACKER_CURRENT_VERSION);
    }
}
