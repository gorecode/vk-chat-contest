package com.gorecode.vk.imageloader;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.uva.log.Log;

public class ListImageLoader extends ImageLoader implements AbsListView.OnScrollListener {
	private static final boolean DEBUG = false;

	private static final String TAG = ListImageLoader.class.getSimpleName();

	private static final long SCROLL_TIMEOUT = 300;

	private int mScrollState = AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

	private final Map<ImageView, Runnable> mPausedDownloads = new HashMap<ImageView, Runnable>();

	private final Handler mHandler = new Handler(Looper.getMainLooper());

	@Inject
	public ListImageLoader(Context context, ImageLoaderConfiguration configuration) {
		super(configuration);

		setDefaultImageProcessor(ImageProcessors.newCornersRounder());
	}

	@Override
	public Future<Bitmap> loadImageForImageView(final String url, final ImageView imageView, final ImageLoadCallbacks listener, final ImageProcessor processor) {
		ImageCachingOptions options = configuration.defaultImageCachingOptions;

		ImageSize targetSize = imageView != null ? getImageSizeScaleTo(imageView) : null;

		DownloadHandle downloadHandle = new DownloadHandle(url, imageView, targetSize, options, listener, processor);

		if (imageView != null) {
			if (DEBUG) Log.trace(TAG, String.format("Starting download of URL for view (%s) = %s", imageView, downloadHandle.url));

			imageView.setTag(IMAGE_TAG_KEY, new WeakReference<DownloadHandle>(downloadHandle));
		}

		Bitmap bmp = configuration.memoryCache.get(downloadHandle.getMemoryCacheKey());

		if (bmp != null && !bmp.isRecycled()) {
			listener.onLoadingComplete(downloadHandle.url, bmp);

			final Bitmap futureResult = bmp;

			FutureTask<Bitmap> future = new FutureTask<Bitmap>(new Callable<Bitmap>() {
				@Override
				public Bitmap call() throws Exception {
					return futureResult;
				}
			});

			future.run();

			downloadHandle.setFuture(future);

			return future;
		}

		if (imageView == null) {
			return super.loadImageForImageView(url, imageView, listener, processor);
		} else {
			if (mScrollState == SCROLL_STATE_IDLE) {
				return super.loadImageForImageView(url, imageView, listener, processor);
			} else {
				Runnable loadImageForImageViewCall = new Runnable() {
					public void run() {
						if (DEBUG) Log.debug(TAG, "url = " + url + ", view = " + imageView);

						loadImageForImageViewNow(url, imageView, listener, processor);
					}
				};

				mPausedDownloads.put(imageView, loadImageForImageViewCall);

				return null;
			}
		}
	}

	public Future<Bitmap> loadImageForImageViewNow(final String url, final ImageView imageView, final ImageLoadCallbacks listener, final ImageProcessor processor) {
		return super.loadImageForImageView(url, imageView, listener, processor);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		mHandler.removeCallbacks(mSetListViewIsNotScrollingRunnable);

		mHandler.postDelayed(mSetListViewIsNotScrollingRunnable, SCROLL_TIMEOUT);

		if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
			if ((firstVisibleItem + visibleItemCount == totalItemCount) && (visibleItemCount != 0) && (totalItemCount != 0)) {
				setListViewIsNotScrolling();
			}

			if ((firstVisibleItem == 0) && (totalItemCount == 0)) {
				setListViewIsNotScrolling();
			}
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (DEBUG) Log.debug(TAG, "Scroll state = " + scrollState);

		mScrollState = scrollState;

		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			setListViewIsNotScrolling();
		}
	}

	private void setListViewIsNotScrolling() {
		mHandler.removeCallbacks(mSetListViewIsNotScrollingRunnable);

		if (DEBUG) Log.debug(TAG, "Scroll state is idle");

		for (ImageView imageView : mPausedDownloads.keySet()) {
			mPausedDownloads.get(imageView).run();
		}
		mPausedDownloads.clear();

		mScrollState = OnScrollListener.SCROLL_STATE_IDLE;
	}

	private final Runnable mSetListViewIsNotScrollingRunnable = new Runnable() {
		@Override
		public void run() {
			setListViewIsNotScrolling();
		}
	};
}
