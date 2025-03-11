package org.qubership.cloud.quarkus.dbaas.mongoclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

public class MongoClientAggregatorTest {
    private final MongoClientAggregator aggregator = new MongoClientAggregator();

    @BeforeEach
    void prepare() {
        aggregator.helper = mock(AnnotationParsingBean.class);
        aggregator.dbaasDsMainType = "tenant";
    }

    @Test
    public void testCorrectClientIsChosen() {
        aggregator.dbaasDsMainType = "tenant";
        assertFalse(aggregator.isServiceDb());
        aggregator.dbaasDsMainType = "service";
        assertTrue(aggregator.isServiceDb());
    }
}