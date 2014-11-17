package com.gorecode.vk.audio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.gorecode.vk.activity.StopAudioPlaybackActivity;
import com.gorecode.vk.audio.AudioPlayer.Playlist;
import com.gorecode.vk.audio.AudioPlayer.State;
import com.gorecode.vk.data.Audio;

public class AudioNotificationController implements AudioPlayer.Listener {
	private static final int NOTIFICATION_ID = 0x667;

	private final Context mContext;
	private final NotificationManager mNotificationService;
	private final AudioPlayer mPlayer;

	private Audio mLastNotificationTrack;

	@Inject
	public AudioNotificationController(Context context, AudioPlayer player) {
		mContext = context;
		mNotificationService = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		mPlayer = player;
	}

	@Override
	public void onStateChanged(AudioPlayer player, State state) {
		updateNotification();
	}

	@Override
	public void onBufferingUpdate(AudioPlayer player, int percent) {
		;
	}

	@Override
	public void onCurrentTrackChanged(AudioPlayer player, int currentTrackIndex) {
		;
	}

	private void updateNotification() {
		AudioPlayer player = mPlayer;

		AudioPlayer.State state = player.getState();

		boolean isNotGoingToPlayMusic = false;

		Playlist plist = player.getPlaylist();

		if (plist == null) {
			isNotGoingToPlayMusic = true;
		} else if (state == State.PAUSED) {
			isNotGoingToPlayMusic = true;
		} else {
			if (state == State.PLAYBACK_COMPLETED || state == State.ERROR && player.getCurrentTrackIndex() == plist.audios.size() - 1) {
				isNotGoingToPlayMusic = true;
			}
		}

		if (isNotGoingToPlayMusic) {
			if (mLastNotificationTrack != null) {
				mNotificationService.cancel(NOTIFICATION_ID);

				mLastNotificationTrack = null;
			}
		} else {
			if (mLastNotificationTrack != mPlayer.getCurrentTrack()) {
				Audio track = mPlayer.getCurrentTrack();

				mLastNotificationTrack = track;

				String tickerText = "";

				if (!Strings.isNullOrEmpty(track.artist) && !Strings.isNullOrEmpty(track.title)) {
					tickerText = track.artist + " - " + track.title;
				} else {
					if (track.artist != null) {
						tickerText = track.artist;
					} else {
						tickerText = track.title;
					}
				}

				Notification notification = new Notification(R.drawable.application_icon, tickerText, System.currentTimeMillis());

				String contentTitle = Strings.isNullOrEmpty(track.artist) ? mContext.getString(R.string.playback_notification_default_content_title) : track.artist;
				String contentText =  Strings.isNullOrEmpty(track.title) ? mContext.getString(R.string.playback_notification_default_content_text) : track.title;
				Intent contentIntent = new Intent(mContext, StopAudioPlaybackActivity.class);
				contentIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND);

				notification.setLatestEventInfo(mContext, contentTitle, contentText, PendingIntent.getActivity(mContext, 0, contentIntent, 0));

				mNotificationService.notify(NOTIFICATION_ID, notification);
			}
		}
	}
}
