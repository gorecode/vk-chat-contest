package com.gorecode.vk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import com.gorecode.vk.application.VkApplication;

public class ButtonMyriad extends Button {
	public ButtonMyriad(Context context) {
		super(context);

		setTypeface(context);
	}

	public ButtonMyriad(Context context, AttributeSet attrs) {
		super(context, attrs);

		setTypeface(context);
	}

	private void setTypeface(Context context) {
		if (isInEditMode()) return;

		setTypeface(VkApplication.from(context).getTypeface(VkApplication.TYPEFACE_MYRIAD));
	}
}
