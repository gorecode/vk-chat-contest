package com.gorecode.vk.activity;

import jsr305.inject.Nullable;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;

import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.imageloader.FailReason;
import com.gorecode.vk.imageloader.ImageLoadCallbacks;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.view.SafeViewFlipper;
import com.gorecode.vk.view.TouchImageView;
import com.uva.concurrent.ObjectHolder;
import com.uva.log.Log;

public class ViewImageActivity extends VkActivity {
	private static final String TAG = ViewImageActivity.class.getSimpleName();

	private static final String EXTRA_IMAGE_BITMAP = "imageBitmap";
	private static final String EXTRA_IMAGE_URL = "imageUrl";
	private static final String EXTRA_IMAGE_FILE_PATH = "imageFilePath";

	private static final float MAX_IMAGE_ZOOM = 5.0f;

	private static final int INDEX_SPINNER = 0;
	private static final int INDEX_CONTENT = 1;

	@InjectView(R.id.flipper)
	private SafeViewFlipper flipperView;
	@InjectView(R.id.image)
	private TouchImageView imageView;

	@InjectExtra(EXTRA_IMAGE_URL)
	@Nullable
	private String extraImageUrl;

	@InjectExtra(EXTRA_IMAGE_FILE_PATH)
	@Nullable
	private String extraImageFilePath;

	@InjectExtra(EXTRA_IMAGE_BITMAP)
	@Nullable
	private Bitmap extraImageBitmap;

	@Inject
	private ImageLoader imgLoader;

	public static void displayBitmap(Context context, Bitmap imageBitmap) {
		Intent intent = new Intent(context, ViewImageActivity.class);

		intent.putExtra(EXTRA_IMAGE_BITMAP, imageBitmap);
		intent.putExtra(EXTRA_IMAGE_URL, (String)null);
		intent.putExtra(EXTRA_IMAGE_FILE_PATH, (String)null);

		context.startActivity(intent);		
	}

	public static void displayWebImage(Context context, String imageUrl) {
		Intent intent = new Intent(context, ViewImageActivity.class);

		intent.putExtra(EXTRA_IMAGE_URL, imageUrl);
		intent.putExtra(EXTRA_IMAGE_BITMAP, (Bitmap)null);
		intent.putExtra(EXTRA_IMAGE_FILE_PATH, (String)null);

		context.startActivity(intent);
	}

	public static void displayFile(Context context, String filePath) {
		Intent intent = new Intent(context, ViewImageActivity.class);

		intent.putExtra(EXTRA_IMAGE_URL, (String)null);
		intent.putExtra(EXTRA_IMAGE_BITMAP, (Bitmap)null);
		intent.putExtra(EXTRA_IMAGE_FILE_PATH, filePath);

		context.startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_image_activity);

		imageView.setMaxZoom(MAX_IMAGE_ZOOM);

		try {
			if (extraImageUrl != null) {
				DownloadImageTask task = new DownloadImageTask();
				task.execute(extraImageUrl);
				getAutoCancelPool().add(task);
			} else if (extraImageBitmap != null) {
				setImageBitmap(extraImageBitmap);
			} else if (extraImageFilePath != null) {
				Bitmap bitmap = BitmapFactory.decodeFile(extraImageFilePath);

				if (bitmap == null) {
					throw new Exception("Cannot decode file " + extraImageFilePath);
				} else {
					setImageBitmap(bitmap);
				}
			} else {
				throw new Exception("No image source specified");
			}
		} catch (Throwable t) {
			Log.exception(TAG, "Unable to display image", new RuntimeException(t));

			ErrorHandlingUtilities.displayErrorSoftly(this, t);

			finish();
		}
	}

	private void setImageBitmap(Bitmap bitmap) {
		imageView.setImageBitmap(bitmap);

		flipperView.setDisplayedChild(INDEX_CONTENT);
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Pair<Bitmap, Exception>> {
		@Override
		protected void onPreExecute() {
			flipperView.setDisplayedChild(INDEX_SPINNER);
		}

		@Override
		protected Pair<Bitmap, Exception> doInBackground(String... params) {
			String imageUrl = params[0];

			final ObjectHolder resultHolder = new ObjectHolder();

			try {
				imgLoader.loadImage(imageUrl, new ImageLoadCallbacks() {
					@Override
					public void onLoadingStarted() {
						;
					}
					
					@Override
					public void onLoadingFailed(FailReason failReason) {
						resultHolder.setValue(failReason);
					}

					@Override
					public void onLoadingComplete(String url, Bitmap bitmap) {
						resultHolder.setValue(bitmap);
					}
				});

				Object result = resultHolder.waitForValue();

				if (result instanceof Bitmap) {
					return Pair.create((Bitmap)result, null);
				} else {
					FailReason failReason = (FailReason)result;

					throw new Exception("Unable to download image, fail reason = " + failReason);
				}				
			} catch (Exception e) {
				Log.exception(TAG, "Unable to download image", e);

				return Pair.create(null, e);
			}
		}

		@Override
		protected void onPostExecute(Pair<Bitmap, Exception> result) {
			if (isCancelled()) return;

			if (result.second != null) {
				ErrorHandlingUtilities.displayErrorSoftly(ViewImageActivity.this, result.second);

				finish();
			} else {
				setImageBitmap(result.first);
			}
		}
	}
}
