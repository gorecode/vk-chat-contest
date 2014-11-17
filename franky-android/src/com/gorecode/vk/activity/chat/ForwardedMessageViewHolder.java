package com.gorecode.vk.activity.chat;

import android.view.View;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.gorecode.vk.activity.dialogs.DialogListItem;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.utilities.TimeFormatter;
import com.gorecode.vk.view.WebImageView;

public class ForwardedMessageViewHolder {
	private final TextView mName;
	private final TextView mMessageText;
	private final TextView mTimeText;
	private final TextView mAttachmentText;
	private final WebImageView mPhotoThumb;
	private final TimeFormatter mTimeFormat;

	private ChatMessage mMessage;

	public ForwardedMessageViewHolder(View view, ImageLoader imageLoader, TimeFormatter timeFormat) {
		mTimeFormat = timeFormat;

		mName = (TextView)view.findViewById(R.id.name);
		mMessageText = (TextView)view.findViewById(R.id.message_text);
		mTimeText = (TextView)view.findViewById(R.id.timestamp);
		mAttachmentText = (TextView)view.findViewById(R.id.attachments_text);
		mPhotoThumb = (WebImageView)view.findViewById(R.id.photo_thumb);
		mPhotoThumb.setImageLoader(imageLoader);
	}

	public ChatMessage getMessage() {
		return mMessage;
	}

	public void setMessage(ChatMessage message) {
		mMessage = message;

		updateViews();
	}

	public void updateViews() {
		mName.setText(mMessage.getSender().getFullname());
		mPhotoThumb.setImageUrls(mMessage.getSender().avatarUrls);
		mMessageText.setText(mMessage.content.text);
		mMessageText.setVisibility(Strings.isNullOrEmpty(mMessage.content.text) ? View.GONE : View.VISIBLE);
		mAttachmentText.setText(DialogListItem.formatAttachmentText(mName.getContext(), mMessage.content));
		mAttachmentText.setVisibility(mMessage.content.getAttachmentsCount() > 0 ? View.VISIBLE : View.GONE);
		mTimeText.setText(mTimeFormat.format(mMessage.timestamp));
	}
}
