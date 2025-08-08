package org.qubership.cloud.dbaas.client.config.msframeworkspecific;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.internal.MongoClientImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Disabled
public class MongoClientTest {

    private static final Logger log = LoggerFactory.getLogger(MongoClientTest.class);
    private static final int ARRAY_SIZE = 100;
    private static String host = "localhost";
    private static String port = "27017";

    static void log(String message) {
        log.info(message);
    }

    @Test
    public void test_100MongoClient() throws Exception {
        log("Run: " + new Throwable().getStackTrace()[0].getMethodName());

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        log("Used Memory before:" + usedMemoryBefore);


        long start = System.currentTimeMillis();

        List<MongoClient> arr = new ArrayList<>(ARRAY_SIZE);
        List<GetDBThread> tArr = new ArrayList<>(ARRAY_SIZE);
        for (int i = 0; i < ARRAY_SIZE; i++) {
            MongoClient localClient = MongoClients.create(host + ":" + port);
            arr.add(localClient);
            tArr.add(new GetDBThread(localClient, i, "test" + i));
        }

        for (GetDBThread thread : tArr) {
            thread.start();
        }

        long end = System.currentTimeMillis();
        log("Time passed for opening: " + (end - start) + " MilliSeconds");

        Thread.sleep(5000);

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        log("Memory increased:" + (usedMemoryAfter - usedMemoryBefore));

        Thread.sleep(5000);

        for (int i = 0; i < ARRAY_SIZE; i++) {
            arr.get(i).close();
        }

        Thread.sleep(5000);
        long usedMemoryAfterClose = runtime.totalMemory() - runtime.freeMemory();
        log("Used Memory after closing connections: " + (usedMemoryAfterClose - usedMemoryBefore));
    }


    @Test
    public void test_1MongoClient() throws Exception {
        log("Run: " + new Throwable().getStackTrace()[0].getMethodName());

        Runtime runtime = Runtime.getRuntime();
        long usedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        log("Used Memory before:" + usedMemoryBefore);


        long start = System.currentTimeMillis();

        MongoClientSettings options = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(host + ":" + port))
                .applyToConnectionPoolSettings(builder -> builder
                        .maxConnectionLifeTime(1, TimeUnit.MINUTES)
                        .maxConnectionIdleTime(1, TimeUnit.MINUTES))
                .build();
        MongoClient client = new MongoClientImpl(options, null);

        List<GetDBThread> arr = new ArrayList<>(ARRAY_SIZE);
        for (int i = 0; i < ARRAY_SIZE; i++) {
            arr.add(new GetDBThread(client, i, "test" + i));
        }

        for (GetDBThread thread : arr) {
            thread.start();
        }

        long end = System.currentTimeMillis();
        log("Time passed for opening: " + (end - start) + " MilliSeconds");

        long usedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        log("Memory increased:" + (usedMemoryAfter - usedMemoryBefore));
        Thread.sleep(15000);
        client.close();
        long usedMemoryAfterClose = runtime.totalMemory() - runtime.freeMemory();
        log("Used Memory after closing connections: " + (usedMemoryAfterClose - usedMemoryBefore));
    }

    class GetDBThread extends Thread {

        MongoClient clientRef;
        int index;
        String dbName;

        public GetDBThread(MongoClient client, int index, String dbName) {
            clientRef = client;
            this.index = index;
            this.dbName = dbName;
        }

        public void run() {
            MongoDatabase db = clientRef.getDatabase(dbName);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            MongoCollection myCollection = db.getCollection(dbName);
            myCollection.drop();
            clientRef = null;
            System.out.println("Hello from a thread " + index);
        }
    }
}
