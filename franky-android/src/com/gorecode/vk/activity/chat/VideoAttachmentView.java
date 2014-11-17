package com.gorecode.vk.activity.chat;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.activity.VideoActivity;
import com.gorecode.vk.data.Video;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.WebImageView;

public class VideoAttachmentView extends LinearLayout {
	private WebImageView mThumbView;
	private TextView mDurationView;

	private Video mVideo;

	private final ImageLoader mImgLoader;

	public VideoAttachmentView(Context context, ImageLoader imgLoader) {
		super(context, null);

		mImgLoader = imgLoader;

		inflate(context, R.layout.video_attachment, this);

		setUpViews();

		setDuplicateParentStateEnabled(true);
	}

	public void setVideo(Video video) {
		mVideo = video;

		updateViews();
	}

	public Video getVideo() {
		return mVideo;
	}

	private void onThumbClicked() {
		Context context = getContext();

		VideoActivity.openVideo(context, mVideo);
	}

	private void updateViews() {
		mThumbView.setImageUrl(mVideo.image);
		mDurationView.setText(formatDuration((int)(mVideo.duration)));
	}

	private void setUpViews() {
		mThumbView = (WebImageView)findViewById(R.id.video_thumb);
		mThumbView.setImageLoader(mImgLoader);
		mThumbView.setOnClickListener(mOnClickHandler);
		mDurationView = (TextView)findViewById(R.id.video_length);
	}

	private static String formatDuration(int durationInSeconds) {
		String result = "";

		int days = 0, hours = 0, minutes = 0, seconds = 0;

		days = durationInSeconds / (3600 * 24);
		hours = durationInSeconds / 3600;
		minutes = (durationInSeconds - hours * 3600) / 60;
		seconds = (durationInSeconds - (hours * 3600 + minutes * 60));

		if (days > 0) {
			result = String.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds);
		} else if (hours > 0) {
			result = String.format("%d:%02d:%02d", hours, minutes, seconds);
		} else if (minutes > 0) {
			result = String.format("%d:%02d", minutes, seconds);
		} else {
			result = String.format("%d", seconds);
		}

		return result;
	}

	private final View.OnClickListener mOnClickHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == mThumbView) {
				onThumbClicked();
			}
		}
	};
}
