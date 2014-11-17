package com.gorecode.vk.config;

import java.util.Enumeration;

import org.json.JSONException;
import org.json.JSONObject;

import com.uva.lang.BooleanUtilities;
import com.uva.location.Location;
import com.uva.log.Log;
import com.uva.utilities.AssertCompat;
import com.uva.utilities.ObserverCollection;

public class Config {
	private static final String TAG = "Config";

	public static interface Listener {
		public void onConfigOptionSet(Config sender, PreferenceScheme scheme, String value);
	}

	private final ObserverCollection<Listener> mListeners = new ObserverCollection<Listener>();
	private final JSONObject mValues = new JSONObject();
	private final ConfigScheme mScheme;

	public Config(ConfigScheme scheme) {
		AssertCompat.notNull(scheme, "Scheme");

		mScheme = scheme;
	}

	public ConfigScheme getConfigScheme() {
		return mScheme;
	}

	public JSONObject exportValues() {
		try {
			return new JSONObject(mValues.toString());
		} catch (Exception e) {
			return null;
		}
	}

	public void importValues(JSONObject state) {
		if (state == null) {
			throw new IllegalArgumentException("Config state cannot be null");
		}

		PreferenceScheme[] preferenceSchemes = mScheme.getAllPreferenceSchemes();

		for (int i = 0; i < preferenceSchemes.length; i++) {
			PreferenceScheme prefScheme = preferenceSchemes[i];

			try {
				setPreference(prefScheme, state.optString(prefScheme.key));
			} catch (IllegalArgumentException e) {
				setPreference(prefScheme, prefScheme.defaultValue);
			}
		}
	}

	public void addListener(Listener listener) {
		mListeners.add(listener);
	}

	public void removeListener(Listener listener) {
		mListeners.remove(listener);
	}

	public Location getLocation(PreferenceScheme opt) {
		String stringVal = getString(opt);

		if (stringVal == null) {
			return null;
		} else {
			try {
				return Location.parseLocation(stringVal);
			} catch (IllegalArgumentException e) {
				return null;
			}
		}
	}

	public boolean getBoolean(PreferenceScheme scheme) {
		String strVal = getString(scheme);

		return BooleanUtilities.parse(strVal);
	}

	public long getLong(PreferenceScheme scheme) {
		String strVal = getString(scheme);

		return Long.parseLong(strVal);
	}

	public int getInteger(PreferenceScheme scheme) {
		String strVal = getString(scheme);

		return Integer.parseInt(strVal);
	}

	public String getString(PreferenceScheme scheme) {
		if (scheme == null) {
			throw new IllegalArgumentException("Option cannot be null");
		}

		return mValues.optString(scheme.key, scheme.defaultValue);
	}

	public void setPreference(PreferenceScheme scheme, long value) {
		setPreference(scheme, Long.toString(value));
	}

	public void setPreference(PreferenceScheme scheme, int value) {
		setPreference(scheme, Integer.toString(value));
	}

	public void setPreference(PreferenceScheme scheme, boolean value) {
		setPreference(scheme, String.valueOf(value));
	}

	public void setPreference(PreferenceScheme scheme, String value) {
		AssertCompat.notNull(scheme, "Scheme");

		Log.trace(TAG, "Setting preference with '" + scheme.key + "' to " + value);

		scheme.valueValidator.validate(value);

		try {
			mValues.put(scheme.key, value);
		} catch (JSONException e) {
			shouldNeverHappen(e);
		}

		Log.trace(TAG, "Preference '" + scheme.key + "' is set to " + value);

		notifyOptionSetted(scheme, value);
	}

	public void setPreference(PreferenceScheme scheme, Location location) {
		if (location == null) {
			setPreference(scheme, (String)null);
		} else {
			setPreference(scheme, location.toString());
		}		
	}

	private void notifyOptionSetted(PreferenceScheme scheme, String value) {
		synchronized (mListeners) {
			Enumeration all = mListeners.toEnumeration();

			while (all.hasMoreElements()) {
				Listener each = (Listener)all.nextElement();

				each.onConfigOptionSet(this, scheme, value);
			}
		}
	}

	private static void shouldNeverHappen(Exception e) {
		throw new RuntimeException(e.getMessage());
	}
}
