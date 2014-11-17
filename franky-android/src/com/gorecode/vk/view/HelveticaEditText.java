package com.gorecode.vk.view;

import com.gorecode.vk.application.VkApplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class HelveticaEditText extends EditText {
	public HelveticaEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		if (!isInEditMode()) {
			setTypeface(VkApplication.from(context).getTypeface(VkApplication.TYPEFACE_HELVETICA));
		}
	}
}
