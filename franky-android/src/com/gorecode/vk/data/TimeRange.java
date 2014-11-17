package com.gorecode.vk.data;

public class TimeRange {
	public long from;
	public long to;
	
	public static TimeRange FROM_EPOCH_TO_INFINITY = new TimeRange (0, Long.MAX_VALUE);

	public static TimeRange older(long time) {
		return olderOrEqual(time + 1);
	}

	public static TimeRange olderOrEqual(long time) {
		return new TimeRange(0, time);
	}

	public static TimeRange newerThan(long time) {
		return new TimeRange(time, Long.MAX_VALUE);
	}

	public TimeRange() {
		;
	}

	public TimeRange(long from, long to) {
		this.from = from;
		this.to = to;
	}
}
