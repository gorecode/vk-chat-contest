package com.gorecode.vk.activity.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.common.base.Preconditions;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.audio.AudioPlayer.Playlist;
import com.gorecode.vk.audio.AudioPlayer.State;
import com.gorecode.vk.data.Audio;

public class AudiosAttachmentView extends LinearLayout {
	private AudioPlayer mAudioPlayer;

	private Playlist mPlaylist;

	private final List<AudioAttachmentView> mAudioViews = new ArrayList<AudioAttachmentView>();

	public AudiosAttachmentView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setAudioPlayer(AudioPlayer player) {
		Preconditions.checkNotNull(player);

		mAudioPlayer = player;
	}

	public Playlist getPlaylist() {
		return mPlaylist;
	}

	public void setPlaylist(Playlist playlist) {
		mPlaylist = playlist;

		updateViews();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (isInEditMode()) return;

		mAudioPlayer.addListener(mAudioPlayerListener);
	}

	@Override
	protected void onDetachedFromWindow() {
		if (isInEditMode()) return;

		mAudioPlayer.removeListener(mAudioPlayerListener);

		super.onDetachedFromWindow();
	}

	private void updateViews() {
		List<Audio> audios = Collections.emptyList();

		if (mPlaylist != null) {
			audios = mPlaylist.audios;
		}

		ViewGroup layout = this;

		mAudioViews.clear();

		if (audios.size() != 0) {
			layout.setVisibility(View.VISIBLE);

			final int total = audios.size();

			for (int i = 0; i < Math.max(total, layout.getChildCount()); i++) {
				if (i < total) {
					Audio audio = audios.get(i);

					AudioAttachmentView view = (AudioAttachmentView)layout.getChildAt(i);

					if (view == null) {
						view = new AudioAttachmentView(getContext(), mAudioPlayer);

						view.setOnPlaybackControlClickListener(mOnPlaybackClick);

						layout.addView(view);
					}

					view.setVisibility(VISIBLE);
					view.setAudio(audio);

					mAudioViews.add(view);
				} else {
					layout.getChildAt(i).setVisibility(GONE);
				}
			}
		} else {
			layout.setVisibility(View.GONE);
		}				
	}

	private final AudioAttachmentView.OnPlaybackControlClickListener mOnPlaybackClick = new AudioAttachmentView.OnPlaybackControlClickListener() {
		@Override
		public void onPlaybackControlClick(AudioAttachmentView view) {
			AudioPlayer player = mAudioPlayer;

			Audio track = view.getAudio();

			if (AudioPlayer.Playlist.isSame(player.getPlaylist(), mPlaylist) && Audio.isSame(track, mAudioPlayer.getCurrentTrack())) {
				AudioPlayer.State playerState = player.getState();

				if (playerState == AudioPlayer.State.PLAYING) {
					player.pause();
				} else {
					player.play();
				}
			} else {
				player.setPlaylist(mPlaylist);
				player.setCurrentTrackById(track.aid);
				player.play();
			}
		} // void
	};

	private final AudioPlayer.Listener mAudioPlayerListener = new AudioPlayer.Listener() {
		@Override
		public void onStateChanged(AudioPlayer player, State state) {
			for (AudioAttachmentView view : mAudioViews) {
				view.onStateChanged(player, state);
			}
		}

		@Override
		public void onCurrentTrackChanged(AudioPlayer player, int currentTrackIndex) {
			for (AudioAttachmentView view : mAudioViews) {
				view.onCurrentTrackChanged(player, currentTrackIndex);				
			}
		}

		@Override
		public void onBufferingUpdate(AudioPlayer player, int percent) {
			for (AudioAttachmentView view : mAudioViews) {
				view.onBufferingUpdate(player, percent);				
			}
		}
	};
}
