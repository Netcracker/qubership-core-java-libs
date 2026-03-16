package com.netcracker.cloud.framework.contexts.acceptlanuages;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageContextObject;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;
import com.netcracker.cloud.framework.contexts.data.ContextDataRequest;
import com.netcracker.cloud.framework.contexts.data.ContextDataResponse;
import com.netcracker.cloud.framework.contexts.data.SimpleIncomingContextData;
import com.netcracker.cloud.framework.contexts.helper.AbstractContextTestWithProperties;
import com.netcracker.cloud.headerstracking.filters.context.AcceptLanguageContext;

import java.util.Map;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static com.netcracker.cloud.framework.contexts.data.ContextDataRequest.CUSTOM_HEADER;

class AcceptLanguageContextObjectPropagationTest extends AbstractContextTestWithProperties {

    private final String TEST_LANGUAGES = "en; ru;";

    static Map<String, String> properties = Map.of("headers.allowed", CUSTOM_HEADER);

    @BeforeAll
    protected static void setup() {
        AbstractContextTestWithProperties.parentSetup(properties);
    }

    @AfterAll
    protected static void cleanup() {
        AbstractContextTestWithProperties.parentCleanup(properties);
    }

    @Test
    void testAcceptLanguageContextPropagation() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertNotNull(ContextManager.get(ACCEPT_LANGUAGE));
        AcceptLanguageContextObject acceptLanguageContextObject = ContextManager.get(ACCEPT_LANGUAGE);
        Assertions.assertEquals(TEST_LANGUAGES, acceptLanguageContextObject.getAcceptedLanguages());
        ContextDataResponse responseContextData = new ContextDataResponse();
        RequestContextPropagation.populateResponse(responseContextData);
        Assertions.assertEquals(TEST_LANGUAGES, responseContextData.getResponseHeaders().get(ACCEPT_LANGUAGE));
        ContextManager.clear(ACCEPT_LANGUAGE);
        Assertions.assertNotNull(ContextManager.get(ACCEPT_LANGUAGE));
    }

    @Test
    void testAcceptLanguageContextWrapper() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());
        Assertions.assertEquals(TEST_LANGUAGES, AcceptLanguageContext.get());

        AcceptLanguageContext.set("fr-Bel;");
        Assertions.assertEquals("fr-Bel;", AcceptLanguageContext.get());

        AcceptLanguageContext.clear();
        Assertions.assertNull(AcceptLanguageContext.get());
    }

    @Test
    void testAcceptLanguageSerializableDataFromCxtManager() {
        RequestContextPropagation.initRequestContext(new ContextDataRequest());

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        Assertions.assertTrue(serializableContextData.containsKey(AcceptLanguageProvider.ACCEPT_LANGUAGE));
    }

    @Test
    void testAcceptLanguageSerializableData() {
        SimpleIncomingContextData contextData = new SimpleIncomingContextData(Map.of(ACCEPT_LANGUAGE, "en; ru;"));
        AcceptLanguageContextObject acceptLanguageContextObject = new AcceptLanguageContextObject(contextData);

        Map<String, Object> serializableContextData = acceptLanguageContextObject.getSerializableContextData();

        Assertions.assertEquals(1, serializableContextData.size());
        Assertions.assertEquals("en; ru;", serializableContextData.get(ACCEPT_LANGUAGE));

        AcceptLanguageContextObject acceptLanguageContextObject2 = new AcceptLanguageContextObject(new SimpleIncomingContextData());
        Assertions.assertEquals(0, acceptLanguageContextObject2.getSerializableContextData().size());
    }
}
