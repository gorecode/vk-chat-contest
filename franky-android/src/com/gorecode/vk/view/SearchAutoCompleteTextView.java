package com.gorecode.vk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class SearchAutoCompleteTextView extends AutoCompleteTextView {
	public SearchAutoCompleteTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SearchAutoCompleteTextView(Context context) {
		super(context, null);
	}

	protected void replaceText(CharSequence text) {
		// As we are using AutoCompleteTextView suggestions to show quick search results ->
		// we don't need to click on item lead to replacement of query text.
	}
}
