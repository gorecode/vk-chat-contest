package com.gorecode.vk.data;

import com.google.common.base.Preconditions;

public class ObjectSubset<T> {
	public T[] content;

	public boolean hasMore;

	public ObjectSubset() {
		;
	}

	public ObjectSubset(T[] content, boolean hasMore) {
		Preconditions.checkNotNull(content);

		this.content = content;
		this.hasMore = hasMore;
	}

	public static <T> ObjectSubset<T> make(T[] content, boolean hasMore) {
		ObjectSubset<T> returnValue = new ObjectSubset<T>();
		returnValue.content = content;
		returnValue.hasMore = hasMore;
		return returnValue;
	}

	public static <T> ObjectSubset<T> from(T[] content) {
		return new ObjectSubset<T>(content, false);
	}

	@Override
	public String toString() {
		return super.toString() + " [count = " + content.length + ", hasMore = " + hasMore + "]";
	}
}
