package com.gorecode.vk.config;

import com.uva.utilities.AssertCompat;

public class ConfigScheme {
	private final PreferenceScheme[] mPreferenceSchemes;

	public ConfigScheme(ConfigScheme source) {
		this(source.mPreferenceSchemes);
	}

	public ConfigScheme(PreferenceScheme[] schemes) {
		AssertCompat.notNull(schemes, "Schemes");

		mPreferenceSchemes = schemes;
	}

	public PreferenceScheme[] getAllPreferenceSchemes() {
		return mPreferenceSchemes;
	}

	public PreferenceScheme getPreferenceScheme(String optName) {
		final PreferenceScheme[] options = mPreferenceSchemes;
		for (int i = 0; i < options.length; i++) {
			PreferenceScheme each = options[i];
			if (each.key.equals(optName)) {
				return each;
			}
		}
		throw new IllegalArgumentException("Option '" + optName + "' is unknown");		
	}
}
