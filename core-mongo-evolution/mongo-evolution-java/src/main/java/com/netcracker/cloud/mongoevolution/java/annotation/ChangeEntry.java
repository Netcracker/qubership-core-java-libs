package com.netcracker.cloud.mongoevolution.java.annotation;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.netcracker.cloud.mongoevolution.java.MongoEvolution;
import lombok.Data;
import org.bson.Document;

import java.lang.reflect.Method;
import java.util.Comparator;

import static java.lang.System.currentTimeMillis;

@Data
public class ChangeEntry {

    public static Comparator<ChangeEntry> COMPARATOR = new Comparator<ChangeEntry>() {
        @Override
        public int compare(ChangeEntry c1, ChangeEntry c2) {
            int version = Long.compare(c1.getVersion(), c2.getVersion());
            if (version != 0) {
                return version;
            }
            int orderChangeLog = Integer.compare(c1.getOrderChangeLog(), c2.getOrderChangeLog());
            return (orderChangeLog != 0) ?
                    orderChangeLog :
                    Integer.compare(c1.getOrderChangeSet(), c2.getOrderChangeSet());
        }
    };

    public static final String CHANGELOG_COLLECTION = "_schema_change_log";
    public static final String KEY_VERSION = "version";
    public static final String KEY_CHANGELOGCLASS = "changeLogClass";
    public static final String KEY_CHANGESETMETHOD = "changeSetMethod";
    private static final String KEY_CHANGELOG_ORDER = "orderChangeLog";
    private static final String KEY_CHANGESET_ORDER = "orderChangeSet";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_UPDATETIME = "timeToUpdate";

    private long version;
    private int orderChangeLog;
    private int orderChangeSet;
    private long dateInMillis;
    private Method changeSetMethod;
    private Object changeLogInstance;

    public ChangeEntry(long version, int orderChangeLog, int orderChangeSet, Method changeSetMethod, Object changeLogInstance) {
        this.version = version;
        this.orderChangeLog = orderChangeLog;
        this.orderChangeSet = orderChangeSet;
        this.dateInMillis = currentTimeMillis();
        this.changeSetMethod = changeSetMethod;
        this.changeLogInstance = changeLogInstance;
    }

    public String getChangeClassName(){
        return changeSetMethod.getDeclaringClass().getName();
    }

    public String getChangeMethodName(){
        return changeSetMethod.getName();
    }

    public Document buildFullDBObject(long updateTimeInSec) {
        return new Document()
                .append(KEY_VERSION, this.version)
                .append(KEY_CHANGELOG_ORDER, this.orderChangeLog)
                .append(KEY_CHANGESET_ORDER, this.orderChangeSet)
                .append(KEY_TIMESTAMP, this.dateInMillis)
                .append(KEY_UPDATETIME, updateTimeInSec)
                .append(KEY_CHANGELOGCLASS, this.getChangeClassName())
                .append(KEY_CHANGESETMETHOD, this.getChangeMethodName());
    }

    public void saveEntryInChangeLog(MongoDatabase db) throws Exception {
        long updateTimeInMillis = currentTimeMillis() - dateInMillis;
        Document entryDoc = this.buildFullDBObject(updateTimeInMillis);

        MongoCollection<Document> collection = db.getCollection(CHANGELOG_COLLECTION);
        collection.insertOne(entryDoc);
        BasicDBObject query = new BasicDBObject().append("_id", entryDoc.get("_id"));
        MongoEvolution.updateFieldWithMongoCurrentDate(collection, KEY_TIMESTAMP, query);
    }

    @Override
    public String toString() {
        return "[ChangeSet: " +
                ", version=" + this.version +
                ", orderChangeLog=" + this.orderChangeLog +
                ", orderChangeSet=" + this.orderChangeSet +
                ", changeLogClass=" + this.getChangeClassName() +
                ", changeSetMethod=" + this.getChangeMethodName() + "]";
    }

}

