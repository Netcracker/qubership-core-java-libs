package com.netcracker.cloud.maas.bluegreen.versiontracker.impl;

import java.util.Objects;
import java.util.function.Predicate;

public class PrintablePredicate<T> implements Predicate<T> {
	private final Predicate<T> predicate;
	private final String representation;

	public PrintablePredicate(Predicate<T> p, String representation) {
		Objects.requireNonNull(p);
		this.predicate = p;
		this.representation = representation;
	}

	@Override
	public boolean test(T v) {
		return predicate.test(v);
	}

	@Override
	public String toString() {
		return representation;
	}
}
