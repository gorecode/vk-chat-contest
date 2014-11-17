package com.gorecode.vk.activity.dialogs;

import android.content.Context;

import com.gorecode.vk.R;
import com.google.common.base.Function;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.utilities.AgoTimeFormat;

public class DialogListItem {
	private static final AgoTimeFormat sTimeFormat = new AgoTimeFormat(VkApplication.getApplication().getResources()) {
		{
			setFormat(AgoTimeFormat.FORMAT_SHORT);
		}
	};

	public static final Function<Dialog, DialogListItem> FUNCTION_TO_ITEM = new Function<Dialog, DialogListItem>() {
		@Override
		public DialogListItem apply(Dialog arg) {
			return forDialog(arg);
		}
	};

	public static final Function<DialogListItem, Dialog> FUNCTION_TO_DIALOG = new Function<DialogListItem, Dialog>() {
		@Override
		public Dialog apply(DialogListItem item) {
			return item.dialog;
		}
	};

	public Dialog dialog;
	public String timestampText;
	public String attachmentText;

	public static String formatDocumentsText(Context context, int numDocuments) {
		if (numDocuments == 1) {
			return context.getString(R.string.attachment_document);
		} else {
			return context.getString(R.string.attachment_documents);
		}
	}

	public static String formatPhotosText(Context context, int numPhotos) {
		if (numPhotos == 1) {
			return context.getString(R.string.attachment_photo);
		} else {
			return context.getString(R.string.attachment_photos);
		}
	}

	public static String formatVideosText(Context context, int numVideos) {
		if (numVideos == 1) {
			return context.getString(R.string.attachment_video);
		} else {
			return context.getString(R.string.attachment_videos);
		}
	}

	public static String formatMessagesText(Context context, int numMessages) {
		if (numMessages == 1) {
			return context.getString(R.string.attachment_message);
		} else {
			return context.getString(R.string.attachment_messages);
		}
	}

	public static String formatAttachmentText(Context context, ChatMessage.Content message) {
		final int numAttachments = message.getAttachmentsCount();

		if (numAttachments == 0) {
			return "";
		}

		if (numAttachments == message.imageUrls.size()) {
			return formatPhotosText(context, numAttachments);
		}

		if (numAttachments == message.audios.size()) {
			if (numAttachments == 1) {
				return context.getString(R.string.attachment_audio);
			} else {
				return context.getString(R.string.attachment_audios);
			}
		}

		if (numAttachments == message.documents.size()) {
			return formatDocumentsText(context, numAttachments);
		}

		if (numAttachments == message.videos.size()) {
			return formatVideosText(context, numAttachments);
		}

		if (numAttachments == message.forwarded.size()) {
			return formatMessagesText(context, numAttachments);
		}

		if (numAttachments == 1 && message.location != null) {
			return context.getString(R.string.attachment_location);
		}

		return context.getString(R.string.attachment_mixed);
	}

	public static DialogListItem forDialog(Dialog dialog) {
		DialogListItem item = new DialogListItem();
		item.dialog = dialog;
		item.updateCaptions();
		return item;
	}

	public void updateDialog(Dialog dialog) {
		final long lastMessageId = this.dialog.lastMessage.id;

		this.dialog = dialog;

		if (lastMessageId != dialog.lastMessage.id) {
			updateCaptions();
		}
	}

	public void updateCaptions() {
		timestampText = sTimeFormat.format(dialog.lastMessage.timestamp);
		attachmentText = formatAttachmentText(VkApplication.getApplication(), dialog.lastMessage.content);
	}
}
