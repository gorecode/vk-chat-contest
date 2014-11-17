package com.gorecode.vk.imageloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.WindowManager;

import com.google.inject.Inject;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.utilities.BitmapUtilities;
import com.uva.io.StreamUtilities;
import com.uva.lang.StringUtilities;
import com.uva.log.Log;

public class GroupChatImageLoader implements ImageLoader.ImageFetcher {
	private static final String TAG = GroupChatImageLoader.class.getSimpleName();

	public static final String GROUP_CHAT_SCHEME = "groupChat";

	private final ImageLoader mImageLoader;
	private final WindowManager mWindowManager;

	@Inject
	public GroupChatImageLoader(Context context, ImageLoader imageLoader) {
		mImageLoader = imageLoader;
		mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
	}

	public static String getGroupChatUri(List<Profile> users) {
		String[] urls = new String[Math.min(4, users.size())];

		for (int i = 0; i < urls.length; i++) {
			urls[i] = users.get(i).avatarUrls.previewUrl;
		}

		return getGroupChatUri(urls);
	}

	public static String getGroupChatUri(String[] urls) {
		return StringUtilities.join(";", urls);
	}

	@Override
	public boolean canFetch(String url) {
		String[] urls = url.split(";");

		return (urls.length > 1);
	}

	@Override
	public void fetchImage(String url, File outFile) throws Exception {
		String[] urls = url.split(";");

		ArrayList<Future<Bitmap>> futures = new ArrayList<Future<Bitmap>>();

		for (int i = 0; i < Math.min(4, urls.length); i++) {
			String subimageUrl = urls[i];

			Log.trace(TAG, "Start fetching subimage = " + subimageUrl);

			futures.add(mImageLoader.loadImage(subimageUrl, null));
		}

		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

		try {
			try {
				Log.debug(TAG, "Waiting for subimages fetching to complete");

				for (Future<Bitmap> future : futures) {
					bitmaps.add(future.get());
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			} 
		} catch (Exception e) {
			Log.trace(TAG, "Error loading subimages, canceling all subtasks");

			for (Future<Bitmap> future : futures) {
				future.cancel(true);
			}
			throw e;
		}

		Log.debug(TAG, "Group chat subimages are fetched");

		Bitmap bmp = BitmapUtilities.createGroupChatPhoto(bitmaps.toArray(new Bitmap[bitmaps.size()]), mWindowManager);

		ByteArrayInputStream is = new ByteArrayInputStream(BitmapUtilities.compressPhotoToPNG(bmp));

		FileOutputStream os = new FileOutputStream(outFile);

		try {
			StreamUtilities.copyStream(is, os);
		} finally {
			os.close();
		}
	}
}
