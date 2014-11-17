package com.gorecode.vk.activity.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.Process;

import com.google.common.eventbus.Subscribe;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.event.LoggedOutEvent;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

public class ChatSender {
	public interface DispatchCallbacks {
		public void onSuccess(ChatSender sender, ChatMessage local, long mid);
		public void onError(ChatSender sender, ChatMessage message, Exception error);
	}

	private static final String TAG = ChatSender.class.getSimpleName();

	private static final Pool sPool = new Pool();

	private volatile static long sLocalIdsTaken;

	private final List<ChatMessage> mPending;
	private final List<ChatMessage> mError;
	private final ExecutorService mExecutor;
	private final VkModel mVk;
	private final Dialog mDialog;

	private DispatchCallbacks mDispatchCallbacks;

	public static Pool getPool() {
		return sPool;
	}

	public ChatSender(Dialog dialog, VkModel vk) {
		mDialog = dialog;
		mVk = vk;

		mPending = new ArrayList<ChatMessage>(128);
		mError = new ArrayList<ChatMessage>(128);
		mExecutor = new ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(256), ThreadFactories.WITH_LOWEST_PRIORITY);
	}

	public void setDispatchCallbacks(DispatchCallbacks callbacks) {
		mDispatchCallbacks = callbacks;
	}

	public Collection<ChatMessage> getPendingMessages() {
		ArrayList<ChatMessage> copy = new ArrayList<ChatMessage>();

		synchronized (this) {
			copy.addAll(mPending);
		}

		return copy;
	}
	public int getLocalMessagesCount() {
		synchronized (this) {
			return mPending.size() + mError.size();
		}
	}

	public ChatMessage enqueueForDispatch(ChatMessage.Content content) {
		Log.debug(TAG, String.format("put message %s into queue for sending", content.text));

		final ChatMessage local = new ChatMessage();

		if (mDialog.isConference()) {
			local.chatId = mDialog.getCid();
		} else {
			local.user = mDialog.getParticipant();
		}

		local.content = content;
		local.unread = true;
		local.direction = ChatMessage.DIRECTION_OUTGOING;
		local.timestamp = System.currentTimeMillis();
		local.id = Long.MAX_VALUE - sLocalIdsTaken++;

		synchronized (this) {
			mPending.add(local);
		}

		mExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				sendSync(local);
			}
		});

		return local;
	}

	private void sendSync(ChatMessage local) {
		Log.debug(TAG, String.format("sending message %s", local.content.text));

		try {
			long mid = -1;

			if (mDialog.isConference()) {
				mid = mVk.sendMessageToGroup(mDialog.getCid(), local.content);
			} else {
				mid = mVk.sendMessageToUser(mDialog.getCid(), local.content);
			}

			Log.message(TAG, String.format("message %s sent", local.content.text));

			synchronized (this) {
				mPending.remove(local);
			}

			DispatchCallbacks callbacks = mDispatchCallbacks;

			if (callbacks != null) {
				callbacks.onSuccess(this, local, mid);
			}			
		} catch (Exception e) {
			Log.exception(TAG, "Error sending message", e);

			synchronized (this) {
				mPending.remove(local);

				mError.add(local);	
			}

			DispatchCallbacks callbacks = mDispatchCallbacks;

			if (callbacks != null) {
				callbacks.onError(this, local, e);
			}
		}
	}

	public static class Pool {
		private final Map<Long, ChatSender> mSenders = new HashMap<Long, ChatSender>();

		public ChatSender getSenderForDialog(Dialog dialog, VkModel vk) {
			long key = dialog.getCid();

			ChatSender sender = mSenders.get(key);

			if (sender == null) {
				sender = new ChatSender(dialog, vk);

				mSenders.put(key, sender);
			}

			return sender;
		}

		@Subscribe
		public void onLogout(LoggedOutEvent event) {
			for (ChatSender sender : mSenders.values()) {
				sender.mExecutor.shutdownNow();
			}
			mSenders.clear();
		}
	}
}