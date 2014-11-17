package com.gorecode.vk.activity.dialogs;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.GroupChatImageLoader;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.WebImageView;

public class DialogListItemView extends FrameLayout {
	private DialogListItem mItem;

	private WebImageView mConversationPhotoView;
	private WebImageView mMyPhotoView;
	private TextView mTitleView;
	private View mMessageTextLayout;
	private TextView mMessageTextView;
	private TextView mAttachmentTextView;
	private TextView mTimestampView;
	private View mMultichatIcon;
	private View mOnlineIcon;
	private View mContentView;

	private final Profile mMyProfile;

	public DialogListItemView(Context context, Profile me, ImageLoader imageLoader, Dialog dialog) {
		this(context, me, imageLoader, dialog.lastMessage.isIncoming() ? R.layout.item_dlgin : R.layout.item_dlgout);
	}

	public DialogListItemView(Context context, Profile me, ImageLoader imageLoader, int resId) {
		super(context, null);

		inflate(context, resId, this);

		setDrawingCacheEnabled(false);

		setBackgroundResource(R.drawable.item_background);

		mContentView = findViewById(R.id.content);
		mConversationPhotoView = (WebImageView)findViewById(R.id.photo_thumb);
		mOnlineIcon = findViewById(R.id.online);
		mTitleView = (TextView)findViewById(R.id.title);
		mMultichatIcon = findViewById(R.id.multichat);
		mMessageTextView = (TextView)findViewById(R.id.message_text);
		mAttachmentTextView = (TextView)findViewById(R.id.attachment_text);
		mTimestampView = (TextView)findViewById(R.id.timestamp);
		mConversationPhotoView.setImageLoader(imageLoader);
		mMyProfile = me;

		if (resId == R.layout.item_dlgout) {
			mMyPhotoView = (WebImageView)findViewById(R.id.my_photo_thumb);
			mMyPhotoView.setImageLoader(imageLoader);
			mMessageTextLayout = findViewById(R.id.message_text_layout);
		}
	}

	public void setItem(DialogListItem value) {
		mItem = value;

		updateViews();
	}

	public DialogListItem getDialog() {
		return mItem;
	}

	private void updateViews() {
		Dialog dialog = mItem.dialog;

		mMultichatIcon.setVisibility(dialog.isConference() ? View.VISIBLE : View.GONE);

		if (dialog.isConference()) {
			mTitleView.setText(dialog.getTitle());

			mOnlineIcon.setVisibility(View.GONE);

			mConversationPhotoView.setImageUrl(GroupChatImageLoader.getGroupChatUri(dialog.activeUsers));
		} else {
			mTitleView.setText(dialog.getParticipant().getFullname());

			mOnlineIcon.setVisibility(dialog.getParticipant().isOnline() ? View.VISIBLE : View.GONE);

			mConversationPhotoView.setImageUrls(dialog.getParticipant().avatarUrls);
		}

		ChatMessage.Content message = dialog.lastMessage.content;

		mTimestampView.setText(mItem.timestampText);

		final TextView messageTextView = mMessageTextView;

		messageTextView.setText(message.text);
		messageTextView.setVisibility(message.hasText() ? View.VISIBLE : View.GONE);

		TextView attachmentTextView = mAttachmentTextView;

		attachmentTextView.setVisibility(message.hasAttachments() ? View.VISIBLE : View.GONE);

		if (dialog.lastMessage.isIncoming()) {
			if (dialog.lastMessage.unread) {
				mContentView.setBackgroundResource(R.drawable.unread_message_background);
			} else {
				mContentView.setBackgroundResource(0);
			}

			messageTextView.setLines(message.hasAttachments() ? 1 : 2);
		} else {
			if (dialog.lastMessage.unread) {
				mMessageTextLayout.setBackgroundResource(R.drawable.unread_message_background_rounded);
			} else {
				mMessageTextLayout.setBackgroundResource(0);
			}

			messageTextView.setLines(1);

			mMyPhotoView.setImageUrls(mMyProfile.avatarUrls);
		}

		if (message.hasAttachments()) {
			attachmentTextView.setVisibility(View.VISIBLE);
			attachmentTextView.setText(mItem.attachmentText);
		} else {
			attachmentTextView.setVisibility(View.GONE);
		}
	}
};

