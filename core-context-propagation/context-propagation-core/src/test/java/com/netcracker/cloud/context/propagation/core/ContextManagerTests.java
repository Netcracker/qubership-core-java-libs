package org.qubership.cloud.context.propagation.core;

import org.junit.jupiter.api.*;
import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;
import org.qubership.cloud.context.propagation.core.providers.explicitregistration.ContextProviderWithoutAnnotation;
import org.qubership.cloud.context.propagation.core.providers.initlevels.CheckInitLevelOne;
import org.qubership.cloud.context.propagation.core.providers.initlevels.CheckInitLevelThree;
import org.qubership.cloud.context.propagation.core.providers.initlevels.CheckInitLevelTwo;
import org.qubership.cloud.context.propagation.core.providers.requestCount.RequestCountContextObject;
import org.qubership.cloud.context.propagation.core.providers.requestCount.RequestCountProvider;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionContextObject;
import org.qubership.cloud.context.propagation.core.providers.xversion.XVersionProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.qubership.cloud.context.propagation.core.ContextManager.LOOKUP_CONTEXT_PROVIDERS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextManagerTests {

    private static final Logger log = LoggerFactory.getLogger(ContextManagerTests.class);

    private static String originalRegistrationPath;

    @BeforeAll
    static void saveOriginalRegistrationPath() {
        originalRegistrationPath = System.getProperty(LOOKUP_CONTEXT_PROVIDERS_PATH);
    }

    @AfterAll
    static void restoreOriginalRegistrationPath() {
        setRegistrationPath(originalRegistrationPath);
    }

    @BeforeEach
    void setUp() throws Exception {
        setRegistrationPath("org.qubership.cloud"); // in some tests registration path is changed, so we need to set default value
        clearRegistry();
        fillRegistry();
    }

    private void fillRegistry() throws Exception {
        ContextManager.init();
    }

    private void clearRegistry() throws Exception {
        Field f = ContextManager.class.getDeclaredField("registry");
        f.setAccessible(true);
        ((Map) f.get(null)).clear();
    }

    @Test
    void testContextProviderRegistered() {
        boolean isXVersionProvider = ContextManager.getContextProviders().stream().anyMatch(contextProvider -> contextProvider instanceof XVersionProvider);
        Assertions.assertTrue(isXVersionProvider);

        boolean isRequestCountProvider = ContextManager.getContextProviders().stream().anyMatch(contextProvider -> contextProvider instanceof RequestCountProvider);
        Assertions.assertTrue(isRequestCountProvider);

    }

    @Test
    void testManualRegistration() {
        Collection<ContextProvider<?>> contextProviders = ContextManager.getContextProviders();
        boolean contextProviderWithoutAnnotationExists = contextProviders.stream()
                .anyMatch(contextProvider -> contextProvider instanceof ContextProviderWithoutAnnotation);
        Assertions.assertFalse(contextProviderWithoutAnnotationExists);
        ContextManager.register(Collections.singletonList(new ContextProviderWithoutAnnotation()));
        contextProviderWithoutAnnotationExists = ContextManager.getContextProviders().stream()
                .anyMatch(contextProvider -> contextProvider instanceof ContextProviderWithoutAnnotation);
        Assertions.assertTrue(contextProviderWithoutAnnotationExists);
    }

    private static void setRegistrationPath(String path) {
        if (path == null) {
            System.clearProperty(LOOKUP_CONTEXT_PROVIDERS_PATH);
        }
        else {
            System.setProperty(LOOKUP_CONTEXT_PROVIDERS_PATH, path);
        }
    }

    @Test
    void testGetContextValue() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        Assertions.assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    @Test
    void testGetSafe() {
        Optional<Integer> xVersional = ContextManager.getSafe(RequestCountProvider.CONTEXT_NAME);
        Assertions.assertTrue(xVersional.isPresent());
    }

    @Test
    void testCreateContextSnapshot() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        Assertions.assertEquals(3, contextSnapshot.size());
        Assertions.assertTrue(contextSnapshot.containsKey(XVersionProvider.CONTEXT_NAME));

        XVersionContextObject actual = ContextManager.get(XVersionProvider.CONTEXT_NAME);

        Assertions.assertEquals("v1", actual.getxVersion());

    }

    @Test
    void testCreateContextSnapshotWithExcludedContexts() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject());
        Set<String> excludedContextNames = Collections.singleton(RequestCountProvider.CONTEXT_NAME);
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshotWithoutContexts(excludedContextNames);

        Assertions.assertEquals(2, contextSnapshot.size());
        Assertions.assertTrue(contextSnapshot.containsKey(XVersionProvider.CONTEXT_NAME));
        Assertions.assertFalse(contextSnapshot.containsKey(RequestCountProvider.CONTEXT_NAME));
    }

    @Test
    void testCreateContextSnapshotWithEmptyExcludedContexts() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject());
        Set<String> excludedContextNames = Collections.emptySet();
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshotWithoutContexts(excludedContextNames);

        Assertions.assertEquals(3, contextSnapshot.size());
        Assertions.assertTrue(contextSnapshot.containsKey(XVersionProvider.CONTEXT_NAME));
        Assertions.assertTrue(contextSnapshot.containsKey(RequestCountProvider.CONTEXT_NAME));
    }

    @Test
    void testCreateContextSnapshotWithNullExcludedContexts() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject());
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshotWithoutContexts(null);
        Assertions.assertEquals(3, contextSnapshot.size());
        Assertions.assertTrue(contextSnapshot.containsKey(XVersionProvider.CONTEXT_NAME));
        Assertions.assertTrue(contextSnapshot.containsKey(RequestCountProvider.CONTEXT_NAME));
    }

    @Test
    void contextSnapshotExistsAfterCleanTest() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        ContextManager.clearAll();

        Assertions.assertNull(ContextManager.get(XVersionProvider.CONTEXT_NAME));
        Assertions.assertEquals(3, contextSnapshot.size());
    }

    @Test
    void activateContextSnapshotAfterClearTest() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, 1);
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        ContextManager.clearAll();

        Assertions.assertEquals(2, ContextManager.getAll().size());
        Assertions.assertNull(ContextManager.get(XVersionProvider.CONTEXT_NAME));

        ContextManager.activateContextSnapshot(contextSnapshot);

        Assertions.assertEquals(3, ContextManager.getAll().size());
        Assertions.assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    @Test
    void testActivateContextAndRewrite() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        XVersionContextObject newXVersionContextObject = new XVersionContextObject();
        newXVersionContextObject.setxVersion("v2");
        ContextManager.set(XVersionProvider.CONTEXT_NAME, newXVersionContextObject);

        Assertions.assertEquals("v2", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());

        ContextManager.activateContextSnapshot(contextSnapshot);

        Assertions.assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());

    }

    @Test
    void executeWithContextTest() {
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject((IncomingContextData) null));
        Map<String, Object> contextSnapshot = ContextManager.createContextSnapshot();

        XVersionContextObject newXVersionContextObject = new XVersionContextObject((IncomingContextData) null);
        ContextManager.set(XVersionProvider.CONTEXT_NAME, newXVersionContextObject);
        newXVersionContextObject.setxVersion("v2");
        Assertions.assertEquals("v2", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());

        ContextManager.executeWithContext(contextSnapshot, () -> {
            Assertions.assertEquals("v1", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
            return null;
        });

        Assertions.assertEquals("v2", ((XVersionContextObject) ContextManager.get(XVersionProvider.CONTEXT_NAME)).getxVersion());
    }

    @Test
    void checkInitLevelTest() throws Exception {
        List<ContextProvider<?>> contextProviders = ContextManager
                .getContextProviders()
                .stream()
                .filter(contextProvider -> contextProvider.getClass().getName().startsWith("org.qubership.cloud.context.propagation.core.providers.initlevels"))
                .collect(Collectors.toList());

        assertEquals(3, contextProviders.size());

        // The order is important, don't change
        assertEquals(CheckInitLevelThree.CONTEXT_NAME, contextProviders.get(0).contextName());
        assertEquals(CheckInitLevelTwo.CONTEXT_NAME, contextProviders.get(1).contextName());
        assertEquals(CheckInitLevelOne.CONTEXT_NAME, contextProviders.get(2).contextName());
    }

    @Test
    void testGetSerializableData() {
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject());
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());

        Map<String, Map<String, Object>> serializableContextData = ContextManager.getSerializableContextData();

        log.info("serializableContextData = " + serializableContextData);
        assertTrue(serializableContextData.containsKey(RequestCountProvider.CONTEXT_NAME));
        assertTrue(serializableContextData.containsKey(XVersionProvider.CONTEXT_NAME));
    }

    @Test
    void testGetSerializableDataWithExclude() {
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject());
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject());

        Map<String, Map<String, Object>> serializableContextData = ContextManager
                .getSerializableContextData(Collections.singleton(XVersionProvider.CONTEXT_NAME));

        log.info("serializableContextData = " + serializableContextData);
        assertTrue(serializableContextData.containsKey(RequestCountProvider.CONTEXT_NAME));
        assertFalse(serializableContextData.containsKey(XVersionProvider.CONTEXT_NAME));
    }

    @Test
    void testActivateWithSerializableData() {
        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject("1"));
        ContextManager.set(XVersionProvider.CONTEXT_NAME, new XVersionContextObject("v1"));
        assertEquals("1", ContextManager.<RequestCountContextObject>get(RequestCountProvider.CONTEXT_NAME).getxVersion());

        Map<String, Map<String, Object>> serializableContextData = ContextManager
                .getSerializableContextData(Collections.singleton(XVersionProvider.CONTEXT_NAME));

        ContextManager.set(RequestCountProvider.CONTEXT_NAME, new RequestCountContextObject("2"));
        assertEquals("2", ContextManager.<RequestCountContextObject>get(RequestCountProvider.CONTEXT_NAME).getxVersion());

        ContextManager.activateWithSerializableContextData(serializableContextData);
        assertEquals("1", ContextManager.<RequestCountContextObject>get(RequestCountProvider.CONTEXT_NAME).getxVersion());
        assertNull(ContextManager.get(XVersionProvider.CONTEXT_NAME));
    }
}