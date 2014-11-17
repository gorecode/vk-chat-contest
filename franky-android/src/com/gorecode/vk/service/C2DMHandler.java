package com.gorecode.vk.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;

import com.google.android.c2dm.C2DMessaging;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.event.AuthTokenReceivedEvent;
import com.gorecode.vk.event.LoggedOutEvent;
import com.gorecode.vk.sync.Session;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

@Singleton
public class C2DMHandler {
	public static final String SENDER_PROJECT_ID = "1031619612146";

	private static final String TAG = C2DMHandler.class.getSimpleName();

	private static final String COLLAPSE_KEY_MESSAGE = "vkmsg";
	private static final String COLLAPSE_KEY_FRIENDSHIP_OFFER = "vkfriend";

	private static final String TYPE_MESSAGE = "msg";
	private static final String TYPE_FRIENDSHIP_OFFER = "friend";

	private static final String EXTRA_COLLAPSE_KEY = "collapse_key";
	private static final String EXTRA_COLLAPSE = "collapse";
	private static final String EXTRA_TYPE = "type";

	private static final String PREFERENCE_STATE = "c2dm:state";

	private static final int STATE_SHUTDOWN = 0x0;
	private static final int STATE_REGISTERING_IN_C2DM = 0x1;
	private static final int STATE_REGISTERING_IN_APPLICATION_SERVER = 0x2;
	private static final int STATE_READY = 0x3;

	private static final boolean DEBUG = true;

	private static final ExecutorService sExecutor = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), ThreadFactories.WITH_LOWEST_PRIORITY);

	private final Context mContext;
	private final SharedPreferences mPrefs;

	private final VkModel mVk;
	private final Session mSession;

	private final ChatCache mChatCache;

	@Inject
	public C2DMHandler(VkModel vk, Session session, ChatCache chatCache) {
		mSession = session;
		mVk = vk;
		mChatCache = chatCache;
		mContext = VkApplication.getApplication();
		mPrefs = mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE);
	}

	@Subscribe
	public void onAuthTokenReceived(AuthTokenReceivedEvent event) {
		Log.debug(TAG, "Got auth token received event");

		register();
	}

	@Subscribe
	public void onLogout(LoggedOutEvent event) {
		Log.debug(TAG, "Got logout event");

		unregister();
	}

	public Context getContext() {
		return mContext;
	}

	public void register() {
		setState(STATE_REGISTERING_IN_C2DM);

		Log.debug(TAG, "requesting c2dm token");

		C2DMessaging.register(mContext, SENDER_PROJECT_ID);
	}

	public void unregister() { 
		if (getState() == STATE_SHUTDOWN) {
			return;
		}

		setState(STATE_SHUTDOWN);

		unregisterFromApplicationServer();

		Log.debug(TAG, "unregistering from c2dm");

		C2DMessaging.unregister(mContext);
	}

	public void onRegistered(Context context, String registrationId) {
		Log.debug(TAG, "have new C2DM registertaion id = " + registrationId);

		if (shouldBeShutdown()) {
			return;
		}

		registerInApplicationServer(registrationId);
	}

	public void onUnregistered(Context context) {
		Log.debug(TAG, "unregistered from C2DM");
	}

	public void onError(Context context, String errorId) {
		Log.error(TAG, "Error registering application in C2DM: " + errorId);
	}

	public void onMessage(Context context, Intent intent) {
		if (DEBUG) {
			Log.debug(TAG, "C2DM message received");
		}

		if (shouldBeShutdown()) {
			return;
		}

		handleMessage(context, intent);
	}

	protected void registerInApplicationServerSync(String registrationId) throws Exception {
		mVk.registerDeviceForPushNotifications(registrationId, true);
	}

	protected void handleMessage(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();

		if (DEBUG) {
			for (String key : bundle.keySet()) {
				Log.trace(TAG, key + " = " + bundle.get(key));
			}

			Log.debug(TAG, "c2dm bundle = " + bundle);
		}

		String collapse = bundle.getString(EXTRA_COLLAPSE);
		String collapseKey = bundle.getString(EXTRA_COLLAPSE_KEY);
		String type = bundle.getString(EXTRA_TYPE);

		if (COLLAPSE_KEY_MESSAGE.equals(collapseKey) || COLLAPSE_KEY_MESSAGE.equals(collapse) || TYPE_MESSAGE.equals(type)) {
			Log.debug(TAG, "new push message");

			if (bundle.containsKey("badge")) {
				final int numUnreadMessages = Integer.parseInt(bundle.getString("badge"));

				mSession.getContext().getUser().notificationSummary.numMessages = numUnreadMessages;
				mSession.getContext().notifyUnhandledNotificationsUpdated();
			}

			long mid = Integer.parseInt(bundle.getString("msg_id"));

			try {
				if (mChatCache.findByMid(mid).iterator().hasNext()) {
					Log.debug(TAG, "message is already in cache, skipping notification");
					return;
				}
			} catch (Exception e) {
				return;
			}

			NotificationService.handlePushMessage(context, mid, bundle.getString("text"));
		} else if (COLLAPSE_KEY_FRIENDSHIP_OFFER.equals(collapseKey) || COLLAPSE_KEY_FRIENDSHIP_OFFER.equals(collapse) || TYPE_FRIENDSHIP_OFFER.equals(type)) {
			Log.debug(TAG, "new push friendship offer");

			Profile sender = Profile.empty(Integer.parseInt(bundle.getString("uid")));

			sender.firstName = bundle.getString("first_name");
			sender.lastName = bundle.getString("last_name");

			NotificationService.handlePushOffer(getContext(), sender);
		} else {
			Log.warning(TAG, "unknown collapse key = " + collapseKey);
		}
	}

	protected void unregisterFromApplicationServerSync(String registrationId) throws Exception {
		mVk.unregisterDeviceFromPushNotifications(registrationId);
	}

	private void unregisterFromApplicationServer() {
		final String registrationId = C2DMessaging.getRegistrationId(mContext);

		if (Strings.isNullOrEmpty(registrationId)) return;

		sExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				Log.debug(TAG, "unregistering device from push notifications from application server");

				try {
					unregisterFromApplicationServerSync(registrationId);
				} catch (Exception e) {
					Log.exception(TAG, "Error unregistering device from push notifications", e);
				}
			}
		});
	}

	private void registerInApplicationServer(final String registrationId) {
		setState(STATE_REGISTERING_IN_APPLICATION_SERVER);

		sExecutor.submit(new Runnable() {
			@Override
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				try {
					Log.debug(TAG, "registering c2dm token in application server");

					registerInApplicationServerSync(registrationId);

					setState(STATE_READY);
				} catch (Exception e) {
					Log.exception(TAG, "Error registering C2DM token in application server", e);
				}
			}
		});
	}

	private boolean shouldBeShutdown() {
		if (getState() == STATE_SHUTDOWN) {
			if (DEBUG) {
				Log.debug(TAG, "state is SHUTDOWN, ignoring event");
			}

			return true;
		} else {
			return false;
		}
	}

	private void setState(int state) {
		if (DEBUG) {
			Log.debug(TAG, "state = " + state);
		}

		synchronized (this) {
			mPrefs.edit().putInt(PREFERENCE_STATE, state).commit();
		}
	}

	private int getState() {
		synchronized (this) {
			return mPrefs.getInt(PREFERENCE_STATE, STATE_SHUTDOWN);
		}
	}
}
