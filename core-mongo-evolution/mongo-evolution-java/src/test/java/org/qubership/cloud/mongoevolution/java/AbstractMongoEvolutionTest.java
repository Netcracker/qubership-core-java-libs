package org.qubership.cloud.mongoevolution.java;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.qubership.cloud.mongoevolution.java.dataaccess.ConnectionSearchKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

public class AbstractMongoEvolutionTest {
    private static final MongoClient mockClient = Mockito.mock(MongoClient.class);
    private static final MongoDatabase mockDatabase = Mockito.mock(MongoDatabase.class);
    private static final ConnectionSearchKey mockConnectionSearchKey = Mockito.mock(ConnectionSearchKey.class);

    @Before
    public void setUp() {
        Mockito.when(mockClient.getDatabase("testdb")).thenReturn(mockDatabase);
    }

    @Test
    public void testDefaultConstructorValues() {
        AbstractMongoEvolution evolution = new AbstractMongoEvolution(mockClient, "testdb", mockConnectionSearchKey);

        Assert.assertEquals(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, evolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), evolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), evolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(AbstractMongoEvolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), evolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    public void testCustomConstructorValues() {
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

        Assert.assertEquals(customWaitTimeSecondsForUpdate, evolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeMillisecWithinUpdate), evolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customWaitTimeForUpdateStatusTask), evolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(customDelayStatusTask), evolution.getDelayTimeMillisecStatusTask());
    }

    @Test
    public void testCustomConstructorWithNegativeValues() {
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

        Assert.assertEquals(evolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE, evolution.getWaitTimeSecondsForUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(evolution.DEFAULT_WAIT_TIME_SECONDS_WITHIN_UPDATE), evolution.getWaitTimeMillisecWithinUpdate());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(evolution.DEFAULT_WAIT_TIME_SECONDS_FOR_UPDATE_STATUS_TASK), evolution.getWaitTimeMillisecForUpdateStatusTask());
        Assert.assertEquals(TimeUnit.SECONDS.toMillis(evolution.DEFAULT_DELAY_TIME_SECONDS_STATUS_TASK), evolution.getDelayTimeMillisecStatusTask());
    }
}
