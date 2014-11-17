package com.gorecode.vk.data;

import java.io.Serializable;

public enum Gender implements Serializable, Cloneable{
	MALE('M'),
	FEMALE('F');
	
	private String asString;

	public static Gender[] all() {
		return new Gender[] { MALE, FEMALE }; 
	}

	public static Gender fromString(String stringVal) {
		Gender[] all = all();

		for (int i = 0; i < all.length; i++) {
			if (all[i].toString().equals(stringVal)) {
				return all[i];
			}
		}

		throw new IllegalArgumentException("Cannot convert '" + stringVal + "' to gender");
	}

	@Override
	public String toString() {
		return asString;
	}
	
	private Gender(char charVal) {
		asString = String.valueOf(charVal);
	}
}
