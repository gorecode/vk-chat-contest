package com.gorecode.vk.service;

import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import roboguice.service.RoboService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;

import com.gorecode.vk.R;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.activity.MainActivity;
import com.gorecode.vk.activity.search.FriendSuggestionsModel;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.config.ApplicationConfig;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UnhandledNotifications;
import com.gorecode.vk.event.ChatMessageStateChangedEvent;
import com.gorecode.vk.event.DialogDeletedEvent;
import com.gorecode.vk.event.FriendshipOfferedEvent;
import com.gorecode.vk.event.LoggedInEvent;
import com.gorecode.vk.event.LoggedOutEvent;
import com.gorecode.vk.event.LongPollConnectionRestoredEvent;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.sync.SessionContext.OnUnhandledNotificationsUpdateListener;
import com.gorecode.vk.utilities.BundleUtilities;
import com.uva.lang.StringUtilities;
import com.uva.lang.ThreadFactories;
import com.uva.log.Log;

public class NotificationService extends RoboService implements OnUnhandledNotificationsUpdateListener, FriendSuggestionsModel.OnModelChangedListener {
	private static final boolean DEBUG = false;

	private static final long UNHANDLED_NOTIFICATIONS_UPDATE_INTERVAL = DEBUG ? 1000 * 30 : 60 * 60 * 1000;

	private static int ALARM_POLICY_SIGNAL = 0x0;
	private static int ALARM_POLICY_SIGNAL_IF_NO_NOTIFICATION = 0x1;
	private static int ALARM_POLICY_DONT_SIGNAL = 0x2;

	private static final String EXTRA_CHAT_MESSAGE_ID = "message_id";
	private static final String EXTRA_CHAT_MESSAGE_TEXT = "message_text";

	private static final String EXTRA_FRIENDSHIP_OFFER_SENDER = "friend";

	private static final int ID_YOU_GOT_NEW_NOTIFICATIONS = R.id.unhandled_notifications_id;

	private static final String TAG = NotificationService.class.getSimpleName();

	public static class LifetimeController {
		private final Context mContext;

		public LifetimeController(Context context) {
			mContext = context;
		}

		@Subscribe
		public void onLogin(LoggedInEvent event) {
			NotificationService.start(mContext);
		}

		@Subscribe
		public void onLogout(LoggedOutEvent event) {
			NotificationService.stop(mContext);
		}
	}

	public class Binder extends android.os.Binder {
		public NotificationService getService() {
			return NotificationService.this;
		}
	}

	private final Handler handler = new Handler(Looper.getMainLooper());

	private final Binder binder = new NotificationService.Binder();

	@Inject
	private SessionContext sessionData;
	@Inject
	private LongPoll longPoll;
	@Inject
	private ApplicationConfig config;
	@Inject
	private EventBus bus;
	@Inject
	private VkModel vk;
	@Inject
	private FriendSuggestionsModel offers;

	private ScheduledThreadPoolExecutor executorService;

	private volatile boolean isDestroyed;

	private Notification lastNotification;

	private static NotificationService sharedInstance;

	public static NotificationService getSharedInstance() {
		return sharedInstance;
	}

	public static void handlePushMessage(Context context, long mid, String text) {
		Intent intent = new Intent(context, NotificationService.class);

		intent.putExtra(EXTRA_CHAT_MESSAGE_ID, mid);
		intent.putExtra(EXTRA_CHAT_MESSAGE_TEXT, text);

		context.startService(intent);		
	}

	public static void handlePushOffer(Context context, Profile user) {
		Intent intent = new Intent(context, NotificationService.class);

		BundleUtilities.putExtra(intent, EXTRA_FRIENDSHIP_OFFER_SENDER, user);

		context.startService(intent);		
	}

	public static void start(Context context) {
		Intent intent = new Intent(context, NotificationService.class);

		context.startService(intent);
	}

	public static void stop(Context context) {
		Intent intent = new Intent(context, NotificationService.class);

		context.stopService(intent);
	}

	@Override
	public void onCreate() {
		Log.debug(TAG, "Starting service");

		super.onCreate();

		sharedInstance = this;

		bus.register(this);

		offers.addOnModelChangedListener(this);

		syncNumOffersWithModel();

		sessionData.addOnUnhandledNotificationsUpdateListener(this);

		longPoll.startPoll();

		executorService = new ScheduledThreadPoolExecutor(1, ThreadFactories.WITH_LOWEST_PRIORITY);
		executorService.scheduleAtFixedRate(syncCountersRunnable, 0, UNHANDLED_NOTIFICATIONS_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);

		updateNotification(ALARM_POLICY_SIGNAL);

		Log.message(TAG, "Service started");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.debug(TAG, "Destroying service");

		sharedInstance = null;

		super.onDestroy();

		isDestroyed = true;

		executorService.shutdownNow();

		longPoll.abortPoll();

		bus.unregister(this);

		sessionData.removeOnUnhandledNotificationsUpdateListener(this);

		offers.removeOnModelChangedListener(this);

		removeNotification();

		Log.message(TAG, "Service destroyed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onUnhandledNotificationsUpdate() {
		runOnUiThreadSafely(new Runnable() {
			@Override
			public void run() {
				if (getCounters().summaryCount() == 0) {
					removeNotification();
				}
			}
		});
	}

	private void handleCommand(Intent intent) {
		// Handle command.

		if (intent == null) return;

		Bundle extras = intent.getExtras();

		if (extras == null) return;

		if (extras.containsKey(EXTRA_CHAT_MESSAGE_ID) && extras.containsKey(EXTRA_CHAT_MESSAGE_TEXT)) {
			final long mid = extras.getLong(EXTRA_CHAT_MESSAGE_ID);

			String text = extras.getString(EXTRA_CHAT_MESSAGE_TEXT);

			updateNotification(ALARM_POLICY_SIGNAL, text);

			new Thread() {
				public void run() {
					try {
						for (ChatMessage message : vk.getMessagesByIds(Collections.singleton(mid))) {
							bus.post(message);
						}
					} catch (Exception e) {
						Log.exception(TAG, "Error getting push message", e);
					}
				}
			}.start();
		}

		if (extras.containsKey(EXTRA_FRIENDSHIP_OFFER_SENDER)) {
			Profile user = BundleUtilities.getProfile(extras, EXTRA_FRIENDSHIP_OFFER_SENDER);

			bus.post(new FriendshipOfferedEvent(user));
		}
	}

	@Subscribe
	public void onLongPollConnectionRestored(LongPollConnectionRestoredEvent event) {
		executorService.submit(syncCountersRunnable);
	}

	@Subscribe
	public void onDialogDeleted(DialogDeletedEvent event) {
		executorService.submit(syncCountersRunnable);
	}
	
	@Subscribe
	public void onMessageStateChanged(final ChatMessageStateChangedEvent event) {
		if (Boolean.TRUE.equals(event.isRead)) {
			getCounters().numMessages = Math.max(0, getCounters().numMessages - 1);

			notifyCountersChanged();
		}
	}

	@Subscribe
	public void onNewMessage(final ChatMessage message) {
		if (message.isOutgoing()) return;

		getCounters().numMessages++;

		notifyCountersChanged();

		updateNotification(ALARM_POLICY_SIGNAL, message.content.text);
	}

	@Subscribe
	public void onNewOffer(final FriendshipOfferedEvent event) {
		String name = event.getSender().getFullname();

		String title = String.format(getString(R.string.notification_title_for_offer_format, name));

		updateNotification(ALARM_POLICY_SIGNAL, title);
	}

	@Override
	public void onModelChanged(FriendSuggestionsModel model) {
		syncNumOffersWithModel();
	}

	private void syncNumOffersWithModel() {
		getCounters().numOffers = offers.getOffers().size();

		notifyCountersChanged();
	}

	public void removeNotification() {
		Log.trace(TAG, "Removing notification from notification area");
		lastNotification = null;
		stopForeground(true);
		Log.debug(TAG, "Notification have been removed from notification area");
	}

	private void updateNotification(int alarmPolicy) {
		updateNotification(alarmPolicy, null);
	}

	private void updateNotification(int alarmPolicy, String ticker) {
		if (isDestroyed || VkApplication.from(this).isInForeground()) {
			removeNotification();
		} else {
			removeNotification();

			if (!config.isNotificationsEnabled()) return;

			UnhandledNotifications unhandled = getCounters();

			final int total = unhandled.summaryCount();

			Log.debug(TAG, String.format("Updating system notification, total = %d", total));

			Notification notification = new Notification(R.drawable.application_icon, ticker != null ? ticker : getString(R.string.notification_default_ticker), System.currentTimeMillis());

			boolean shouldSignal = false;

			if (alarmPolicy == ALARM_POLICY_SIGNAL) {
				shouldSignal = true;
			}
			if (alarmPolicy == ALARM_POLICY_SIGNAL_IF_NO_NOTIFICATION) {
				shouldSignal = lastNotification == null;
			}
			if (alarmPolicy == ALARM_POLICY_DONT_SIGNAL) {
				shouldSignal = false;
			}

			if (shouldSignal) {
				notification.defaults = Notification.DEFAULT_LIGHTS;

				if (config.isNotificationSoundEnabled()) {
					notification.sound = Uri.parse(String.format("android.resource://%s/raw/%d", getPackageName(), R.raw.alert_sound));
				}
				if (config.isNotificationVibrationEnabled()) {
					notification.defaults = notification.defaults | Notification.DEFAULT_VIBRATE;
				}
			}

			String contentTitle = getString(R.string.application_name);
			String contentText = formatNotificationText(unhandled);
			Intent contentIntent = new Intent(this, MainActivity.class);
			contentIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

			notification.setLatestEventInfo(this, contentTitle, contentText, PendingIntent.getActivity(this, 0, contentIntent, 0));

			Log.debug(TAG, "notification.sound = " + notification.sound);

			startForeground(ID_YOU_GOT_NEW_NOTIFICATIONS, notification);

			lastNotification = notification;

			Log.debug(TAG, "System notification updated");
		}
	}

	private String formatNotificationText(UnhandledNotifications unhandled) {
		final int messages = unhandled.numMessages;
		final int offers = unhandled.numOffers;

		String messagesPart = (messages == 0) ? "" : String.format(getString(R.string.notification_content_text_messages_part_format), messages);
		String offersPart = (offers == 0) ? "" : String.format(getString(R.string.notification_content_text_offers_part_format), offers);

		String content = "";

		if (messages > 0 && offers > 0) {
			content = StringUtilities.join(getString(R.string.notification_content_text_separator), messagesPart, offersPart);
		} else if (messages > 0) {
			content = messagesPart;
		} else if (offers > 0) {
			content = offersPart;
		}

		return String.format(getString(R.string.notification_content_text_format), content);
	}

	private void runOnUiThreadSafely(final Runnable target) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (isDestroyed) return;

				target.run();
			}
		});
	}

	private UnhandledNotifications getCounters() {
		return sessionData.getUser().notificationSummary;
	}

	private void setCounters(UnhandledNotifications counters) {
		sessionData.getUser().notificationSummary = counters;
	}

	private void notifyCountersChanged() {
		sessionData.notifyUnhandledNotificationsUpdated();
	}

	private final Runnable syncCountersRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

				Log.debug(TAG, "Updating unhandled notifications");
				UnhandledNotifications notificationsCounter = vk.getUnhandledNotifications();
				setCounters(notificationsCounter);
				notifyCountersChanged();
				Log.debug(TAG, "Unhandled notifications updated");
			} catch (Exception e) {
				Log.exception(TAG, "Unable to update unahndled notifications", e);
			}
		}
	};
}
