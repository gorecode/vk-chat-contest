package com.uva.location;


public class PlatformLocationTracker {
	private static LocationTracker sDefined;

	public static LocationTracker get() {
		if (sDefined == null) {
			throw new IllegalStateException("Platform location tracker is not defined");
		}
		return sDefined;
	}

	public static void define(LocationTracker tracker) {
		if (sDefined != null) {
			throw new IllegalStateException("Platform location tracker already defined");
		}
		sDefined = tracker;
	}

	private PlatformLocationTracker() {
		;
	}
}
