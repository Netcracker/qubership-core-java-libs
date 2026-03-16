package com.netcracker.cloud.maas.client.impl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
	private final Supplier<T> supplier;
	// use dedicated flag to indicate once completed computation, because supplier#get() can return null value as a valid result
	// and we can't rely on value == null clause to determine state
	private volatile boolean initialized = false;
	private volatile T value = null;

	public Lazy(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T get() {
		if (supplier == null) {
			throw new IllegalArgumentException("Supplier for lazy value is set to null but get() value is called anyway. Check your application logic or provide valid supplier");
		}

		if (!initialized) {
			synchronized (this) {
				if (!initialized) {
					value = supplier.get();
					initialized = true;
				}
			}
		}
		return value;
	}

	public <R> Optional<R> map(Function<T, R> f) {
		if (initialized) {
			return Optional.ofNullable(f.apply(value));
		} else {
			return Optional.empty();
		}
	}
}
