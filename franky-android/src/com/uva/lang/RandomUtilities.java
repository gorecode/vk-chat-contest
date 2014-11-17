package com.uva.lang;

import java.util.List;
import java.util.Random;

public class RandomUtilities {
	public static final Random instance = new Random();

	public static Random instance() {
		return instance;
	}

	public static <T> T nextListElement(Random random, List<T> source) {
		if (source.size() == 0) throw new IllegalArgumentException("Source list cannot be empty");

		return source.get(nextInt(random, 0, source.size()));
	}

	public static <T> T nextArrayElement(T[] source) {
		return nextArrayElement(instance, source);
	}

	public static <T> T nextArrayElement(Random random, T[] source) {
		if (source.length == 0) throw new IllegalArgumentException("Source array cannot be empty");

		return source[nextInt(random, 0, source.length - 1)];
	}

	public static int nextInt(Random random, int min, int max) {
		if (max - min <= 0) {
			throw new IllegalArgumentException("Difference between max and min must be > 0");
		}
		return min + random.nextInt(max - min);
	}

	public static boolean randomBoolean(Random rand) {
		return rand.nextInt(2) == 1;
	}

	private RandomUtilities() {
	}
}
