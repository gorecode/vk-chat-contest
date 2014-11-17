package com.gorecode.vk.data.loaders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.ObjectSubset;

public abstract class CollectionLoaderByOffset<D> implements CollectionLoader<D> {
	private final Object mLock = new Object();

	private final Collection<D> mData;

	private boolean mOffsetInitialized;
	private int mOffset;

	public CollectionLoaderByOffset(Collection<D> data) {
		Preconditions.checkNotNull(data);

		mData = data;
	}

	public void increaseOffset(int delta) {
		mOffset = Math.max(0, mOffset + delta);
	}

	public void decreaseOffset(int delta) {
		mOffset = Math.max(0, mOffset - delta);
	}

	public int getOffset() {
		return mOffset;
	}

	public void setOffset(int offset) {
		mOffset = offset;
	}

	@Override
	public ObjectSubset<D> loadMoreData() throws Exception {
		int offset = 0;

		synchronized (mLock) {
			if (!mOffsetInitialized) {
				mOffset = mData.size();

				mOffsetInitialized = true;
			}

			offset = mOffset;
		}

		ObjectSubset<D> chunk = loadDataPageFully(offset, DEFAULT_PAGE_SIZE);

		synchronized (mLock) {
			mOffset = Math.max(mData.size(), mOffset + chunk.content.length);
		}

		return chunk;
	}

	public Collection<D> getData() {
		return mData;
	}

	public D[] loadFreshData() throws Exception {
		// FIXME: Find a better way to fetch only "fresh" data.
		return loadDataPageFully(0, DEFAULT_PAGE_SIZE * 3).content;
	}

	public ObjectSubset<D> loadDataPageFully(int offset, int prefferedLimit) throws Exception {
		return loadDataPageFully(offset, prefferedLimit, DEFAULT_PAGE_SIZE);
	}

	public ObjectSubset<D> loadDataPageFully(int offset, int preferredLimit, int pageLimit) throws Exception {
		ArrayList<D> data = new ArrayList<D>();

		ObjectSubset<D> page;

		do {
			page = loadDataPage(offset, pageLimit);
			data.addAll(Arrays.asList(page.content));
			offset += page.content.length;
		} while (page.hasMore && data.size() < preferredLimit);

		return new ObjectSubset<D>(data.toArray(page.content), page.hasMore);
	}

	/**
	 * Loads page of data from server, assuming that server has constraints on LIMIT attribute of selection query. 
	 * @param offset OFFSET in server SELECT SQL query.
	 * @param limit LIMIT in server SELECT SQL query, request can be rejected by server if value is too long (typically 100-200 is top acceptable value).
	 * @return subset with loaded data.
	 * @throws Exception when error occurs.
	 */
	abstract public ObjectSubset<D> loadDataPage(int offset, int limit) throws Exception;
}
