package com.gorecode.vk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.application.VkApplication;

public class TextViewCustomFont extends TextView{

	public TextViewCustomFont(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		if(isInEditMode())
			return;
		
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextViewCustomFont);
		String userFont  = a.getString(R.styleable.TextViewCustomFont_typeface);
		
		if (userFont != null && userFont !="") {
			this.setTypeface(VkApplication.from(context).getTypeface(userFont));
		}
		
		a.recycle();
	}

}
