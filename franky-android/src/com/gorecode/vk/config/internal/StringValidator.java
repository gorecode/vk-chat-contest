package com.gorecode.vk.config.internal;

import com.gorecode.vk.config.PreferenceValueValidator;
import com.uva.lang.ObjectUtilities;
import com.uva.lang.StringUtilities;

public class StringValidator implements PreferenceValueValidator {
	private final boolean mAcceptEmpty;

	public StringValidator() {
		this(false);
	}

	public StringValidator(boolean acceptEmptyString) {
		mAcceptEmpty = acceptEmptyString;
	}

	public void validate(String value) {
		if (!mAcceptEmpty && StringUtilities.isEmpty(value)) {
			throw new IllegalArgumentException("Cannot be empty");
		}
	}		
}
