package com.gorecode.vk.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// defective structure not suitable for containing group chat information for VK.
public class Dialog implements Serializable, Cloneable {
	public static final long INVALID_ID = 0;

	private static final long serialVersionUID = -60756185214996697L;

	public final List<Profile> activeUsers = new ArrayList<Profile>();

	public ChatMessage lastMessage;

	public int totalParticipants;

	public long ownerId;

	public Profile getParticipant() {
		return lastMessage.getParticipant();
	}

	public Profile findParticipant(long uid) {
		for (int i = 0; i < activeUsers.size(); i++) {
			if (activeUsers.get(i).id == uid) {
				return activeUsers.get(i);
			}
		}
		return null;
	}

	public void setLastMessage(ChatMessage message) {
		this.lastMessage = message;

		if (message != null) {
			putActiveParticipant(message.user);
		}
	}

	public void putActiveParticipant(Profile participant) {
		if (participant == null) return;

		for (int i = 0; i < activeUsers.size(); i++) {
			if (activeUsers.get(i).id == participant.id) {
				activeUsers.set(i, participant);
				return;
			}
		}

		activeUsers.add(participant);
	}

	public void removeActiveParticipant(long uid) {
		for (int i = 0; i < activeUsers.size(); i++) {
			if (activeUsers.get(i).id == uid) {
				activeUsers.remove(i);
			}
		}
	}

	public long getOwnerId() {
		return ownerId;
	}
	
	public int getTotalParticipantsCount() {
		if (isConference()) {
			if (totalParticipants > activeUsers.size()) {
				return totalParticipants;
			} else {
				return activeUsers.size();
			}
		}
		return activeUsers.size();
	}

	public static Dialog withOneMessage(ChatMessage message) {
		Dialog d = new Dialog();
		d.setLastMessage(message);
		return d;
	}

	public String getTitle() {
		return lastMessage.content.subject;
	}

	public void setTitle(String title) {
		if (lastMessage == null) {
			lastMessage = new ChatMessage();
			lastMessage.user = activeUsers.get(0);
			lastMessage.timestamp = System.currentTimeMillis();
		}

		lastMessage.content.subject = title;
	}

	@Override
	public Dialog clone() {
		try {
			return (Dialog)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public long getCid() {
		return lastMessage.getCid();
	}

	public boolean isConference() {
		return lastMessage.isFromConference();
	}
}
