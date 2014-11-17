package com.gorecode.vk.activity.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.imageloader.GroupChatImageLoader;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.WebImageView;

public class GroupChatDescriptorView extends FrameLayout {
	private GroupChatDescriptor mGroupChat;

	private final WebImageView mConversationPhotoView;
	private final TextView mTitleView;
	private final View mMultichatIcon;
	private final View mOnlineIcon;

	public GroupChatDescriptorView(Context context, ImageLoader imageLoader) {
		super(context, null);

		inflate(context, R.layout.item_dlgin, this);

		setBackgroundResource(R.drawable.item_background);

		mConversationPhotoView = (WebImageView)findViewById(R.id.photo_thumb);
		mConversationPhotoView.setImageLoader(imageLoader);
		mOnlineIcon = findViewById(R.id.online);
		mTitleView = (TextView)findViewById(R.id.title);
		mMultichatIcon = findViewById(R.id.multichat);
	}

	public void setGroupChat(GroupChatDescriptor gc) {
		mGroupChat = gc;

		updateViews();
	}

	public GroupChatDescriptor getItem() {
		return mGroupChat;
	}

	private void updateViews() {
		mMultichatIcon.setVisibility(mGroupChat != null ? View.VISIBLE : View.GONE);

		mTitleView.setText(mGroupChat.getTitle());

		mOnlineIcon.setVisibility(View.GONE);

		mConversationPhotoView.setImageUrl(GroupChatImageLoader.getGroupChatUri(mGroupChat.users));
	}
};

