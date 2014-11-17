package com.gorecode.vk.config;

import com.gorecode.vk.config.internal.BooleanValidator;
import com.gorecode.vk.config.internal.IntegerValidator;
import com.gorecode.vk.config.internal.LocationValidator;
import com.gorecode.vk.config.internal.LongValidator;
import com.gorecode.vk.config.internal.StringValidator;
import com.uva.location.Location;
import com.uva.utilities.AssertCompat;

public class PreferenceScheme {
	public static class Category {
		public static final int STRING = 0x0;
		public static final int BOOLEAN = 0x1;
		public static final int INTEGER = 0x2;
		public static final int LOCATION = 0x3;
		public static final int CUSTOM = 0x4;
		public static final int LONG = 0x5;

		private Category() {
			;
		}
	};

	public final String key;
	public final int category;
	public final String defaultValue;
	public final PreferenceValueValidator valueValidator;

	public static PreferenceScheme createBooleanScheme(String key, boolean defaultValue) {
		return new PreferenceScheme(key, String.valueOf(defaultValue), new BooleanValidator());
	}

	public static PreferenceScheme createStringScheme(String key, String defaultValue, boolean acceptEmpty) {
		return new PreferenceScheme(key, defaultValue, new StringValidator(acceptEmpty));
	}

	public static PreferenceScheme createIntegerScheme(String key, int defaultValue) {
		return createIntegerScheme(key, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static PreferenceScheme createLongScheme(String key, long defaultValue) {
		return new PreferenceScheme(key, String.valueOf(defaultValue), new LongValidator());
	}

	public static PreferenceScheme createIntegerScheme(String key, int defaultValue, int minValue, int maxValue) {
		return new PreferenceScheme(key, String.valueOf(defaultValue), new IntegerValidator(minValue, maxValue));
	}

	public static PreferenceScheme createLocationScheme(String key, Location defaultValue) {
		String locationString = null;

		if (defaultValue != null) {
			locationString = defaultValue.toString();
		}

		return new PreferenceScheme(key, locationString, new LocationValidator());
	}

	/**
	 * @deprecated use static factory methods.
	 */
	public PreferenceScheme(String key, String defaultValue, PreferenceValueValidator validator) {
		AssertCompat.notNull(key, "Key");
		AssertCompat.notNull(validator, "Value validator");

		try {
			validator.validate(defaultValue);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Default option value must pass validation");
		}

		this.key = key;
		this.defaultValue = defaultValue;
		this.valueValidator = validator;

		category = determineCategory(valueValidator);
	}

	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		if (other instanceof PreferenceScheme) {
			return key.equals(((PreferenceScheme)other).key);
		}
		return false;
	}

	public int hashCode() {
		return key.hashCode();
	}

	private static int determineCategory(PreferenceValueValidator validator) {
		if (validator instanceof StringValidator) {
			return Category.STRING;
		}
		if (validator instanceof IntegerValidator) {
			return Category.INTEGER;
		}
		if (validator instanceof BooleanValidator) {
			return Category.BOOLEAN;
		}
		if (validator instanceof LocationValidator) {
			return Category.LOCATION;
		}
		if (validator instanceof LongValidator) {
			return Category.LONG;
		}
		return Category.CUSTOM;
	}
}
