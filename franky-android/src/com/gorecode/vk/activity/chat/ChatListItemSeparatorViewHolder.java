package com.gorecode.vk.activity.chat;

import com.gorecode.vk.R;

import android.view.View;
import android.widget.TextView;

public class ChatListItemSeparatorViewHolder {
	private final TextView mTextView;

	private ChatListItem mItem;

	public static ChatListItemSeparatorViewHolder forView(View view) {
		ChatListItemSeparatorViewHolder holder = (ChatListItemSeparatorViewHolder)view.getTag();

		if (holder == null) {
			holder = new ChatListItemSeparatorViewHolder(view);

			view.setTag(holder);
		}

		return holder;
	}

	public ChatListItemSeparatorViewHolder(View view) {
		mTextView = (TextView)view.findViewById(R.id.text);
	}

	public void setItem(ChatListItem item) {
		assert mItem.isSeparator();

		mItem = item;

		updateViews();
	}

	public void updateViews() {
		mTextView.setText(mItem.getTimestampText());
	}
}
