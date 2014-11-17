package com.uva.lang;

public class BooleanUtilities {
	public static boolean parse(String string) {
		boolean isBooleanString = "true".equals(string) || "false".equals(string);

		if (!isBooleanString) {
			throw new IllegalArgumentException(string + " is not valid boolean");
		}

		return StringUtilities.equalsIgnoreCase("true", string);
	}

	public static boolean tryParse(String string) {
		try {
			parse(string);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private BooleanUtilities() {
	}
}
