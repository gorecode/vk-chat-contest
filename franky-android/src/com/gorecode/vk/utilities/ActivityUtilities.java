package com.gorecode.vk.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class ActivityUtilities {
	
	public static <T extends Activity> void display(Context context, Class<T> clazz) {
		context.startActivity(new Intent(context, clazz));
	}

	public static void restartLater(final Activity thiz, final Intent intent) {
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				restart(thiz, intent);
			}
		});
	}

	public static void restart(final Activity thiz) {
		restart(thiz, thiz.getIntent());
	}

	public static void restart(final Activity thiz, Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

		thiz.overridePendingTransition(0, 0);
		thiz.startActivity(intent);

		thiz.overridePendingTransition(0, 0);
		
		int flags = intent.getFlags();
		boolean isClearTop = (flags & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0; 
		boolean isSingleTop = (flags & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0;				
		if (!isClearTop && !isSingleTop) {
			thiz.finish();
		}
	}

	private ActivityUtilities() {
		;
	}
}
