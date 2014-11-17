package com.gorecode.vk.utilities;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.uva.log.Log;
import com.uva.log.Message;

public class BitmapUtilities {
	private static final String TAG = "BitmapUtilities";

	public static Bitmap roundCorners(Bitmap bitmap, int cornersRadiusInPixels) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = cornersRadiusInPixels;

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	public static Bitmap createGroupChatPhoto(Bitmap[] userPhotos, WindowManager windowManager) {
		DisplayMetrics metrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metrics);
		int size = (int)(50.0F * metrics.density);
		Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		if (userPhotos.length != 2) {
			if (userPhotos.length != 3) {
				if (userPhotos.length == 4) {
					canvas.drawBitmap(userPhotos[0], new Rect(0, 0, userPhotos[0].getWidth(), userPhotos[0].getHeight()), new Rect(0, 0, size / 2, size / 2), paint);
					canvas.drawBitmap(userPhotos[1], new Rect(0, 0, userPhotos[1].getWidth(), userPhotos[1].getHeight()), new Rect(0, 1 + size / 2, size / 2, size), paint);
					canvas.drawBitmap(userPhotos[2], new Rect(0, 0, userPhotos[2].getWidth(), userPhotos[2].getHeight()), new Rect(1 + size / 2, 0, size, size / 2), paint);
					canvas.drawBitmap(userPhotos[3], new Rect(0, 0, userPhotos[3].getWidth(), userPhotos[3].getHeight()), new Rect(1 + size / 2, 1 + size / 2, size, size), paint);
				}
			} else {
				canvas.drawBitmap(userPhotos[0], new Rect(userPhotos[0].getWidth() / 4, 0, 3 * (userPhotos[0].getWidth() / 4), userPhotos[0].getHeight()), new Rect(0, 0, size / 2, size), paint);
				canvas.drawBitmap(userPhotos[1], new Rect(0, 0, userPhotos[1].getWidth(), userPhotos[1].getHeight()), new Rect(1 + size / 2, 0, size, size / 2), paint);
				canvas.drawBitmap(userPhotos[2], new Rect(0, 0, userPhotos[2].getWidth(), userPhotos[2].getHeight()), new Rect(1 + size / 2, 1 + size / 2, size, size), paint);
			}
		} else {
			canvas.drawBitmap(userPhotos[0], new Rect(userPhotos[0].getWidth() / 4, 0, 3 * (userPhotos[0].getWidth() / 4), userPhotos[0].getHeight()), new Rect(0, 0, size / 2, size), paint);
			canvas.drawBitmap(userPhotos[1], new Rect(userPhotos[1].getWidth() / 4, 0, 3 * (userPhotos[1].getWidth() / 4), userPhotos[1].getHeight()), new Rect(1 + size / 2, 0, size, size), paint);
		}
		return bitmap;
	}

	public static Bitmap createBitmapFromBytesSilent(byte[] bytes) {
		try {
			return createBitmapFromBytes(bytes);
		} catch (IOException e) {
			Log.exception(TAG, Message.ERROR, e);
		} catch (OutOfMemoryError e) {
			Log.warning(TAG, "OutOfMemoryError while creating bitmap - ignoring (will return null)");
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] createBytesFromBitmapSilent(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}
		try {
			return createBytesFromBitmap(bitmap);
		} catch (IOException e) {
			Log.exception(TAG, Message.ERROR, e);
		}
		return null;
	}

	public static Bitmap createBitmapFromBytes(byte[] bytes) throws IOException {
		if (bytes == null) {
			return null;
		}

		try {
			return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	public static byte[] createBytesFromBitmap(Bitmap bitmap) throws IOException {
		if (bitmap == null) {
			return null;
		}
		try {
			ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
			bitmap.compress(CompressFormat.PNG, 100, bytesOut);
			return bytesOut.toByteArray();
		} catch (RuntimeException e) {
			throw new IOException(e.getMessage());
		}
	}

	public static Bitmap centerCrop(Bitmap source, int newHeight, int newWidth) {
		int sourceWidth = source.getWidth();
		int sourceHeight = source.getHeight();

		// Compute the scaling factors to fit the new height and width, respectively.
		// To cover the final image, the final scaling will be the bigger 
		// of these two.
		float xScale = (float) newWidth / sourceWidth;
		float yScale = (float) newHeight / sourceHeight;
		float scale = Math.max(xScale, yScale);

		// Now get the size of the source bitmap when scaled
		float scaledWidth = scale * sourceWidth;
		float scaledHeight = scale * sourceHeight;

		// Let's find out the upper left coordinates if the scaled bitmap
		// should be centered in the new size give by the parameters
		float left = (newWidth - scaledWidth) / 2;
		float top = (newHeight - scaledHeight) / 2;

		// The target rectangle for the new, scaled version of the source bitmap will now
		// be
		RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

		// Finally, we create a new bitmap of the specified size and draw our new,
		// scaled bitmap onto it.
		Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
		Canvas canvas = new Canvas(dest);
		canvas.drawBitmap(source, null, targetRect, null);

		return dest;
	}

	public static Bitmap loadPreview(String path, int maxImageWidth, int maxImageHeight) throws Exception {
		BitmapFactory.Options decodeOpts = new BitmapFactory.Options();

		int maxImageSize = Math.max(maxImageWidth, maxImageHeight);

		decodeOpts.inSampleSize = getDownScaleFactorToFit(path, maxImageSize);

		Bitmap previewImage = null;

		try {
			previewImage = BitmapFactory.decodeFile(path, decodeOpts);

			if (previewImage == null) {
				throw new Exception("Unable to decode attaced image preview");
			}
		} catch (OutOfMemoryError e) {
			throw new Exception("Out Of Memory", e);
		}

		Log.debug(TAG, "Preview loaded size = " + String.format("%1$dx%2$d", previewImage.getWidth(), previewImage.getHeight()));

		return previewImage;
	}

	public static int getDownScaleFactorToFit(String sourceFile, int maxSize) throws IOException {
		InputStream in = null;

		in = new FileInputStream(sourceFile);

		Log.debug(TAG, "Decoding size of image");
		//Decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
		o.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(in, null, o);
		in.close();

		Log.debug(TAG, "Decoded size (" + o.outWidth + ", " + o.outHeight + "), maximum allowed side is " + maxSize);

		int downScale = 1;

		if (o.outHeight > maxSize || o.outWidth > maxSize) {
			downScale = (int)Math.pow(2, (int) Math.round(Math.log(maxSize / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
		}

		Log.debug(TAG, "DownScale factor is " + downScale);

		return downScale;
	}

	public static byte[] compressPhotoToPNG(Bitmap photo) {
		if (photo == null) return null;
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		if (photo.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
			return stream.toByteArray();
		} else {
			Log.error(TAG, "Failed to compress profile bitmap");
			return null;
		}
	}

	private BitmapUtilities() {
		;
	}
}
