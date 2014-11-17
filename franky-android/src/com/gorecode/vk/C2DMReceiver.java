package com.gorecode.vk;

import java.io.IOException;

import roboguice.RoboGuice;

import android.content.Context;
import android.content.Intent;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.service.C2DMHandler;

public class C2DMReceiver extends C2DMBaseReceiver {
	private C2DMHandler mDelegate;

	public C2DMReceiver() {
		super(C2DMHandler.SENDER_PROJECT_ID);

		mDelegate = RoboGuice.getInjector(VkApplication.getApplication()).getInstance(C2DMHandler.class);
	}

	@Override
	public void onMessage(Context context, Intent intent) {
		mDelegate.onMessage(context, intent);
	}

	@Override
	public void onError(Context context, String errorId) {
		mDelegate.onError(context, errorId);
	}

	@Override
	public void onRegistered(Context context, String registrationId) throws IOException {
		mDelegate.onRegistered(context, registrationId);
	}

	@Override
	public void onUnregistered(Context context) {
		mDelegate.onUnregistered(context);
	}
}