package com.gorecode.vk.activity.chat;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.audio.AudioPlayer.State;
import com.gorecode.vk.data.Audio;

public class AudioAttachmentView extends LinearLayout implements AudioPlayer.Listener, SeekBar.OnSeekBarChangeListener {
	public static interface OnPlaybackControlClickListener {
		public void onPlaybackControlClick(AudioAttachmentView view);
	}

	private ImageView mControlPlaybackView;
	private TextView mAuthorView;
	private TextView mTitleView;
	private SeekBar mSeekBar;

	private OnPlaybackControlClickListener mOnPlaybackControlClickListener;

	private final AudioPlayer mPlayer;

	private Audio mAudio;

	public AudioAttachmentView(Context context, AudioPlayer player) {
		super(context, null);

		inflate(context, R.layout.audio_attachment, this);

		mPlayer = player;

		setUpViews();
	}

	public void setAudio(Audio audio) {
		mAudio = audio;

		updateViews();
	}

	public Audio getAudio() {
		return mAudio;
	}

	public void setOnPlaybackControlClickListener(OnPlaybackControlClickListener listener) {
		mOnPlaybackControlClickListener = listener;
	}

	private void onControlPlaybackButtonClicked() {
		if (mOnPlaybackControlClickListener != null) {
			mOnPlaybackControlClickListener.onPlaybackControlClick(this);
		}
	}

	private void updateProgressViews() {
		if (Audio.isSame(mAudio, mPlayer.getCurrentTrack())) {
			int currentPositionInSeconds = (int)(mPlayer.getCurrentPosition() / 1000);
			int durationInSeconds = Math.max((int)mAudio.duration, 1);

			mSeekBar.setEnabled(true);
			mSeekBar.setProgress(Math.min(mSeekBar.getMax(), Math.round((currentPositionInSeconds * 100) / durationInSeconds)));
			mSeekBar.setSecondaryProgress(Math.min(mSeekBar.getMax(), mPlayer.getBufferingPercent()));

			AudioPlayer.State playerState = mPlayer.getState();

			if (playerState == AudioPlayer.State.PREPARING_FOR_PLAYBACK) {
				cannotPressPlayAndPause();
			} else if (playerState == AudioPlayer.State.PLAYING) {
				canPressPause();
			} else {
				canPressPlay();
			}
		} else {
			mSeekBar.setEnabled(false);
			mSeekBar.setProgress(0);
			mSeekBar.setSecondaryProgress(0);

			canPressPlayOnly();
		}		
	}

	private void updateViews() {
		if (mAudio == null) {
			return;
		}

		mAuthorView.setText(mAudio.artist);

		mTitleView.setText(mAudio.title);

		updateProgressViews();
	}

	private void canPressPlayOnly() {
		mControlPlaybackView.setImageResource(R.drawable.audio_play);
		mControlPlaybackView.setEnabled(true);
		mSeekBar.setEnabled(false);
	}

	private void canPressPlay() {
		canPressPlayOnly();

		mSeekBar.setEnabled(true);
	}

	private void canPressPause() {
		mControlPlaybackView.setImageResource(R.drawable.audio_pause);
		mControlPlaybackView.setEnabled(true);
		mSeekBar.setEnabled(true);
	}

	private void cannotPressPlayAndPause() {
		mControlPlaybackView.setImageResource(R.drawable.audio_pause);
		mControlPlaybackView.setEnabled(false);
		mSeekBar.setEnabled(false);
	}

	private void setUpViews() {
		setDuplicateParentStateEnabled(true);

		mControlPlaybackView = (ImageView)findViewById(R.id.play_or_pause_button);
		mControlPlaybackView.setOnClickListener(mOnClickHandler);
		mAuthorView = (TextView)findViewById(R.id.author);
		mTitleView = (TextView)findViewById(R.id.title);
		mSeekBar = (SeekBar)findViewById(R.id.progress);
		mSeekBar.setMax(100);
		mSeekBar.setOnSeekBarChangeListener(this);
	}

	private final View.OnClickListener mOnClickHandler = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v == mControlPlaybackView) {
				onControlPlaybackButtonClicked();
			}
		}
	};

	@Override
	public void onStateChanged(AudioPlayer player, State state) {
		updateViews();
	}

	@Override
	public void onBufferingUpdate(AudioPlayer player, int percent) {
		updateProgressViews();
	}

	@Override
	public void onCurrentTrackChanged(AudioPlayer player, int currentTrackIndex) {
		;
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (seekBar != mSeekBar) return;

		if (fromUser) {
			final int seekPositionInSeconds = (progress * (int)mAudio.duration) / 100;

			mPlayer.seekTo(seekPositionInSeconds * 1000);

			updateViews();
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		;
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		;
	}
}
