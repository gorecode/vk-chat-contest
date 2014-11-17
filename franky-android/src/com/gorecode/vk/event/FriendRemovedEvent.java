package com.gorecode.vk.event;

import com.gorecode.vk.data.Profile;

public class FriendRemovedEvent {
	private final Profile mFriend;

	public FriendRemovedEvent(Profile friend) {
		mFriend = friend;
	}

	public Profile getFriend() {
		return mFriend;
	}
}
