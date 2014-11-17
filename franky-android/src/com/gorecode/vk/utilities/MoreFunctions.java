package com.gorecode.vk.utilities;

import com.google.common.base.Function;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.Profile;

public class MoreFunctions {
	public static Function<Dialog, ChatMessage> getLastMessage() {
		return new Function<Dialog, ChatMessage>() {
			@Override
			public ChatMessage apply(Dialog dialog) {
				return dialog.lastMessage;
			}
		};
	}

	public static Function<Profile, Long> getUid() {
		return new Function<Profile, Long>() {
			@Override
			public Long apply(Profile user) {
				return user.id;
			}
		};
	}

	public static Function<ChatMessage, Long> getMid() {
		return new Function<ChatMessage, Long>() {
			@Override
			public Long apply(ChatMessage message) {
				return message.id;
			}
		};
	}

	private MoreFunctions() {
		;
	}
}
