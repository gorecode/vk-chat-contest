package com.gorecode.vk.activity;

import android.content.Context;
import android.os.Bundle;

import com.gorecode.vk.task.AutoCancelPool;
import com.gorecode.vk.view.VkActionBar;

public interface VkActivityContract {
	public static final int ANIMATIONS_SLIDE_RIGHT = 0x0;
	public static final int ANIMATIONS_SLIDE_BOTTOM = 0x1;

	public static final String EXTRA_ACCESS_TOKEN = "franky:accessToken";
	public static final String EXTRA_USER_ID = "franky:userId";

	public static final String EXTRA_DIALOG = "franky:dialog";
	public static final String EXTRA_PROFILE_ID = "franky:profile_id";
	public static final String EXTRA_PROFILE = "franky:profile";
	public static final String EXTRA_STATUS = "franky:status";
	public static final String EXTRA_PERSON_ID = "franky:person_ID";
	public static final String EXTRA_PERSON = "franky:person";
	public static final String EXTRA_PHOTO = "franky:photo";
	public static final String EXTRA_PHOTOS = "franky:photos";
	public static final String EXTRA_PHOTO_ID = "franky:photo_id";
	public static final String EXTRA_OWNER = "franky:owner";
	public static final String EXTRA_OWNER_ID = "franky:owner_id";

	/**
	 * Returns action bar or null if there's no action bar in activity.
	 */
	public VkActionBar getVkActionBar();

	public AutoCancelPool getAutoCancelPool();

	public Context getContext();

	public void setAnimations(int animations);

	public void onSaveInstanceState(Bundle outState);
	public void onRestoreInstanceState(Bundle savedInstanceState);

	/**
	 * @return true if onDestroy() was called.
	 */
	public boolean isDestroyed();
}