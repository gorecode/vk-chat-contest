package com.gorecode.vk.view;

import com.gorecode.vk.application.VkApplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class MyriadTextView extends TextView {
	public MyriadTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (!isInEditMode()) {
			setTypeface(VkApplication.from(context).getTypeface(VkApplication.TYPEFACE_MYRIAD));
		}
	}
}
