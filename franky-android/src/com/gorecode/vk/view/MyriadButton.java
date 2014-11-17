package com.gorecode.vk.view;

import com.gorecode.vk.application.VkApplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class MyriadButton extends Button {
	public MyriadButton(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (!isInEditMode()) {
			setTypeface(VkApplication.from(context).getTypeface(VkApplication.TYPEFACE_MYRIAD));
		}
	}
}
