package com.netcracker.cloud.context.propagation.quarkus.runtime.microprofile.context;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextObject;
import com.netcracker.cloud.framework.contexts.xversion.XVersionContextObject;
import com.netcracker.cloud.headerstracking.filters.context.RequestIdContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class CoreThreadContextProviderTest {
    private static final Map<String, Object> contextData = Map.of(
            XRequestIdContextObject.X_REQUEST_ID, "x-request-id-test-val",
            XVersionContextObject.X_VERSION_SERIALIZATION_NAME, "x-version-test-val"
    );

    @BeforeEach
    public void init() {
        RequestContextPropagation.initRequestContext(new RequestContextData(contextData));
    }

    @Test
    public void testContextSwitchInSameThread() {
        RequestIdContextSwitcher contextSwitcher = new RequestIdContextSwitcher();
        contextSwitcher.contextProvider = new CoreThreadContextProvider();
        contextSwitcher.newRequestId = "request-id-val-after-context-switch";

        contextSwitcher.run();

        Assertions.assertNotEquals(extractRequestId(contextSwitcher.snapshotAfterContextSwitch), extractRequestId(contextSwitcher.snapshotBeforeContextSwitch));
        Assertions.assertEquals(extractRequestId(contextSwitcher.restoredContextSnapshot), extractRequestId(contextSwitcher.snapshotBeforeContextSwitch));
    }

    private String extractRequestId(Map<String, Object> contextSnapshot) {
        return ((XRequestIdContextObject) contextSnapshot.get(XRequestIdContextObject.X_REQUEST_ID)).getRequestId();
    }

    static class RequestIdContextSwitcher implements Runnable {
        String newRequestId;
        ThreadContextProvider contextProvider;

        Map<String, Object> snapshotBeforeContextSwitch;
        Map<String, Object> snapshotAfterContextSwitch;
        Map<String, Object> restoredContextSnapshot;

        @Override
        public void run() {
            snapshotBeforeContextSwitch = ContextManager.createContextSnapshot();
            ThreadContextController contextController = contextProvider.currentContext(Collections.emptyMap()).begin();
            RequestIdContext.set(newRequestId);
            snapshotAfterContextSwitch = ContextManager.createContextSnapshot();
            contextController.endContext();
            restoredContextSnapshot = ContextManager.createContextSnapshot();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RequestIdContextSwitcher that = (RequestIdContextSwitcher) o;
            return Objects.equals(snapshotBeforeContextSwitch, that.snapshotBeforeContextSwitch) && Objects.equals(snapshotAfterContextSwitch, that.snapshotAfterContextSwitch) && Objects.equals(restoredContextSnapshot, that.restoredContextSnapshot);
        }

        @Override
        public int hashCode() {
            return Objects.hash(snapshotBeforeContextSwitch, snapshotAfterContextSwitch, restoredContextSnapshot);
        }
    }
}
