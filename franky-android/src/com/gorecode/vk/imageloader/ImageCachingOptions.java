package com.gorecode.vk.imageloader;

public final class ImageCachingOptions {
	private final boolean cacheInMemory;
	private final boolean cacheOnDisc;

	private ImageCachingOptions(Builder builder) {
		cacheInMemory = builder.cacheInMemory;
		cacheOnDisc = builder.cacheOnDisc;
	}

	boolean isCacheInMemory() {
		return cacheInMemory;
	}

	boolean isCacheOnDisc() {
		return cacheOnDisc;
	}

	/**
	 * Builder for {@link ImageCachingOptions}
	 * 
	 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
	 */
	public static class Builder {
		private boolean cacheInMemory = false;
		private boolean cacheOnDisc = false;

		/** Loaded image will be cached in memory */
		public Builder cacheInMemory() {
			cacheInMemory = true;
			return this;
		}

		/** Loaded image will be cached on disc */
		public Builder cacheOnDisc() {
			cacheOnDisc = true;
			return this;
		}

		/** Builds configured {@link ImageCachingOptions} object */
		public ImageCachingOptions build() {
			return new ImageCachingOptions(this);
		}
	}
}
