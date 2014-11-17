package com.uva.utilities;

public class AssertCompat {
	public static void isTrue(boolean condition, String message) {
		if (!condition) {
			throw new RuntimeException(message);
		}
	}

	public static void notNull(Object value, String valueName) {
		if (value == null) {
			throw new IllegalArgumentException(valueName + " cannot be null");
		}
	}

	public static void shouldNeverHappen(Exception e) {
		throw new RuntimeException(e.getMessage());
	}

	private AssertCompat() {
		;
	}
}
