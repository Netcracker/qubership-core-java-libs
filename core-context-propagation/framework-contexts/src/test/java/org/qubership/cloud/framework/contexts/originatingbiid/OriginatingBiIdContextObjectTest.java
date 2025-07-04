package org.qubership.cloud.framework.contexts.originatingbiid;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.qubership.cloud.framework.contexts.data.ContextDataResponse;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.headerstracking.filters.context.OriginatingBiIdContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject.ORIGINATING_BI_ID_SERIALIZATION_NAME;

class OriginatingBiIdContextObjectTest extends AbstractContextTestWithProperties {

    @Test
    void isOriginatingBiIdContextObjectSerializedIfSet() {
        String testUUID = UUID.randomUUID().toString();
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, testUUID)
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        originatingBiIdContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME);
        assertNotNull(o);
        assertInstanceOf(String.class, o);
        assertEquals(testUUID, o);
    }

    @Test
    void isOriginatingBiIdContextObjectSerializedIfNotSet() {
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(
                new ContextDataRequest()
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        originatingBiIdContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    void isOriginatingBiIdContextObjectSerializedIfHeaderEmpty() {
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, "")
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        originatingBiIdContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME);
        assertEquals("", o);
    }

    @Test
    void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNull(actualOriginatingBiId);
    }

    @Test
    void getValueFromRequest() {
        String uuid = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, uuid)
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNotNull(actualOriginatingBiId);
        assertEquals(uuid, actualOriginatingBiId);
    }

    @Test
    void testOriginatingBiIdPropagationIfProcessIdIsNotSet() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNull(actualOriginatingBiId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertNull(responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationIfProcessIdIsSetDuringExecution() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNull(actualOriginatingBiId);
        String testUUID = UUID.randomUUID().toString();
        OriginatingBiIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationIfHeaderIsEmpty() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, "")
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertEquals("", actualOriginatingBiId);
        String testUUID = UUID.randomUUID().toString();
        OriginatingBiIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagation() {
        String expectedUUID = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, expectedUUID)
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNotNull(actualOriginatingBiId);
        assertEquals(expectedUUID, actualOriginatingBiId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(expectedUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationWithResponsePropagatableDataIfProcessIdIsNotSet() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNull(actualOriginatingBiId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertNull(responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationWithResponsePropagatableDataIfProcessIdSetDuringExecution() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNull(actualOriginatingBiId);
        String testUUID = UUID.randomUUID().toString();
        OriginatingBiIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationWithResponsePropagatableDataIfHeaderIsEmpty() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, "")
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertEquals("", actualOriginatingBiId);
        String testUUID = UUID.randomUUID().toString();
        OriginatingBiIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    void testOriginatingBiIdPropagationWithResponsePropagatableData() {
        String expectedUUID = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(ORIGINATING_BI_ID_SERIALIZATION_NAME, expectedUUID)
        );
        assertNotNull(ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME));
        String actualOriginatingBiId = OriginatingBiIdContext.get();
        assertNotNull(actualOriginatingBiId);
        assertEquals(expectedUUID, actualOriginatingBiId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(expectedUUID, responseContextData.getResponseHeaders().get(ORIGINATING_BI_ID_SERIALIZATION_NAME));
    }

    @Test
    public void testOriginatingBiSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(ORIGINATING_BI_ID_SERIALIZATION_NAME, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        assertTrue(serializableContextData.containsKey(OriginatingBiIdProvider.CONTEXT_NAME));
    }

    @Test
    public void testOriginatingBiSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(ORIGINATING_BI_ID_SERIALIZATION_NAME, "12345"));
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(contextData);

        Map<String, Object> serializableContextData = originatingBiIdContextObject.getSerializableContextData();

        assertEquals(1, serializableContextData.size());
        assertEquals("12345", serializableContextData.get(ORIGINATING_BI_ID_SERIALIZATION_NAME));

        OriginatingBiIdContextObject originatingBiIdContextObject1 = new OriginatingBiIdContextObject(new SimpleIncomingContextData());
        assertEquals(0, originatingBiIdContextObject1.getSerializableContextData().size());
    }

    @Test
    void originatingBiIdContextShouldFillMdc() {
        String expectedId = UUID.randomUUID().toString();
        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(expectedId);

        ContextManager.set(OriginatingBiIdProvider.CONTEXT_NAME, originatingBiIdContextObject);
        OriginatingBiIdContextObject actualObject = ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME);
        assertNotNull(actualObject);
        assertEquals(expectedId, actualObject.getOriginatingBiId());
        assertEquals(expectedId, MDC.get("originating_bi_id"));

        ContextManager.clear(OriginatingBiIdProvider.CONTEXT_NAME);
        actualObject = ContextManager.get(OriginatingBiIdProvider.CONTEXT_NAME);
        assertNotNull(actualObject);
        Assertions.assertNull(actualObject.getOriginatingBiId());
        Assertions.assertNull(MDC.get("originating_bi_id"));
    }
}