package com.gorecode.vk.imageloader.cache;

import java.io.File;
import java.util.ArrayList;

/**
 * Interface for cache on file system
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com), enikey.
 */
public abstract class DiscCache {
	private final ArrayList<File> deleteOnClear = new ArrayList<File>();

	/**
	 * Returns {@linkplain File file object} appropriate incoming URL.<br />
	 * <b>NOTE:</b> Must <b>not to return</b> a null. Method must return specific {@linkplain File file object} for
	 * incoming URL whether file exists or not.
	 */
	final public File getFile(String url) {
		File file = getCacheFileForUrl(url);
		deleteOnClear.add(file);
		return file;
	}

	protected abstract File getCacheFileForUrl(String url);

	/** Clears cache directory */
	public void clear() {
		for (File f : deleteOnClear) {
			f.delete();
		}
		deleteOnClear.clear();
	}
}