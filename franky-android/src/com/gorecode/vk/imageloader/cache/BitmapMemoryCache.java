package com.gorecode.vk.imageloader.cache;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class BitmapMemoryCache {
	private final InternalCache mInternalCache;

	public BitmapMemoryCache(int maxSize) {
		mInternalCache = new InternalCache(maxSize);
	}

	public synchronized Bitmap get(String key) {
		return mInternalCache.get(key);
	}

	public synchronized void put(String key, Bitmap value) {
		mInternalCache.put(key, value);
	}

	private static class InternalCache extends LruCache<String, Bitmap> {
		public InternalCache(int maxSize) {
			super(maxSize);
		}

		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getWidth() * bitmap.getHeight() * 4;
		}		
	}
}
