package org.qubership.cloud.headerstracking.filters.context;

import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.xversionname.XVersionNameContextObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.qubership.cloud.framework.contexts.xversionname.XVersionNameProvider.CONTEXT_NAME;

class XVersionNameContextTest extends AbstractContextTest {

    @Test
    void testRequestWithEmptyHeader() {
        assertNull(XVersionNameContext.get());
        XVersionNameContext.set("   ");
        assertNull(XVersionNameContext.get());
    }

    @Test
    void testRequestWithHeader() {
        assertNull(XVersionNameContext.get());
        XVersionNameContext.set("candidate");
        assertEquals("candidate", XVersionNameContext.get());
    }

    @Test
    void testClearContext() {
        ContextManager.set(CONTEXT_NAME, new XVersionNameContextObject("legacy"));
        assertEquals("legacy", XVersionNameContext.get());
        XVersionNameContext.clear();
        assertNull(XVersionNameContext.get());
    }
}
