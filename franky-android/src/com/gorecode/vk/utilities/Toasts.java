package com.gorecode.vk.utilities;

import android.content.Context;
import android.widget.Toast;

public class Toasts {
	public static Toast makeText(Context context, int resId) {
		return makeText(context, context.getString(resId));
	}

	public static Toast makeText(Context context, String text) {
		return Toast.makeText(context, text, getDurationForMessage(text));
	}

	public static void todo(Context context, String message) {
		message = "TODO: " + message;

		Toast.makeText(context, message, getDurationForMessage(message)).show();
	}

	public static int getDurationForMessage(String message) {
		return messageCanBeReadWithin(message, 2.0f) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
	}

	static boolean messageCanBeReadWithin(String message, float periodInSeconds) {
		final int symbolsPerMinuteByAvgProfile = 1000; // Very slow speed of reading.
		final int symbolsPerSecondByAvgProfile = symbolsPerMinuteByAvgProfile / 60;
 
		return ((float)message.length() / (float)symbolsPerSecondByAvgProfile <= periodInSeconds);
	}

	private Toasts() {
		;
	}
}
