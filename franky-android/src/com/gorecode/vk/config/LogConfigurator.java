package com.gorecode.vk.config;

import java.io.IOException;

import com.uva.io.FileStorage;
import com.uva.lang.BooleanUtilities;
import com.uva.log.Log;
import com.uva.log.Message;
import com.uva.log.SplitterChannel;
import com.uva.log.StreamChannel;

public class LogConfigurator implements Config.Listener {
	private static final String TAG = "LogAutoConfigure";
	private static final String LOG_FILE = "franky.log";

	protected final SplitterChannel mLogSplitter;
	protected final ApplicationConfig mConfig;
	protected final FileStorage mFileStorage;

	private StreamChannel mFileChannel;

	public LogConfigurator(FileStorage fileStorage, ApplicationConfig config, SplitterChannel splitter) {
		this(fileStorage, config, splitter, true);
	}

	public LogConfigurator(FileStorage fileStorage, ApplicationConfig config, SplitterChannel splitter, boolean autoAtach) {
		mFileStorage = fileStorage;
		mLogSplitter = splitter;
		mConfig = config;

		if (autoAtach) {
			mConfig.addListener(this);
		}
	}

	public void configureLogChannel() {
		reactOnConfig();
	}

	public void onConfigOptionSet(Config sender, PreferenceScheme scheme, String value) {
		if (scheme == ApplicationConfig.LOG_LEVEL) {
			Log.setSeverityFilter(sender.getInteger(ApplicationConfig.LOG_LEVEL));
		} else if (scheme == ApplicationConfig.LOG_TO_FILE) {
			boolean enabled = BooleanUtilities.parse(value);

			enableFileLogging(enabled);
		}
	}

	public void enableFileLogging(boolean enabled) {
		if (enabled) {
			enableFileLogging(false);

			createFileLogChannel();
			attachFileLogChannel();
		} else {
			detachFileLogChannel();
			deleteFileLogChannel();
		}
	}

	protected void reactOnConfig() {
		PreferenceScheme intrestingOptions[] = new PreferenceScheme[] { ApplicationConfig.LOG_LEVEL, ApplicationConfig.LOG_TO_FILE };

		for (int i = 0; i < intrestingOptions.length; i++) {
			PreferenceScheme each = intrestingOptions[i];

			onConfigOptionSet(mConfig, each, mConfig.getString(each));
		}
	}

	private void createFileLogChannel() {
		try {
			Log.debug(TAG, "Creating log file channel");
			mFileChannel = new StreamChannel(mFileStorage.openFileOutput(LOG_FILE, true));
			Log.debug(TAG, "Log file channel created");
		} catch (IOException e) {
			Log.exception(TAG, Message.ERROR, e);
		}
	}

	private void deleteFileLogChannel() {
		if (mFileChannel == null) {
			return;
		}
		mFileChannel.close();
		mFileChannel = null;
	}

	private void attachFileLogChannel() {
		if (mFileChannel == null) {
			return;
		}

		mLogSplitter.addChannel(mFileChannel);
	}

	private void detachFileLogChannel() {
		if (mFileChannel == null) {
			return;
		}

		mLogSplitter.removeChannel(mFileChannel);    
	}
}
