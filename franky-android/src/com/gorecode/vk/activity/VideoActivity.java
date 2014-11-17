package com.gorecode.vk.activity;

import roboguice.RoboGuice;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.gorecode.vk.R;
import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.data.Video;
import com.gorecode.vk.event.VideoPlaybackStateChangedEvent;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.task.LongActionContext;
import com.uva.log.Log;

@ContentView(R.layout.video_activity)
public class VideoActivity extends VkActivity {
	private static final String TAG = VideoActivity.class.getSimpleName();

	private static final String EXTRA_VIDEO = "videoId";

	private static final String PREFERENCE_POSITION = "position";

	@InjectView(R.id.video)
	private VideoView mVideoView;

	@Inject
	private SharedPreferences mPreferences;
	@Inject
	private EventBus mBus;

	private MediaController mMediaController;

	private Video mVideo;
	private Uri mVideoUri;

	public static void openVideo(final Context context, final Video video) {
		LongAction<Void, Video> action = new LongAction<Void, Video>(context) {
			@Override
			protected Video doInBackgroundOrThrow(Void params) throws Exception {
				if (video.hasUrls()) {
					return video;
				}

				// XXX: Special "VKontakte" magic.

				long ownerId = video.ownerId;

				if (GroupChatDescriptor.isGroupChatUid(ownerId)) {
					ownerId = -GroupChatDescriptor.convertUidToChatId(ownerId);
				}

				String query = String.format("%d_%d", ownerId, video.vid);

				VkModel vk = RoboGuice.getInjector(context).getInstance(VkModel.class);

				Video videoDetails = vk.getVideo(query, null, null, null, null).get(0);

				if (!videoDetails.hasUrls()) {
					throw new Exception("Cannot play video cause it has no direct urls");
				}

				return videoDetails;
			}

			@Override
			protected void onComplete(LongActionContext<Void, Video> executionResult) {
				if (executionResult.isCompletedSuccessfuly()) {
					((Activity)context).startActivity(getDisplayIntent(context, executionResult.result));
				} else {
					Log.exception(TAG, "Error getting video information", executionResult.error);
				}
			}
		};
		action.wrapWithProgress(true);
		action.execute();		
	}

	public static Intent getDisplayIntent(Context context, Video video) {
		if (video.urlExternal != null) {
			return new Intent(Intent.ACTION_VIEW, Uri.parse(video.urlExternal));
		} else {
			Intent intent = new Intent(context, VideoActivity.class);
			intent.putExtra(EXTRA_VIDEO, video);
			return intent;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();

		mMediaController = new MediaController(this);

		mVideoView.setMediaController(mMediaController);
		mVideoView.setOnErrorListener(mOnErrorHandler);

		mPreferences.edit().putInt(PREFERENCE_POSITION, 0).commit();

		if (extras.containsKey(EXTRA_VIDEO)) {
			mVideo = (Video)extras.getSerializable(EXTRA_VIDEO);

			if (mVideo.url480 != null) {
				setVideoUrl(mVideo.url480);
			} else if (mVideo.url320 != null) {
				setVideoUrl(mVideo.url320);
			} else if (mVideo.url240 != null) {
				setVideoUrl(mVideo.url240);
			} else if (mVideo.url720 != null) {
				setVideoUrl(mVideo.url720);
			} else {
				finish();
			}
		} else {
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		int position = mPreferences.getInt(PREFERENCE_POSITION, 0);

		mVideoView.seekTo(position);
		mVideoView.start();

		mMediaController.show();

		mBus.post(new VideoPlaybackStateChangedEvent(true));
	}

	@Override
	protected void onPause() {
		super.onPause();

		int position = mVideoView.getCurrentPosition();

		mPreferences.edit().putInt(PREFERENCE_POSITION, position).commit();

		mBus.post(new VideoPlaybackStateChangedEvent(false));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_video, menu);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.quality_240).setVisible(mVideo.url240 != null);
		menu.findItem(R.id.quality_320).setVisible(mVideo.url320 != null);
		menu.findItem(R.id.quality_480).setVisible(mVideo.url480 != null);
		menu.findItem(R.id.quality_720).setVisible(mVideo.url720 != null);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.quality_240:
			setVideoUrl(mVideo.url240);
			return true;
		case R.id.quality_320:
			setVideoUrl(mVideo.url320);
			return true;
		case R.id.quality_480:
			setVideoUrl(mVideo.url480);
			return true;
		case R.id.quality_720:
			setVideoUrl(mVideo.url720);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setVideoUrl(String videoUrl) {
		setVideoUri(Uri.parse(videoUrl));
	}

	private void setVideoUri(Uri videoUri) {
		if (Objects.equal(mVideoUri, videoUri)) return;

		final int prevPosition = mVideoView.getCurrentPosition();

		mVideoUri = videoUri;

		mVideoView.setVideoURI(mVideoUri);

		if (prevPosition > 0) {
			mVideoView.seekTo(prevPosition);
			mVideoView.start();
		}
	}

	private final MediaPlayer.OnErrorListener mOnErrorHandler = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Toast.makeText(VideoActivity.this, R.string.error_playing_video, Toast.LENGTH_SHORT).show();

			return false;
		}
	};
}
