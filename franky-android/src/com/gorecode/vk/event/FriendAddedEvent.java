package com.gorecode.vk.event;

import com.gorecode.vk.data.Profile;

public class FriendAddedEvent {
	private final Profile mFriend;

	public FriendAddedEvent(Profile friend) {
		mFriend = friend;
	}

	public Profile getFriend() {
		return mFriend;
	}
}
