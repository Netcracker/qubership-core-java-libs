package org.qubership.cloud.framework.contexts.businessprocess;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.framework.contexts.data.ContextDataRequest;
import org.qubership.cloud.framework.contexts.data.ContextDataResponse;
import org.qubership.cloud.framework.contexts.data.SimpleIncomingContextData;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.headerstracking.filters.context.BusinessProcessIdContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessContextObject.BUSINESS_PROCESS_ID_SERIALIZATION_NAME;

public class BusinessProcessContextObjectTest extends AbstractContextTestWithProperties {

    @Test
    public void isBusinessProcessContextObjectSerializedIfSet() {
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
    public void isBusinessProcessContextObjectSerializedIfNotSet() {
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(
                new ContextDataRequest()
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        businessProcessContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    public void isBusinessProcessContextObjectSerializedIfHeaderEmpty() {
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(
                new ContextDataRequest(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "")
        );
        ContextDataResponse outgoingContextData = new ContextDataResponse();
        businessProcessContextObject.serialize(outgoingContextData);
        Object o = outgoingContextData.getResponseHeaders().get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME);
        assertNull(o);
    }

    @Test
    public void getDefaultValue() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        assertNotNull(ContextManager.get(BusinessProcessProvider.CONTEXT_NAME));
        String actualBusinessProcessId = BusinessProcessIdContext.get();
        assertNull(actualBusinessProcessId);
    }

    @Test
    public void getValueFromRequest() {
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
    public void testBusinessProcessIdPropagationIfProcessIdIsNotSet() {
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
    public void testBusinessProcessIdPropagationIfProcessIdIsSetDuringExecution() {
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
    public void testBusinessProcessIdPropagationIfHeaderIsEmpty() {
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
    public void testBusinessProcessIdPropagation() {
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
    public void testBusinessProcessIdPropagationWithResponsePropagatableDataIfProcessIdIsNotSet() {
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
    public void testBusinessProcessIdPropagationWithResponsePropagatableDataIfProcessIdSetDuringExecution() {
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
    public void testBusinessProcessIdPropagationWithResponsePropagatableDataIfHeaderIsEmpty() {
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
    public void testBusinessProcessIdPropagationWithResponsePropagatableData() {
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
    public void testBusinessProcessSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new SimpleIncomingContextData(Map.of(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "12345")));

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        assertTrue(serializableContextData.containsKey(BusinessProcessProvider.CONTEXT_NAME));
    }

    @Test
    public void testBusinessProcessSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(BUSINESS_PROCESS_ID_SERIALIZATION_NAME, "12345"));
        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(contextData);

        Map<String, Object> serializableContextData = businessProcessContextObject.getSerializableContextData();

        assertEquals(1, serializableContextData.size());
        assertEquals("12345", serializableContextData.get(BUSINESS_PROCESS_ID_SERIALIZATION_NAME));

        BusinessProcessContextObject businessProcessContextObject1 = new BusinessProcessContextObject(new SimpleIncomingContextData());
        assertEquals(0, businessProcessContextObject1.getSerializableContextData().size());
    }
}
