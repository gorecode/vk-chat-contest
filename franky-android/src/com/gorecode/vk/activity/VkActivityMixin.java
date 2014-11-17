package com.gorecode.vk.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.gorecode.vk.R;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.sync.Session;
import com.gorecode.vk.task.AutoCancelPool;
import com.gorecode.vk.view.VkActionBar;

public class VkActivityMixin {
	private static final int DEFAULT_ANIMATIONS = -1;

	private static final String STATE_ANIMATIONS = "animations";

	private static final String TAG = "FrankyActivityMixin";

	private final Activity activity;

	private final AutoCancelPool autoCancelPool = new AutoCancelPool();

	private boolean onDestroyWasCalled;

	private Session session;

	static long sLastTimeWasInForeground;
	static long sLastTimeWasInBackground;

	private int animations = DEFAULT_ANIMATIONS;

	public static long getLastTimeWasInForeground() {
		return sLastTimeWasInForeground;
	}

	public static long getLastTimeWasInBackground() {
		return sLastTimeWasInBackground;
	}
	
	public VkActivityMixin(Activity activity) {
		this(activity, VkApplication.from(activity).getSession());
	}

	public VkActivityMixin(Activity activity, Session session) {
		if (!(activity instanceof VkActivityContract)) {
			throw new IllegalArgumentException("FrankyActivityMinin can be used only with Activity that implements FrankyActivityContact");
		}

		this.activity = activity;
		this.session = session;
	}

	public AutoCancelPool getAutoCancelPool() {
		return autoCancelPool;
	}

	public Context getContext() {
		return activity;
	}

	public VkActionBar getActionBar() {
		return (VkActionBar)activity.findViewById(R.id.actionBar);
	}

	public void onCreate(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			animations = savedInstanceState.getInt(STATE_ANIMATIONS, DEFAULT_ANIMATIONS);
		}

		if (animations == VkActivityContract.ANIMATIONS_SLIDE_RIGHT) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
		if (animations == VkActivityContract.ANIMATIONS_SLIDE_BOTTOM) {
			activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out);
		}

		onDestroyWasCalled = false;
	}

	public void onPause() {
		sLastTimeWasInBackground = System.currentTimeMillis();
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_ANIMATIONS, animations);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		animations = savedInstanceState.getInt(STATE_ANIMATIONS, DEFAULT_ANIMATIONS);
	}

	public void finish() {
		if (animations == VkActivityContract.ANIMATIONS_SLIDE_RIGHT) {
			activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);	
		}
		if (animations == VkActivityContract.ANIMATIONS_SLIDE_BOTTOM) {
			activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom);
		}
	}

	public void onResume() {
		sLastTimeWasInForeground = System.currentTimeMillis();

		if (session.hasAccessToken()) {
			session.markUserAsOnlineIfNeeded();
		}
	}

	public void onDestroy() {
		onDestroyWasCalled = true;

		autoCancelPool.drain();
	}

	public void setAnimations(int animations) {
		this.animations = animations;

		if (animations == VkActivityContract.ANIMATIONS_SLIDE_RIGHT) {
			activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
		}
		if (animations == VkActivityContract.ANIMATIONS_SLIDE_BOTTOM) {
			activity.overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out);
		}
	}

	public boolean isDestroyed() {
		return onDestroyWasCalled;
	}
}
