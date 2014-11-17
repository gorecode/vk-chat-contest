package com.gorecode.vk.activity;

import java.util.ArrayList;
import java.util.List;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.gorecode.vk.activity.chat.ChatActivity;
import com.gorecode.vk.activity.friends.FriendsModel;
import com.gorecode.vk.activity.search.FriendSuggestionsModel;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.ImageUrls;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.event.FriendAddedEvent;
import com.gorecode.vk.event.FriendRemovedEvent;
import com.gorecode.vk.event.FriendshipRejectedEvent;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.BundleUtilities;
import com.gorecode.vk.utilities.Patterns;
import com.gorecode.vk.view.VkActionBar;
import com.gorecode.vk.view.WebImageView;
import com.uva.log.Log;

@ContentView(R.layout.user_activity)
public class UserActivity extends VkActivity {
	private static final String TAG = UserActivity.class.getSimpleName();

	public static final int USER_CATEGORY_STRANGER = 0x0;
	public static final int USER_CATEGORY_OFFER = 0x1;
	public static final int USER_CATEGORY_FRIEND = 0x2;
	public static final int USER_CATEGORY_ALMOST_FRIEND = 0x4;
	public static final int USER_CATEGORY_SUBSCRIBER = USER_CATEGORY_STRANGER;

	private static final String EXTRA_USER_CATEGORY = "userCategory";

	@InjectView(R.id.actionBar)
	private VkActionBar mActionBar;
	@InjectView(R.id.photo)
	private WebImageView mPhotoView;
	@InjectView(R.id.addToFriendsButton)
	private Button mAddToFriendsButton;
	@InjectView(R.id.rejectOfferButton)
	private TextView mRejectOfferButton;
	@InjectView(R.id.sendMessageButton)
	private Button mSendMessageButton;
	@InjectView(R.id.callButton)
	private Button mCallButton;
	@InjectView(R.id.removeFromFriendsButton)
	private TextView mRemoveFromFriendsButton;

	private final List<View> mButtons = new ArrayList<View>();

	@Inject
	private VkModel mVk;
	@Inject
	private EventBus mBus;
	@Inject
	private FriendsModel mFriendsModel;
	@Inject
	private FriendSuggestionsModel mSuggestionsModel;

	private Profile mUser;

	private int mUserCategory;

	public static Intent getDisplayIntent(Context context, Profile user) {
		Intent intent = new Intent(context, UserActivity.class);
		BundleUtilities.putExtra(intent, EXTRA_PERSON, user);
		return intent;		
	}

	public static Intent getDisplayIntent(Context context, Profile user, int userCategory) {
		Intent intent = new Intent(context, UserActivity.class);
		intent.putExtra(EXTRA_USER_CATEGORY, userCategory);
		BundleUtilities.putExtra(intent, EXTRA_PERSON, user);
		return intent;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_RIGHT);

		unpackExtras();

		setUpViews();

		updateViews();
	}

	@InjectOnClickListener(R.id.photo)
	public void onPhotoClicked(View v) {
		ImageUrls imageLink = mUser.avatarUrls;

		if (imageLink != null) {
			ViewImageActivity.displayWebImage(this, imageLink.fullsizeUrl);
		}
	}

	@InjectOnClickListener(R.id.addToFriendsButton)
	public void onAddToFriendsButtonClicked(View v) {
		LongAction<?, ?> action = new LongAction<Void, Void>(this) {
			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				mVk.addToFriends(mUser.id);

				return null;
			}

			@Override
			public void onSuccess(Void unused) {
				if (mUserCategory == USER_CATEGORY_STRANGER) {
					Toast.makeText(getContext(), getString(R.string.toast_friendship_offer_sent), Toast.LENGTH_SHORT).show();

					mUserCategory = USER_CATEGORY_ALMOST_FRIEND;
				} else {
					mBus.post(new FriendAddedEvent(mUser));

					mUserCategory = USER_CATEGORY_FRIEND;
				}

				updateViews();
			}
		};
		action.wrapWithProgress(true);
		action.execute();
	}

	@InjectOnClickListener(R.id.rejectOfferButton)
	public void onRejectOfferButtonClicked(View v) {
		LongAction<?, ?> action = new LongAction<Void, Void>(this) {
			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				mVk.removeFromFriends(mUser.id);

				return null;
			}

			@Override
			public void onSuccess(Void unused) {
				mBus.post(new FriendshipRejectedEvent(mUser));

				mUserCategory = USER_CATEGORY_SUBSCRIBER;

				updateViews();
			}
		};
		action.wrapWithProgress(true);
		action.execute();
	}

	@InjectOnClickListener(R.id.sendMessageButton)
	public void onSendMessageButtonClicked(View v) {
		ChatActivity.display(this, mUser);
	}

	@InjectOnClickListener(R.id.callButton)
	public void onCallButtonClicked(View v) {
		try {
			String number = (String)v.getTag();
			Log.debug(TAG, "calling on phone = " + number);
			Intent intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse("tel:" + number));
			startActivity(intent);
		} catch (Exception e) {
			Log.exception(TAG, "Error starting phone activity", e);
		}
	}

	@InjectOnClickListener(R.id.removeFromFriendsButton)
	public void onRemoveFromFriendsButtonClicked(View v) {
		LongAction<?, ?> action = new LongAction<Void, Void>(this) {
			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				mVk.removeFromFriends(mUser.id);

				return null;
			}

			@Override
			public void onSuccess(Void unused) {
				mBus.post(new FriendRemovedEvent(mUser));

				mUserCategory = USER_CATEGORY_SUBSCRIBER;

				updateViews();
			}
		};
		action.wrapWithProgress(true);
		action.execute();
	}

	private void updateViews() {
		mActionBar.setTitle(mUser.getFullname());

		mPhotoView.setImageUrl(mUser.avatarUrls != null ? mUser.avatarUrls.previewUrl : null);

		if (mUserCategory == USER_CATEGORY_OFFER) {
			hideAllButtonsExcept(mAddToFriendsButton, mRejectOfferButton);
		}
		if (mUserCategory == USER_CATEGORY_FRIEND) {
			hideAllButtonsExcept(mRemoveFromFriendsButton, mSendMessageButton);

			showCallButtons();
		}
		if (mUserCategory == USER_CATEGORY_ALMOST_FRIEND) {
			hideAllButtonsExcept(mSendMessageButton);

			showCallButtons();
		}
		if (mUserCategory == USER_CATEGORY_STRANGER) {
			hideAllButtonsExcept(mAddToFriendsButton, mSendMessageButton);
		}
	}

	private void showCallButtons() {
		String home = mUser.homePhone;
		String mobile = mUser.mobilePhone;

		if (!Strings.isNullOrEmpty(home) && Patterns.PHONE.matcher(home).matches()) {
			mCallButton.setTag(home);
			mCallButton.setVisibility(View.VISIBLE);
			mCallButton.setText(String.format(getString(R.string.call_button_text_format), home));
		}

		if (!Strings.isNullOrEmpty(mobile) && Patterns.PHONE.matcher(mobile).matches()) {
			mCallButton.setTag(mobile);
			mCallButton.setVisibility(View.VISIBLE);
			mCallButton.setText(String.format(getString(R.string.call_button_text_format), mobile));
		}
	}

	private void hideAllButtonsExcept(View... visibleButtons) {
		for (View button : mButtons) {
			boolean hide = true;

			for (View buttonToShow : visibleButtons) {
				if (button == buttonToShow) {
					hide = false;
					break;
				}
			}

			button.setVisibility(hide ? View.GONE : View.VISIBLE);
		}
	}

	private void unpackExtras() {
		Bundle bundle = getIntent().getExtras();

		mUser = BundleUtilities.getProfile(bundle, EXTRA_PERSON);

		Log.debug(TAG, "home = " + mUser.homePhone);
		Log.debug(TAG, "mobile = " + mUser.mobilePhone);

		if (bundle.containsKey(EXTRA_USER_CATEGORY)) {
			mUserCategory = bundle.getInt(EXTRA_USER_CATEGORY);
		} else {
			mUserCategory = USER_CATEGORY_STRANGER;

			if (mFriendsModel.getFriendsSet().containsKey(mUser.id)) {
				mUserCategory = USER_CATEGORY_FRIEND;
			}
		}
	}

	private void setUpViews() {
		mButtons.add(mAddToFriendsButton);
		mButtons.add(mRejectOfferButton);
		mButtons.add(mSendMessageButton);
		mButtons.add(mCallButton);
		mButtons.add(mRemoveFromFriendsButton);

		Aibolit.doInjections(this);
	}
}
