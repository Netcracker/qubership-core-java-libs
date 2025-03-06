package org.qubership.cloud.context.propagation.core.supports.strategies;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ThreadLocalWithStackTest {

    private ThreadLocalWithStack<Integer> threadLocalStack;

    @BeforeEach
    public void setUp() {
        threadLocalStack = new ThreadLocalWithStack<>();
    }

    @Test
    public void testPushElement() {
        threadLocalStack.pushElement(5);
        assertEquals(1, threadLocalStack.size());
    }

    @Test
    public void testGetLastElement() {
        assertNull(threadLocalStack.getLastElement());
        threadLocalStack.pushElement(10);
        assertEquals(10, threadLocalStack.getLastElement());
    }

    @Test
    public void testPullElement() {
        assertNull(threadLocalStack.pullElement());
        threadLocalStack.pushElement(15);
        assertEquals(15, threadLocalStack.pullElement());
        assertEquals(0, threadLocalStack.size());
    }

    @Test
    public void testSetLastElement() {
        threadLocalStack.pushElement(20);
        threadLocalStack.setLastElement(25);
        assertEquals(25, threadLocalStack.getLastElement());
    }

    @Test
    public void testSize() {
        assertEquals(0, threadLocalStack.size());
        threadLocalStack.pushElement(30);
        assertEquals(1, threadLocalStack.size());
    }

    @Test
    public void testClear() {
        threadLocalStack.pushElement(35);
        threadLocalStack.clear();
        assertEquals(0, threadLocalStack.size());
    }

    @Test
    public void testGetLastElementAtIndex() {
        assertNull(threadLocalStack.getElement(0));
        threadLocalStack.pushElement(40);
        threadLocalStack.pushElement(45);
        assertEquals(40, threadLocalStack.getElement(0));
        assertEquals(45, threadLocalStack.getElement(1));
    }
}
