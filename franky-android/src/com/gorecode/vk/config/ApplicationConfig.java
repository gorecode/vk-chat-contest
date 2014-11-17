package com.gorecode.vk.config;

import com.gorecode.vk.application.BuildInfo;
import com.gorecode.vk.config.internal.BooleanValidator;
import com.gorecode.vk.config.internal.IntegerValidator;
import com.uva.log.Message;

// FIXME: Унаследовано из BB кода. Заменить на SharedPreferences.
public class ApplicationConfig extends Config {
	public static final PreferenceScheme LOG_LEVEL;
	public static final PreferenceScheme LOG_TO_FILE;

	public static final PreferenceScheme PUSH_NOTIFICATIONS_ENABLED = PreferenceScheme.createBooleanScheme("push_enabled", true);
	public static final PreferenceScheme PUSH_NOTIFICATIONS_SOUND = PreferenceScheme.createBooleanScheme("push_sound", true);
	public static final PreferenceScheme PUSH_NOTIFICATIONS_VIBRATION = PreferenceScheme.createBooleanScheme("push_vibration", true);

	static {
		LOG_LEVEL = new PreferenceScheme(
			"logLevel",
			Integer.toString(BuildInfo.IS_DEBUG_BUILD ? Message.DEBUG : Message.INFORMATION),
			new IntegerValidator(Message.CRITICAL_ERROR, Message.DUMP)
		);
		
		LOG_TO_FILE = new PreferenceScheme(
			"logToFile", 
			String.valueOf(false),
			new BooleanValidator()
		);
	}

	public static ConfigScheme createScheme() {
		ConfigSchemeBuilder builder = new ConfigSchemeBuilder();

		builder.add(LOG_LEVEL);
		builder.add(LOG_TO_FILE);
		builder.add(PUSH_NOTIFICATIONS_ENABLED);
		builder.add(PUSH_NOTIFICATIONS_SOUND);
		builder.add(PUSH_NOTIFICATIONS_VIBRATION);

		return builder.compile();
	}

	public ApplicationConfig() {
		super(ApplicationConfig.createScheme());
	}

	ApplicationConfig(ConfigScheme scheme) {
		super(scheme);
	}

	public boolean isNotificationsEnabled() {
		return getBoolean(PUSH_NOTIFICATIONS_ENABLED);
	}

	public boolean isNotificationSoundEnabled() {
		return getBoolean(PUSH_NOTIFICATIONS_SOUND);
	}

	public boolean isNotificationVibrationEnabled() {
		return getBoolean(PUSH_NOTIFICATIONS_VIBRATION);
	}
}
