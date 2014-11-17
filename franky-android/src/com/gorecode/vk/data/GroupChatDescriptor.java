package com.gorecode.vk.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

public class GroupChatDescriptor implements Serializable {
	private static final long GROUP_CHAT_UID_OFFSET = 200000000;

	private static final long serialVersionUID = -286025429388386055L;

	public static boolean isGroupChatUid(long uid) {
		return uid > GROUP_CHAT_UID_OFFSET;
	}

	public static long convertChatIdToUid(long chatId) {
		return chatId + GROUP_CHAT_UID_OFFSET;
	}

	public static long convertUidToChatId(long uid) {
		Preconditions.checkArgument(isGroupChatUid(uid));

		return uid - GROUP_CHAT_UID_OFFSET;
	}

	public long chatId;
	public String title;
	public List<Profile> users = new ArrayList<Profile>();
	public long ownerId;

	public long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(long uid) {
		ownerId = uid;
	}

	public boolean hasChatId() {
		return chatId != 0;
	}

	public boolean hasTitle() {
		return title != null;
	}

	public long getChatId() {
		return chatId;
	}

	public void setChatId(long cid) {
		chatId = cid;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Profile findUser(long uid) {
		for (Profile user : users) {
			if (user.id == uid) {
				return user;
			}
		}
		return null;
	}

	public void addUser(Profile participant) {
		if (findUser(participant.id) == null) {
			users.add(participant);
		}
	}

	public void removeUser(long uid) {
		int i = 0;
		for (Profile user : users) {
			if (user.id == uid) {
				users.remove(i);
				return;
			}
			i++;
		}			
	}
}
