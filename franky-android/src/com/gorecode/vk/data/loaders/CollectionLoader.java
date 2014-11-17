package com.gorecode.vk.data.loaders;

import com.gorecode.vk.data.ObjectSubset;

public interface CollectionLoader<D> {
	public static final int DEFAULT_PAGE_SIZE = 25;

	public D[] loadFreshData() throws Exception;

	public ObjectSubset<D> loadMoreData() throws Exception;
}
