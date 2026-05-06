package com.netcracker.cloud.context.propagation.spring.common.filter;

import java.util.Enumeration;
import java.util.Iterator;

public class EnumerationImpl<T> implements Enumeration<T> {
    private final Iterator<T> iterator;

    public EnumerationImpl(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasMoreElements() {
        return this.iterator.hasNext();
    }

    @Override
    public T nextElement() {
        return this.iterator.next();
    }
}
