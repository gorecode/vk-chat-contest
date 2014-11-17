package com.gorecode.vk.data;


/**
 * Profile availability.
 *
 * @author enikey.
 */
public enum Availability {
	ONLINE("ON"),
	OFFLINE("OFF");

	private final String code;

	@Override
	public String toString() {
		return code;
	}

	private Availability(String code) {
		this.code = code;
	}
}
