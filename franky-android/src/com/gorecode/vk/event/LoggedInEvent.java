package com.gorecode.vk.event;

import com.google.common.base.Preconditions;
import com.gorecode.vk.data.Profile;

public class LoggedInEvent {
	public final Profile user;

	public LoggedInEvent(Profile user) {
		Preconditions.checkNotNull(user);

		this.user = user;
	}
}
