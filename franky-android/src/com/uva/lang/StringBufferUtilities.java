package com.uva.lang;

public class StringBufferUtilities {
	/**
	 * Stores given integer value as string into given string buffer.
	 * @param buffer the string buffer.
	 * @param value the integer.
	 * @param minSize count of characters to reserve for value (it this value is 2, then int value 1 become "02" etc).
	 */
	public static void append(StringBuffer buffer, int value, int minSize) {
		final String valueAsString = String.valueOf(value);

		final int padding = minSize - valueAsString.length();

		for (int i = 0; i < padding; i++) {
			buffer.append("0");
		}

		buffer.append(valueAsString);
	}

	private StringBufferUtilities() {
		;
	}
}
