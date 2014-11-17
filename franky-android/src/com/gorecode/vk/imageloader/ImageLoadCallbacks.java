package com.gorecode.vk.imageloader;

import android.graphics.Bitmap;

/**
 * Listener for image loading process
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public interface ImageLoadCallbacks {
	void onLoadingStarted();
	void onLoadingFailed(FailReason failReason);
	void onLoadingComplete(String url, Bitmap bitmap);
}
