package com.gorecode.vk.data;

public class Range {
	public Range() {
		;
	}

	public Range(int offset, int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	public int offset;
	public int limit;
}
