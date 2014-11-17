package com.gorecode.vk.activity.chat;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.gorecode.vk.R;
import com.gorecode.vk.activity.LocationActivity;
import com.gorecode.vk.activity.ViewImageActivity;
import com.gorecode.vk.activity.dialogs.DialogListItem;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.Document;
import com.gorecode.vk.data.ImageUrls;
import com.gorecode.vk.data.Size;
import com.gorecode.vk.data.Video;
import com.gorecode.vk.googlemaps.GoogleStaticMap;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.imageloader.ImageProcessors;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.utilities.TimeFormatter;
import com.gorecode.vk.utilities.Toasts;
import com.gorecode.vk.view.BetterScrollDetector;
import com.gorecode.vk.view.ViewUtilities;
import com.gorecode.vk.view.WebImageView;
import com.uva.location.Location;

public class ChatListItemView extends FrameLayout {
	private TextView mMessageTextView;
	private TextView mTimestampTextView;
	private WebImageView mPhotoThumbView;

	private ViewGroup mAttachmentsLayout;

	private ViewGroup mImagesSectionView;
	private ViewGroup mImagesView;
	private TextView mImagesSectionTitleView;

	private AudiosAttachmentView mAudiosView;

	private ViewGroup mVideosSectionView;
	private ViewGroup mVideosView;
	private TextView mVideosSectionTitleView;

	private ViewGroup mDocumentsSectionView;
	private ViewGroup mDocumentsView;
	private TextView mDocumentsSectionTitleView;

	private ViewGroup mMessagesSectionView;
	private ViewGroup mMessagesView;
	private TextView mMessagesSectionTitleView;

	private ViewGroup mLocationLayout;
	private TextView mLocationTitle;
	private WebImageView mLocationThumb;

	private boolean mEnableAttachmentSupport;

	private ChatListItem mItem;

	private final SessionContext mSessionContext;
	private final ImageLoader mImageLoader;
	private final AudioPlayer mAudioPlayer;
	private final Dialog mDialog;
	private final GoogleStaticMap mGoogleStaticMap;
	private final TimeFormatter mLastSeenTimeFormat;

	private final BetterScrollDetector mListViewScrollDetector;

	private final int mBackgroundForUnread;
	private final int mBackgroundForRead;

	private boolean mForceShowParticipant;

	public ChatListItemView(Context context, Dialog dialog, ImageLoader imageLoader, AudioPlayer audioPlayer, GoogleStaticMap gmaps, TimeFormatter lastSeenTimeFormat, SessionContext sessionContext, int resourceId, boolean enableAttachmentSupport, boolean isForGroupChat, BetterScrollDetector listViewScrollDetector) {
		super(context, null);

		setDrawingCacheEnabled(false);

		mBackgroundForUnread = getResources().getColor(R.color.message_background_unread);
		mBackgroundForRead = getResources().getColor(R.color.message_background_read);

		mLastSeenTimeFormat = lastSeenTimeFormat;
		mSessionContext = sessionContext;
		mImageLoader = imageLoader;
		mAudioPlayer = audioPlayer;
		mDialog = dialog;
		mGoogleStaticMap = gmaps;
		mListViewScrollDetector = listViewScrollDetector;

		inflate(context, resourceId, this);

		mMessageTextView  = (TextView)this.findViewById(R.id.message_text);
		mMessageTextView.setMovementMethod(LinkMovementMethod.getInstance());

		mTimestampTextView = (TextView)this.findViewById(R.id.timestamp);
		mPhotoThumbView = (WebImageView)this.findViewById(R.id.photo_thumb);
		mPhotoThumbView.setImageLoader(mImageLoader);

		if (!isForGroupChat) {
			((ViewGroup)mPhotoThumbView.getParent()).removeView(mPhotoThumbView);
		}

		setClickable(true);
		setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mItem.dispatchState == ChatListItem.DISPATCH_STATE_SENT || mItem.dispatchState == ChatListItem.DISPATCH_STATE_SENT_NOW) {
					toggle();	
				} else {
					Toasts.makeText(getContext(), R.string.toast_could_not_select_unsent_message).show();
				}
			}
		});

		mEnableAttachmentSupport = enableAttachmentSupport;

		if (mEnableAttachmentSupport) {
			mAttachmentsLayout = (ViewGroup)findViewById(R.id.attachments_layout);

			inflate(context, R.layout.view_message_attachments, mAttachmentsLayout);

			mImagesSectionView = (ViewGroup)mAttachmentsLayout.findViewById(R.id.imageAttachmentsLayout);
			mImagesView = (ViewGroup)mAttachmentsLayout.findViewById(R.id.imageAttachmentsItemsLayout);
			mImagesSectionTitleView = (TextView)mImagesSectionView.findViewById(R.id.imageAttachments_title);

			mAudiosView = (AudiosAttachmentView)mAttachmentsLayout.findViewById(R.id.audioAttachmentsLayout);
			mAudiosView.setAudioPlayer(mAudioPlayer);

			mDocumentsSectionView = (ViewGroup)mAttachmentsLayout.findViewById(R.id.documentAttachmentsLayout);
			mDocumentsView = (ViewGroup)mDocumentsSectionView.findViewById(R.id.documentAttachmentsItemsLayout);
			mDocumentsSectionTitleView = (TextView)mDocumentsSectionView.findViewById(R.id.documentAttachments_title);

			mVideosSectionView = (ViewGroup)mAttachmentsLayout.findViewById(R.id.videoAttachmentsLayout);
			mVideosView  = (ViewGroup)mVideosSectionView.findViewById(R.id.videoAttachmentsItemsLayout);
			mVideosSectionTitleView = (TextView)mVideosSectionView.findViewById(R.id.videoAttachments_title);

			mMessagesSectionView = (ViewGroup)mAttachmentsLayout.findViewById(R.id.messageAttachmentsLayout);
			mMessagesView = (ViewGroup)mMessagesSectionView.findViewById(R.id.messageAttachmentsItemsLayout);
			mMessagesSectionTitleView = (TextView)mMessagesSectionView.findViewById(R.id.messageAttachments_title);

			mLocationLayout = (ViewGroup)findViewById(R.id.locationAttachmentsLayout);
			mLocationTitle = (TextView)mLocationLayout.findViewById(R.id.location_title);
			mLocationThumb = (WebImageView)mLocationLayout.findViewById(R.id.location_thumb);
			mLocationThumb.setImageLoader(mImageLoader);
			mLocationThumb.setOnClickListener(mOnLocationClick);
		}
	}

	public void setForceShowParticipant(boolean forceShow) {
		mForceShowParticipant = forceShow;
	}

	//@Override
	public boolean isChecked() {
		return mItem.isMarked;
	}

	//@Override
	public void setChecked(boolean checked) {
		if (mItem.isMarked != checked) {
			mItem.isMarked = checked;

			super.setSelected(checked);
		}
	}

	//@Override
	public void toggle() {
		setChecked(!isChecked());
	}

	@Override
	public void setSelected(boolean selected) {
		;
	}

	public ChatListItem getItem() {
		return mItem;
	}

	public void setItem(final ChatListItem item) {
		mItem = item;

		updateViews();
	}

	private void updateViews() {
		final ChatListItem item = mItem;

		super.setSelected(item.isMarked);

		mTimestampTextView.setText(item.getTimestampText());

		ChatMessage message = item.message;

		if (message.isOutgoing()) {
			final boolean isSent = item.dispatchState == ChatListItem.DISPATCH_STATE_SENT_NOW;
			final boolean isError = item.dispatchState == ChatListItem.DISPATCH_STATE_ERROR;

			if (isSent) {
				mTimestampTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.sent, 0, 0, 0);
			} else if (isError) {
				mTimestampTextView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.error, 0, 0, 0);
			} else {
				mTimestampTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		}

		if (message.unread) {
			setBackgroundColor(mBackgroundForUnread);
		} else {
			setBackgroundColor(mBackgroundForRead);
		}

		mMessageTextView.setText(mItem.getLinkifiedText());

		mMessageTextView.setVisibility(Strings.isNullOrEmpty(message.content.text) ? GONE : VISIBLE);

		if (mDialog.isConference() || mForceShowParticipant) {
			if (item.message.isOutgoing()) {
				mPhotoThumbView.setImageUrls(mSessionContext.getUser().avatarUrls, ImageProcessors.PHOTO_ROUNDER);
			} else {
				mPhotoThumbView.setImageUrls(item.message.getSender().avatarUrls, ImageProcessors.PHOTO_ROUNDER);
			}
		}

		if (mEnableAttachmentSupport) {
			updateImageAttachmentViews();
			updateAudioAttachmentViews();
			updateVideoAttachmentViews();
			updateDocumentAttachmentViews();
			updateMessageAttachmentViews();
			updateLocationAttachmentViews();
		}
	}

	private void updateVideoAttachmentViews() {
		List<Video> attachments = mItem.message.content.videos;

		if (attachments.size() != 0) {
			mVideosSectionView.setVisibility(View.VISIBLE);
			mVideosSectionTitleView.setText(DialogListItem.formatVideosText(getContext(), mItem.message.content.videos.size()));

			final int total = attachments.size();

			for (int i = 0; i < Math.max(total, mVideosView.getChildCount()); i++) {
				if (i < total) {
					Video attachment = attachments.get(i);

					VideoAttachmentView view = (VideoAttachmentView)mVideosView.getChildAt(i);

					if (view == null) {
						view = new VideoAttachmentView(getContext(), mImageLoader);

						mVideosView.addView(view);
					}

					view.setVisibility(VISIBLE);
					view.setVideo(attachment);
				} else {
					mVideosView.getChildAt(i).setVisibility(GONE);
				}
			}
		} else {
			mVideosSectionView.setVisibility(View.GONE);
		}				
	}

	private void updateImageAttachmentViews() {
		List<ImageUrls> images = mItem.message.content.imageUrls;

		if(images.size() != 0) {
			mImagesSectionView.setVisibility(View.VISIBLE);
			mImagesSectionTitleView.setText(DialogListItem.formatPhotosText(getContext(), mItem.message.content.imageUrls.size()));

			final int totalImages = images.size();

			for (int i = 0; i < Math.max(totalImages, mImagesView.getChildCount()); i++) {
				if (i < totalImages) {
					ImageUrls urls = images.get(i);

					WebImageView imageView;

					View view = (View)mImagesView.getChildAt(i);

					if (view == null) {
						view = inflate(getContext(), R.layout.photo_attachment, null);
						
						imageView = (WebImageView)view.findViewById(R.id.photo);
						imageView.setOnClickListener(mOnImageClick);
						imageView.setScaleType(ScaleType.CENTER_CROP);
						imageView.setImageLoader(mImageLoader);

						view.setTag(imageView);

						mImagesView.addView(view);
					} else {
						imageView = (WebImageView)view.getTag();
					}

					imageView.setTag(urls);
					imageView.setVisibility(VISIBLE);
					imageView.setImageUrl(urls.previewUrl, ImageProcessors.PHOTO_ROUNDER);
				} else {
					mImagesView.getChildAt(i).setVisibility(GONE);
				}
			}
		} else {
			mImagesSectionView.setVisibility(View.GONE);
		}
	}

	private void updateDocumentAttachmentViews() {
		List<Document> attachments = mItem.message.content.documents;

		if (attachments.size() != 0) {
			mDocumentsSectionView.setVisibility(View.VISIBLE);
			mDocumentsSectionTitleView.setText(DialogListItem.formatDocumentsText(getContext(), mItem.message.content.documents.size()));

			final int total = attachments.size();

			for (int i = 0; i < Math.max(total, mDocumentsView.getChildCount()); i++) {
				if (i < total) {
					Document attachment = attachments.get(i);

					DocumentAttachmentView view = (DocumentAttachmentView)mDocumentsView.getChildAt(i);

					if (view == null) {
						view = new DocumentAttachmentView(getContext(), mImageLoader);

						mDocumentsView.addView(view);
					}

					view.setVisibility(VISIBLE);
					view.setDocument(attachment);
				} else {
					mDocumentsView.getChildAt(i).setVisibility(GONE);
				}
			}
		} else {
			mDocumentsSectionView.setVisibility(View.GONE);
		}				
	}

	private void updateLocationAttachmentViews() {
		Location location = mItem.message.content.location;

		if (location != null) {
			String imageUrl = mGoogleStaticMap.getUrlForImage(new Size(mLocationThumb.getWidth(), mLocationThumb.getHeight()), location, GoogleStaticMap.ZOOM_MIDDLE);
			mLocationLayout.setVisibility(VISIBLE);
			mLocationThumb.setImageUrl(imageUrl);
		} else {
			mLocationLayout.setVisibility(GONE);
		}
	}

	private void updateMessageAttachmentViews() {
		List<ChatMessage> attachments = mItem.message.content.forwarded;

		if (attachments.size() != 0) {
			mMessagesSectionView.setVisibility(View.VISIBLE);
			mMessagesSectionTitleView.setText(DialogListItem.formatMessagesText(getContext(), mItem.message.content.forwarded.size()));

			final int total = attachments.size();

			for (int i = 0; i < Math.max(total, mMessagesView.getChildCount()); i++) {
				if (i < total) {
					ChatMessage attachment = attachments.get(i);

					View view = (View)mMessagesView.getChildAt(i);

					if (view == null) {
						view = inflate(getContext(), R.layout.forwarded_message, null);
						view.setTag(new ForwardedMessageViewHolder(view, mImageLoader, mLastSeenTimeFormat));
						view.setOnClickListener(mForwardedMessageClickListener);
						view.setClickable(true);

						mMessagesView.addView(view);
					}

					ForwardedMessageViewHolder viewHolder = (ForwardedMessageViewHolder)view.getTag();

					view.setVisibility(VISIBLE);
					viewHolder.setMessage(attachment);
				} else {
					mMessagesView.getChildAt(i).setVisibility(GONE);
				}
			}
		} else {
			mMessagesSectionView.setVisibility(View.GONE);
		}
	}

	private void updateAudioAttachmentViews() {
		mAudiosView.setPlaylist(AudioPlayer.Playlist.fromMessage(mItem.message));
	}

	private final OnClickListener mOnImageClick = new OnClickListener() {
		public void onClick(View v) {
			ImageUrls urls = (ImageUrls)v.getTag();

			if (urls != null) {
				ViewImageActivity.displayWebImage(getContext(), urls.fullsizeUrl);
			} 
		}
	};

	private final OnClickListener mOnLocationClick = new OnClickListener() {
		public void onClick(View v) {
			if (mItem.message.content.location != null) {
				getContext().startActivity(LocationActivity.getDisplayIntent(getContext(), mItem.message.content.location));
			}
		}
	};

	private final OnClickListener mForwardedMessageClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			ChatMessage message = ((ForwardedMessageViewHolder)v.getTag()).getMessage();

			getContext().startActivity(ChatMessageActivity.getDisplayIntent(getContext(), message));
		}
	};
}
