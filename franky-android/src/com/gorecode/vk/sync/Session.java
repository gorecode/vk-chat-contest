package com.gorecode.vk.sync;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import roboguice.RoboGuice;

import android.os.Process;

import com.google.common.eventbus.EventBus;
import com.google.inject.Injector;
import com.gorecode.vk.activity.friends.FriendsModel;
import com.gorecode.vk.activity.search.FriendSuggestionsModel;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.cache.DialogsCache;
import com.gorecode.vk.config.ApplicationConfig;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.event.AuthTokenReceivedEvent;
import com.gorecode.vk.event.LoggedInEvent;
import com.gorecode.vk.event.LoggedOutEvent;
import com.uva.concurrent.AbstractScheduler;
import com.uva.io.FileStorage;
import com.uva.log.Log;
import com.uva.net.ConnectivityManager;

public class Session {
	private static final long USER_INACTIVITY_TIMEOUT = 15 * 60 * 1000;

	@SuppressWarnings("unused")
	private static final String TAG = Session.class.getSimpleName();

	private final FileStorage mStateStorage;
	private final SessionContext mState;
	private final EventBus mBus;
	private final VkModel mVk;

	private ScheduledExecutorService mExecutor;

	private volatile long mLastTimeSetOnline;

	public Session(EventBus eventBus, ConnectivityManager connectivityManager, AbstractScheduler scheduler, ApplicationConfig config, FileStorage instanceStateStore) {
		this.mBus = eventBus;

		this.mStateStorage = instanceStateStore;
		this.mState = SessionContext.restoreOrCreate(instanceStateStore);
		this.mVk = new VkModel(eventBus, mState);
	}

	public boolean hasAccessToken() {
		return mState.isUserAuthorized();
	}

	public void login(String username, String password) throws Exception {
		VkModel.AuthResult authResult = mVk.auth(username, password);

		String accessToken = authResult.accessToken;

		long uid = authResult.userId;

		mState.authResult = authResult;
		mState.user = Profile.empty(uid);

		syncUserNow();

		clearCaches();

		mBus.post(new AuthTokenReceivedEvent(mState.user, accessToken));
		mBus.post(new LoggedInEvent(mState.user));

		startUp();
	}

	public void clearCaches() {
		Injector injector = RoboGuice.getInjector(VkApplication.getApplication());

		DialogsCache dialogsCache = injector.getInstance(DialogsCache.class);

		dialogsCache.clearLastUpdateTime();
		dialogsCache.deleteAll();

		ChatCache chatCache = injector.getInstance(ChatCache.class);

		chatCache.deleteAll();

		FriendsModel friends = injector.getInstance(FriendsModel.class);

		friends.cleanup();

		FriendSuggestionsModel suggestions = injector.getInstance(FriendSuggestionsModel.class);

		suggestions.cleanup();
	}

	public void syncUserNow() throws Exception {
		mState.user = mVk.getUser(mState.getUserId());

		saveInstanceState();
	}

	public SessionContext getContext() {
		return mState;
	}

	public void saveInstanceState() {
		synchronized(this) {
			mState.saveInstanceState(mStateStorage);
		}
	}

	public void logout() {
		shutdown();

		mBus.post(new LoggedOutEvent());
		mState.authResult = null;
		mState.user = null;
		saveInstanceState();
	}

	public void startUp() {
		mExecutor = Executors.newSingleThreadScheduledExecutor();
		mExecutor.scheduleAtFixedRate(mMarkUserAsOnlineIfNeededSync, 0, USER_INACTIVITY_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	public void shutdown() {
		mExecutor.shutdownNow();
	}

	public void markUserAsOnlineIfNeeded() {
		mExecutor.submit(mMarkUserAsOnlineIfNeededSync);
	}

	private void markUserAsOnlineIfNeededSync() {
		boolean shouldMarkUserAsOnline = false;

		if (VkApplication.getApplication().isInForeground()) {
			if (System.currentTimeMillis() - mLastTimeSetOnline >= USER_INACTIVITY_TIMEOUT) {
				shouldMarkUserAsOnline = true;
			}
		}

		if (shouldMarkUserAsOnline) {
			try {
				Log.debug(TAG, "marking user as being online");

				mVk.setOnline();

				mLastTimeSetOnline = System.currentTimeMillis();
			} catch (Exception e) {
				Log.exception(TAG, "error marking user as online", e);
			}
		}
	}

	private final Runnable mMarkUserAsOnlineIfNeededSync = new Runnable() {
		@Override
		public void run() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

			markUserAsOnlineIfNeededSync();
		}
	};
}
