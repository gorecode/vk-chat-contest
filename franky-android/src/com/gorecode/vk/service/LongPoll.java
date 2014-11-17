package com.gorecode.vk.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.Availability;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.TypingNotification;
import com.gorecode.vk.error.LongPollSessionExpiredException;
import com.gorecode.vk.event.AvailabilityChangedEvent;
import com.gorecode.vk.event.ChatMessageStateChangedEvent;
import com.gorecode.vk.event.DialogChangedEvent;
import com.gorecode.vk.event.LongPollConnectionRestoredEvent;
import com.gorecode.vk.event.TypingNotificationEvent;
import com.gorecode.vk.sync.SessionContext;
import com.perm.kate.api.Api;
import com.perm.kate.api.LongPollServerInfo;
import com.perm.kate.api.LongPollServiceResponse;
import com.perm.kate.api.LongPollServiceResponse.Update;
import com.perm.kate.api.VkMessage;
import com.perm.kate.api.VkUser;
import com.uva.log.Log;

public class LongPoll {
	private static final String TAG = LongPoll.class.getSimpleName();

	private final VkModel mModel;
	private final EventBus mBus;

	private Thread mLongPollThread;

	private volatile long mLastRequestTime;

	@Inject
	public LongPoll(EventBus bus, SessionContext context, VkModel model) {
		mModel = model;
		mBus = bus;
	}

	public void startPoll() {
		mLongPollThread = new Thread(mRunLongPoll);
		mLongPollThread.start();
	}

	public void abortPoll() {
		mLongPollThread.interrupt();
	}

	private void handleLongPollResponse(LongPollServiceResponse response) throws Exception {
		HashMap<Long, VkUser> vkUsers = fetchUsersFromUpdates(response.updates);

		fireCorrespondingEvents(response.updates, vkUsers);
	}

	private HashMap<Long, VkUser> fetchUsersFromUpdates(List<LongPollServiceResponse.Update> updates) throws Exception {
		ArrayList<Long> uids = new ArrayList<Long>();

		for (Update update : updates) {
			if (update.user_id != 0) {
				uids.add(update.user_id);
			}
		}

		HashMap<Long, VkUser> vkUsers = new HashMap<Long, VkUser>();

		if (uids.size() == 0) return vkUsers;

		for (VkUser vkUser : mModel.getProfiles(uids, null, null, null)) {
			vkUsers.put(vkUser.uid, vkUser);
		}

		return vkUsers;
	}

	private void fireCorrespondingEvents(Collection<Update> updates, Map<Long, VkUser> users) throws Exception {
		// event: ChatMessageReadStateChanged.
		// event: ChatMessageSentEvent.
		// event: ChatMessageReceivedEvent.
		// event: ChatMessageDeletedEvent.

		ArrayList<Long> messagesToFetch = new ArrayList<Long>();
		ArrayList<Long> dialogsToFetch = new ArrayList<Long>();

		for (Update update : updates) {
			if (update.event == Update.EVENT_MESSAGE_DELETED) {
				ChatMessageStateChangedEvent event = new ChatMessageStateChangedEvent();
				event.isDeleted = true;
				mBus.post(event);				
			}
			if (update.event == Update.EVENT_MESSAGE_ADDED) {
				messagesToFetch.add(update.message_id);
			}
			if (update.event == Update.EVENT_FRIEND_CAME_OFFLINE) {
				if (update.user_id != 0) {
					Log.debug(TAG, String.format("%s user came offline", update.user_id));

					mBus.post(new AvailabilityChangedEvent(update.user_id, Availability.OFFLINE));
				}
			}
			if (update.event == Update.EVENT_FRIEND_CAME_ONLINE) {
				if (update.user_id != 0) {
					Log.debug(TAG, String.format("%s user came online", update.user_id));

					mBus.post(new AvailabilityChangedEvent(update.user_id, Availability.ONLINE));
				}
			}
			if (update.event == Update.EVENT_PARTICIPANT_IN_DIALOG_IS_TYPING) {
				mBus.post(new TypingNotificationEvent(Profile.empty(update.user_id), TypingNotification.TYPING));
			}
			if (update.event == Update.EVENT_PARTICIPANT_IN_GROUP_CHAT_IS_TYPING) {
				mBus.post(new TypingNotificationEvent(Profile.empty(update.user_id), update.chat_id, TypingNotification.TYPING));
			}
			if (update.event == Update.EVENT_MESSAGE_FLAGS_REPLACED) {
				ChatMessageStateChangedEvent event = new ChatMessageStateChangedEvent();
				event.messageId = update.message_id;
				event.isDeleted = (update.flags & VkMessage.DELETED) != 0;
				event.isRead = ((update.flags & VkMessage.UNREAD) == 0) || ((update.flags & VkMessage.FIXED) != 0);
				Log.debug(TAG, String.format("Message flags changed (mid = %d, is deleted = %s, is read = %s)", event.messageId, event.isDeleted, event.isRead));
				mBus.post(event);
			}
			if (update.event == Update.EVENT_MESSAGE_FLAGS_ADD) {
				ChatMessageStateChangedEvent event = new ChatMessageStateChangedEvent();

				event.messageId = update.message_id;

				if ((update.mask & VkMessage.DELETED) != 0) {
					event.isDeleted = true;
				}
				if ((update.mask & VkMessage.FIXED) != 0) {
					event.isRead = true;
				}

				Log.debug(TAG, String.format("Message flags changed (mid = %d, is deleted = %s, is read = %s)", event.messageId, event.isDeleted, event.isRead));
				mBus.post(event);
			}
			if (update.event == Update.EVENT_MESSAGE_FLAGS_REMOVE) {
				if (update.mask == 0) continue;

				ChatMessageStateChangedEvent event = new ChatMessageStateChangedEvent();

				event.messageId = update.message_id;

				if ((update.mask & VkMessage.DELETED) != 0) {
					event.isDeleted = false;
				}
				if ((update.mask & VkMessage.UNREAD) != 0) {
					event.isRead = true;
				}

				Log.debug(TAG, String.format("Message flags changed (mid = %d, is deleted = %s, is read = %s)", event.messageId, event.isDeleted, event.isRead));

				mBus.post(event);
			}
			if (update.event == Update.EVENT_CONFERENCE_CHANGED) {
				dialogsToFetch.add(update.chat_id);
			}
		} // for

		for (ChatMessage newMessages : mModel.getMessagesByIds(messagesToFetch)) {
			mBus.post(newMessages);
		}

		for (Long cid : dialogsToFetch) {
			Dialog dialog = mModel.getDialogById(cid);

			Log.debug(TAG, "firing DialogChangedEvent");

			mBus.post(new DialogChangedEvent(dialog));
		}
	}

	private final Runnable mRunLongPoll = new Runnable() {
		@Override
		public void run() {
			LongPollServerInfo info = null;

			Api api = mModel;

			int longPollErrorsCount = 0;

			long lastLongPollSuccessTime = 0;

			while (!Thread.interrupted()) {
				waitForInternet();

				try {
					boolean fireConnectionRestoredEvent = (longPollErrorsCount > 0 && System.currentTimeMillis() - lastLongPollSuccessTime > 3 * 60 * 1000);

					if (info == null) {
						info = api.getLongPollServer();

						longPollErrorsCount = 0;
					}

					try {
						LongPollServiceResponse response = null;

						try {
							response = api.getLongPollServerUpdates(info);

							lastLongPollSuccessTime = System.currentTimeMillis();
						} catch (Exception e) {
							longPollErrorsCount++;

							if (longPollErrorsCount > 6) {
								info = null;
							}

							throw e;
						}

						try {
							handleLongPollResponse(response);

							if (fireConnectionRestoredEvent) {
								Log.debug(TAG, "fire long poll connection restored event");

								mBus.post(new LongPollConnectionRestoredEvent());
							}
						} catch (Exception e) {
							Log.exception(TAG, "Error handling long poll response", e);

							continue;
						}

						mLastRequestTime = response.ts;

						info.ts = mLastRequestTime;
					} catch (LongPollSessionExpiredException e) {
						Log.exception(TAG, "Long poll session expired", e);

						info = null;
					}
				} catch (Exception e) {
					Log.exception(TAG, "Error during long poll run", e);

					try {
						Thread.sleep(3000);
					} catch (InterruptedException interrupted) {
						return;
					}
				}
			}
		}

		private void waitForInternet() {
			;
		}
	};
}
