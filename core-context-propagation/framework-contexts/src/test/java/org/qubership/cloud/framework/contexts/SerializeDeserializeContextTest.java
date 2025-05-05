package org.qubership.cloud.framework.contexts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.context.propagation.core.contextdata.DeserializedIncomingContextData;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestContextObject;
import org.qubership.cloud.context.propagation.core.contexts.common.RequestProvider;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import org.qubership.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersContextObject;
import org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider;
import org.qubership.cloud.framework.contexts.apiversion.ApiVersionContextObject;
import org.qubership.cloud.framework.contexts.apiversion.ApiVersionProvider;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessContextObject;
import org.qubership.cloud.framework.contexts.businessprocess.BusinessProcessProvider;
import org.qubership.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdContextObject;
import org.qubership.cloud.framework.contexts.originatingbiid.OriginatingBiIdProvider;
import org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObject;
import org.qubership.cloud.framework.contexts.xversion.XVersionContextObjectTest;
import org.qubership.cloud.framework.contexts.xversion.XVersionProvider;

import java.util.*;

import static org.qubership.cloud.framework.contexts.allowedheaders.AllowedHeadersProvider.HEADERS_PROPERTY;
import static org.qubership.cloud.framework.contexts.xrequestid.XRequestIdContextObject.X_REQUEST_ID;
import static org.qubership.cloud.framework.contexts.xversion.XVersionContextObject.X_VERSION_SERIALIZATION_NAME;

@Slf4j
public class SerializeDeserializeContextTest extends AbstractContextTestWithProperties {

    static Map<String, String> properties = Map.of(HEADERS_PROPERTY, "allowed-header-1");

    @BeforeAll
    protected static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    protected static void cleanup() {
        AbstractContextTestWithProperties.parentCleanup(properties);
    }

    @Test
    public void example() throws JsonProcessingException {
        Map<String, Map<String, Object>> contextSnapshot = ContextManager.getSerializableContextData();

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(contextSnapshot);

//        Business logic

        Map<String, Map<String, Object>> deserialized = objectMapper.readValue(serialized, new TypeReference<>() {
        });

        ContextManager.activateWithSerializableContextData(deserialized);

    }

    @Test
    public void checkSerializableDeserializable() throws JsonProcessingException {
        fillXVersionContext("2");
        Assertions.assertEquals("2",
                ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getXVersion());

        Map<String, Map<String, Object>> contextSnapshot = ContextManager.getSerializableContextData();

        log.info("snapshot: {} ", contextSnapshot);

        ObjectMapper objectMapper = new ObjectMapper();

        String serialized = objectMapper.writeValueAsString(contextSnapshot);
        log.info("serialized: {}", serialized);
//
        fillXVersionContext("3");
        Assertions.assertEquals("3",
                ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getXVersion());

        Map<String, Map<String, Object>> deserialized = objectMapper.readValue(serialized, new TypeReference<>() {
        });
        log.info("deserialized: {}", deserialized);
//
        ContextManager.activateWithSerializableContextData(deserialized); // takes context snapshot. By name and context object restores context.
        Assertions.assertEquals("2",
                ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getXVersion());
    }

    @Test
    public void fullTest() throws JsonProcessingException {
        ContextValuesStorage expectedValues = ContextValuesStorage.builder()
                .allowedHeaders(Collections.singletonMap("allowed-header-1", "value-of-allowed-header-1"))
                .acceptedLanguages("ru")
                .businessProcessId(UUID.randomUUID().toString())
//                .requestHeaders(Collections.singletonMap("request-header-1", Collections.singletonList("value-of-request-header-1")))
                .originatingBiId(UUID.randomUUID().toString())
                .requestId(UUID.randomUUID().toString())
                .apiVersion("v3")
                .xversion("2")
                .build();
        fillContexts(expectedValues);
        checkContextState(expectedValues);

        Map<String, Map<String, Object>> contextSnapshot = ContextManager.getSerializableContextData();
        log.info("snapshot: {} ", contextSnapshot);

        ObjectMapper objectMapper = new ObjectMapper();
        String serialized = objectMapper.writeValueAsString(contextSnapshot);
        log.info("serialized: {}", serialized);

        ContextValuesStorage changedCtxValues = ContextValuesStorage.builder()
                .allowedHeaders(Collections.singletonMap("allowed-header-2", "value-of-allowed-header-2"))
                .acceptedLanguages("en")
                .businessProcessId(UUID.randomUUID().toString())
                .xversion("3")
//                .requestHeaders(Collections.singletonMap("request-header-2", Collections.singletonList("value-of-request-header-2")))
                .originatingBiId(UUID.randomUUID().toString())
                .requestId(UUID.randomUUID().toString())
                .apiVersion("v4")
                .build();
        fillContexts(changedCtxValues);
        checkContextState(changedCtxValues);

        Map<String, Map<String, Object>> deserialized = objectMapper.readValue(serialized, new TypeReference<>() {
        });
        log.info("deserialized: {}", deserialized);

        ContextManager.activateWithSerializableContextData(deserialized); // takes context snapshot. By name and context object restores context.

        Assertions.assertEquals("2",
                ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getXVersion());
        checkContextState(expectedValues);
    }

    private void fillXVersionContext(String xVersion) {
        IncomingContextData incomingCtx = new XVersionContextObjectTest.IncomingContextDataImpl(X_VERSION_SERIALIZATION_NAME, xVersion);
        XVersionContextObject xVersionContextObject = new XVersionContextObject(incomingCtx);
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);
    }

    private void checkContextState(ContextValuesStorage expectedValues) {
//        Assertions.assertEquals(expectedValues.getRequestHeaders(),
//                ContextManager.<RequestContextObject>get(RequestProvider.REQUEST_CONTEXT_NAME).getHttpHeaders());

        Assertions.assertEquals(expectedValues.getOriginatingBiId(),
                ContextManager.<OriginatingBiIdContextObject>get(OriginatingBiIdProvider.CONTEXT_NAME).getOriginatingBiId());

        Assertions.assertEquals(expectedValues.getApiVersion(),
                ContextManager.<ApiVersionContextObject>get(ApiVersionProvider.API_VERSION_CONTEXT_NAME).getVersion());

        Assertions.assertEquals(expectedValues.getBusinessProcessId(),
                ContextManager.<BusinessProcessContextObject>get(BusinessProcessProvider.CONTEXT_NAME).getBusinessProcessId());

        Assertions.assertEquals(expectedValues.getAcceptedLanguages(),
                ContextManager.<AcceptLanguageContextObject>get(AcceptLanguageProvider.ACCEPT_LANGUAGE).getAcceptedLanguages());

        Assertions.assertEquals(expectedValues.getXversion(),
                ContextManager.<XVersionContextObject>get(XVersionProvider.CONTEXT_NAME).getXVersion());

        Assertions.assertEquals(expectedValues.getRequestId(),
                ContextManager.<XRequestIdContextObject>get(X_REQUEST_ID).getRequestId());

        Assertions.assertEquals(expectedValues.getAllowedHeaders(),
                ContextManager.<AllowedHeadersContextObject>get(AllowedHeadersProvider.ALLOWED_HEADER).getHeaders());

    }

    private void fillContexts(ContextValuesStorage valuesStorage) {
        RequestContextObject requestContextObject = new RequestContextObject(valuesStorage.getRequestHeaders());
        ContextManager.set(RequestProvider.REQUEST_CONTEXT_NAME, requestContextObject);

        OriginatingBiIdContextObject originatingBiIdContextObject = new OriginatingBiIdContextObject(valuesStorage.getOriginatingBiId());
        ContextManager.set(OriginatingBiIdProvider.CONTEXT_NAME, originatingBiIdContextObject);

        ApiVersionContextObject apiVersionContextObject = new ApiVersionContextObject(valuesStorage.getApiVersion());
        ContextManager.set(ApiVersionProvider.API_VERSION_CONTEXT_NAME, apiVersionContextObject);

        BusinessProcessContextObject businessProcessContextObject = new BusinessProcessContextObject(valuesStorage.getBusinessProcessId());
        ContextManager.set(BusinessProcessProvider.CONTEXT_NAME, businessProcessContextObject);

        AcceptLanguageContextObject acceptLanguageContextObject = new AcceptLanguageContextObject(valuesStorage.getAcceptedLanguages());
        ContextManager.set(AcceptLanguageProvider.ACCEPT_LANGUAGE, acceptLanguageContextObject);

        final IncomingContextData incomingCtx = new XVersionContextObjectTest.IncomingContextDataImpl(X_VERSION_SERIALIZATION_NAME, valuesStorage.getXversion());
        XVersionContextObject xVersionContextObject = new XVersionContextObject(incomingCtx);
        ContextManager.set(XVersionProvider.CONTEXT_NAME, xVersionContextObject);

        XRequestIdContextObject xRequestIdContextObject = new XRequestIdContextObject(valuesStorage.getRequestId());
        ContextManager.set(X_REQUEST_ID, xRequestIdContextObject);

        AllowedHeadersContextObject allowedHeadersContextObject = new AllowedHeadersContextObject(
                new DeserializedIncomingContextData(valuesStorage.getAllowedHeaders()),
                new ArrayList<>(valuesStorage.getAllowedHeaders().keySet()));
        ContextManager.set(AllowedHeadersProvider.ALLOWED_HEADER, allowedHeadersContextObject);
    }

    @Builder
    @Getter
    private static class ContextValuesStorage {
        private Map<String, List<String>> requestHeaders;
        private String originatingBiId;
        private String apiVersion;
        private String businessProcessId;
        private String acceptedLanguages;
        private String xversion;
        private Map<String, Object> allowedHeaders;
        private String requestId;
    }
}