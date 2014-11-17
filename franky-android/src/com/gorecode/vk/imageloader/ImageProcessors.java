package com.gorecode.vk.imageloader;

import android.graphics.Bitmap;

import com.gorecode.vk.utilities.BitmapUtilities;

public class ImageProcessors {
	public static final ImageProcessor PHOTO_ROUNDER = newCornersRounder();

	public static final ImageProcessor newCornersRounder() {
		return new ImageProcessor() {
			@Override
			public Bitmap progressImage(Bitmap bitmap) {
				final int w = bitmap.getWidth();
				final int h = bitmap.getHeight();

				final int delta = Math.max(w, h) >> 3;

				if (w - h <= delta) {
					int radius = ((bitmap.getWidth() + bitmap.getHeight()) >> 1) >> 3;

					return BitmapUtilities.roundCorners(bitmap, radius);
				} else {
					int a = 0;
					a++;
					return bitmap;
				}
			}
		};
	}

	private ImageProcessors() {
		;
	}
}
