package com.uva.lang;

import java.util.List;
import java.util.Random;
import java.util.Vector;

import com.google.common.base.Function;


public class ArrayUtilities {
	private ArrayUtilities() {
		;
	}

	public static Object[] filter(Object[] source, Predicate predicate) {
		Vector filtered = new Vector();

		for (int i = 0; i < source.length; i++) {
			if (predicate.apply(source[i])) {
				filtered.addElement(source[i]);
			}
		}

		return VectorUtilities.toArray(filtered, new Object[filtered.size()]);
	}

	public static void shuffle(Object[] source, Random random, int passCount) {
		for (int i = 0; i < passCount; i++) {
			shuffleOnce(source, random);
		}
	}

	public static void shuffleOnce(Object[] source, Random random) {
		for (int i = 0; i < source.length; i++) {
			int randomIndex = RandomUtilities.nextInt(random, 0, source.length);

			Object current = source[i];
			source[i] = source[randomIndex];
			source[randomIndex] = current;
		}
	}

	public static <F, T> T[] transform(List<F> source, T[] destination, Function<F, T> func) {
		int i = 0;
		for (F v : source) {
			destination[i++] = func.apply(v);
		}
		return destination;
	}

	public static <F, T> Object[] transform(F[] source, Object[] destination, Function<F, T> func) {
		for (int i = 0; i < source.length; i++) {
			destination[i] = func == null ? source[i] : func.apply(source[i]);
		}
		return destination;
	}

	public static Vector toVector(Object[] source) {
		final Vector vec = new Vector(source.length);

		for (int i = 0; i < source.length; i++) {
			vec.addElement(source[i]);
		}
		return vec;
	}

	/**
	 * Gets index of specified element within specified array.
	 * @param array array within to search.
	 * @param element element to search.
	 * @return returns first index of element found of -1, if no element found.
	 */
	public static int indexOf(Object[] array, Object element) {
		if (element == null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == null) return i;
			}
		}
		else {
			for (int i = 0; i < array.length; i++) {
				if (element.equals(array[i])) return i;
			}
		}
		return -1;
	}
	
	/**
	 * Gets first index of element within specified array. 
	 * @param array array array within to search.
	 * @param element element to search.
	 * @param comparator comparator to compare elements.
	 * @return returns first index of element found of -1, if no element found.
	 */
	public static int indexOf(Object[] array, Object element, Comparator comparator) {
		if (element == null) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] == null) return i;
			}
		}
		else {
			for (int i = 0; i < array.length; i++) {
				if (comparator.compare(element, array[i]) == 0) return i;
			}
		}
		return -1;
	}
	
	/**
	 * Returns whether given array contains element.
	 * @param array array within to search.
	 * @param element element to search.
	 * @return true, if element found and false otherwise.
	 */
	public static boolean contains(Object[] array, Object element) {
		return (indexOf(array, element) >= 0);
	}

	public static Object[] substract(Object[] source, Object[] operand, ComparisonOperator eqOp) {
		Vector substracted = new Vector(); 
		for (int i = 0; i < source.length; i++) {
			Object each = source[i];
			boolean found = false;
			for (int j = 0; j < operand.length; j++) {
				if (eqOp.equals(each, operand[j])) {
					found = true;
					break;
				}
			}
			if (!found) {
				substracted.addElement(each);
			}
		}
		Object[] result = new Object[substracted.size()];
		substracted.copyInto(result);
		return result;		
	}

	public static Object[] intersect(Object[] array1, Object[] array2, ComparisonOperator eqOp) {
		Vector intersection = new Vector(); 
		for (int i = 0; i < array1.length; i++) {
			Object each = array1[i];
			for (int j = 0; j < array2.length; j++) {
				if (eqOp.equals(each, array2[j])) {
					intersection.addElement(each);
					break;
				}
			}
		}
		Object[] result = new Object[intersection.size()];
		intersection.copyInto(result);
		return result;
	}
}
