package com.gorecode.vk.event;

public class VideoPlaybackStateChangedEvent {
	private final boolean mIsPlaying;

	public VideoPlaybackStateChangedEvent(boolean isPlaying) {
		mIsPlaying = isPlaying;
	}

	public boolean isPlaying() {
		return mIsPlaying;
	}
}
