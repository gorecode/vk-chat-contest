package com.gorecode.vk.imageloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

/**
 * Decodes images to {@link Bitmap}
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
final class ImageDecoder {

	private ImageDecoder() {
	}

	/**
	 * Decodes image from URL into {@link Bitmap}. Image is scaled close to incoming {@link ImageSize image size} during
	 * decoding. Initial image size is reduced by the power of 2 (according Android recommendations)
	 * 
	 * @param imageUrl
	 *            Image URL (<b>i.e.:</b> "http://site.com/image.png", "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Image size to scale to during decoding
	 * @return Decoded bitmap
	 * @throws IOException
	 */
	public static Bitmap decodeFile(URL imageUrl, ImageSize targetImageSize) throws IOException {
		InputStream is = imageUrl.openStream();

		is = imageUrl.openStream();

		Bitmap result;

		try {
			result = BitmapFactory.decodeStream(is, null, null);
		} finally {
			is.close();
		}

		return result;
	}
}
