package com.gorecode.vk.audio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.media.AudioManager;
import android.media.MediaPlayer;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.gorecode.vk.data.Audio;
import com.gorecode.vk.data.ChatMessage;
import com.uva.log.Log;
import com.uva.utilities.ObserverCollection;

@Singleton
public class AudioPlayer implements MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener {
	public static interface Listener {
		public void onStateChanged(AudioPlayer player, State state);
		public void onBufferingUpdate(AudioPlayer player, int percent);
		public void onCurrentTrackChanged(AudioPlayer player, int currentTrackIndex);
	}

	public static class Playlist {
		public final long id;
		public final List<Audio> audios;

		public Playlist(long id, List<Audio> audios) {
			Preconditions.checkNotNull(audios);

			this.id = id;
			this.audios = audios;
		}

		public static boolean isSame(Playlist p1, Playlist p2) {
			if ((p1 == null) || (p2 == null)) return false;

			return (p1.id == p2.id);
		}

		public static Playlist fromMessage(ChatMessage message) {
			return new Playlist(message.id, message.content.audios);
		}
	}

	public static enum State {
		RESET,
		PREPARING_FOR_PLAYBACK,
		PLAYING,
		PAUSED,
		ERROR,
		PLAYBACK_COMPLETED;
	}

	private static final String TAG = AudioPlayer.class.getSimpleName();

	private static final boolean DEBUG = true;

	private static final Map<Integer, String> MEDIA_ERRORS_DESCRIPTION = new HashMap<Integer, String>();

	static {
		MEDIA_ERRORS_DESCRIPTION.put(MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK, "The video is streamed and its container is not valid for progressive playback i.e the video's index (e.g moov atom) is not at the start of the file.");
		MEDIA_ERRORS_DESCRIPTION.put(MediaPlayer.MEDIA_ERROR_SERVER_DIED, "Media server died.");
		MEDIA_ERRORS_DESCRIPTION.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, "Unspecified media player error.");
	}

	private final MediaPlayer mPlayer = new MediaPlayer();

	private ObserverCollection<Listener> mObservers = new ObserverCollection<AudioPlayer.Listener>();

	private int mBufferingPercent;
	private int mSeekPosition = -1;

	private State mState;

	private Playlist mPlaylist;

	private int mCurrentTrackIndex = -1;

	public AudioPlayer() {
		mState = State.RESET;

		mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mPlayer.setOnErrorListener(this);
		mPlayer.setOnPreparedListener(this);
		mPlayer.setOnSeekCompleteListener(this);
		mPlayer.setOnCompletionListener(this);
		mPlayer.setOnBufferingUpdateListener(this);
	}

	public void addListener(Listener listener) {
		mObservers.add(listener);
	}

	public void removeListener(Listener listener) {
		mObservers.remove(listener);
	}

	public int getBufferingPercent() {
		return mBufferingPercent;
	}

	public State getState() {
		return mState;
	}

	public Audio getCurrentTrack() {
		if (mCurrentTrackIndex == -1) {
			return null;
		}
		return mPlaylist.audios.get(mCurrentTrackIndex);
	}

	public int getCurrentTrackIndex() {
		return mCurrentTrackIndex;
	}

	public Playlist getPlaylist() {
		return mPlaylist;
	}

	public void reset() {
		mPlaylist = null;
		mCurrentTrackIndex = -1;
		mPlayer.reset();
		setState(State.RESET);
	}

	public void setPlaylist(Playlist playlist) {
		mPlaylist = playlist;

		mCurrentTrackIndex = mPlaylist.audios.size() - 1;
	}

	public boolean setCurrentTrackById(long audioId) {
		int index = -1;

		for (int i = 0; i < mPlaylist.audios.size(); i++) {
			Audio audio = mPlaylist.audios.get(i);

			if (audio.aid == audioId) {
				index = i;
				break;
			}
		}

		if (index == -1) return false;

		setCurrentTrackByIndex(index);

		return true;
	}

	public void setCurrentTrackByIndex(int index) {
		Preconditions.checkState(mPlaylist != null);
		Preconditions.checkState(mPlaylist.audios.size() > 0);
		Preconditions.checkArgument(index >= 0 && index < mPlaylist.audios.size());

		mPlayer.reset();

		mCurrentTrackIndex = index;

		setState(State.RESET);

		notifyCurrentTrackChanged();
	}

	public long getCurrentPosition() {
		if (mState == State.ERROR) return 0;

		try {
			if (mSeekPosition != -1) {
				return mSeekPosition;
			}
			return mPlayer.getCurrentPosition();
		} catch (Exception e) {
			Log.exception(TAG, "Error during MediaPlayer.getCurrentPosition() call", e);

			return 0;
		}
	}

	public void dispose() {
		mPlayer.release();
	}

	public boolean play() {
		try {
			if (mState == State.ERROR || mState == State.RESET) {
				if (mState == State.ERROR) {
					setState(State.RESET);
				}

				Audio audio = getCurrentTrack();

				if (audio == null) {
					Log.warning(TAG, "No url to play");

					return false;
				}

				if (DEBUG) {
					Log.debug(TAG, "reseting MediaPlayer");
				}

				mPlayer.reset();
				mPlayer.setDataSource(audio.url);
				mPlayer.prepareAsync();

				setState(State.PREPARING_FOR_PLAYBACK);

				return true;
			}

			if (mState == State.PLAYING || mState == State.PREPARING_FOR_PLAYBACK) {
				return true;
			}

			if (mState == State.PAUSED || mState == State.PLAYBACK_COMPLETED) {
				mPlayer.start();

				setState(State.PLAYING);

				return true;
			}

			return false;
		} catch (Exception e) {
			Log.exception(TAG, "Error during preparing MediaPlayer for playback", e);

			return false;
		}
	}

	public boolean pause() {
		if (mState != State.PLAYING && mState != State.PAUSED) return false;

		try {
			mPlayer.pause();

			setState(State.PAUSED);
		} catch (Exception e) {
			Log.exception(TAG, "Error during MediaPlayer.pause() call", e);

			return false;
		}

		return true;
	}

	public boolean seekTo(int msec) {
		if (mState == State.RESET || mState == State.ERROR || mState == State.PREPARING_FOR_PLAYBACK) {
			return false;
		}

		if (DEBUG) {
			Log.debug(TAG, String.format("seekTo(%d)", msec));
		}

		try {
			mPlayer.seekTo(msec);

			mSeekPosition = msec;
		} catch (Exception e) {
			Log.exception(TAG, "Error during MediaPlayer.seekTo() call", e);

			return false;
		}

		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		if (mp != mPlayer) return false;

		Log.error(TAG, String.format("AudioPlayer error (code = %d, extra = %d, description = %s", what, extra, MEDIA_ERRORS_DESCRIPTION.get(what)));

		setState(State.ERROR);

		playNextTrackIfAvailable();

		return true;
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		if (mp != mPlayer) return;

		try {
			mPlayer.start();

			setState(State.PLAYING);
		} catch (Exception e) {
			Log.exception(TAG, "Error during MediaPlayer.start() call", e);
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		if (mp != mPlayer) return;

		setBufferingPercent(percent + 1);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		setState(State.PLAYBACK_COMPLETED);

		playNextTrackIfAvailable();
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (mp != mPlayer) return;

		workaroundAndroidIssue4124();

		if (DEBUG) {
			Log.debug(TAG, "seek completed");
		}
	}

	private void setState(State state) {
		if (DEBUG) {
			Log.debug(TAG, "state = " + state);
		}

		if (state == State.RESET) {
			mBufferingPercent = 0;
			mSeekPosition = -1;
		}

		mState = state;

		notifyStateChanged();
	}

	private void playNextTrackIfAvailable() {
		if (mPlaylist == null) {
			return;
		}

		final int nextIndex = mCurrentTrackIndex + 1;

		if (nextIndex < mPlaylist.audios.size()) {
			setCurrentTrackByIndex(nextIndex);

			play();
		}
	}

	private void setBufferingPercent(int percent) {
		if (DEBUG) {
			Log.debug(TAG, "buffering percent changed to " + percent);
		}

		workaroundAndroidIssue4124();

		mBufferingPercent = percent;

		notifyBufferingUpdated();
	}

	private void workaroundAndroidIssue4124() {
		if (mSeekPosition == -1) return;

		final int currentPosition = mPlayer.getCurrentPosition();
		final int eps = mPlayer.getDuration() / 50;

		if (Math.abs(currentPosition - mSeekPosition) <= eps) {
			mSeekPosition = -1;
		}		
	}

	private void notifyBufferingUpdated() {
		for (Listener observer : mObservers) {
			observer.onBufferingUpdate(this, mBufferingPercent);
		}
	}

	private void notifyCurrentTrackChanged() {
		for (Listener observer : mObservers) {
			observer.onCurrentTrackChanged(this, mCurrentTrackIndex);
		}
	}

	public void notifyStateChanged() {
		for (Listener observer : mObservers) {
			observer.onStateChanged(this, mState);
		}
	}
}
