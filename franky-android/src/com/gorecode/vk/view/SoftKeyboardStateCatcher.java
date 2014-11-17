package com.gorecode.vk.view;

/* wrapper for catching open and close soft keyboard events
 * set WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE for activity where use
 * wrap activity into ScrollView if edit field on it's bottom part (else SOFT_INPUT_ADJUST_RESIZE will cause soft keyboard overlap this field) 
 * */

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class SoftKeyboardStateCatcher extends LinearLayout {

	private OnSoftKeyboardStateChangedListener onSoftKeyboardStateChangedListener;
	
	public abstract interface OnSoftKeyboardStateChangedListener {
		public abstract void onSoftKeyboardShown();
		public abstract void onSoftKeyboardHidden();
	}
	
	public SoftKeyboardStateCatcher(Context context, int resourceId) {
		super(context);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(resourceId, this);
	}
	
	public void setOnSoftKeyboardStateChangedListener(OnSoftKeyboardStateChangedListener listener) {
		onSoftKeyboardStateChangedListener = listener;
	}

	public void removeOnSoftKeyboardStateChangedListener() {
		onSoftKeyboardStateChangedListener = null;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	
		int proposedHeight = MeasureSpec.getSize(heightMeasureSpec);
		int actualHeight = getHeight();
		
		if (actualHeight > proposedHeight) {
			onSoftKeyboardStateChangedListener.onSoftKeyboardShown();
		} else  {
			if (actualHeight < proposedHeight) {
				onSoftKeyboardStateChangedListener.onSoftKeyboardHidden();
			}
		}
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
