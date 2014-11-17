package com.gorecode.vk.event;

import com.gorecode.vk.data.Profile;

public class FriendshipRejectedEvent {
	private final Profile mUser;

	public FriendshipRejectedEvent(Profile user) {
		mUser = user;
	}

	public Profile getSender() {
		return mUser;
	}
}
