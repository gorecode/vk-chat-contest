package com.gorecode.vk.activity.friends;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Profile;

public class FriendListItem {
	private final Profile mFriend;
	private final String mSeparatorText;

	private FriendListItem(Profile friend, String separatorText) {
		Preconditions.checkArgument(friend != null || separatorText != null);

		mFriend = friend;
		mSeparatorText = separatorText;
	}

	public Profile getFriend() {
		return mFriend;
	}

	public String getSeparatorText() {
		return mSeparatorText;
	}

	public static FriendListItem newSeparator(Profile nextFriend) {
		char firstLetter = nextFriend.getFullname().toUpperCase().charAt(0);

		return new FriendListItem(null, String.valueOf(firstLetter));
	}

	public static FriendListItem newFriend(Profile friend) {
		return new FriendListItem(friend, null);
	}

	public boolean isFriend() {
		return mFriend != null;
	}

	public boolean isSeparator() {
		return mSeparatorText != null;
	}
}
