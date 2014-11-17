package com.uva.lang;

/**
 * Repeats java.util.Comparator functionality.
 * @author vvs
 *
 */
public interface Comparator {
	public static final int LESS = -1;
	public static final int EQUAL = 0;
	public static final int GREATER = 1;

	int compare(Object o1, Object o2);
}
