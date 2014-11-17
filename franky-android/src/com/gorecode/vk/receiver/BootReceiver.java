package com.gorecode.vk.receiver;

import com.uva.log.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
	private static final String TAG = BootReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.message(TAG, "Boot wake up received");
	}
}