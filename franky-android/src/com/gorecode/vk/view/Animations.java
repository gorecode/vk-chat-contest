package com.gorecode.vk.view;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public class Animations {
	public static final Animation DEFAULT_HIDE_ANIMATION = new AlphaAnimation(1.0f, 0.0f);
	public static final Animation DEFAULT_SHOW_ANIMATION = new AlphaAnimation(0.0f, 1.0f);

	public static final Animation POPUP_SHOW_ANIMATION = DEFAULT_SHOW_ANIMATION;
	public static final Animation POPUP_HIDE_ANIMATION = DEFAULT_HIDE_ANIMATION;

	private static final long DEFAULT_ANIMATION_DURATION = 300;

	static {
		DEFAULT_HIDE_ANIMATION.setDuration(DEFAULT_ANIMATION_DURATION);
		DEFAULT_SHOW_ANIMATION.setDuration(DEFAULT_ANIMATION_DURATION);
	}

	private Animations() {
		;
	}
}
