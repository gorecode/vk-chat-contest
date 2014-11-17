package com.gorecode.vk.event;

import com.perm.kate.api.Captcha;

public class CaptchaEnterCompletedEvent {
	private final Captcha mCaptcha;
	private final String mKey;

	public CaptchaEnterCompletedEvent(Captcha captcha) {
		this(captcha, null);
	}

	public CaptchaEnterCompletedEvent(Captcha captcha, String key) {
		mCaptcha = captcha;
		mKey = key;
	}

	public Captcha getCaptcha() {
		return mCaptcha;
	}

	public String getKey() {
		return mKey;
	}

	public boolean hasKey() {
		return mKey != null;
	}
}
