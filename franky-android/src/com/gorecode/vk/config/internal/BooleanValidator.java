package com.gorecode.vk.config.internal;

import com.gorecode.vk.config.PreferenceValueValidator;
import com.uva.lang.BooleanUtilities;

public class BooleanValidator implements PreferenceValueValidator {
	public void validate(String value) {
		BooleanUtilities.parse(value);
	}
}
