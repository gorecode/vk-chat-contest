package com.gorecode.vk.config;

import java.util.Vector;

import com.uva.lang.VectorUtilities;
import com.uva.utilities.AssertCompat;

public class ConfigSchemeBuilder {
	private final Vector mScheme = new Vector();

	public void add(String prefKey, String defaultValue, PreferenceValueValidator valueValidator) {
		add(new PreferenceScheme(prefKey, defaultValue, valueValidator));
	}

	public void add(ConfigScheme configScheme) {
		add(configScheme.getAllPreferenceSchemes());
	}

	public void add(PreferenceScheme[] preferenceSchemes) {
		for (int i = 0; i < preferenceSchemes.length; i++) {
			add(preferenceSchemes[i]);
		}
	}

	public void add(PreferenceScheme prefScheme) {
		AssertCompat.notNull(prefScheme, "Preference scheme");

		if (mScheme.contains(prefScheme)) {
			throw new IllegalArgumentException(prefScheme.key + " already in scheme");
		}
		mScheme.addElement(prefScheme);
	}

	public void clear() {
		mScheme.removeAllElements();
	}

	public ConfigScheme compile() {
		PreferenceScheme[] schemeAsArray = new PreferenceScheme[mScheme.size()];
		VectorUtilities.toArray(mScheme, schemeAsArray);
		return new ConfigScheme(schemeAsArray);
	}
}
