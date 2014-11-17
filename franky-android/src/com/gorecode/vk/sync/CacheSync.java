package com.gorecode.vk.sync;


import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.cache.DialogsCache;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.event.ChatMessageStateChangedEvent;
import com.gorecode.vk.event.DialogChangedEvent;
import com.gorecode.vk.event.DialogDeletedEvent;
import com.gorecode.vk.event.LoggedInEvent;
import com.gorecode.vk.event.LoggedOutEvent;
import com.uva.log.Log;

public class CacheSync {
	private static final String TAG = CacheSync.class.getSimpleName();

	private final DialogsCache mDialogs;
	private final ChatCache mChats;

	@Inject
	public CacheSync(DialogsCache dialogs, ChatCache chats) {
		mDialogs = dialogs;
		mChats = chats;
	}

	@Subscribe
	public void onLogout(LoggedOutEvent event) {
		mDialogs.clearLastUpdateTime();
		mDialogs.deleteAll();
		mChats.deleteAll();
	}

	@Subscribe
	public void onNewMessage(ChatMessage message) {
		Log.debug(TAG, "onNewMessage()");

		handleMessage(message);
	}

	@Subscribe
	public void onDialogChanged(DialogChangedEvent event) {
		Log.debug(TAG, "onDialogChanged()");

		mDialogs.saveAsync(event.dialog);
	}

	@Subscribe
	public void onDialogDeleted(DialogDeletedEvent event) {
		mDialogs.deleteEntity(event.dialog);
	}

	@Subscribe
	public void onMessageStateChange(ChatMessageStateChangedEvent event) {
		Log.debug(TAG, "onMessageStateChange()");

		if (Boolean.TRUE.equals(event.isDeleted)) {
			mChats.deleteByMid(event.messageId);
		} else {
			ChatMessage message = mChats.findOneByMid(event.messageId);

			if (message == null) {
				Log.warning(TAG, String.format("message with id %d not found", event.messageId));

				return;
			}

			event.apply(message);

			handleMessage(message);
		}
	}

	private void handleMessage(ChatMessage message) {
		mChats.saveAsync(message);

		Dialog dialog = mDialogs.findByCid(message.getCid());

		if (dialog == null) {
			dialog = Dialog.withOneMessage(message);
		} else {
			if (dialog.lastMessage.timestamp < message.timestamp) {
				dialog.lastMessage = message;
			}
		}

		mDialogs.saveAsync(dialog);		
	}
}
