package com.gorecode.vk.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.UserListItemView;

public class UserListAdapter extends com.gorecode.vk.adapter.ListAdapter<Profile> {
	private final Context mContext;
	private final ImageLoader mImageLoader;
	
	public UserListAdapter(Context context, ImageLoader imageLoader, List<Profile> users) {
		super(users);

		mImageLoader = imageLoader;
		mContext = context;
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).id;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = new UserListItemView(mContext);

			((UserListItemView)convertView).setImageLoader(mImageLoader);
		}

		((UserListItemView)convertView).setUser(getItem(position));

		return convertView;
	}
}