package com.uva.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

public class ObserverCollection<T> implements Iterable<T> {
	private final ArrayList<T> observers = new ArrayList<T>();

	public void add(T observer) {
		AssertCompat.notNull(observer, "Observer");

		observers.add(observer);
	}

	public void remove(T observer) {
		observers.remove(observer);
	}

	public int getCount() {
		return observers.size();
	}

	@SuppressWarnings("unchecked")
	public Enumeration<T> toEnumeration() {
		synchronized (observers) {
			return Collections.enumeration((ArrayList<T>)observers.clone());
		}
	}

	public void callForEach(com.google.common.base.Function<T, Void> func) {
		Enumeration<T> e = toEnumeration();

		while (e.hasMoreElements()) {
			func.apply(e.nextElement());
		}
	}

	@Override
	public Iterator<T> iterator() {
		return observers.iterator();
	}
}
