package com.netcracker.cloud.context.propagation.core.supports.strategies;

import java.util.ArrayList;
import java.util.List;

final class ThreadLocalWithStack<T> {
    private ThreadLocal<List<T>> localStack = new ThreadLocal<>();

    void pushElement(T obj) {
        getStack(true).add(obj);
    }

    private List<T> getStack(boolean create) {
        List<T> stack = localStack.get();
        if (stack == null && create) {
            stack = new ArrayList<T>();
            localStack.set(stack);
        }
        return stack;
    }

    T getLastElement() {
        List<T> stack = localStack.get();
        if (stack == null || stack.isEmpty())
            return null;
        return stack.get(stack.size() - 1);
    }

    T pullElement() {
        List<T> stack = localStack.get();
        if (stack == null || stack.isEmpty())
            return null;
        return stack.remove(stack.size() - 1);
    }

    void setLastElement(T obj) {
        List<T> stack = getStack(true);
        int size = stack.size();
        if (size == 0) {
            stack.add(obj);
        } else {
            stack.set(size - 1, obj);
        }
    }

    int size() {
        List<T> stack = getStack(false);
        return stack == null ? 0 : stack.size();
    }

    void clear() {
        localStack.set(null);
    }

    T getElement(int i) {
        List<T> stack = getStack(false);
        if (stack == null) {
            return null;
        }
        if (i < 0 || i > size()) {
            return null;
        }
        return stack.get(i);
    }
}
