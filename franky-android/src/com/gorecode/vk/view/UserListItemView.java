package com.gorecode.vk.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gorecode.vk.R;
import com.gorecode.vk.data.Availability;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.ImageLoader;

public class UserListItemView extends FrameLayout {
	private WebImageView mPhotoView;
	private TextView mNameView;
	private View mIsOnlineLayout;

	private Profile mUser;

	public UserListItemView(Context context, int layoutResId) {
		super(context);

		inflate(context, layoutResId, this);

		setUpViews();
	}

	public UserListItemView(Context context) {
		this(context, null);
	}

	public UserListItemView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.user_list_item, this);

		setUpViews();
	}

	public Profile getUser() {
		return mUser;
	}

	public void setUser(Profile user) {
		mUser = user;

		updateViews();
	}

	public void setImageLoader(ImageLoader imageLoader) {
		mPhotoView.setImageLoader(imageLoader);
	}

	private void updateViews() {
		mPhotoView.setImageUrls(mUser.avatarUrls);
		mNameView.setText(mUser.getFullname());
		mIsOnlineLayout.setVisibility(mUser.availability == Availability.ONLINE ? VISIBLE : GONE);
	}

	private void setUpViews() {
		setDrawingCacheEnabled(false);

		mPhotoView = (WebImageView)findViewById(R.id.item_image);
		mNameView = (TextView)findViewById(R.id.item_text);
		mIsOnlineLayout = findViewById(R.id.item_online);
	}
}
