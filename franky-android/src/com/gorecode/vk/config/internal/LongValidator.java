package com.gorecode.vk.config.internal;

import com.gorecode.vk.config.PreferenceValueValidator;

public class LongValidator implements PreferenceValueValidator {
	public final long minValue;
	public final long maxValue;

	public LongValidator() {
		minValue = Long.MIN_VALUE;
		maxValue = Long.MAX_VALUE;
	}

	public LongValidator(long min, long max) {
		if (max < min) {
			throw new IllegalArgumentException("Max value must be > than minimum");
		}
		minValue = min;
		maxValue = max;
	}

	public void validate(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Long number cannot be null");
		}
		try {
			long parsedValue = Long.parseLong(value);
			if ((parsedValue > maxValue)||(parsedValue < minValue)) {
				throw new IllegalArgumentException("Value should be in interval from "+minValue+" to "+maxValue);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(value + " is not valid long");
		}
	}
}