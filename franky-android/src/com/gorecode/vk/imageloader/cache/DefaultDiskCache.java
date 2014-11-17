package com.gorecode.vk.imageloader.cache;

import java.io.File;

import com.gorecode.vk.utilities.FileCache;

import android.content.Context;

public class DefaultDiskCache extends DiscCache {
	private static final File INVALID_FILE = new File("");

	private final Context context;

	public DefaultDiskCache(Context context) {
		this.context = context;
	}

	@Override
	protected File getCacheFileForUrl(String url) {
		String fileName = url.hashCode() + ".imgcache";

		File f = FileCache.addFileToCache(context, fileName);

		if (f == null) {
			return INVALID_FILE;
		}

		return f;
	}
}
