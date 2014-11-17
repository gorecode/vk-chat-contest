package com.gorecode.vk.activity.chat;

import java.util.Comparator;

import com.gorecode.vk.collections.SortedTable;
import com.gorecode.vk.data.ChatMessage;

public class ChatTable extends SortedTable<ChatMessage> {
	public static final Comparator<ChatMessage> COMPARATOR_BY_TIMESTAMP = new Comparator<ChatMessage>() {
		@Override
		public int compare(ChatMessage object1, ChatMessage object2) {
			final long t1 = object1.timestamp;
			final long t2 = object2.timestamp;
			if (t1 > t2) return 1;
			if (t1 < t2) return -1;
			return 0;
		}
	};

	public ChatTable() {
		super(COMPARATOR_BY_TIMESTAMP);
	}

	@Override
	public long getIdOfObject(ChatMessage object) {
		return object.id;
	}
}
