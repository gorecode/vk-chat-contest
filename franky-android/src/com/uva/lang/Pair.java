package com.uva.lang;

public class Pair<F, S> {
	public final F first;
	public final S second;

	public static <F, S> Pair<F, S> create(F first, S second) {
		return new Pair(first, second);
	}

	private Pair(F f, S s) {
		this.first = f;
		this.second = s;
	}
}
