package com.gorecode.vk.imageloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import android.graphics.Bitmap;
import android.os.Process;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.google.inject.Inject;
import com.uva.io.StreamUtilities;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;
import com.uva.log.Message;

/**
 * Singletone for image loading and displaying at {@link ImageView ImageViews}<br />
 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be called before any other method.
 * 
 * @author Sergey Tarasevich (nostra13[at]gmail[dot]com)
 */
public class ImageLoader {
	private static final boolean DEBUG = false;

	public static interface ImageFetcher {
		public boolean canFetch(String url);

		public void fetchImage(String url, File outFile) throws Exception;
	}

	protected static final int IMAGE_TAG_KEY = "imageLoaderTag".hashCode();

	static final String TAG = ImageLoader.class.getSimpleName();

	private static final String ERROR_INIT_CONFIG_WITH_NULL = "ImageLoader configuration can not be initialized with null";

	protected final static ExecutorService executorForCpu = Executors.newSingleThreadExecutor(ThreadFactories.WITH_LOWEST_PRIORITY);
	protected final static ExecutorService executorForNetwork = Executors.newFixedThreadPool(3, ThreadFactories.WITH_LOWEST_PRIORITY);

	protected ImageLoaderConfiguration configuration;
	protected ImageProcessor defaultImageProcessor;
	protected final HashSet<ImageFetcher> fetchers = new HashSet<ImageLoader.ImageFetcher>();

	public ImageLoader() {
	}

	@Inject
	public ImageLoader(ImageLoaderConfiguration configuration) {
		init(configuration);
	}

	public ImageLoaderConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Initializes ImageLoader's singletone instance with configuration. Method shoiuld be called <b>once</b> (each
	 * following call will have no effect)<br />
	 * 
	 * @param configuration
	 *            {@linkplain ImageLoaderConfiguration ImageLoader configuration}
	 * @throws IllegalArgumentException
	 *             if <b>configuration</b> parameter is null
	 */
	public synchronized void init(ImageLoaderConfiguration configuration) {
		if (configuration == null) {
			throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
		}
		if (this.configuration == null) {
			this.configuration = configuration;
		}
	}

	public void registerFetcher(ImageFetcher fetcher) {
		fetchers.add(fetcher);
	}

	public void unregisterFetcher(ImageFetcher fetcher) {
		fetchers.remove(fetcher);
	}

	public Future<Bitmap> loadImage(String url, ImageLoadCallbacks listener) {
		return loadImage(url, listener, null);
	}

	public Future<Bitmap> loadImage(String url, ImageLoadCallbacks listener, ImageProcessor processor) {
		return loadImageForImageView(url, null, listener, processor);
	}

	public Future<Bitmap> loadImageForImageView(String url, ImageView imageView, ImageLoadCallbacks listener) {
		return loadImageForImageView(url, imageView, listener, null);
	}

	public Future<Bitmap> loadImageForImageView(String url, ImageView imageView, ImageLoadCallbacks listener, ImageProcessor processor) {
		if (listener == null) {
			listener = new EmptyListener();
		}

		assert configuration != null;
		assert url != null;

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

		listener.onLoadingStarted();

		Future<Bitmap> future = executorForNetwork.submit(new LoadImageTask(downloadHandle));

		downloadHandle.setFuture(future);

		return future;
	}

	public void setDefaultImageProcessor(ImageProcessor processor) {
		this.defaultImageProcessor = processor;
	}

	/** Stops all running display image tasks, discards all other scheduled tasks */
	public void stop() {
		; // TODO )
	}

	/**
	 * Clear disc cache.<br />
	 * Do nothing if {@link #init(ImageLoaderConfiguration)} method wasn't called before.
	 */
	public void clearDiscCache() {
		if (configuration != null) {
			configuration.discCache.clear();
		}
	}

	protected Bitmap loadBitmap(final DownloadHandle imageLoadingInfo) throws Exception {
		if (!imageLoadingInfo.isConsistent()) {
			throw new Exception("Not consistent");
		}

		final File f = configuration.discCache.getFile(imageLoadingInfo.url);

		Bitmap bitmap = null;

		// Try to load image from disc cache
		if (f != null && f.exists()) {
			Bitmap b = decodeImage(imageLoadingInfo, f.toURL());

			if (b != null) {
				return b;
			}
		}

		// Load image from Web
		URL imageUrlForDecoding = null;

		if (imageLoadingInfo.options.isCacheOnDisc()) {
			downloadImageToDisc(imageLoadingInfo, f);

			imageUrlForDecoding = f.toURL();
		} else {
			imageUrlForDecoding = new URL(imageLoadingInfo.url);
		}

		bitmap = decodeImage(imageLoadingInfo, imageUrlForDecoding);

		if (bitmap == null) {
			throw new Exception("Unable to decode bitmap");
		}

		return bitmap;
	}

	private Bitmap decodeImage(final DownloadHandle imageLoadingInfo, final URL imageUrl) throws Exception {
		return executorForCpu.submit(new Callable<Bitmap>() {
			@Override
			public Bitmap call() throws Exception {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				Bitmap bitmap = ImageDecoder.decodeFile(imageUrl, imageLoadingInfo.targetSize);

				Thread.sleep(50);

				return bitmap;
			}
		}).get();
	}

	private void downloadImageToDisc(DownloadHandle imageLoadingInfo, File targetFile) throws Exception {
		String uri = imageLoadingInfo.url;

		for (ImageFetcher fetcher : fetchers) {
			if (fetcher.canFetch(uri)) {
				if (DEBUG) Log.debug(TAG, "Fetching image using " + fetcher.getClass().getSimpleName() + " fetcher");
				fetcher.fetchImage(uri, targetFile);
				if (DEBUG) Log.debug(TAG, "Fetching complete");
				return;
			}
		}

		if (DEBUG) Log.trace(TAG, "Downloading image, url = " + imageLoadingInfo.url);
		HttpURLConnection conn = (HttpURLConnection) new URL(imageLoadingInfo.url).openConnection();
		conn.setConnectTimeout(configuration.httpConnectTimeout);
		conn.setReadTimeout(configuration.httpReadTimeout);
		if (conn instanceof HttpsURLConnection) { // This action is necessary in order to prevent "Verify host name" error.
			((HttpsURLConnection)conn).setHostnameVerifier(new AllowAllHostnameVerifier());
		}
		BufferedInputStream is = new BufferedInputStream(conn.getInputStream(), 8 * 1024);
		try {
			OutputStream os = new FileOutputStream(targetFile);
			try {
				StreamUtilities.copyStream(is, os);
			} finally {
				os.close();
			}
		} finally {
			is.close();
		}
		if (DEBUG) Log.trace(TAG, "Image downloaded");
	}

	/**
	 * Defines image size for loading at memory (for memory economy) by {@link ImageView} parameters.<br />
	 * Size computing algorithm:<br />
	 * 1) Get <b>maxWidth</b> and <b>maxHeight</b>. If both of them are not set then go to step #2.<br />
	 * 2) Get <b>layout_width</b> and <b>layout_height</b>. If both of them haven't exact value then go to step #3.</br>
	 * 3) null.
	 */
	protected ImageSize getImageSizeScaleTo(ImageView imageView) {
		int width = -1;
		int height = -1;

		// Check maxWidth and maxHeight parameters
		try {
			Field maxWidthField = ImageView.class.getDeclaredField("mMaxWidth");
			Field maxHeightField = ImageView.class.getDeclaredField("mMaxHeight");
			maxWidthField.setAccessible(true);
			maxHeightField.setAccessible(true);
			int maxWidth = (Integer) maxWidthField.get(imageView);
			int maxHeight = (Integer) maxHeightField.get(imageView);

			if (maxWidth >= 0 && maxWidth < Integer.MAX_VALUE) {
				width = maxWidth;
			}
			if (maxHeight >= 0 && maxHeight < Integer.MAX_VALUE) {
				height = maxHeight;
			}
		} catch (Exception e) {
			if (DEBUG) Log.exception(TAG, e.getMessage(), e);
		}

		if (width < 0 && height < 0) {
			// Get layout width and height parameters
			LayoutParams params = imageView.getLayoutParams();
			width = params.width;
			height = params.height;
		}

		// Get device screen dimensions
		if (width < 0 || height < 0) {
			return null;
		}

		return new ImageSize(width, height);
	}

	/** Information about display image task */
	protected final class DownloadHandle {
		public final String url;
		public final ImageView imageView;
		public final ImageSize targetSize;
		public final ImageCachingOptions options;
		private final ImageProcessor processor;
		private final List<ImageLoadCallbacks> listeners = Collections.synchronizedList(new ArrayList<ImageLoadCallbacks>());
		private Future<Bitmap> future;

		public DownloadHandle(String url, ImageView imageView, ImageSize targetSize, ImageCachingOptions options, ImageLoadCallbacks listener, ImageProcessor processor) {
			this.url = url;
			this.imageView = imageView;
			this.targetSize = targetSize;
			this.processor = processor;
			this.options = options;
			this.listeners.add(listener);
		}

		public String getMemoryCacheKey() {
			return url + (targetSize == null ? "" : targetSize.toString());
		}

		public Future<Bitmap> getFuture() throws Exception {
			synchronized (this) {
				while (future == null) {
					wait();
				}
			}

			return future;
		}

		/** Whether current URL matches to URL from ImageView tag */
		@SuppressWarnings("unchecked")
		boolean isConsistent() {
			if (imageView == null) return true;

			WeakReference<DownloadHandle> handleRef = (WeakReference<DownloadHandle>)imageView.getTag(IMAGE_TAG_KEY);

			if (handleRef == null) return false;

			DownloadHandle handle = handleRef.get();

			if (handle == null) return false;
			if (handle == this) return true;

			return (getMemoryCacheKey().equals(handle.getMemoryCacheKey())); 
		}

		void setFuture(Future<Bitmap> future) {
			synchronized (this) {
				this.future = future;

				notifyAll();
			}
		}
	}

	private class LoadImageTask implements java.util.concurrent.Callable<Bitmap> {
		private final DownloadHandle imageLoadingInfo;

		public LoadImageTask(DownloadHandle imageLoadingInfo) {
			this.imageLoadingInfo = imageLoadingInfo;
		}

		@Override
		public Bitmap call() throws Exception {
			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			Bitmap bmp = null;

			try {
				bmp = loadBitmap(imageLoadingInfo);
			} catch (Exception e) {
				if (DEBUG) Log.exception(TAG, Message.DEBUG, String.format("Exception while loading bitmap from URL=%s : %s", imageLoadingInfo.url, e.getMessage()), e);

				fireImageLoadingFailedEvent(new FailReason(e));

				throw e;
			}

			if (!imageLoadingInfo.isConsistent()) {
				throw new Exception("Not consistent");
			}

			Bitmap oldBitmap = bmp;

			final Bitmap bmpToProcess = bmp;

			if (imageLoadingInfo.processor != null) {
				bmp = executorForCpu.submit(new Callable<Bitmap>() {
					@Override
					public Bitmap call() throws Exception {
						Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

						return imageLoadingInfo.processor.progressImage(bmpToProcess);
					}
				}).get(); 
			} else {
				if (defaultImageProcessor != null) {
					bmp = executorForCpu.submit(new Callable<Bitmap>() {
						@Override
						public Bitmap call() throws Exception {
							Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

							return defaultImageProcessor.progressImage(bmpToProcess);
						}
					}).get(); 
				}
			}

			if (oldBitmap != bmp) oldBitmap.recycle();

			if (!imageLoadingInfo.isConsistent()) {
				throw new Exception("Not consistent");
			}

			if (bmp != null && !Thread.currentThread().isInterrupted()) {
				if (imageLoadingInfo.options.isCacheInMemory()) {
					configuration.memoryCache.put(imageLoadingInfo.getMemoryCacheKey(), bmp);
				}

				fireImageLoadingCompleteEvent(imageLoadingInfo.url, bmp);
			}

			return bmp;

		}

		private void fireImageLoadingCompleteEvent(String url, Bitmap bitmap) {
			synchronized (imageLoadingInfo.listeners) {
				for (ImageLoadCallbacks listener : imageLoadingInfo.listeners) {
					listener.onLoadingComplete(url, bitmap);
				}
			}			
		}

		private void fireImageLoadingFailedEvent(final FailReason failReason) {
			synchronized (imageLoadingInfo.listeners) {
				for (ImageLoadCallbacks listener : imageLoadingInfo.listeners) {
					listener.onLoadingFailed(failReason);
				}
			}
		}
	}

	private class EmptyListener implements ImageLoadCallbacks {
		@Override
		public void onLoadingStarted() {
		}

		@Override
		public void onLoadingFailed(FailReason failReason) {
		}

		@Override
		public void onLoadingComplete(String url, Bitmap bitmap) {
		}
	}
}
