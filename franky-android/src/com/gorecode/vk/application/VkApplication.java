package com.gorecode.vk.application;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import roboguice.RoboGuice;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.util.Modules;
import com.gorecode.vk.activity.CaptchaActivity;
import com.gorecode.vk.activity.chat.ChatSender;
import com.gorecode.vk.activity.friends.FriendsModel;
import com.gorecode.vk.activity.search.FriendSuggestionsModel;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.audio.AudioNotificationController;
import com.gorecode.vk.audio.AudioPauseController;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.cache.DialogsCache;
import com.gorecode.vk.config.ApplicationConfig;
import com.gorecode.vk.config.Config;
import com.gorecode.vk.config.FileConfigStorage;
import com.gorecode.vk.config.LogConfigurator;
import com.gorecode.vk.event.AuthTokenReceivedEvent;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.imageloader.ImageLoaderConfiguration;
import com.gorecode.vk.service.C2DMHandler;
import com.gorecode.vk.service.NotificationService;
import com.gorecode.vk.sync.CacheSync;
import com.gorecode.vk.sync.Session;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.utilities.FileCache;
import com.gorecode.vk.utilities.UiThreadRunner;
import com.uva.concurrent.HandlerScheduler;
import com.uva.io.AndroidFileStorageImpl;
import com.uva.io.FileStorage;
import com.uva.io.JSEFileStorageImpl;
import com.uva.io.PlatformFileStorage;
import com.uva.location.AndroidLocationTracker;
import com.uva.location.LocationTracker;
import com.uva.log.AndroidNativeLogChannel;
import com.uva.log.AsyncChannel;
import com.uva.log.FormattingChannel;
import com.uva.log.JSEExceptionFormatter;
import com.uva.log.Log;
import com.uva.log.Message;
import com.uva.log.PPFFormatter;
import com.uva.log.SplitterChannel;
import com.uva.net.AndroidConnectivityManager;
import com.uva.net.AndroidHttpConnector;
import com.uva.net.JSESocketConnector;
import com.uva.net.NetworkEmulator;
import com.uva.net.PlatformHttpConnector;
import com.uva.net.PlatformSocketConnector;

public class VkApplication extends Application {
	public static final String TYPEFACE_MYRIAD = "myriad.ttf";
	public static final String TYPEFACE_HELVETICA = "helvetica.ttf";

	private final static String TAG = "FrankyApplication";
	private final static String CONFIG_FILENAME = "config.bin";

	private static final HashMap<String, Typeface> s_typefaces = new HashMap<String, Typeface>();

	private static VkApplication sApplication;

	//
	// System-level objects.
	//

	private AndroidLocationTracker mLocationTracker;

	private FileCache mFileCache;

	//
	// Application level 1 objects (common for every application).
	//

	private final SplitterChannel mLogChannel = new SplitterChannel();
	private ApplicationConfig mConfig;
	private FileConfigStorage mConfigStorage;

	//
	// Application level 2 objects (specific to our applcation).
	//

	private final NetworkEmulator mNetEmu = new NetworkEmulator();
	private Session mSession;
	private SessionContext mSessionContext;
	private ImageLoader mImageLoader;
	private AsyncEventBus mEventBus;
	private AndroidConnectivityManager mConnectivityManager;
	private SQLiteDatabase mDb;
	private ChatCache mChatCache;
	private DialogsCache mDialogsCache;

	public static VkApplication from(Context context) {
		return (VkApplication)context.getApplicationContext();
	}

	public void onCreate() {
		sApplication = this;

		super.onCreate();

		UiThreadRunner.setGlobalRunner(new UiThreadRunner());

		// dispatch events on UI thread
		mEventBus = new AsyncEventBus(new java.util.concurrent.Executor() {			
			@Override
			public void execute(Runnable r) {
				runOnUiThread(r);
			}
		});

		PlatformFileStorage.define(new FileStorage(new AndroidFileStorageImpl(this)));
		PlatformSocketConnector.define(new JSESocketConnector());
		PlatformHttpConnector.define(new AndroidHttpConnector());

		initializeLog();

		Thread.setDefaultUncaughtExceptionHandler(new EpicFailHandler());

		logBuildInfo();

		Log.debug(TAG, "Initializing application");

		mFileCache = new FileCache(this);
		mLocationTracker = new AndroidLocationTracker((LocationManager)getSystemService(Context.LOCATION_SERVICE));
		mConfigStorage = new FileConfigStorage(PlatformFileStorage.getDefined(), CONFIG_FILENAME);

		initializeConfig();

		enableAutoConfiguration();

		ImageLoaderConfiguration configuration = 
				new ImageLoaderConfiguration.Builder(this).
				maxImageWidthForMemoryCache(320).
				maxImageHeightForMemoryCache(480).
				build();

		mDb = new VkDbOpenHelper(this).getWritableDatabase();
		mChatCache = new ChatCache(mDb);
		mDialogsCache = new DialogsCache(mDb);

		mImageLoader = new ImageLoader();
		mImageLoader.init(configuration);

		mConnectivityManager = new AndroidConnectivityManager(this);

		mSession = new Session(
				mEventBus,
				new AndroidConnectivityManager(this),
				new HandlerScheduler(new Handler(Looper.getMainLooper())),
				mConfig,
				new FileStorage(new AndroidFileStorageImpl(this)));
		mSessionContext = mSession.getContext();

		RoboGuice.setBaseApplicationInjector(this, RoboGuice.DEFAULT_STAGE, Modules.override(RoboGuice.newDefaultRoboModule(this)).with(createGuiceModule()));

		Injector injector = RoboGuice.getInjector(this);

		FriendsModel friendsModel = injector.getInstance(FriendsModel.class);

		injector.getInstance(AudioPlayer.class).addListener(injector.getInstance(AudioNotificationController.class));

		mEventBus.register(new CacheSync(mDialogsCache, mChatCache));
		mEventBus.register(new NotificationService.LifetimeController(this));
		mEventBus.register(friendsModel);
		mEventBus.register(injector.getInstance(C2DMHandler.class));
		mEventBus.register(injector.getInstance(FriendSuggestionsModel.class));
		mEventBus.register(ChatSender.getPool());
		mEventBus.register(injector.getInstance(AudioPauseController.class));

		if (mSessionContext.isUserAuthorized()) {
			/// XXX: I suppose must be a better, the Android way to restore application state after killing.
			Log.message(TAG, "Application process is probably was killed, restoring global application state");

			mSession.startUp();

			NotificationService.start(this);

			mEventBus.post(new AuthTokenReceivedEvent(mSessionContext.getUser(), mSessionContext.getAccessToken()));
		}

		keepApplicationAlive();

		Log.debug(TAG, "Application initialized");
	}

	public Typeface getTypeface(String typefaceFilename) {
		Typeface tf = s_typefaces.get(typefaceFilename);

		if(tf == null) {
			tf = Typeface.createFromAsset(getAssets(), typefaceFilename);
			s_typefaces.put(typefaceFilename, tf);
		}

		return tf;
	}

	public final void runOnUiThread(Runnable action) {
		UiThreadRunner.get().runOnUiThread(action);
	}

	public static VkApplication getApplication() {
		return sApplication;
	}

	public boolean isInForeground() {
		Context context = this;
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
		if (appProcesses == null) {
			return false;
		}
		final String packageName = context.getPackageName();
		for (RunningAppProcessInfo appProcess : appProcesses) {
			if (appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
				return true;
			}
		}
		return false;
	}

	protected Module createGuiceModule() {
		return new Module() {
			@Override
			public void configure(Binder binder) {
				binder.bind(ImageLoaderConfiguration.class).toInstance(mImageLoader.getConfiguration());
				binder.bind(EventBus.class).toInstance(mEventBus);
				binder.bind(LocationTracker.class).toInstance(mLocationTracker);

				binder.bind(SQLiteDatabase.class).toInstance(mDb);

				binder.bind(ChatCache.class).toInstance(mChatCache);
				binder.bind(DialogsCache.class).toInstance(mDialogsCache);

				binder.bind(ApplicationConfig.class).toInstance(mConfig);
				binder.bind(ApplicationConfig.class).toInstance(mConfig);				
				binder.bind(Config.class).toInstance(mConfig);

				binder.bind(VkApplication.class).toInstance(VkApplication.this);
				binder.bind(Session.class).toInstance(mSession);
				binder.bind(FileCache.class).toInstance(mFileCache);
				binder.bind(ImageLoader.class).toInstance(mImageLoader);
				binder.bind(SessionContext.class).toInstance(mSessionContext);
				binder.bind(AndroidConnectivityManager.class).toInstance(mConnectivityManager);
				binder.bind(VkModel.class).toProvider(new Provider<VkModel>() {
					@Override
					public VkModel get() {
						VkModel vkModel = new VkModel(mEventBus, mSessionContext);
						vkModel.setCaptchaCallback(new CaptchaActivity.CaptchaKeyProvider(getApplicationContext()));
						return vkModel;
					}
				});
			}
		};
	}

	public SessionContext getSessionContext() {
		return mSessionContext;
	}

	public Session getSession() {
		return mSession;
	}

	public Injector getInjector() {
		return RoboGuice.getInjector(this);
	}

	public AndroidLocationTracker getLocationTracker() {
		return mLocationTracker;
	}

	public FileConfigStorage getConfigStorage() {
		return mConfigStorage;
	}

	public ApplicationConfig getConfig() {
		return mConfig;
	}

	public EventBus getEventBus() {
		return mEventBus;
	}

	public String getVersionName() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException shouldNeverHappen) {
			throw new RuntimeException("Can't determine app version: " + shouldNeverHappen.getMessage());
		}
	}

	public ImageLoader getImageLoader() {
		return mImageLoader;
	}

	public AndroidConnectivityManager getConnectivityManager() {
		return mConnectivityManager;
	}

	public void saveConfig() {
		try {
			mConfigStorage.saveConfigState(mConfig);
		} catch (IOException e) {
			Log.exception(TAG, e);
		}
	}

	private void logBuildInfo() {
		Log.message(TAG, "Build mode = " + (BuildInfo.IS_DEBUG_BUILD ? "debug" : "release"));
		Log.message(TAG, "Build date = " + BuildInfo.BUILD_DATE);
		Log.message(TAG, "Build number = " + BuildInfo.BUILD_NUMBER);
		Log.message(TAG, "Build revision = " + BuildInfo.BUILD_REVISION);
	}

	private void enableAutoConfiguration() {
		JSEFileStorageImpl sdcard = new JSEFileStorageImpl(Environment.getExternalStorageDirectory());

		LogConfigurator logConfigurator = new LogConfigurator(new FileStorage(sdcard), mConfig, mLogChannel);

		logConfigurator.configureLogChannel();

		mConfig.addListener(logConfigurator);
	}

	private void initializeLog() {		
		mLogChannel.addChannel(new AndroidNativeLogChannel());

		Log.initialize(new FormattingChannel(new PPFFormatter(), mLogChannel), new JSEExceptionFormatter(), Message.DUMP);
	}

	private void initializeConfig() {
		mConfig = new ApplicationConfig();

		try {
			Log.trace(TAG, "Loading config from storage");
			mConfigStorage.restoreConfigState(mConfig);
			Log.trace(TAG, "Loaded");			
		} catch (IOException e) {
			Log.exception(TAG, Message.WARNING, "Failed to load configuration", e);		
		}
	}

	private void keepApplicationAlive() {
		// How prevent application from being killed by Android? Just restart it every 30 minutes.

		PendingIntent serviceIntent = PendingIntent.getService(this, 0, new Intent(this, NotificationService.class), PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager alarmService = (AlarmManager)getSystemService(ALARM_SERVICE);

		alarmService.setRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, serviceIntent);
	}
}
