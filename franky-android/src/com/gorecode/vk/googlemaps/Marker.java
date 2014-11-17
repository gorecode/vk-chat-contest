package com.gorecode.vk.googlemaps;

import com.uva.location.Location;

public class Marker {
	/**
	 * Required.
	 */
	public Location location;
	/**
	 * Optional.
	 */
	public String color;
	/**
	 * Optional.
	 */
	public Character alphaCharacter;

	public Marker() {
		;
	}

	public static Marker forLocation(Location location) {
		Marker m = new Marker();
		m.location = location;
		return m;
	}
}
