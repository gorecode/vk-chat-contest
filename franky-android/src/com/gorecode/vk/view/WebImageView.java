package com.gorecode.vk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.gorecode.vk.R;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.ImageUrls;
import com.gorecode.vk.imageloader.FailReason;
import com.gorecode.vk.imageloader.ImageLoadCallbacks;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.imageloader.ImageProcessor;
import com.uva.lang.ObjectUtilities;

public class WebImageView extends ImageView {
	private static final int DEFAULT_NO_IMAGE_RESOURCE = R.drawable.contact_nophoto;
	private static final int DEFAULT_LOAD_IMAGE_RESOURCE = R.drawable.contact_nophoto;

	private ImageLoader imgLoader;

	private String imageUrl;

	private int noImageResource;
	private int loadImageResource;

	private boolean isDownloadPending;

	public WebImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray styled = context.obtainStyledAttributes(attrs, R.styleable.WebImageView);

		noImageResource = styled.getResourceId(R.styleable.WebImageView_noImageResource, DEFAULT_NO_IMAGE_RESOURCE);
		loadImageResource = styled.getResourceId(R.styleable.WebImageView_loadImageResource, DEFAULT_LOAD_IMAGE_RESOURCE);

		styled.recycle();

		setUpViews();
	}

	public void setImageBitmap(Bitmap bitmap) {
		setImageUrl(null, null);

		if(bitmap != null) {
			super.setImageBitmap(bitmap);
		} else {
			super.setImageResource(noImageResource);
		}
	}

	public void setImageLoader(ImageLoader imageLoader) {
		imgLoader = imageLoader;
	}

	public void setImageUrl(String imageUrl) {
		setImageUrl(imageUrl, null);
	}

	public void setImageUrl(String imageUrl, ImageProcessor processor) {
		if (ObjectUtilities.equals(this.imageUrl, imageUrl) && isDownloadPending) {
			return;
		}

		this.imageUrl = imageUrl;

		if (imageUrl == null) {
			super.setImageResource(noImageResource);

			isDownloadPending = false;
		} else {
			isDownloadPending = true;

			setImageResource(loadImageResource);

			getImageLoader().loadImageForImageView(imageUrl, this, imgLoaderCallbacks, processor);
		}		
	}

	public void setImageUrls(ImageUrls imageUrls) {
		setImageUrls(imageUrls, null);
	}

	public void setImageUrls(ImageUrls imageUrls, ImageProcessor processor) {
		setImageUrl(imageUrls != null ? imageUrls.previewUrl : null, processor);
	}

	public String getImageUrl() {
		return imageUrl;
	}

	private ImageLoader getImageLoader() {
		if (imgLoader == null) {
			imgLoader = VkApplication.from(getContext()).getImageLoader();
		}
		return imgLoader;
	}

	private void setUpViews() {
		setImageResource(noImageResource);
	}

	private final ImageLoadCallbacks imgLoaderCallbacks = new ImageLoadCallbacks() {
		@Override
		public void onLoadingStarted() {
			;
		}

		@Override
		public void onLoadingFailed(final FailReason failReason) {
			VkApplication.from(getContext()).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					isDownloadPending = false;
				}
			});
		}

		@Override
		public void onLoadingComplete(final String url, final Bitmap bitmap) {
			VkApplication.from(getContext()).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!ObjectUtilities.equals(url, WebImageView.this.imageUrl)) return;

					isDownloadPending = false;

					setImageBitmap(bitmap);
				}
			});
		}
	};
}
