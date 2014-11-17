package com.uva.lang;

public class Flag {
	public static boolean isSet(long value, long flag) {
		return ((value & flag) == flag);
	}

	private Flag() {
		;
	}
}
