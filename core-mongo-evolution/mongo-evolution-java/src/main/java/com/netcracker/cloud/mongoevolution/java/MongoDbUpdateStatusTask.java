package org.qubership.cloud.mongoevolution.java;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Setter;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

class MongoDbUpdateStatusTask extends TimerTask {
    private final Logger log = LoggerFactory.getLogger(MongoDbUpdateStatusTask.class);

    AbstractMongoEvolution tracker;
    Timer timer;
    MongoDatabase database;

    @Setter
    Long versionBeforeUpdate;


    public MongoDbUpdateStatusTask(Timer timer, AbstractMongoEvolution tracker, MongoDatabase database) {
        this.timer = timer;
        this.tracker = tracker;
		this.database = database;
        this.versionBeforeUpdate = null;
    }

    @Override
    public void run() {
        try {
			if(!tracker.isDatabaseUpdateLockAlive()){
			    log.error("Update process is not active for {}", database.getName());
			    MongoCollection<Document> updatesTracker = database.getCollection(MongoEvolution.TRACKER_COLLECTION);
			    boolean updateInProgress = false;
			    tracker.insertUpdateFlag(updatesTracker, versionBeforeUpdate, updateInProgress);
			    timer.cancel();
			}
		} catch (Exception e) {
            log.error("MongoDbUpdateStatusTask failed {}", e);
		}
    }
}
