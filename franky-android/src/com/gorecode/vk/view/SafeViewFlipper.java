package com.gorecode.vk.view;

import com.uva.log.Log;

import android.content.Context;
import android.util.AttributeSet;

public class SafeViewFlipper extends android.widget.ViewFlipper {
	private static final String TAG = "SafeViewFlipper";

	public SafeViewFlipper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SafeViewFlipper(Context context) {
		super(context);
	}

	@Override
	protected void onDetachedFromWindow() {
		try{
			super.onDetachedFromWindow();
		} catch (Exception e) {
			Log.warning(TAG, "Workaround Android issue #6191.");

			stopFlipping();
		}
	}
}