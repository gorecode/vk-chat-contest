package com.gorecode.vk.event;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Message;

public class MessageReceivedEvent<MessageClazz extends Message<?>> {
	private final MessageClazz message;

	public MessageReceivedEvent(MessageClazz message) {
		Preconditions.checkNotNull(message);

		this.message = message;
	}

	public MessageClazz getMessage() {
		return message;
	}
}
