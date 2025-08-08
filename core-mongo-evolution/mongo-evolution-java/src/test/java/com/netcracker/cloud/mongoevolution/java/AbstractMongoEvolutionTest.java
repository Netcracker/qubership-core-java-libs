package com.netcracker.cloud.mongoevolution.java;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractMongoEvolutionTest {
    private static final MongoClient mockClient = Mockito.mock(MongoClient.class);
    private static final MongoDatabase mockDatabase = Mockito.mock(MongoDatabase.class);
    private static final ConnectionSearchKey mockConnectionSearchKey = Mockito.mock(ConnectionSearchKey.class);

    @BeforeEach
    void setUp() {
        Mockito.when(mockClient.getDatabase("testdb")).thenReturn(mockDatabase);
    }

    @Test
    void testDefaultConstructorValues() {
        AbstractMongoEvolution evolution = new AbstractMongoEvolution(mockClient, "testdb", mockConnectionSearchKey);

        assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, evolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), evolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), evolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), evolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    void testCustomConstructorValues() {
        long customWaitTimeSecondsForUpdate = 45;
        long customWaitTimeMillisecWithinUpdate = 15;
        long customWaitTimeForUpdateStatusTask = 60000;
        long customDelayStatusTask = 45;

        AbstractMongoEvolution evolution = new AbstractMongoEvolution(
                mockClient,
                "testdb",
                mockConnectionSearchKey,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        assertEquals(customWaitTimeSecondsForUpdate, evolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeMillisecWithinUpdate), evolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeForUpdateStatusTask), evolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(customDelayStatusTask), evolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    void testCustomConstructorWithNegativeValues() {
        long customWaitTimeSecondsForUpdate = -45;
        long customWaitTimeMillisecWithinUpdate = -15;
        long customWaitTimeForUpdateStatusTask = -60000;
        long customDelayStatusTask = -45;

        AbstractMongoEvolution evolution = new AbstractMongoEvolution(
                mockClient,
                "testdb",
                mockConnectionSearchKey,
                customWaitTimeSecondsForUpdate,
                customWaitTimeMillisecWithinUpdate,
                customWaitTimeForUpdateStatusTask,
                customDelayStatusTask
        );

        assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, evolution.getWaitTimeSecondsForUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), evolution.getWaitTimeMillisecWithinUpdate());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), evolution.getWaitTimeMillisecForUpdateStatusTask());
        assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), evolution.getDelayTimeMillisecStatusTask());
    }
}
