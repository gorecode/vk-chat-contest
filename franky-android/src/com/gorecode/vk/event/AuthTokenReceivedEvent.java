package com.gorecode.vk.event;

import com.gorecode.vk.data.Profile;

public class AuthTokenReceivedEvent {
	private final Profile mUser;
	private final String mToken;

	public AuthTokenReceivedEvent(Profile user, String token) {
		mUser = user;
		mToken = token;
	}

	public Profile getUser() {
		return mUser;
	}

	public String getToken() {
		return mToken;
	}
}
