package com.gorecode.vk.activity;

import roboguice.activity.RoboMapActivity;
import android.content.Context;
import android.os.Bundle;

import com.gorecode.vk.task.AutoCancelPool;
import com.gorecode.vk.view.VkActionBar;

public abstract class VkMapActivity extends RoboMapActivity implements VkActivityContract {
	private VkActivityMixin frankyMixin;

	@Override
	public void finish() {
		super.finish();
		frankyMixin.finish();
	}

	@Override
	public VkActionBar getVkActionBar() {
		return frankyMixin.getActionBar();
	}

	@Override
	public AutoCancelPool getAutoCancelPool() {
		return frankyMixin.getAutoCancelPool();
	}

	@Override
	public Context getContext() {
		return frankyMixin.getContext();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		frankyMixin = new VkActivityMixin(this);

		super.onCreate(savedInstanceState);

		frankyMixin.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		frankyMixin.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		frankyMixin.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		frankyMixin.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onPause() {		
		super.onPause();
		frankyMixin.onPause();
	}

	@Override
	protected void onDestroy() {
		frankyMixin.onDestroy();
		super.onDestroy();
	}

	@Override
	public boolean isDestroyed() {
		return frankyMixin.isDestroyed();
	}

	@Override
	public void setAnimations(int animations) {
		frankyMixin.setAnimations(animations);
	}
}
