package com.gorecode.vk.audio;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.event.VideoPlaybackStateChangedEvent;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class AudioPauseController extends PhoneStateListener {
	private final AudioPlayer mPlayer;

	private boolean mPausedByCall;
	private boolean mPausedByVideo;

	@Inject
	public AudioPauseController(Context context, AudioPlayer player) {
		TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

		telephony.listen(this, PhoneStateListener.LISTEN_CALL_STATE);

		mPlayer = player;
	}

	@Subscribe
	public void onVideoPlaybackStateChanged(VideoPlaybackStateChangedEvent event) {
		if (event.isPlaying() && !mPausedByVideo) {
			mPausedByVideo = mPlayer.pause();
		}
		if (!event.isPlaying() && mPausedByVideo) {
			mPausedByVideo = false;

			if (!mPausedByCall) {
				mPlayer.play();
			}
		}
	}

	@Override
	public void onCallStateChanged(int callState, String phoneNumber) {
		if (callState != TelephonyManager.CALL_STATE_IDLE && !mPausedByCall) {
			mPausedByCall = mPlayer.pause();
		}
		if (callState == TelephonyManager.CALL_STATE_IDLE && mPausedByCall) {
			mPausedByCall = false;

			if (!mPausedByVideo) {
				mPlayer.play();
			}
		}
	}
}
