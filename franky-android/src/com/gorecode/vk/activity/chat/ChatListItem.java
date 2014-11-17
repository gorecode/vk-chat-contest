package com.gorecode.vk.activity.chat;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.util.Linkify;

import com.google.common.base.Strings;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.utilities.AgoTimeFormat;

public class ChatListItem {
	public static final int DISPATCH_STATE_SENT = 0x0;
	public static final int DISPATCH_STATE_SENT_NOW = 0x1;
	public static final int DISPATCH_STATE_PENDING = 0x2;
	public static final int DISPATCH_STATE_ERROR = 0x3;

	public static final int TYPE_MESSAGE = 0x0;
	public static final int TYPE_SEPARATOR = 0x1;

	private static final AgoTimeFormat TIME_FORMAT;

	static {
		TIME_FORMAT = new AgoTimeFormat(VkApplication.getApplication().getResources());
	}

	private static final AtomicInteger sNextSeparatorId = new AtomicInteger(1);

	public int type = TYPE_MESSAGE;

	public ChatMessage message;

	public boolean isMarked;
	public int dispatchState = DISPATCH_STATE_SENT;

	private long separatorId;

	private Spannable linkyfiedText;
	private String timestampText;

	public ChatListItem(ChatMessage message, boolean isSelected) {
		this.message = message;
		this.isMarked = isSelected;
	}

	public ChatListItem(ChatMessage message) {
		this.message = message;
	}

	public String getTimestampText() {
		return timestampText;
	}

	public boolean isSeparator() {
		return type == TYPE_SEPARATOR;
	}

	public boolean isMessage() {
		return type == TYPE_MESSAGE;
	}

	public Spannable getLinkifiedText() {
		return linkyfiedText;
	}

	public static ChatListItem newMessage(ChatMessage message) {
		ChatListItem item = new ChatListItem(message);

		item.timestampText = TIME_FORMAT.formatTimeOnly(new Date(item.message.timestamp));

		if (Strings.isNullOrEmpty(item.message.content.text)) {
			item.linkyfiedText = new SpannableString("");	
		} else {
			item.linkyfiedText = new SpannableString(item.message.content.text);

			Linkify.addLinks(item.linkyfiedText, Linkify.ALL);
		}
		
		return item;		
	}

	public static ChatListItem newSeparator(ChatMessage nextMessage) {
		ChatListItem item = new ChatListItem(nextMessage);
		item.type = TYPE_SEPARATOR;
		item.separatorId = sNextSeparatorId.incrementAndGet();
		item.timestampText = TIME_FORMAT.formatDayOnly(new Date(nextMessage.timestamp));
		return item;
	}

	public long getTimestamp() {
		if (message != null) {
			return message.timestamp;
		} else {
			throw new RuntimeException("You should never reach here");
		}
	}

	public long getId() {
		if (isSeparator()) {
			return Long.MAX_VALUE - separatorId;
		} else {
			return message.id;
		}
	}
}
