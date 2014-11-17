package com.gorecode.vk.data.loaders;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import android.os.Process;

import com.gorecode.vk.data.ObjectSubset;
import com.uva.log.Log;

public class BackwardPreloader<D> implements CollectionLoader<D> {
	private static final String TAG = "BackwardPreloader";

	private final ExecutorService mExecutor;
	private final CollectionLoader<D> mLoader;

	private volatile Future<ObjectSubset<D>> mPreloadFuture;

	public BackwardPreloader(ExecutorService executor, CollectionLoader<D> loader) {
		mExecutor = executor;
		mLoader = loader;
	}

	@Override
	public D[] loadFreshData() throws Exception {
		return mLoader.loadFreshData();
	}

	@Override
	public ObjectSubset<D> loadMoreData() throws Exception {
		if (mPreloadFuture != null) {
			try {
				ObjectSubset<D> data = mPreloadFuture.get();

				preloadMoreData();

				return data;
			} catch (Exception e) {
				mPreloadFuture = null;

				throw e;
			}
		} else {
			ObjectSubset<D> data = mLoader.loadMoreData();

			preloadMoreData();

			return data;
		}
	}

	public void preloadMoreData() {
		Log.debug(TAG, "Preloading started");

		mPreloadFuture = mExecutor.submit(new PreloadTask());
	}

	private class PreloadTask implements Callable<ObjectSubset<D>> {
		@Override
		public ObjectSubset<D> call() throws Exception {
			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			try {
				return mLoader.loadMoreData();
			} finally {
				Log.debug(TAG, "Preloading complete");
			}
		}		
	}
}
