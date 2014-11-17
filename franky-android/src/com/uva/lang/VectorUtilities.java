package com.uva.lang;

import java.util.Vector;

import com.uva.utilities.AssertCompat;

public final class VectorUtilities {
	public static Object[] toArray(Vector source, Object[] destination) {
		AssertCompat.notNull(source, "Source vector");
		AssertCompat.notNull(destination, "Destination array");

		if (source.size() > destination.length) {
			throw new IllegalArgumentException("Not enough space in destination array");
		}

		final int length = Math.min(source.size(), destination.length);

		for (int i = 0; i < length; i++) {
			destination[i] = source.elementAt(i);
		}

		return destination;
	}

	public static long[] toLongArray(Vector source) {
		synchronized (source) {
			final int size = source.size();
			final long arrayOfLong[] = new long[size];
			for (int i = 0; i < size; i++) {
				arrayOfLong[i] = ((Long)source.elementAt(i)).longValue();
			}
			return arrayOfLong;
		}
	}

	public static <T> boolean replace(Vector<T> vector, T source, T replacement) {
		int indexOfSource = vector.indexOf(source);
		if (indexOfSource == -1) return false;
		vector.removeElementAt(indexOfSource);
		vector.insertElementAt(replacement, indexOfSource);
		return true;
	}

	public static <T> Vector<T> safeCopy(Vector<T> source) {
		synchronized (source) {
			return copy(source);
		}
	}

	public static <T> Vector<T> copy(Vector<T> source) {
		Vector<T> clone = new Vector<T>(source.size());
		copy(source, clone);
		return clone;
	}

	public static <T> void copy(Vector<T> source, Vector<T> destination) {
		destination.removeAllElements();
		for (int i = 0; i < source.size(); i++) {
			destination.addElement(source.elementAt(i));
		}		
	}

	public static <T> void add(Vector<T> destination, Vector<T> source) {
		for (int i = 0; i < source.size(); i++) {
			destination.addElement(source.elementAt(i));
		}
	}

	private VectorUtilities() {
		;
	}
}
