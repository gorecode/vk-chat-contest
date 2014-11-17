package com.uva.lang;

public class SystemClock implements Clock {
	private final static SystemClock sInstance = new SystemClock();

	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public static SystemClock getInstance() {
		return sInstance;
	}
}
