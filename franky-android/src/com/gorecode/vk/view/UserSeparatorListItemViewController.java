package com.gorecode.vk.view;

import com.gorecode.vk.R;

import android.view.View;
import android.widget.TextView;

public class UserSeparatorListItemViewController {
	private final TextView mTextView;

	public UserSeparatorListItemViewController(View view) {
		mTextView = (TextView)view.findViewById(R.id.item_text);
	}

	public static UserSeparatorListItemViewController bind(View view) {
		Object tag = view.getTag();

		if (tag != null && tag instanceof UserSeparatorListItemViewController) {
			return (UserSeparatorListItemViewController)tag;
		} else {
			UserSeparatorListItemViewController newController = new UserSeparatorListItemViewController(view);
			view.setTag(newController);
			return newController;
		}
	}

	public void setText(String text) {
		mTextView.setText(text);
	}
}
