package com.netcracker.cloud.framework.contexts.businessprocess;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.data.ContextDataResponse;
import com.netcracker.cloud.framework.contexts.data.SimpleIncomingContextData;
import com.netcracker.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import com.netcracker.cloud.headerstracking.filters.context.BusinessProcessIdContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static com.netcracker.cloud.framework.contexts.businessprocess.BusinessProcessContextObject.BUSINESS_PROCESS_ID_SERIALIZATION_NAME;

class BusinessProcessContextObjectTest extends AbstractContextTestWithProperties {

    @Test
    void isBusinessProcessContextObjectSerializedIfSet() {
        String testUUID = UUID.randomUUID().toString();
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, testUUID)
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        businessProcessContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        assertNotNull(o);
        assertInstanceOf(String.class, o);
        assertEquals(testUUID, o);
    }

    @Test
    void isBusinessProcessContextObjectSerializedIfNotSet() {
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(
                new ContextDataRequest()
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        businessProcessContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    void isBusinessProcessContextObjectSerializedIfHeaderEmpty() {
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "")
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        businessProcessContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
    }

    @Test
    void getValueFromRequest() {
        String uuid = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, uuid)
        );
        Assertions.assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNotNull(actualBusinessProcessId);
        assertEquals(uuid, actualBusinessProcessId);
    }

    @Test
    void testBusinessProcessIdPropagationIfProcessIdIsNotSet() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertNull(responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationIfProcessIdIsSetDuringExecution() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        String testUUID = UUID.randomUUID().toString();
        BusinessProcessIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationIfHeaderIsEmpty() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "")
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        String testUUID = UUID.randomUUID().toString();
        BusinessProcessIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagation() {
        String expectedUUID = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, expectedUUID)
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNotNull(actualBusinessProcessId);
        assertEquals(expectedUUID, actualBusinessProcessId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        assertEquals(expectedUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationWithResponsePropagatableDataIfProcessIdIsNotSet() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertNull(responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationWithResponsePropagatableDataIfProcessIdSetDuringExecution() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest()
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        String testUUID = UUID.randomUUID().toString();
        BusinessProcessIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationWithResponsePropagatableDataIfHeaderIsEmpty() {
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "")
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
        String testUUID = UUID.randomUUID().toString();
        BusinessProcessIdContext.set(testUUID);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(testUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessIdPropagationWithResponsePropagatableData() {
        String expectedUUID = UUID.randomUUID().toString();
        RequestContextPropagation.initRequestContext(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, expectedUUID)
        );
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNotNull(actualBusinessProcessId);
        assertEquals(expectedUUID, actualBusinessProcessId);
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.setResponsePropagatableData(responseContextData);
        assertEquals(expectedUUID, responseContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));
    }

    @Test
    void testBusinessProcessSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        assertTrue(serializableContextData.containsKey(BusinessProcessProvider.CONTEXT_NAME));
    }

    @Test
    void testBusinessProcessSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "12345"));
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(contextData);

        Map<String, Object> serializableContextData = businessProcessContextObject.getSerializableContextData();

        assertEquals(1, serializableContextData.size());
        assertEquals("12345", serializableContextData.get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));

        BusinessProcessContextObject businessProcessContextObject1 = new BusinessProcessContextObject(new SimpleIncomingContextData());
        assertEquals(0, businessProcessContextObject1.getSerializableContextData().size());
    }
}
