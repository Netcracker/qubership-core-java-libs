package org.qubership.cloud.headerstracking.filters.context;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.xversionname.XVersionNameContextObject;
import org.junit.Test;
import org.qubership.cloud.headerstracking.filters.context.XVersionNameContext;

import static org.qubership.cloud.framework.contexts.xversionname.XVersionNameProvider.CONTEXT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class XVersionNameContextTest extends AbstractContextTest {

    @Test
    public void testRequestWithEmptyHeader() {
        assertNull(XVersionNameContext.get());
        XVersionNameContext.set("   ");
        assertNull(XVersionNameContext.get());
    }

    @Test
    public void testRequestWithHeader() {
        assertNull(XVersionNameContext.get());
        XVersionNameContext.set("candidate");
        assertEquals("candidate", XVersionNameContext.get());
    }

    @Test
    public void testClearContext() {
        ContextManager.set(CONTEXT_NAME, new XVersionNameContextObject("legacy"));
        assertEquals("legacy", XVersionNameContext.get());
        XVersionNameContext.clear();
        assertNull(XVersionNameContext.get());
    }
}
