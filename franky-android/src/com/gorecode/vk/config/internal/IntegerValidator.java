package com.gorecode.vk.config.internal;

import com.gorecode.vk.config.PreferenceValueValidator;

public class IntegerValidator implements PreferenceValueValidator {
	public final int minValue;
	public final int maxValue;

	public IntegerValidator() {
		minValue = Integer.MIN_VALUE;
		maxValue = Integer.MAX_VALUE;
	}

	public IntegerValidator(int min, int max) {
		if (max < min) {
			throw new IllegalArgumentException("Max value must be > than minimum");
		}
		minValue = min;
		maxValue = max;
	}

	public void validate(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Integer number cannot be null");
		}
		try {
			int intValue = Integer.parseInt(value);
			if ((intValue > maxValue)||(intValue < minValue)) {
				throw new IllegalArgumentException("Value should be in interval from "+minValue+" to "+maxValue);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(value + " is not valid integer");
		}
	}
}