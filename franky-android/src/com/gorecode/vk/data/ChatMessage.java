package com.gorecode.vk.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.perm.kate.api.VkAttachment;
import com.perm.kate.api.VkMessage;
import com.perm.kate.api.VkPhoto;
import com.perm.kate.api.VkUser;
import com.uva.location.Location;

public class ChatMessage extends Message<ChatMessage.Content> implements Serializable {
	private static final long serialVersionUID = 2113643072556853621L;

	public static class Content extends Message.Content {		
		private static final long serialVersionUID = -3180429538060015171L;

		public static final int TYPE_HINT_CHAT = 0x1;
		public static final int TYPE_HINT_FLIRT = 0x2;

		public String subject;
		/**
		 * Attached location.
		 */
		public Location location;
		/**
		 * Encoded image bytes, not null only in outgoing image messages, always null in incoming messages.
		 */
		public byte[] imageBytes;
		/**
		 * Urls of image, not null if image is attached.
		 * @deprecated, use attachedPhotos
		 */
		public ArrayList<ImageUrls> imageUrls = new ArrayList<ImageUrls>();
		public ArrayList<Audio> audios = new ArrayList<Audio>();
		public ArrayList<Document> documents = new ArrayList<Document>();
		public ArrayList<Video> videos = new ArrayList<Video>();
		public ArrayList<ChatMessage> forwarded = new ArrayList<ChatMessage>();

		private final ArrayList<VkPhoto> mAttachedPhotos = new ArrayList<VkPhoto>();

		public Content() {
			;
		}

		public Content(String message, ImageUrls imageUrls) {
			this.text = makeMessageSafe(message);
			this.imageUrls.add(imageUrls);
		}

		public Content(String message, byte[] imageBytes) {
			this.text = makeMessageSafe(message);
			this.imageBytes = imageBytes;
		}


		public Content(String message) {
			super(message);
		}

		public Content(String message, Location location) {
			this.text  = makeMessageSafe(message);
			this.location = location;
		}

		public Content(String message, Location location, byte[] imageBytes) {
			this.text = makeMessageSafe(message);
			this.location = location;
			this.imageBytes = imageBytes;
		}

		@Override
		public String toString() {
			Objects.ToStringHelper helper = Objects.toStringHelper(ChatMessage.Content.class);
			helper.add("text", text);
			return helper.toString();
		}

		public boolean hasText() {
			return !Strings.isNullOrEmpty(text);
		}

		public int getAttachmentsCount() {
			return imageUrls.size() + audios.size() + videos.size() + forwarded.size() + documents.size() + ((location != null) ? 1 : 0);
		}

		public boolean hasAttachments() {
			return getAttachmentsCount() > 0;
		}

		public List<VkPhoto> getAttachedPhotos() {
			return mAttachedPhotos;
		}

		public void addAttachments(Collection<VkPhoto> vkPhotos) {
			for (VkPhoto each : vkPhotos) {
				addAttachment(each);
			}
		}

		public void addAttachment(VkPhoto vkPhoto) {
			mAttachedPhotos.add(vkPhoto);

			imageUrls.add(ImageUrls.fromVkPhoto(vkPhoto));
		}
	}

	public long chatId;
	public boolean unread;

	public boolean isFromConference() {
		return chatId != 0;
	}

	public long getCid() {
		if (isFromConference()) {
			return chatId;
		} else {
			return getParticipant().id;
		}
	}

	@Override
	public String toString() {
		Objects.ToStringHelper helper = Objects.toStringHelper(ChatMessage.class);
		helper.add("id", id);
		helper.add("content", content);
		return helper.toString();
	}

	public static ChatMessage from(VkMessage vkMessage, Map<Long, VkUser> vkUsers, long participantId) {
		ChatMessage message = new ChatMessage();

		message.direction = vkMessage.is_out ? ChatMessage.DIRECTION_OUTGOING : ChatMessage.DIRECTION_INCOMING;
		message.id = vkMessage.mid;
		message.content = new ChatMessage.Content(vkMessage.body);
		message.content.subject = vkMessage.title;
		message.unread = !vkMessage.read_state;
		message.chatId = vkMessage.chat_id != null ? vkMessage.chat_id : Dialog.INVALID_ID;
		message.timestamp = Long.valueOf(vkMessage.date) * 1000;

		VkUser vkParticipant = vkUsers.get(participantId);

		if (vkParticipant != null) {
			message.user = Profile.fromVkUser(vkParticipant);
		} else {
			message.user = Profile.empty(participantId);
		}

		if (vkMessage.lat != null && vkMessage.lon != null) {
			message.content.location = new Location(vkMessage.lat, vkMessage.lon);
		}

		message.content.audios.clear();

		for (VkAttachment vkAttachment : vkMessage.attachments) {
			if (vkAttachment.photo != null) {
				message.content.imageUrls.add(ImageUrls.fromVkPhoto(vkAttachment.photo));
			}
			if (vkAttachment.audio != null && vkAttachment.audio.url != null) {
				message.content.audios.add(vkAttachment.audio);
			}
			if (vkAttachment.document != null && vkAttachment.document.url != null) {
				message.content.documents.add(vkAttachment.document);
			}
			if (vkAttachment.video != null) {
				message.content.videos.add(vkAttachment.video);
			}
		}

		for (VkMessage vkForwardedMessage : vkMessage.fwd_messages) {
			message.content.forwarded.add(ChatMessage.from(vkForwardedMessage, vkUsers, vkForwardedMessage.uid));
		}

		return message;
	}
}
