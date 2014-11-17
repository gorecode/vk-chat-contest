package com.gorecode.vk.error;

import com.perm.kate.api.KException;

public class LongPollSessionExpiredException extends KException {
	public LongPollSessionExpiredException() {
		super(2, "Long poll session expired, please renew server info using getLongPollServer() method");
	}
}
