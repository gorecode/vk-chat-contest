package com.gorecode.vk.activity.chat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.gorecode.vk.R;
import com.gorecode.vk.activity.GroupChatActivity;
import com.gorecode.vk.activity.LocationActivity;
import com.gorecode.vk.activity.PickImageActivity;
import com.gorecode.vk.activity.UserActivity;
import com.gorecode.vk.activity.VkActivityContract;
import com.gorecode.vk.activity.VkFragmentActivity;
import com.gorecode.vk.adapter.LoaderAdapter;
import com.gorecode.vk.adapter.TableAdapter;
import com.gorecode.vk.adapter.TableLoaderAdapter;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.audio.AudioPlayer;
import com.gorecode.vk.cache.ChatCache;
import com.gorecode.vk.collections.Table;
import com.gorecode.vk.collections.TableChange;
import com.gorecode.vk.data.Availability;
import com.gorecode.vk.data.ChatMessage;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.data.GroupChatDescriptor;
import com.gorecode.vk.data.Message;
import com.gorecode.vk.data.ObjectSubset;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.Size;
import com.gorecode.vk.data.TypingNotification;
import com.gorecode.vk.data.loaders.BackwardPreloader;
import com.gorecode.vk.data.loaders.CollectionLoader;
import com.gorecode.vk.data.loaders.CollectionLoaderByOffset;
import com.gorecode.vk.event.AvailabilityChangedEvent;
import com.gorecode.vk.event.ChatMessageStateChangedEvent;
import com.gorecode.vk.event.DialogChangedEvent;
import com.gorecode.vk.event.DialogDeletedEvent;
import com.gorecode.vk.event.LongPollConnectionRestoredEvent;
import com.gorecode.vk.event.TypingNotificationEvent;
import com.gorecode.vk.googlemaps.GoogleStaticMap;
import com.gorecode.vk.googlemaps.Marker;
import com.gorecode.vk.imageloader.ImageProcessors;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.sync.SessionContext;
import com.gorecode.vk.task.AttachPhotosTask;
import com.gorecode.vk.task.AutoCancelPool;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.task.LongActionContext;
import com.gorecode.vk.utilities.AgoTimeFormat;
import com.gorecode.vk.utilities.BitmapUtilities;
import com.gorecode.vk.utilities.BundleUtilities;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.Toasts;
import com.gorecode.vk.view.VkActionBar;
import com.gorecode.vk.view.Animations;
import com.gorecode.vk.view.BetterScrollDetector;
import com.gorecode.vk.view.ForwardingOnScrollListener;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.ViewAnimationHelper;
import com.gorecode.vk.view.ViewUtilities;
import com.gorecode.vk.view.WebImageView;
import com.perm.kate.api.Params;
import com.perm.kate.api.VkPhoto;
import com.uva.lang.StringUtilities;
import com.uva.lang.ThreadFactories;
import com.uva.location.Location;
import com.uva.log.Log;

public class ChatActivity extends VkFragmentActivity {
	public static final String TAG = ChatActivity.class.getSimpleName();

	private static final String EXTRA_DIALOG = "dialog";

	private static final long ANIMATION_DURATION_LAST_SEEN = 300;

	private static final int FOOTER_INDEX_LAST_SEEN = 0;
	private static final int FOOTER_INDEX_TYPING = 1;

	private static final int TYPING_NOTIFICATION_AUTOHIDE_TIMEOUT = 5000;

	private static final int REQUEST_CODE_PICK_LOCATION = 0x1;
	private static final int REQUEST_CODE_VIEW_OR_EDIT_DIALOG = 0x2;
	private static final int REQUEST_CODE_PICK_PHOTO = 0x3;

	public static final int LOCATION_PREVIEW_ZOOM = 13;

	private static final ExecutorService sRequestExecutor = new ThreadPoolExecutor(0, 2, 3, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), ThreadFactories.WITH_LOWEST_PRIORITY);

	@Inject
	private ChatCache mChatCache;
	private ChatSender mChatSender;
	private ChatTable mChatMessages;
	private ChatListModel mChatListModel;
	private ChatListAdapter mChatListAdapter;
	private ChatTableLoader mChatMessagesLoader;
	private ChatTableLoaderAdapter mChatMessagesBackwardLoaderAdapter;

	@Inject
	private SessionContext mSessionContext;
	@Inject
	private VkModel mModel;
	@Inject
	private EventBus mEventBus;
	@Inject
	private LayoutInflater mInflater;

	private boolean mIsViewDestroyed;

	@InjectView(R.id.popup_menu)
	private View mPopupMenu;
	@InjectView(R.id.title)
	private TextView mActivityTitle;
	@InjectView(R.id.message_edit)
	private EditText mEdit;
	@InjectView(R.id.loaderLayout)
	private LoaderLayout mLoaderLayout;
	@InjectView(R.id.send_message_button)
	private Button mSendButton;
	@InjectView(android.R.id.list)
	private ListView mListView;
	@InjectView(R.id.group_chat_users_text)
	private TextView mGroupChatUsersCountText;
	@InjectView(R.id.edit_group_chat_button)
	private View mEditGroupChatButton;
	@InjectView(R.id.photo)
	private WebImageView mPhotoView;
	@InjectView(R.id.messages_menu)
	private View mSelectionMenu;
	private ViewAnimationHelper mSelectionMenuAnimationHelper;
	@InjectView(R.id.attachments_layout)
	private View mAttachmentsLayout;
	@InjectView(R.id.add_attachment_button)
	private ImageView mAddAttachmentButton;
	@InjectView(R.id.attachments_list)
	private ViewGroup mAttachmentsListView;
	@InjectView(R.id.attach_location_button)
	private View mAttachLocationButton;
	@InjectView(R.id.attach_photo_from_camera_button)
	private View mAttachPhotoFromCameraButton;
	@InjectView(R.id.attach_photo_from_gallery_button)
	private View mAttachPhotoFromGalleryButton;
	@InjectView(R.id.delete_button)
	private Button mDeleteSelectionButton;
	@InjectView(R.id.forward_button)
	private Button mForwardSelectionButton;

	private VkActionBar mActionBar;
	private View mHeaderView;
	private View mFooterView;
	private TextView mLastSeenView;
	private ViewAnimationHelper mLastSeenViewAnimator;
	private TextView mTypingView;
	private ViewAnimationHelper mTypingViewAnimator;

	private BetterScrollDetector mListViewScrollDetector;

	private ViewAnimationHelper mPopupMenuAnimator;
	private ViewAnimationHelper mBackwardLoadingIndicator;

	@Inject
	private AgoTimeFormat mLastSeenTimeFormat;

	@Inject
	private ListImageLoader mListImageLoader;

	@Inject
	private AudioPlayer mAudioPlayer;

	private final AutoCancelPool mAutoCancelPool = new AutoCancelPool();
	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private Dialog mDialog;

	private TypingNotificationEmitter mTypingNotificationsEmitter;

	private HashMap<Long, Profile> mTypingPeople = new HashMap<Long, Profile>(); 

	private boolean mIsScrolledToBottom;

	private GoogleStaticMap mGoogleStaticMap;

	private SyncLocalChatTask mPendingSync;

	private Location mAttachedLocation;

	private final List<PhotoAttachment> mAttachedPhotos = new ArrayList<PhotoAttachment>();

	private boolean mIsPaused;

	private static final List<ChatMessage> sAttachedMessages = new ArrayList<ChatMessage>();

	public static void display(Context context, Profile participant) {
		context.startActivity(getDisplayIntent(context, participant));
	}

	public static Intent getDisplayIntent(Context context, ChatMessage message) {
		if (message.isFromConference()) {
			return getDisplayIntent(context, Dialog.withOneMessage(message));
		} else {
			return getDisplayIntent(context, message.getParticipant());
		}
	}

	public static Intent getDisplayIntent(Context context, Dialog groupChat) {
		Intent intent = new Intent(context, ChatActivity.class);

		BundleUtilities.putExtra(intent, EXTRA_DIALOG, groupChat);

		return intent;		
	}

	public static Intent getDisplayIntent(Context context, GroupChatDescriptor groupChat) {
		Dialog dialog = new Dialog();

		dialog.totalParticipants = groupChat.users.size();
		dialog.lastMessage = new ChatMessage();
		dialog.lastMessage.content = new ChatMessage.Content("");
		dialog.lastMessage.content.subject = groupChat.title;
		dialog.lastMessage.chatId = groupChat.chatId;

		for (Profile participant : groupChat.users) {
			dialog.putActiveParticipant(participant);
		}

		return getDisplayIntent(context, dialog);		
	}

	public static Intent getDisplayIntent(Context context, Profile participant) {
		ChatMessage message = new ChatMessage();

		message.direction = Message.DIRECTION_OUTGOING;
		message.user = participant;

		Dialog dialog = Dialog.withOneMessage(message);

		return getDisplayIntent(context, dialog);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_RIGHT);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.chat_activity);

		getParametersFrom(getIntent());

		mGoogleStaticMap = new GoogleStaticMap(this);
		mGoogleStaticMap.setMapType(GoogleStaticMap.MAP_TYPE_ROADMAP);

		Activity activity = getActivity();

		Aibolit.doInjections(this, getActivity());

		mChatSender = ChatSender.getPool().getSenderForDialog(mDialog, mModel);
		mChatSender.setDispatchCallbacks(mChatSenderCallbacks);
		mChatMessages = new ChatTable() {
			private boolean mAutoMarkingEnabled = true;

			private final List<Long> mMids = new ArrayList<Long>();

			@Override
			public TableChange<ChatMessage> put(ChatMessage object) {
				if (mAutoMarkingEnabled && object.isIncoming() && object.unread) {
					if (!mIsPaused) {
						markMessagesAsReadAsync(Collections.singleton(object.id));
					}
				}
				return super.put(object);
			}

			@Override
			public Collection<TableChange<ChatMessage>> putAll(Iterable<ChatMessage> objects) {
				mAutoMarkingEnabled = false;

				try {
					mMids.clear();

					for (ChatMessage object : objects) {
						if (object.isIncoming() && object.unread) {
							mMids.add(object.id);	
						}
					}

					return super.putAll(objects);
				} finally {
					mAutoMarkingEnabled = true;

					if (!mIsPaused) {
						markMessagesAsReadAsync(mMids);
					}
				}
			}
		};

		mChatMessagesLoader = new ChatTableLoader(mChatMessages);
		mChatListModel = new ChatListModel(activity, mChatMessages);
		mChatListAdapter = new ChatListAdapter();
		mChatMessagesBackwardLoaderAdapter = new ChatTableLoaderAdapter(new BackwardPreloader<ChatMessage>(sRequestExecutor, new ChatTableLoader(mChatMessages)));
		mChatMessagesBackwardLoaderAdapter.displayLoadStateInLoaderLayoutOnLoadingFromScratch(mLoaderLayout);
		mChatMessagesBackwardLoaderAdapter.registerCallbacks(mRestoreListViewScrollState);
		mChatMessagesBackwardLoaderAdapter.registerCallbacks(mScrollToBottomOnce);

		for (ChatMessage unsent : mChatSender.getPendingMessages()) {
			ChatListItem item = ChatListItem.newMessage(unsent);

			item.dispatchState = ChatListItem.DISPATCH_STATE_PENDING;

			mChatListModel.put(item);
		}

		ChatMessage[] cached = Iterables.toArray(mChatCache.findByCid(mDialog.getCid()), ChatMessage.class);

		if (cached.length > 0) {
			mChatMessages.putAll(cached);
			mChatMessages.notifyTableChanged();
		}

		Log.debug(TAG, "got " + cached.length + " messages in cache");

		setUpViews();

		updateAttachmentViews();
		updateViews();

		if (mChatListModel.size() > 0) {
			scrollToBottomAndUpdateLastSeen();
		}

		mEventBus.register(this);

		syncLocalChat();
	}

	@Override
	public void onResume() {
		super.onResume();

		mIsPaused = false;

		markAllMessagesAsReadAsync();
	}

	@Override
	public void onPause() {
		super.onPause();

		mIsPaused = true;

		markAllMessagesAsReadAsync();
	}

	@Override
	public void onDestroy() {
		mIsViewDestroyed = true;

		mAutoCancelPool.drain();

		mChatSender.setDispatchCallbacks(null);

		mEventBus.unregister(this);

		mListImageLoader.stop();

		super.onDestroy();
	}

	@InjectOnClickListener(R.id.cancel_button)
	public void onResetSelectionButtonClicked(View v) {
		resetSelection();
	}

	@InjectOnClickListener(R.id.delete_button)
	public void onDeleteSelectionButtonClicked(View v) {
		deleteSelection();
	}

	@InjectOnClickListener(R.id.forward_button)
	public void onForwardSelectionButtonClicked(View v) {
		forwardSelection();
	}

	public boolean shouldNotTouchViews() {
		return mIsViewDestroyed || isDestroyed();
	}

	@Subscribe
	public void onLongPollConnectionRestored(LongPollConnectionRestoredEvent event) {
		syncLocalChat();
	}

	@Subscribe
	public void onDialogChanged(DialogChangedEvent event) {
		Log.debug(TAG, "Dialog changed");

		Dialog dialog = event.dialog;

		if (dialog.getCid() != mDialog.getCid()) return;

		mDialog = dialog;

		updateViews();
	}

	@Subscribe
	public void onAvailabilityChanged(final AvailabilityChangedEvent event) {
		if (mDialog.isConference()) return;

		if (mDialog.getParticipant().id != event.getUserId()) return;

		final Profile participant = mDialog.getParticipant();

		participant.availability = event.getAvailability();

		if (!mDialog.isConference()) {
			if (event.getAvailability() == Availability.OFFLINE) {
				participant.lastActivityTime = System.currentTimeMillis();
			}
		}

		updateViews();
	}

	@Subscribe
	public void onTypingNotificationReceived(TypingNotificationEvent event) {
		if (mDialog.getCid() != event.getCid()) return;

		Profile user = mDialog.findParticipant(event.user.id);

		if (user == null) {
			user = event.user;
		}

		mDialog.putActiveParticipant(user);

		final Profile participant = user;

		Runnable participantIsNotTypingRunnable = new Runnable() {
			@Override
			public void run() {
				if (shouldNotTouchViews()) return;

				setTypingStatus(participant, TypingNotification.STOPPED);
			}
		};

		mHandler.removeCallbacks(participantIsNotTypingRunnable);
		mHandler.postDelayed(participantIsNotTypingRunnable, TYPING_NOTIFICATION_AUTOHIDE_TIMEOUT);

		setTypingStatus(participant, event.notification);
	}

	@Subscribe
	public void onDialogDeleted(DialogDeletedEvent event) {
		if (mDialog.getCid() == event.dialog.getCid()) {
			mChatCache.deleteAll(mDialog.getCid());

			mChatMessagesLoader.setOffset(0);
			mChatMessages.clear();
			mChatMessages.notifyTableChanged();
		}
	}

	@Subscribe
	public void onNewMessage(ChatMessage message) {
		if (!isForThisChat(message)) return;

		Log.debug(TAG, "onNewMessage, content = " + message.content);

		if (message.isIncoming() && message.unread) {
			setTypingStatus(message.getParticipant(), TypingNotification.STOPPED);

			markMessagesAsReadAsync(Collections.singleton(message.id));
		}

		mChatCache.saveAsync(message);

		if (message.isIncoming()) {
			mChatMessages.put(message);
			mChatMessages.notifyTableChanged();
		} else {
			boolean isSentFromThisClient = mChatListModel.removeById(message.id) != null;

			if (!isSentFromThisClient) {
				Log.debug(TAG, "message can be sent from another running client, check first pending message");

				ChatListItem unsent = null;

				for (int i = mChatListModel.size() - 1; i >= 0; i--) {
					ChatListItem item = mChatListModel.asList().get(i);

					if (item.dispatchState != ChatListItem.DISPATCH_STATE_PENDING) {
						break;
					} else {
						unsent = item;
					}
				}

				if (unsent != null) {
					Log.debug(TAG, "first pending message in queue is " + unsent.message.content);

					final ChatMessage.Content c1 = unsent.message.content;
					final ChatMessage.Content c2 = message.content;

					final boolean sameText = Objects.equal(c1.text, c2.text);
					final boolean sameNumAttachments = Objects.equal(c1.getAttachmentsCount(), c2.getAttachmentsCount());
					final boolean sameNumPhotos = c1.imageUrls.size() == c2.imageUrls.size();
					final boolean sameNumMessages = c1.forwarded.size() == c2.forwarded.size();
					final boolean sameNumVideos = c1.videos.size() == c2.videos.size();
					final boolean sameNumDocuments = c1.documents.size() == c2.documents.size();

					if (sameText && sameNumAttachments && sameNumMessages && sameNumVideos && sameNumDocuments && sameNumPhotos) {
						Log.debug(TAG, "new outgoing message & first pending message looks identical");

						mChatListModel.removeById(unsent.getId());

						isSentFromThisClient = true;
					}
				}
			}

			mChatMessages.put(message);
			mChatMessages.notifyTableChanged();

			if (isSentFromThisClient) {
				mChatListModel.setDispatchStateState(message.id, ChatListItem.DISPATCH_STATE_SENT_NOW);
				mChatListModel.notifyTableChanged();
			}
		}
	}

	@Subscribe
	public void onMessageStateChanged(ChatMessageStateChangedEvent event) {
		ChatMessage message = mChatMessages.getById(event.messageId);

		if (message == null) return;

		if (Boolean.TRUE.equals(event.isDeleted)) {
			mChatMessages.removeById(message.id);
			mChatMessagesLoader.decreaseOffset(1);
		} else {
			event.apply(message);

			mChatMessages.put(message);			
		}

		mChatMessages.notifyTableChanged();
	}

	protected void scrollToBottomAndUpdateLastSeen() {
		scrollToBottomOnce();

		updateLastSeenAsync();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_PICK_LOCATION:
			if (resultCode == Activity.RESULT_OK) {
				mAttachedLocation = LocationActivity.getResult(data);

				updateAttachmentViews();
			}
			break;
		case REQUEST_CODE_PICK_PHOTO:
			if (resultCode == Activity.RESULT_OK) {
				try {
					PhotoAttachment attachment = new PhotoAttachment();

					final int maxThumbSize = ViewUtilities.dipsToPixels(this, 74);

					attachment.file = PickImageActivity.getImageFile(data);
					attachment.thumb = PhotoAttachment.createThumb(attachment.file.getAbsolutePath(), maxThumbSize);

					mAttachedPhotos.add(attachment);

					updateAttachmentViews();
				} catch (Exception e) {
					Log.exception(TAG, "Error getting photo attachment thumb", e);

					ErrorHandlingUtilities.displayErrorSoftly(this, e);
				}
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@InjectOnClickListener(R.id.send_message_button)
	protected void onSendButtonClick(View view) {
		String messageText = mEdit.getText().toString().trim();

		if (messageText.length() == 0 && numAttachments() == 0) return;

		final ChatMessage.Content content = new ChatMessage.Content(messageText);

		content.location = mAttachedLocation;
		content.forwarded.addAll(sAttachedMessages);

		if (mAttachedPhotos.size() > 0) {
			new AttachPhotosTask(mModel) {
				private final ProgressDialog mProgressDialog = new ProgressDialog(getActivity());

				@Override
				public void onPreExecute() {
					Log.debug(TAG, "Start uploading image attachment");

					if (isCancelled()) {
						return;
					}

					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					mProgressDialog.setMessage(getActivity().getText(R.string.message_uploading_images));

					blockUi();
				}

				@Override
				public void onProgressUpdate(Integer... percentsComplete) {
					if (isCancelled()) return;

					int percent = percentsComplete[0];

					mProgressDialog.setProgress(percent);
				}

				@Override
				public void onPostExecute(Pair<List<VkPhoto>, Exception> result) {
					if (!shouldNotTouchViews()) {
						unblockUi();
					}

					if (isCancelled()) {
						return;
					}

					if (result.second != null) {
						Log.exception(TAG, "Unable to upload image attachment", result.second);

						ErrorHandlingUtilities.displayErrorSoftly(ChatActivity.this, result.second);
					}
					if (result.first != null) {
						Log.debug(TAG, "Image attachment has been uploaded");

						content.addAttachments(result.first);

						sendMessage(content);
					}
				}

				@Override
				public void onCancelled() {
					if (!shouldNotTouchViews()) {
						unblockUi();
					}					
				}

				private void blockUi() {
					mProgressDialog.show();

					mSendButton.setEnabled(false);
				}

				private void unblockUi() {
					mSendButton.setEnabled(true);

					mProgressDialog.dismiss();
				}

			}.execute(Iterables.toArray(Collections2.transform(mAttachedPhotos, new Function<PhotoAttachment, File>() {
				@Override
				public File apply(PhotoAttachment attachment) {
					return attachment.file;
				}
			}), File.class));
		} else {
			sendMessage(content);
		}
	}

	@InjectOnClickListener(R.id.attach_location_button)
	protected void onAddLocationButtonClick(View view) {
		pickLocationForAttachment();
	}

	@InjectOnClickListener(R.id.attach_location_popup_item)
	protected void onAddLocationPopupItemClick(View view) {
		pickLocationForAttachment();

		mPopupMenuAnimator.hideView();
	}

	@InjectOnClickListener(R.id.attach_photo_from_camera_button)
	protected void onAddPhotoFromCameraButtonClick(View v) {
		pickPhotoFromCamera();
	}

	@InjectOnClickListener(R.id.attach_photo_from_camera_popup_item)
	protected void onAddPhotoFromCameraPopupItemClick(View v) {
		pickPhotoFromCamera();

		mPopupMenuAnimator.hideView();
	}

	@InjectOnClickListener(R.id.attach_photo_from_gallery_button)
	protected void onAddPhotoFromGalleryButtonClick(View view) {
		pickPhotoFromGallery();
	}	

	@InjectOnClickListener(R.id.attach_photo_from_gallery_popup_item)
	protected void onAddPhotoFromGalleryPopupItemClick(View view) {
		pickPhotoFromGallery();

		mPopupMenuAnimator.hideView();
	}	

	@Override
	public void onBackPressed() {
		if (mAttachmentsLayout.getVisibility() == View.VISIBLE) {
			mAttachmentsLayout.setVisibility(View.GONE);
		} else if (mPopupMenu.getVisibility() == View.VISIBLE) {
			mPopupMenuAnimator.hideView();
		} else {
			super.onBackPressed();
		}
	}

	@InjectOnClickListener(R.id.add_attachment_button)
	protected void onAddAttachmentButtonClick(View view) {
		if (numAttachments() > 0) {
			if (mAttachmentsLayout.getVisibility() == View.VISIBLE) {
				mAttachmentsLayout.setVisibility(View.GONE);
			} else {
				mAttachmentsLayout.setVisibility(View.VISIBLE);
			}
		} else {
			if (mPopupMenu.getVisibility() == View.GONE) {
				mPopupMenuAnimator.showView();
			} else {
				mPopupMenuAnimator.hideView();
			}
		}
	}

	private void pickLocationForAttachment() {
		Intent intent = new Intent(getActivity(), LocationActivity.class);
		intent.setAction(Intent.ACTION_PICK);
		startActivityForResult(intent, REQUEST_CODE_PICK_LOCATION);
	}

	private void pickPhotoFromCamera() {
		PickImageActivity.display(this, REQUEST_CODE_PICK_PHOTO, PickImageActivity.SOURCE_CAMERA);
	}

	private void pickPhotoFromGallery() {
		PickImageActivity.display(this, REQUEST_CODE_PICK_PHOTO, PickImageActivity.SOURCE_GALLERY);
	}

	private int numAttachments() {
		return (mAttachedLocation != null ? 1 : 0) + sAttachedMessages.size() + mAttachedPhotos.size();
	}

	private boolean isForThisChat(ChatMessage message) {
		return (mDialog.getCid() == message.getCid());
	}

	private void deleteSelection() {
		new DeleteMessagesTask().execute(mChatListModel.getSelection());
	}

	private void forwardSelection() {
		sAttachedMessages.clear();

		for (Long mid : mChatListModel.getSelection()) {
			sAttachedMessages.add(mChatMessages.getById(mid));
		}

		Toasts.makeText(this, R.string.toast_messages_attached).show();

		resetSelection();

		updateAttachmentViews();
		updateViews();
	}

	private void resetSelection() {
		mChatListModel.resetSelection();

		updateViews();
	}

	private void scrollToBottomOnce() {
		runOnUiThreadSafely(new Runnable() {
			@Override
			public void run() {
				if (mIsScrolledToBottom) return;

				mIsScrolledToBottom = true;

				scrollToBottom();
			}
		});
	}

	private void scrollToBottom() {
		runOnUiThreadSafely(new Runnable() {
			public void run() {
				int lastPosition = getListView().getCount() - 1;

				getListView().setSelection(lastPosition);
			}
		});
	}

	private void updateLastSeenAsync() {
		if (!mDialog.isConference()) {
			new UpdateLastSeenTask().execute();
		}
	}

	private void syncLocalChat() {
		Log.trace(TAG, "loadFreshChatMessages() called");

		if (mChatMessages.size() == 0) {
			// Load first portion of data.
			Log.trace(TAG, "No data was loader, call mChatLoaderAdapter.loadMoreData()");

			mChatMessagesBackwardLoaderAdapter.loadMoreData();
		} else {
			// Load all new messages.
			if (mPendingSync == null) {
				Log.trace(TAG, "Start SyncLocalChatTask");
				mPendingSync = new SyncLocalChatTask();
				mAutoCancelPool.add(mPendingSync);
				mPendingSync.execute();
			}
		}
	}

	private void sendMessage(ChatMessage.Content content) {
		ChatMessage localMessage = mChatSender.enqueueForDispatch(content);

		mAttachedLocation = null;
		sAttachedMessages.clear();
		mAttachedPhotos.clear();

		updateAttachmentViews();

		mEdit.setText("");								
		mSendButton.setEnabled(true);

		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);

		ChatListItem item = ChatListItem.newMessage(localMessage);

		item.dispatchState = ChatListItem.DISPATCH_STATE_PENDING;

		mChatListModel.put(item);
		mChatListModel.notifyTableChanged();

		scrollToBottom();
	}

	private void markMessagesAsReadAsync(final Collection<Long> mids) {
		if (mids.size() > 0) {
			sRequestExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						mModel.markMessagesAsRead(mids);

						Log.debug(TAG, "All chat messages are marked as read");
					} catch (Exception e) {
						Log.exception(TAG, com.uva.log.Message.WARNING, "Unable to mark chat messages as read", e);
					}
				}
			});
		}		
	}

	private void markAllMessagesAsReadAsync() {
		Log.trace(TAG, "Marking all messages as read");

		final List<Long> mids = Lists.transform(mChatMessages.asList(), new Function<ChatMessage, Long>() {
			@Override
			public Long apply(ChatMessage arg) {
				return arg.id;
			}
		});

		markMessagesAsReadAsync(mids);
	}

	private void setTypingStatus(Profile participant, TypingNotification status) {
		Log.debug(TAG, String.format("Opponent typing status = %s", status));

		if (status == TypingNotification.TYPING) {
			mTypingPeople.put(participant.id, participant);
		} else {
			mTypingPeople.remove(participant.id);
		}

		updateViews();
	}

	private ListView getListView() {
		return mListView;
	}

	private void getParametersFrom(Intent intent) {
		Bundle extras = getIntent().getExtras();

		if (extras == null) {
			throw new IllegalArgumentException("Bad intent, use getDisplayIntent() for valid intent");
		}

		mDialog = BundleUtilities.getDialog(extras, EXTRA_DIALOG);
	}

	private Activity getActivity() {
		return this;
	}

	private void setUpViews() {
		Activity activity = getActivity();

		LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		mAttachmentsLayout.setVisibility(View.GONE);

		mPopupMenu.setVisibility(View.GONE);
		mPopupMenuAnimator = new ViewAnimationHelper(mPopupMenu, Animations.POPUP_HIDE_ANIMATION, Animations.POPUP_SHOW_ANIMATION);

		mSelectionMenuAnimationHelper = new ViewAnimationHelper(mSelectionMenu, Animations.DEFAULT_HIDE_ANIMATION, Animations.DEFAULT_SHOW_ANIMATION);
		mSelectionMenu.setVisibility(View.GONE);

		mActionBar = ((VkActivityContract)activity).getVkActionBar();
		mActionBar.setActionButtonOnClickListener(mOnActionBarRightButtonClicked);

		mLoaderLayout.setHideMethod(LoaderLayout.HIDE_METHOD_SET_INVISIBLE);

		mHeaderView = inflater.inflate(R.layout.item_loading, null);

		mBackwardLoadingIndicator = new ViewAnimationHelper(mHeaderView.findViewById(R.id.text), Animations.DEFAULT_HIDE_ANIMATION, Animations.DEFAULT_SHOW_ANIMATION);
		mBackwardLoadingIndicator.setHideMethod(ViewAnimationHelper.HIDE_METHOD_SET_INVISIBLE);

		mFooterView = inflater.inflate(R.layout.chat_list_footer, null);

		mLastSeenView = (TextView)mFooterView.findViewById(R.id.text1);
		mLastSeenView.setVisibility(View.INVISIBLE);
		mLastSeenViewAnimator = new ViewAnimationHelper(mLastSeenView);
		mLastSeenViewAnimator.setHideAnimation(new AlphaAnimation(1.0f, 0.0f));
		mLastSeenViewAnimator.setShowAnimation(new AlphaAnimation(0.0f, 1.0f));
		mLastSeenViewAnimator.setAnimationsDuration(ANIMATION_DURATION_LAST_SEEN);
		mLastSeenViewAnimator.setHideMethod(ViewAnimationHelper.HIDE_METHOD_SET_INVISIBLE);

		mTypingNotificationsEmitter = new TypingNotificationEmitter(mDialog, mModel, sRequestExecutor);

		mTypingView = (TextView)mFooterView.findViewById(R.id.text2);
		mTypingView.setVisibility(View.INVISIBLE);
		mTypingViewAnimator = new ViewAnimationHelper(mTypingView);
		mTypingViewAnimator.setHideAnimation(new AlphaAnimation(1.0f, 0.0f));
		mTypingViewAnimator.setShowAnimation(new AlphaAnimation(0.0f, 1.0f));
		mTypingViewAnimator.setAnimationsDuration(ANIMATION_DURATION_LAST_SEEN);
		mTypingViewAnimator.setHideMethod(ViewAnimationHelper.HIDE_METHOD_SET_INVISIBLE);

		mListViewScrollDetector = new BetterScrollDetector();
		mListViewScrollDetector.setOnScrollStoppedListener(new BetterScrollDetector.OnScrollStoppedListener() {
			@Override
			public void onScrollStopped() {
				;
			}
		});

		ForwardingOnScrollListener listViewOnScrollListener = new ForwardingOnScrollListener();

		listViewOnScrollListener.addListener(mChatMessagesBackwardLoaderAdapter);
		listViewOnScrollListener.addListener(mListImageLoader);
		listViewOnScrollListener.addListener(mListViewScrollDetector);

		mListView.setEmptyView(findViewById(android.R.id.empty));
		mListView.setOnScrollListener(listViewOnScrollListener);
		mListView.setStackFromBottom(true);
		mListView.addHeaderView(mHeaderView);
		mListView.addFooterView(mFooterView);
		mListView.setAdapter(mChatMessagesBackwardLoaderAdapter);
		mListView.setDrawingCacheEnabled(false);
		mListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		mPhotoView.setVisibility(mDialog.isConference() ? View.GONE : View.VISIBLE);

		if (!mDialog.isConference()) {
			mPhotoView.setImageUrls(mDialog.getParticipant().avatarUrls, ImageProcessors.PHOTO_ROUNDER);
		}

		mEditGroupChatButton.setVisibility(mDialog.isConference() ? View.VISIBLE : View.GONE);

		mEdit.addTextChangedListener(mTypingNotificationsEmitter);
	}

	private void runOnUiThreadSafely(final Runnable target) {
		Activity activity = getActivity();

		if (activity == null) return;

		activity.runOnUiThread(new Runnable() {
			public void run() {
				if (shouldNotTouchViews()) return;

				target.run();
			}
		});		
	}

	private void updateAttachmentViews() {
		if (shouldNotTouchViews()) {
			return;
		}

		final boolean canAttachMorePhotos = mAttachedPhotos.size() < 5;

		mAttachLocationButton.setVisibility(mAttachedLocation != null ? View.GONE : View.VISIBLE);
		mAttachPhotoFromCameraButton.setVisibility(canAttachMorePhotos ? View.VISIBLE : View.GONE);
		mAttachPhotoFromGalleryButton.setVisibility(canAttachMorePhotos ? View.VISIBLE : View.GONE);

		mAttachmentsListView.removeViews(0, mAttachmentsListView.getChildCount() - 3);

		if (numAttachments() == 0) {
			mAttachmentsLayout.setVisibility(View.GONE);
		} else {
			mAttachmentsLayout.setVisibility(View.VISIBLE);

			for (final PhotoAttachment attachment : mAttachedPhotos) {
				View view = mInflater.inflate(R.layout.view_attachment_preview, null);

				((WebImageView)view.findViewById(R.id.photo)).setImageBitmap(attachment.thumb);

				view.findViewById(R.id.content_layout).setBackgroundResource(0);

				view.setTag(attachment);
				view.findViewById(R.id.delete_attachment_button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mAttachedPhotos.remove(attachment);

						updateAttachmentViews();
					}
				});

				mAttachmentsListView.addView(view, 0);
			}

			if (mAttachedLocation != null) {
				View view = mInflater.inflate(R.layout.view_attachment_preview, null);

				GoogleStaticMap staticMap = new GoogleStaticMap(getActivity());

				String imageUrl = staticMap.getUrlForImage(new Size(320, 320), Marker.forLocation(mAttachedLocation));

				((WebImageView)view.findViewById(R.id.photo)).setImageUrl(imageUrl, ImageProcessors.PHOTO_ROUNDER);

				view.findViewById(R.id.content_layout).setBackgroundResource(0);

				view.setTag(mAttachedLocation);
				view.findViewById(R.id.delete_attachment_button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mAttachedLocation = null;

						updateAttachmentViews();
					}
				});

				mAttachmentsListView.addView(view, 0);				
			}

			if (sAttachedMessages.size() > 0) {
				View view = mInflater.inflate(R.layout.view_attachment_preview, null);

				((WebImageView)view.findViewById(R.id.photo)).setImageResource(R.drawable.msg_active);

				view.findViewById(R.id.content_layout).setBackgroundResource(0);

				view.setTag(sAttachedMessages);
				view.findViewById(R.id.delete_attachment_button).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sAttachedMessages.clear();

						updateAttachmentViews();
					}
				});

				mAttachmentsListView.addView(view, 0);
			}
		}

		View firstView = mAttachmentsListView.getChildAt(0);

		Object firstTag = firstView.getTag();

		mAttachmentsListView.requestLayout();

		if (firstTag != null) {
			try {
				if (firstTag instanceof PhotoAttachment) {
					PhotoAttachment attachment = (PhotoAttachment)firstTag;

					Bitmap buttonImage = Bitmap.createScaledBitmap(attachment.thumb, mAddAttachmentButton.getWidth(), mAddAttachmentButton.getHeight(), true);

					mAddAttachmentButton.setImageBitmap(buttonImage);
				} else if (firstTag instanceof Location) {
					mAddAttachmentButton.setImageResource(R.drawable.attach_geob_small);
				} else if (firstTag instanceof List) { 
					mAddAttachmentButton.setImageResource(R.drawable.attach_galleryb_small);
				} else {
					mAddAttachmentButton.setImageBitmap(null);
				}
			} catch (Throwable t) {
				mAddAttachmentButton.setImageBitmap(null);
			}
		} else {
			mAddAttachmentButton.setImageBitmap(null);
		}
	}

	private void updateViews() {
		if (shouldNotTouchViews()) {
			return;
		}

		final int numSelected = mChatListModel.getSelection().size();

		if (numSelected == 0) {
			mSelectionMenuAnimationHelper.hideView();
		} else {
			mDeleteSelectionButton.setText(getString(R.string.chat_messages_button_delete, numSelected));

			mForwardSelectionButton.setText(getString(R.string.chat_messages_button_forward, numSelected));

			mSelectionMenuAnimationHelper.showView();
		}

		if (!mDialog.isConference()) {
			Profile participant = mDialog.getParticipant();

			Availability availability = participant.availability;

			mActionBar.setTitle(participant.getFullname());

			if (availability == Availability.ONLINE) {
				mActivityTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.online, 0);
			} else if (availability == Availability.OFFLINE) {
				mActivityTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			mLastSeenView.setText(String.format(getString(R.string.chat_last_activity_format), mLastSeenTimeFormat.format(participant.lastActivityTime)));

			if (participant.availability == Availability.OFFLINE) {
				if (participant.lastActivityTime > 0) {
					((ViewFlipper)mFooterView.findViewById(R.id.flipper)).setDisplayedChild(FOOTER_INDEX_LAST_SEEN);

					mLastSeenViewAnimator.showView();
				}
			} else {
				mLastSeenViewAnimator.hideView();
			}
		} else {
			mActionBar.setTitle(mDialog.getTitle());
		}

		mGroupChatUsersCountText.setText(String.valueOf(mDialog.getTotalParticipantsCount()));

		if (mTypingPeople.isEmpty()) {
			mTypingViewAnimator.hideView();
		} else {
			// Someone is typing.
			((ViewFlipper)mFooterView.findViewById(R.id.flipper)).setDisplayedChild(FOOTER_INDEX_TYPING);

			Context context = getActivity();

			int formatId = mTypingPeople.size() == 1 ? R.string.chat_activity_for_one_format : R.string.chat_activity_for_multiply_format;

			String typingPeopleString = StringUtilities.join(", ", Iterables.toArray(Iterables.transform(mTypingPeople.values(), new Function<Profile, String>() {
				@Override
				public String apply(Profile arg) {
					return arg.getFullname();
				}
			}), String.class));

			String text = context.getString(formatId, typingPeopleString);

			mTypingView.setText(text);

			mTypingViewAnimator.showView();
		}
	}

	private final TableLoaderAdapter.EmptyCallbacks<ChatMessage> mScrollToBottomOnce = new TableLoaderAdapter.EmptyCallbacks<ChatMessage>() {
		@Override
		public void onLoadComplete(Exception error, ObjectSubset<ChatMessage> data) {
			mChatMessagesBackwardLoaderAdapter.unregisterCallbacks(this);

			scrollToBottomAndUpdateLastSeen();
		}
	};

	private final View.OnClickListener mOnActionBarRightButtonClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mDialog.isConference()) {
				startActivityForResult(GroupChatActivity.getDisplayIntent(getActivity(), mDialog.getCid()), REQUEST_CODE_VIEW_OR_EDIT_DIALOG);
			} else {
				Profile user = mDialog.getParticipant();

				startActivity(UserActivity.getDisplayIntent(ChatActivity.this, user));
			}
		}
	};

	private final ChatSender.DispatchCallbacks mChatSenderCallbacks = new ChatSender.DispatchCallbacks() {
		@Override
		public void onSuccess(final ChatSender sender, final ChatMessage local, final long mid) {
			runOnUiThreadSafely(new Runnable() {
				@Override
				public void run() {
					TableChange<ChatListItem> change = mChatListModel.removeById(local.id);

					if (change != null) {
						ChatListItem removed = change.getValue();

						removed.message.id = mid;

						mChatListModel.put(removed);
						mChatListModel.notifyTableChanged();
					}

					Log.debug(TAG, "message have been sent, content = " + local.content + ", mid = " + mid);
				}
			});
		}

		@Override
		public void onError(final ChatSender sender, final ChatMessage message, final Exception error) {
			runOnUiThreadSafely(new Runnable() {
				@Override
				public void run() {
					mChatListModel.setDispatchStateState(message.id, ChatListItem.DISPATCH_STATE_ERROR);
					mChatListModel.notifyTableChanged();
				}
			});
		}
	};

	private final TableLoaderAdapter.Callbacks<ChatMessage> mRestoreListViewScrollState = new TableLoaderAdapter.EmptyCallbacks<ChatMessage>() {
		private static final long INVALID_ID = Long.MIN_VALUE;

		private long mFirstVisibleChatItemId;
		private int mFirstVisibleChatItemViewTop;

		@Override
		public void onPreLoadComplete(Exception error, ObjectSubset<ChatMessage> data) {
			if (shouldNotTouchViews()) return;

			ListView lv = getListView();

			int firstVisibleItemPosition = lv.getFirstVisiblePosition();

			mFirstVisibleChatItemId = INVALID_ID;
			mFirstVisibleChatItemViewTop = 0;

			Log.trace(TAG, "First visible item position = " + firstVisibleItemPosition);

			try {
				for (int i = 0; i < lv.getChildCount(); i++) {
					int position = firstVisibleItemPosition + i;

					Object item = lv.getItemAtPosition(position);

					if (item instanceof ChatListItem) {
						mFirstVisibleChatItemId = lv.getItemIdAtPosition(position);

						View firstVisibleChatItemView = lv.getChildAt(i);

						if (firstVisibleChatItemView != null) {
							mFirstVisibleChatItemViewTop = firstVisibleChatItemView.getTop();
						}

						Log.trace(TAG, "First visible chat item position = " + position);
						Log.trace(TAG, "First visible chat item id = " + mFirstVisibleChatItemId);
						Log.trace(TAG, "First visible chat item top = " + mFirstVisibleChatItemViewTop);
						Log.trace(TAG, "First visible chat item = " + mChatMessages.getById(mFirstVisibleChatItemId));

						break;
					}
				}
			} catch (Exception e) {
				Log.exception(TAG, com.uva.log.Message.WARNING, "unexpected error during restoring LV state", e);
			}
		}

		@Override
		public void onLoadComplete(Exception error, ObjectSubset<ChatMessage> data) {
			if (shouldNotTouchViews()) return;
			if (mFirstVisibleChatItemId == INVALID_ID) return;
			if (error != null) return;

			ListView lv = getListView();

			for (int position = 0; position < lv.getCount(); position++) {
				if (lv.getItemIdAtPosition(position) == mFirstVisibleChatItemId) {
					int y = Math.min(mFirstVisibleChatItemViewTop, mFirstVisibleChatItemViewTop);

					long id = lv.getItemIdAtPosition(position);

					Log.trace(TAG, String.format("Last visible chat item found, position = %d, value = %s, y offset from list view top = %d", position, mChatMessages.getById(id), y));

					lv.setSelectionFromTop(position, y);

					return;
				}
			}

			Log.trace(TAG, "Last visible item not found");
		}
	};


	private class SyncLocalChatTask extends LongAction<Void, ChatMessage[]> {
		private final ViewAnimationHelper mAnimator;

		public SyncLocalChatTask() {
			super(getActivity());

			mAnimator = new ViewAnimationHelper(mFooterView.findViewById(R.id.progress), Animations.DEFAULT_HIDE_ANIMATION, Animations.DEFAULT_SHOW_ANIMATION);
		}

		@Override
		public void blockUi() {
			mAnimator.showView();
		}

		@Override
		public void unblockUi() {
			mAnimator.hideView();
		}

		@Override
		protected ChatMessage[] doInBackgroundOrThrow(Void params) throws Exception {
			return sRequestExecutor.submit(new Callable<ChatMessage[]>() {
				@Override
				public ChatMessage[] call() throws Exception {
					Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

					ChatMessage[] messages = null;
					try {
						Log.debug(TAG, "Loading fresh chat messages");
						messages = mChatMessagesLoader.loadFreshData();
						Log.debug(TAG, "Loaded messages count = " + messages.length);

						mChatCache.save(Arrays.asList(messages));
					} catch (Exception e) {
						Log.exception(TAG, "Unable to load fresh chat messages", e);
						throw e;
					}

					return messages;
				}
			}).get();
		}

		@Override
		protected void onError(Exception error) {
			mPendingSync = null;
		}

		@Override
		protected void onSuccess(ChatMessage[] messages) {
			mPendingSync = null;

			mChatMessages.putAll(messages);
			mChatMessages.notifyTableChanged();
		}
	}

	private class ChatTableLoader extends CollectionLoaderByOffset<ChatMessage> {
		private boolean mHasMore;

		public ChatTableLoader(Table<ChatMessage> data) {
			super(data);
		}

		@Override
		public ObjectSubset<ChatMessage> loadDataPage(int offset, int limit) throws Exception {
			ArrayList<ChatMessage> chunk = null;

			if (mDialog.isConference()) {
				chunk = mModel.getMessagesFromGroupChat(mDialog.getCid(), offset, limit);
			} else {
				chunk = mModel.getMessagesFromDialog(mDialog.getParticipant().id, offset, limit);
			}

			mChatCache.save(chunk);

			mHasMore = chunk.size() > 0;

			ChatMessage[] asArray = chunk.toArray(new ChatMessage[chunk.size()]);

			return new ObjectSubset<ChatMessage>(asArray, mHasMore);
		}
	}

	private class ChatTableLoaderAdapter extends TableLoaderAdapter<ChatMessage> implements LoaderAdapter.Callbacks<ChatMessage>, OnScrollListener {
		public ChatTableLoaderAdapter(CollectionLoader<ChatMessage> loader) {
			super(getActivity(), loader, mChatMessages, mChatListAdapter);

			setMixinPlacement(PLACEMENT_TOP);

			registerCallbacks(this);
		}

		@Override
		protected View inflatePendingView(ViewGroup parent) {
			return new View(getContext());
		}

		@Override
		protected View inflateErrorView(ViewGroup parent) {
			return new View(getContext());
		}

		@Override
		public void onLoadBegin() {
			mBackwardLoadingIndicator.showView();
		}

		@Override
		public void onPreLoadComplete(Exception error, ObjectSubset<ChatMessage> data) {
			;
		}

		@Override
		public void onLoadComplete(Exception error, ObjectSubset<ChatMessage> data) {
			mBackwardLoadingIndicator.hideView();

			if (error != null) {
				ErrorHandlingUtilities.displayErrorSoftly(getContext(), error);
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (firstVisibleItem == totalItemCount) {
				loadMoreData();
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			;
		}
	}

	public class ChatListAdapter extends TableAdapter<ChatListItem> {
		private final int VIEW_TYPE_INCOMING_MESSAGE_WITHOUT_ATTACHMENTS = 0x0;
		private final int VIEW_TYPE_OUTGOING_MESSAGE_WITHOUT_ATTACHMENTS = 0x1;
		private final int VIEW_TYPE_INCOMING_MESSAGE_WITH_ATTACHMENTS = 0x2;
		private final int VIEW_TYPE_OUTGOING_MESSAGE_WITH_ATTACHMENTS = 0x3;
		private final int VIEW_TYPE_SEPARATOR = 0x4;

		private final Context context = getActivity();

		public ChatListAdapter() {
			super(mChatListModel);
		}

		@Override
		public int getViewTypeCount() {
			return 5;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItem(position).isMessage();
		}

		@Override
		public int getItemViewType(int position) {
			final ChatListItem item = getItem(position);

			if (item.isSeparator()) {
				return VIEW_TYPE_SEPARATOR;
			} else {
				final boolean hasAttachments = item.message.content.hasAttachments();

				if (item.message.isIncoming()) {
					if (hasAttachments) {
						return VIEW_TYPE_INCOMING_MESSAGE_WITH_ATTACHMENTS;
					} else {
						return VIEW_TYPE_INCOMING_MESSAGE_WITHOUT_ATTACHMENTS;
					}
				} else {
					if (hasAttachments) {
						return VIEW_TYPE_OUTGOING_MESSAGE_WITH_ATTACHMENTS;
					} else {
						return VIEW_TYPE_OUTGOING_MESSAGE_WITHOUT_ATTACHMENTS;
					}
				}
			}
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			ChatListItem item = getItem(position);

			if (item.isSeparator()) {
				if (view == null) {
					view = mInflater.inflate(R.layout.item_msg_separator, null);
				}

				ChatListItemSeparatorViewHolder.forView(view).setItem(item);
			} else {
				if (view == null) {
					int resourceId = item.message.isIncoming() ? R.layout.item_msgin : R.layout.item_msgout;

					view = new ChatListItemView(context, mDialog, mListImageLoader, mAudioPlayer, mGoogleStaticMap, mLastSeenTimeFormat, mSessionContext, resourceId, item.message.content.hasAttachments(), item.message.isFromConference(), mListViewScrollDetector) {
						@Override
						public void setChecked(boolean checked) {
							if (isChecked() != checked) {
								super.setChecked(checked);

								updateViews();
							}
						}
					};
				}

				((ChatListItemView)view).setItem(item);
			}

			return view;
		}
	}

	private class DeleteMessagesTask extends LongAction<Collection<Long>, Void> {
		public DeleteMessagesTask() {
			super(getActivity());

			wrapWithProgress(false);
		}

		@Override
		protected Void doInBackgroundOrThrow(Collection<Long> mids) throws Exception {
			mModel.deleteMessages(mids);

			return null;
		}

		@Override
		protected void onComplete(LongActionContext<Collection<Long>, Void> executionResult) {
			if (shouldNotTouchViews()) return;

			if (executionResult.isCompletedSuccessfuly()) {
				Collection<Long> mids = executionResult.input;

				for (Long mid : mids) {
					mChatMessages.removeById(mid);
				}

				mChatMessagesLoader.decreaseOffset(mids.size());

				mChatListModel.resetSelection();

				mChatMessages.notifyTableChanged();

				updateViews();
			} else {
				Log.exception(TAG, "error during RestoreOrDeleteTask execution", executionResult.error);
			}
		}
	}

	private class UpdateLastSeenTask extends LongAction<Void, Pair<Availability, Long>> {
		public UpdateLastSeenTask() {
			super(getActivity());
		}

		@Override
		public void displayError(Throwable error) {
			Log.exception(TAG, "Unable to update participant last activity", error);
		}

		@Override
		protected Pair<Availability, Long> doInBackgroundOrThrow(Void params) throws Exception {
			Params request = new Params("messages.getLastActivity");

			request.put("uid", mDialog.getParticipant().id);

			JSONObject response = mModel.sendRequestViaHttp(request).getJSONObject("response");

			Availability availability = response.getInt("online") == 0 ? Availability.OFFLINE : Availability.ONLINE;

			long lastActivityTime = response.getLong("time") * 1000;

			return Pair.create(availability, lastActivityTime);
		}

		@Override
		protected void onSuccess(Pair<Availability, Long> result) {
			Profile participant = mDialog.getParticipant();
			participant.availability = result.first;
			participant.lastActivityTime = result.second;

			if (shouldNotTouchViews()) return;

			updateViews();
		}
	}

	private static class PhotoAttachment {
		public static Bitmap createThumb(String path, int maxSize) throws Exception {
			try {
				Bitmap nonRectPreview = BitmapUtilities.loadPreview(path, maxSize * 2, maxSize * 2);

				Bitmap rectPreview = BitmapUtilities.centerCrop(nonRectPreview, maxSize, maxSize);

				nonRectPreview.recycle();
				nonRectPreview = null;

				Bitmap preview = BitmapUtilities.roundCorners(rectPreview, rectPreview.getWidth() / 10);

				rectPreview.recycle();
				rectPreview = null;

				return preview;
			} catch (OutOfMemoryError oom) {
				throw new Exception(oom);
			}
		}

		public File file;
		public Bitmap thumb;
	}
}
