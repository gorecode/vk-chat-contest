package com.gorecode.vk.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Pair;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UnhandledNotifications;
import com.gorecode.vk.event.AllFriendshipOffersRejectedEvent;
import com.gorecode.vk.event.DialogDeletedEvent;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.utilities.MoreFunctions;
import com.perm.kate.api.Api;
import com.perm.kate.api.Params;
import com.perm.kate.api.VkMessage;
import com.perm.kate.api.VkPhoto;
import com.perm.kate.api.VkUser;
import com.uva.lang.StringUtilities;
import com.uva.log.Log;
import com.uva.log.Message;

public class VkModel extends Api {
	public static class AuthResult implements Serializable {
		private static final long serialVersionUID = 5873553629418214743L;

		public String secret;
		public String accessToken;
		public String accessTokenForHttps;
		public long userId;
	}

	public static final String VK_APPLICATION_ID = "2940033";
	public static final String VK_APPLICATION_SECRET_KEY = "HahCxieHEZd1kaymPor4";

	public static final String REQUIRED_USER_FIELDS = "uid,first_name,last_name,photo,photo_rec,photo_medium,photo_medium_rec,photo_big,online,contacts";

	private static final String TAG = VkModel.class.getSimpleName();

	private final EventBus mEventBus;
	private final SessionContext mContext;

	private final HashMap<Long, Profile> mKnownUsers = new HashMap<Long, Profile>();

	@Inject
	public VkModel(EventBus eventBus, SessionContext context) {
		mEventBus = eventBus;
		mContext = context;
	}

	@Override
	public String getSecret() {
		return mContext.getSecret();
	}

	@Override
	public String getAccessToken() {
		if (mContext == null) {
			return null;
		}
		return mContext.getAccessToken();
	}

	@Override
	public String getAccessTokenForHttps() {
		if (mContext == null) {
			return null;
		}
		return mContext.getAccessTokenForHttps();
	}

	public String signup(String phone, String firstName, String lastName, boolean testMode) throws Exception {
		return signup(phone, firstName, lastName, testMode, null);
	}

	public String signup(String phone, String firstName, String lastName, boolean testMode, String sid) throws Exception {
		final Params params = new Params("auth.signup");

		params.put("phone", phone);
		params.put("first_name", firstName);
		params.put("last_name", lastName);
		params.put("client_id", VK_APPLICATION_ID);
		params.put("client_secret", VK_APPLICATION_SECRET_KEY);
		params.put("test_mode", testMode ? 1 : 0);

		if (sid != null) {
			params.put("sid", sid);
		}

		return sendRequestViaHttps(params).getJSONObject("response").getString("sid");
	}

	public long confirmSignup(String phone, String code, String password, boolean testMode) throws Exception {
		final Params params = new Params("auth.confirm");

		params.put("phone", phone);
		params.put("code", code);
		params.put("password", password);
		params.put("client_id", VK_APPLICATION_ID);
		params.put("client_secret", VK_APPLICATION_SECRET_KEY);
		params.put("test_mode", testMode ? 1 : 0);

		return sendRequestViaHttps(params).getJSONObject("response").getLong("uid");		
	}

	public void importContacts(List<String> phonesAndEmails) throws Exception {
		Params params = new Params("account.importContacts");
		params.put("contacts", StringUtilities.join(",", phonesAndEmails));

		sendRequestViaHttps(params);
	}

	public Collection<Pair<String, Profile>> getFriendsByPhones(List<String> phones) throws Exception {
		final Params params = new Params("friends.getByPhones");

		params.put("fields", REQUIRED_USER_FIELDS);
		params.put("phones", StringUtilities.join(",", phones));

		JSONObject responseJson = sendRequestViaHttps(params);

		JSONArray usersJson = responseJson.optJSONArray("response");

		if (usersJson != null) {
			List<Pair<String, Profile>> results = new ArrayList<Pair<String,Profile>>();

			for (int i = 0; i < usersJson.length(); i++) {
				JSONObject userJson = usersJson.getJSONObject(i);

				Profile user = Profile.fromVkUser(VkUser.parse(userJson));

				results.add(Pair.create(userJson.getString("phone"), user));
			}

			return results;
		} else {
			return Collections.emptyList();
		}
	}

	public AuthResult auth(String login, String password) throws Exception {
		final Params params = new Params("");

		params.put("grant_type", "password");
		params.put("client_id", VK_APPLICATION_ID);
		params.put("client_secret", VK_APPLICATION_SECRET_KEY);
		params.put("scope", "friends,offers,messages,offline,video,audio,docs,photos,wall,nohttps");
		params.put("username", login);
		params.put("password", password);

		JSONObject responseJson = sendRequest(params, new Function<Params, String>() {
			@Override
			public String apply(Params arg0) {
				return "https://api.vk.com/oauth/token?" + params.getParamsString();
			}
		});

		AuthResult result = new AuthResult();

		result.accessToken = responseJson.getString("access_token");
		result.userId = responseJson.getLong("user_id");
		result.secret = responseJson.getString("secret");

		params.put("scope", "friends,offers,messages,offline,video,audio,docs,wall,photos");

		responseJson = sendRequest(params, new Function<Params, String>() {
			@Override
			public String apply(Params arg0) {
				return "https://api.vk.com/oauth/token?" + params.getParamsString();
			}
		});

		result.accessTokenForHttps = responseJson.getString("access_token");

		return result;
	}

	public Profile getUser(long uid) throws Exception {
		List<Profile> users = getUsers(Collections.singleton(uid));

		if (users.size() == 1) return users.get(0);

		throw new Exception("User not found");
	}

	public boolean checkPhone(String phone) throws Exception {
		Params request = new Params("auth.checkPhone");

		request.put("phone", phone);
		request.put("client_id", VK_APPLICATION_ID);
		request.put("client_secret", VK_APPLICATION_SECRET_KEY);

		return sendRequestViaHttps(request).getInt("response") == 1;
	}

	public List<Profile> getUsers(Collection<Long> uids) throws Exception {
		String uidsString = StringUtilities.join(",", Iterables.toArray(Iterables.transform(uids, Functions.toStringFunction()), String.class));

		Params request = new Params("users.get");

		request.put("uids", uidsString);
		request.put("fields", VkModel.REQUIRED_USER_FIELDS);

		JSONArray jsonArray = sendRequestViaHttp(request).getJSONArray("response");

		return Profile.fromVkUsers(parseUsers(jsonArray));
	}

	public void addToFriends(long uid) throws Exception {
		Params request = new Params("friends.add");

		request.put("uid", uid);

		sendRequestViaHttp(request);
	}

	public void removeFromFriends(long uid) throws Exception {
		Params request = new Params("friends.delete");

		request.put("uid", uid);

		sendRequestViaHttp(request);		
	}

	public void rejectAllFriendshipOffers() throws Exception {
		Params request = new Params("friends.deleteAllRequests");

		mEventBus.post(new AllFriendshipOffersRejectedEvent());

		sendRequestViaHttp(request);
	}

	public void registerDeviceForPushNotifications(String registrationId, boolean textOfChatMessagesWanted) throws Exception {
		Params request = new Params("account.registerDevice");
		request.put("token", registrationId);
		request.put("device_model", Build.VERSION.RELEASE);
		request.put("system_version", Build.MODEL);
		request.put("no_text", textOfChatMessagesWanted ? "0" : "1");

		sendRequestViaHttp(request);
	}

	public void unregisterDeviceFromPushNotifications(String registratinoId) throws Exception {
		Params request = new Params("account.unregisterDevice");

		request.put("token", registratinoId);

		sendRequestViaHttp(request);
	}

	public List<Profile> findUsers(String query, int offset, int count) throws Exception {
		List<VkUser> vkUsers = searchUser(query, REQUIRED_USER_FIELDS, Long.valueOf(count), Long.valueOf(offset));

		List<Profile> users = new ArrayList<Profile>(vkUsers.size());

		for (VkUser vkUser : vkUsers) {
			users.add(Profile.fromVkUser(vkUser));
		}

		return users;
	}

	public List<Pair<Profile, GroupChatDescriptor>> searchDialogs(String query) throws Exception {
		JSONObject sp = new JSONObject();

		sp.put("q", query);
		sp.put("fields", REQUIRED_USER_FIELDS);

		String code =
				"var m = API.messages.searchDialogs(%s);" +
						"var uids = \"\";" + 
						"var i = 0;" +
						"while (m[i] + \"\" != \"\") {" +
						"if (m[i].users + \"\" != \"\") {" +
						"uids = uids + (m[i].users + \"\") + \",\";" +
						"}" +
						"i = i + 1;" +
						"}" +
						"uids = uids + \"0\";" +
						"var u = API.getProfiles({uids:uids,fields:\"%s\"});" +
						"return {m:m, u:u, uids:uids};";


		code = String.format(code, sp.toString(), REQUIRED_USER_FIELDS);

		JSONObject json = execute(code);

		ArrayList<Pair<Profile, GroupChatDescriptor>> results = new ArrayList<Pair<Profile, GroupChatDescriptor>>();

		JSONArray itemsJson = json.getJSONArray("m");

		if (itemsJson.length() == 0) {
			return results;
		}

		JSONArray usersJson = json.getJSONArray("u");

		final int COLLECT_USERS = 0x0;
		final int COLLECT_DESCRIPTORS = 0x1;

		for (Profile user : Profile.fromVkUsers(parseUsers(usersJson))) {
			mKnownUsers.put(user.id, user);
		}

		for (int step = COLLECT_USERS; step <= COLLECT_DESCRIPTORS; step++) {
			for (int i = 0; i < itemsJson.length(); i++) {
				JSONObject itemJson = itemsJson.getJSONObject(i);

				String type = itemJson.getString("type");

				if ("profile".equals(type) && step == COLLECT_USERS) {
					Profile user = Profile.fromVkUser(VkUser.parse(itemJson));

					mKnownUsers.put(user.id, user);

					results.add(Pair.create(user, (GroupChatDescriptor)null));
				}

				if ("chat".equals(type) && step == COLLECT_DESCRIPTORS) {
					GroupChatDescriptor descriptor = new GroupChatDescriptor();

					descriptor.chatId = itemJson.getLong("chat_id");
					descriptor.title = itemJson.optString("title", "");

					JSONArray uidsJson = itemJson.getJSONArray("users");

					for (int j = 0; j < uidsJson.length(); j++) {
						Profile user = mKnownUsers.get(uidsJson.getLong(j));

						if (user != null) {
							descriptor.users.add(user);
						}
					}

					results.add(Pair.create((Profile)null, descriptor));
				}
			}
		}

		return results;
	}

	public UnhandledNotifications getUnhandledNotifications() throws Exception {
		return getUnhandledNotifications(0);
	}

	public UnhandledNotifications getUnhandledNotifications(long timeOffsetInSeconds) throws Exception {
		String code = "return {c:API.getCounters(),m:API.messages.get({filters:1,count:100,time_offset:" + timeOffsetInSeconds + "})};";

		JSONObject response = execute(code);

		UnhandledNotifications notificationsCounter = new UnhandledNotifications();

		JSONArray messages = response.optJSONArray("m");

		int unread = 0;

		if (messages != null) {
			for (VkMessage vkMessage : Api.parseMessages(messages, false, 0, false ,0)) {
				if (!vkMessage.read_state && !vkMessage.is_out) {
					unread++;
				}
			}
		}

		notificationsCounter.numMessages = unread;

		JSONObject countersJson = response.optJSONObject("c");

		if (countersJson != null) {
			notificationsCounter.numOffers = countersJson.optInt("friends", 0);
		}

		return notificationsCounter;
	}

	public void restoreMessages(Collection<Long> mids) throws Exception {
		StringBuilder codeBuilder = new StringBuilder();

		for (Long mid : mids) {
			String deleteByMidCode = String.format("API.messages.restore({mid:%d});", mid);

			codeBuilder.append(deleteByMidCode);
		}

		codeBuilder.append("return{response:{}};");

		String code = codeBuilder.toString();

		execute(code);		
	}

	public void deleteMessages(Collection<Long> mids) throws Exception {
		for (Long mid : mids) {
			deleteMessage(mid);
		}
	}

	public void deleteDialog(Dialog dialog) throws Exception {
		if (dialog.isConference()) {
			deleteMessageThread(null, dialog.lastMessage.chatId);
		} else {
			deleteMessageThread(dialog.lastMessage.getParticipant().id, null);
		}

		mEventBus.post(new DialogDeletedEvent(dialog));
	}

	public ArrayList<ChatMessage> getMessagesFromDialog(long participantId, int offset, int count) throws Exception {
		return getMessages(participantId, null, offset, count);
	}

	public ArrayList<ChatMessage> getMessagesFromGroupChat(long chatId, int offset, int count) throws Exception {
		return getMessages(null, chatId, offset, count);
	}

	public long sendMessageToUser(long userId, ChatMessage.Content content) throws Exception {
		return sendMessage(userId, null, content);
	}

	public long sendMessageToGroup(long chatId, ChatMessage.Content content) throws Exception {
		return sendMessage(null, chatId, content);
	}

	public ArrayList<ChatMessage> getMessagesByIds(Collection<Long> mids) throws Exception {
		if (mids.size() == 0) return new ArrayList<ChatMessage>();

		String[] midsStrings = Iterables.toArray(Iterables.transform(mids, Functions.toStringFunction()), String.class);

		String code =
				"var m=API.messages.getById({\"mids\":\"%s\"});" +
						"var p1=API.getProfiles({uids:m@.uid,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p2=API.getProfiles({uids:m@.chat_active,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p3=API.getProfiles({uid:%d,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"return{messages:m,profiles:p1,more_profiles:p2,one_more_profile:p3};";

		code = String.format(code, StringUtilities.join(",", midsStrings), mContext.getUserId());

		JSONObject response = execute(code);

		JSONArray messages = response.optJSONArray("messages");
		JSONArray profiles = response.optJSONArray("profiles");
		JSONArray moreProfiles = response.optJSONArray("more_profiles");
		JSONArray oneMoreProfile = response.optJSONArray("one_more_profile");

		ArrayList<VkMessage> vkMessages = Api.parseMessages(messages, false, 0, false, mContext.getUserId());

		HashMap<Long, VkUser> vkUsers = new HashMap<Long, VkUser>();

		for (VkUser vkUser : Api.parseUsers(profiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}
		for (VkUser vkUser : Api.parseUsers(moreProfiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}
		for (VkUser vkUser : Api.parseUsers(oneMoreProfile)) {
			vkUsers.put(vkUser.uid, vkUser);
		}

		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

		for (VkUser vkUser : vkUsers.values()) {
			Profile user = Profile.fromVkUser(vkUser);

			mKnownUsers.put(user.id, user);
		}

		for (VkMessage vkMessage : vkMessages) {
			chatMessages.add(ChatMessage.from(vkMessage, vkUsers, vkMessage.uid));
		}

		resolveUnknownUsersSilently(chatMessages, mKnownUsers);

		return chatMessages;
	}

	public Profile getKnownUser(long uid) {
		Profile user = mKnownUsers.get(uid);

		if (user != null) {
			return user;
		} else {
			return Profile.empty(uid);
		}
	}

	public void markMessagesAsRead(Collection<Long> mids) throws Exception {
		markAsNewOrAsRead(mids, true);
	}

	public void sendTypingNotification() throws Exception {
		sendTypingNotification(0);
	}

	public void changeDialogTitle(long chatId, String title) throws Exception {
		Params request = new Params("messages.editChat");

		request.put("chat_id", chatId);
		request.put("title", title);

		sendRequestViaHttp(request);
	}

	public void addParticipantToDialog(long chatId, long uid) throws Exception {
		Params request = new Params("messages.addChatUser");

		request.put("chat_id", chatId);
		request.put("uid", uid);

		sendRequestViaHttp(request);		
	}

	public void removeParticipantFromDialog(long chatId, long uid) throws Exception {
		Params request = new Params("messages.removeChatUser");

		request.put("chat_id", chatId);
		request.put("uid", uid);

		sendRequestViaHttp(request);
	}

	public void sendTypingNotification(long chatId) throws Exception {
		Params params = new Params("messages.setActivity");

		params.put("type", "typing");

		if (chatId == 0) {
			params.put("uid", mContext.getUserId());
		} else {
			params.put("chat_id", String.valueOf(chatId));
		}

		sendRequestViaHttp(params);
	}

	public long sendMessage(Long userId, Long chatId, ChatMessage.Content content) throws Exception {
		Preconditions.checkArgument(userId != null || chatId != null);

		// TODO: Support for capcha.

		String text = content.text;

		ArrayList<String> attachments = new ArrayList<String>();

		for (VkPhoto vkPhoto : content.getAttachedPhotos()) {
			attachments.add("photo" + vkPhoto.owner_id + "_" + vkPhoto.pid);
		}

		Collection<Long> forwarded = null;

		if (content.forwarded.size() > 0) {
			forwarded = Lists.transform(content.forwarded, MoreFunctions.getMid());
		}

		return Long.parseLong(sendMessage(userId, chatId, text, null, "1", attachments, content.location, forwarded, null, null));
	}

	public ArrayList<ChatMessage> getMessagesByQuery(String query, int offset, int count) throws Exception {
		JSONObject searchParams = new JSONObject();

		searchParams.put("q", query);
		searchParams.put("offset", offset);
		searchParams.put("count", count);

		String code =
				"var m=API.messages.search(%s);" +
						"var p1=API.getProfiles({uids:m@.uid,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p2=API.getProfiles({uids:m@.chat_active,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"return{messages:m,profiles:p1,more_profiles:p2};";

		code = String.format(code, searchParams.toString());

		JSONObject response = execute(code);

		JSONArray messages = response.optJSONArray("messages");
		JSONArray profiles = response.optJSONArray("profiles");
		JSONArray moreProfiles = response.optJSONArray("more_profiles");

		ArrayList<VkMessage> vkMessages = Api.parseMessages(messages, false, 0, false, mContext.getUserId());

		HashMap<Long, VkUser> vkUsers = new HashMap<Long, VkUser>();

		for (VkUser vkUser : Api.parseUsers(profiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}
		for (VkUser vkUser : Api.parseUsers(moreProfiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}

		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

		for (VkUser vkUser : vkUsers.values()) {
			Profile user = Profile.fromVkUser(vkUser);

			mKnownUsers.put(user.id, user);
		}

		for (VkMessage vkMessage : vkMessages) {
			chatMessages.add(ChatMessage.from(vkMessage, vkUsers, vkMessage.uid));
		}

		resolveUnknownUsersSilently(chatMessages, mKnownUsers);

		return chatMessages;
	}

	public ArrayList<ChatMessage> getMessages(Long participantId, Long chatId, int offset, int count) throws Exception {
		String code =
				"var m=API.messages.getHistory({\"%s\":\"%d\", \"count\":\"%d\",\"offset\":\"%d\"});" +
						"var p1=API.getProfiles({uids:m@.from_id,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p2=API.getProfiles({uids:m@.chat_active,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p3=API.getProfiles({uid:%d,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"return{messages:m,profiles:p1,more_profiles:p2,one_more_profile:p3};";

		long id = participantId != null ? participantId : chatId;

		String field = participantId != null ? "uid" : "chat_id";

		code = String.format(code, field, id, count, offset, participantId);

		JSONObject response = execute(code);

		JSONArray messages = response.optJSONArray("messages");
		JSONArray profiles = response.optJSONArray("profiles");
		JSONArray moreProfiles = response.optJSONArray("more_profiles");
		JSONArray oneMoreProfile = response.optJSONArray("one_more_profile");

		ArrayList<VkMessage> vkMessages = Api.parseMessages(messages, true, 0, true, mContext.getUserId());

		HashMap<Long, VkUser> vkUsers = new HashMap<Long, VkUser>();

		for (VkUser vkUser : Api.parseUsers(profiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}
		for (VkUser vkUser : Api.parseUsers(moreProfiles)) {
			vkUsers.put(vkUser.uid, vkUser);
		}
		for (VkUser vkUser : Api.parseUsers(oneMoreProfile)) {
			vkUsers.put(vkUser.uid, vkUser);
		}

		ArrayList<ChatMessage> chatMessages = new ArrayList<ChatMessage>();

		for (VkUser vkUser : vkUsers.values()) {
			Profile user = Profile.fromVkUser(vkUser);

			mKnownUsers.put(user.id, user);
		}

		for (VkMessage vkMessage : vkMessages) {
			if (chatId != null) {
				vkMessage.chat_id = chatId;

				chatMessages.add(ChatMessage.from(vkMessage, vkUsers, vkMessage.uid));
			} else {
				chatMessages.add(ChatMessage.from(vkMessage, vkUsers, participantId));
			}
		}

		resolveUnknownUsersSilently(chatMessages, mKnownUsers);

		return chatMessages;
	}

	public long createGroupChat(Collection<Long> uids, String title) throws Exception { 
		Params request = new Params("messages.createChat");

		request.put("uids", getUidsString(uids));
		request.put("title", title);

		return sendRequestViaHttp(request).getLong("response");
	}

	public GroupChatDescriptor getGroupChatDescriptor(long cid) throws Exception {
		String code =
				"var c=API.messages.getChat({chat_id:%d});" +
						"var p=API.getProfiles({uids:c.users,fields:\"%s\"});" +
						"return{c:c,p:p};";

		code = String.format(code, cid, VkModel.REQUIRED_USER_FIELDS);

		JSONObject responseJson = execute(code);

		JSONObject chatJson = responseJson.getJSONObject("c");
		JSONArray usersJson = responseJson.optJSONArray("p");

		GroupChatDescriptor result = new GroupChatDescriptor();

		result.chatId = cid;
		result.title = chatJson.optString("title", "");
		result.ownerId = chatJson.optLong("admin_id", -1);

		if (usersJson != null) {
			result.users = Profile.fromVkUsers(parseUsers(usersJson));
		}

		return result;
	}

	// Note: will fail for non group chats.
	public Dialog getDialogById(long cid) throws Exception {
		String code =
				"var m=API.messages.getDialogs({\"chat_id\":\"%1$d\",\"count\":\"1\",\"offset\":\"0\"});" +
						"var p1=API.getProfiles({uid:%2$d,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var d=API.messages.getChat({chat_id:%1$d});" +
						"var p2=API.getProfiles({uids:d.users,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"return{messages:m,p1:p1,p2:p2,d:d};";

		code = String.format(code, cid, mContext.getUserId());

		Log.debug(TAG, "Executing code " + code);

		JSONObject response = execute(code);

		Dialog dialog = new Dialog();

		JSONObject dialogJson = response.getJSONObject("d");

		try {
			ArrayList<VkMessage> vkMessages = Api.parseMessages(response.optJSONArray("messages"), false, 0, false ,0);

			VkMessage vkMessage = vkMessages.get(0);

			HashMap<Long, VkUser> vkUsers = parseVkUsers(response, "p1", "p2");

			dialog.setLastMessage(ChatMessage.from(vkMessage, vkUsers, vkMessage.uid));
		} catch (Exception e) {
			ChatMessage message = new ChatMessage();

			message.chatId = cid;
			message.content = new ChatMessage.Content("");

			dialog.setLastMessage(message);
		}

		dialog.setTitle(dialogJson.optString("title", ""));
		dialog.ownerId = dialogJson.optLong("admin_id", -1);

		JSONArray participants = response.getJSONArray("p2");

		for (int i = 0; i < participants.length(); i++) {
			VkUser vkUser = VkUser.parse(participants.getJSONObject(i));

			dialog.putActiveParticipant(Profile.fromVkUser(vkUser));
		}

		dialog.totalParticipants = dialog.activeUsers.size();

		return dialog;
	}

	public ArrayList<Dialog> getDialogs(int offset, int count) throws Exception {
		String code =
				"var m=API.messages.getDialogs({\"count\":\"%d\",\"offset\":\"%d\"});" +
						"var p1=API.getProfiles({uids:m@.uid,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p2=API.getProfiles({uids:m@.chat_active,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"var p3=API.getProfiles({uid:%d,fields:\"first_name,last_name,photo,photo_medium_rec,photo_medium,photo_big,online\"});" +
						"return{messages:m,p1:p1,p2:p2,p3:p3};";

		code = String.format(code, count, offset, mContext.getUserId());

		Log.debug(TAG, "Executing code " + code);

		JSONObject response = execute(code);

		ArrayList<VkMessage> vkMessages = Api.parseMessages(response.optJSONArray("messages"), false, 0, false ,0);

		HashMap<Long, VkUser> vkUsers = parseVkUsers(response, "p1", "p2", "p3");

		ArrayList<Dialog> dialogs = new ArrayList<Dialog>();

		for (VkMessage vkMessage : vkMessages) {
			Dialog dialog = Dialog.withOneMessage(ChatMessage.from(vkMessage, vkUsers, vkMessage.uid));

			if (vkMessage.users_count != null) {
				dialog.totalParticipants = vkMessage.users_count;
			}
			if (vkMessage.admin_id != null) {
				dialog.ownerId = vkMessage.admin_id;
			}

			for (long participantId : vkMessage.chat_active) {
				VkUser vkAnotherParticipant = vkUsers.get(participantId);

				if (vkAnotherParticipant != null) {
					Profile user = Profile.fromVkUser(vkAnotherParticipant);

					mKnownUsers.put(user.id, user);

					dialog.putActiveParticipant(user);
				}
			}

			dialogs.add(dialog);
		}

		return dialogs;
	}

	private int resolveUnknownUsersSilently(Collection<ChatMessage> messages, Map<Long, Profile> knownUsers) {
		try {
			Log.trace(TAG, "Resolving unknown users");
			int resolved = resolveUnknownUsers(messages, mKnownUsers);
			Log.trace(TAG, String.format("Resolved %d users", resolved));
			return resolved;
		} catch (Exception e) {
			Log.exception(TAG, Message.WARNING, "Error resolving unknown users", e);
			return 0;
		}
	}

	/**
	 * Resolves all unknown users containing in given messages and places them into messages and knownUsers.
	 * <p>
	 * Method is useful then chat message can contain forwarded messages with people you don't know.
	 * 
	 * @param messages in, out messages that can contain unknown users, after successful execution of method all users are known.
	 * @param knownUsers in, out known users, after successful execution of method all unknown users (before this call) will be added to this map.
	 */
	private int resolveUnknownUsers(final Collection<ChatMessage> messages, final Map<Long, Profile> knownUsers) throws Exception {
		Collection<Profile> unknownUsers = Collections2.filter(getParticipants(messages), new Predicate<Profile>() {
			@Override
			public boolean apply(Profile user) {
				return user.isEmpty() && !knownUsers.containsKey(user.id);
			}
		});

		Collection<Long> uids = Collections2.transform(unknownUsers, new Function<Profile, Long>() {
			@Override
			public Long apply(Profile user) {
				return user.id;
			}
		});

		if (uids.size() > 0) {
			Collection<VkUser> vkUsers = getProfiles(uids, null, null, null);

			for (VkUser vkUser : vkUsers) {
				Profile user = Profile.fromVkUser(vkUser);

				knownUsers.put(user.id, user);
			}
		}

		updateParticipants(messages, knownUsers);

		return uids.size();
	}

	private static void updateParticipants(Collection<ChatMessage> messages, Map<Long, Profile> knownUsers) {
		for (ChatMessage message : messages) {
			long uid = message.user.id;

			Profile user = knownUsers.get(uid);

			if (user != null) {
				message.user = user;
			}

			updateParticipants(message.content.forwarded, knownUsers);
		}
	}

	private static Collection<Profile> getParticipants(Collection<ChatMessage> messages) {
		return getParticipants(messages, new ArrayList<Profile>());
	}

	private static Collection<Profile> getParticipants(Collection<ChatMessage> messages, Collection<Profile> outUsers) {
		for (ChatMessage message : messages) {
			outUsers.add(message.user);

			getParticipants(message.content.forwarded, outUsers);
		}

		return outUsers; 
	}

	private static HashMap<Long, VkUser> parseVkUsers(JSONObject json, String... keys) throws JSONException {
		HashMap<Long, VkUser> vkUsers = new HashMap<Long, VkUser>();

		for (String key : keys) {
			JSONArray jsonArray = json.optJSONArray(key);

			if (jsonArray == null) continue;

			for (int i = 0; i < jsonArray.length(); i++) {
				VkUser vkUser = VkUser.parse(jsonArray.getJSONObject(i));

				vkUsers.put(vkUser.uid, vkUser);
			}
		}

		return vkUsers;
	}
}
