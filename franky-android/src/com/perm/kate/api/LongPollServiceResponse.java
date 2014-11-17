package com.perm.kate.api;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LongPollServiceResponse {
	public static class Update {
		public static final int EVENT_MESSAGE_DELETED = 0;
		public static final int EVENT_MESSAGE_FLAGS_REPLACED = 1;
		public static final int EVENT_MESSAGE_FLAGS_ADD = 2;
		public static final int EVENT_MESSAGE_FLAGS_REMOVE = 3;
		public static final int EVENT_MESSAGE_ADDED = 4;
		public static final int EVENT_FRIEND_CAME_ONLINE = 8;
		public static final int EVENT_FRIEND_CAME_OFFLINE = 9;
		public static final int EVENT_CONFERENCE_CHANGED = 51;
		public static final int EVENT_PARTICIPANT_IN_DIALOG_IS_TYPING = 61;
		public static final int EVENT_PARTICIPANT_IN_GROUP_CHAT_IS_TYPING = 62;

		public long event;
		public long message_id;
		public long chat_id;
		public long user_id;
		public long timestamp;
		public int mask;
		public int flags;
		public String subject;
		public String text;
		public boolean self;
		public ArrayList<VkAttachment> attachments;
		
		public static Update parse(JSONArray json) throws JSONException {
			Update update = new Update();

			update.event = json.getInt(0);

			if (update.event == EVENT_FRIEND_CAME_OFFLINE) {
				update.user_id = -json.getLong(1);
			}
			if (update.event == EVENT_FRIEND_CAME_ONLINE) {
				update.user_id = -json.getLong(1);
			}
			if (update.event == EVENT_MESSAGE_DELETED) {
				update.message_id = json.getInt(1);
			}
			if (update.event == EVENT_MESSAGE_FLAGS_REPLACED) {
				update.message_id = json.getLong(1);
				update.flags = json.getInt(2);
			}
			if (update.event == EVENT_MESSAGE_FLAGS_ADD || update.event == EVENT_MESSAGE_FLAGS_REMOVE) {
				update.message_id = json.getLong(1);
				update.mask = json.getInt(2);
				update.user_id = json.optLong(3, 0);
			}
			if (update.event == EVENT_MESSAGE_ADDED) {
				update.message_id = json.getLong(1);
				update.flags = json.getInt(2);
				update.user_id = json.getLong(3);
				update.timestamp = json.getLong(4);
				update.subject = json.getString(5);
				update.text = json.getString(6);

		        if ((update.flags & VkMessage.GROUP_CHAT) != 0) {
		            update.chat_id = update.user_id & 63;
		            JSONObject o= json.getJSONObject(7);
		            update.user_id = o.getLong("from");
		        }

				update.attachments = new ArrayList<VkAttachment>();
			}
			if (update.event == EVENT_CONFERENCE_CHANGED) {
				update.chat_id = json.getInt(1);
				update.self = json.getInt(2) == 1;
			}
			if (update.event == EVENT_PARTICIPANT_IN_DIALOG_IS_TYPING) {
				update.user_id = json.getLong(1);
			}
			if (update.event == EVENT_PARTICIPANT_IN_GROUP_CHAT_IS_TYPING) {
				update.user_id = json.getLong(1);
				update.chat_id = json.getLong(2);
			}
			return update;
		}

		public static ArrayList<Update> parseArray(JSONArray array) throws JSONException {
			ArrayList<Update> updates = new ArrayList<Update>();

			for (int i = 0; i < array.length(); i++) {
				updates.add(Update.parse(array.getJSONArray(i)));
			}

			return updates;
		}
	}

	public long ts;
	public ArrayList<Update> updates = new ArrayList<Update>();

	public static LongPollServiceResponse parse(JSONObject json) throws JSONException {
		LongPollServiceResponse response = new LongPollServiceResponse();
		response.ts = json.getLong("ts");
		response.updates = Update.parseArray(json.getJSONArray("updates"));
		return response;
	}

}
