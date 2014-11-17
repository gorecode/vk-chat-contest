package com.gorecode.vk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

public class TouchInterceptEditText extends EditText {

	public TouchInterceptEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public boolean onTouchEvent(MotionEvent event) {
		if( super.onTouchEvent(event)) {
			getParent().requestDisallowInterceptTouchEvent(true);
			return true;
		}
		return false;
	}
}
