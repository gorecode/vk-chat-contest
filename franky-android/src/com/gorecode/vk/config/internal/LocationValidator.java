package com.gorecode.vk.config.internal;

import com.gorecode.vk.config.PreferenceValueValidator;
import com.uva.location.Location;

public class LocationValidator implements PreferenceValueValidator {
	public void validate(String value) {
		if (value == null) {
			return;
		}
		Location.parseLocation(value);
	}
}
