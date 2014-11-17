package com.gorecode.vk.event;

import com.gorecode.vk.data.ChatMessage;

public class ChatMessageStateChangedEvent {
	public long messageId;

	public Boolean isRead;
	public Boolean isDeleted;

	public void apply(ChatMessage message) {
		if (isRead != null) {
			message.unread = !isRead;
		}
	}
}
