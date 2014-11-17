package com.gorecode.vk.event;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Availability;

public class AvailabilityChangedEvent {
	private long userId;
	private Availability availability;

	public AvailabilityChangedEvent(long userId, Availability availability) {
		Preconditions.checkNotNull(availability);

		this.userId = userId;
		this.availability = availability;
	}

	public long getUserId() {
		return userId;
	}

	public Availability getAvailability() {
		return availability;
	}
}
