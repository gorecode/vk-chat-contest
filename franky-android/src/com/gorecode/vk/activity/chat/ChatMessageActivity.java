package com.gorecode.vk.activity.chat;

import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.activity.VkActivity;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.googlemaps.GoogleStaticMap;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.utilities.AgoTimeFormat;
import com.gorecode.vk.utilities.BundleUtilities;

public class ChatMessageActivity extends VkActivity {
	private static final String EXTRA_CHAT_MESSAGE = "chatMessage";

	@Inject
	private GoogleStaticMap mGmap;
	@Inject
	private ImageLoader mImageLoader;
	@Inject
	private AudioPlayer mAudioPlayer;
	@Inject
	private AgoTimeFormat mLastSeenTimeFormat;
	@Inject
	private SessionContext mSessionContext;

	@InjectView(R.id.message_layout)
	private ViewGroup mMessageLayout;

	public static Intent getDisplayIntent(Context context, ChatMessage message) {
		Intent intent = new Intent(context, ChatMessageActivity.class);

		BundleUtilities.putExtra(intent, EXTRA_CHAT_MESSAGE, message);

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(null);

		setAnimations(ANIMATIONS_SLIDE_RIGHT);

		setContentView(R.layout.chat_message_activity);

		ChatMessage message = BundleUtilities.getChatMessage(getIntent().getExtras(), EXTRA_CHAT_MESSAGE);

		Dialog dialog = Dialog.withOneMessage(message);

		ChatListItemView messageView = new ChatListItemView(this, dialog, mImageLoader, mAudioPlayer, mGmap, mLastSeenTimeFormat, mSessionContext, R.layout.item_msgin, true, true, null) {
			@Override
			public void setChecked(boolean checked) {
				; // Don't want it to be checkable.
			}
		};

		messageView.setForceShowParticipant(true);
		messageView.setItem(ChatListItem.newMessage(message));
		messageView.setClickable(false);
		messageView.setFocusable(false);
		messageView.setFocusableInTouchMode(false);

		mMessageLayout.addView(messageView);
	}
}
