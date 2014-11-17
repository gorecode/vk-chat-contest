package com.gorecode.vk.event;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.TypingNotification;

public class TypingNotificationEvent {
	public final Profile user;
	public final long chatId;
	public final TypingNotification notification;

	public long getCid() {
		if (chatId != 0) {
			return chatId;
		} else {
			return user.id;
		}
	}

	public TypingNotificationEvent(Profile user, long chatId, TypingNotification notification) {
		Preconditions.checkNotNull(user);
		Preconditions.checkNotNull(notification);

		this.user = user;
		this.notification = notification;
		this.chatId = chatId;
	}

	public TypingNotificationEvent(Profile user, TypingNotification notification) {
		Preconditions.checkNotNull(user);
		Preconditions.checkNotNull(notification);

		this.user = user;
		this.notification = notification;
		this.chatId = 0;
	}
}
