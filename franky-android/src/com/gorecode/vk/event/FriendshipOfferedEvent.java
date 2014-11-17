package com.gorecode.vk.event;

import com.gorecode.vk.data.Profile;

public class FriendshipOfferedEvent {
	private final Profile mSender;

	public FriendshipOfferedEvent(Profile sender) {
		mSender = sender;
	}

	public Profile getSender() {
		return mSender;
	}
}
