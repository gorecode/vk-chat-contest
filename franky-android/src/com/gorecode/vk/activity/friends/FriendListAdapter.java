package com.gorecode.vk.activity.friends;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import com.gorecode.vk.R;
import com.google.common.base.Strings;
import com.gorecode.vk.adapter.ListAdapter;
import com.gorecode.vk.data.Availability;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UserSearch;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.view.UserListItemView;
import com.gorecode.vk.view.UserSeparatorListItemViewController;
import com.uva.log.Log;

public class FriendListAdapter extends ListAdapter<FriendListItem> implements android.widget.ListAdapter, FriendsModel.OnModelChangedListener {
	private static final String TAG = FriendListAdapter.class.getSimpleName();

	private static final int VIEW_TYPE_USER = 0x0;
	private static final int VIEW_TYPE_SEPARATOR = 0x1;

	private final Context mContext;
	private final FriendsModel mModel;
	private final ImageLoader mImageLoader;
	private final PeopleFilter mFilter;

	private final List<Profile> mAllFriends;
	private List<Profile> mFriends;

	public FriendListAdapter(Context context, ImageLoader imageLoader, FriendsModel model) {
		super(new ArrayList<FriendListItem>());

		mContext = context;
		mImageLoader = imageLoader;
		mModel = model;

		mFilter = new PeopleFilter();

		mAllFriends = new ArrayList<Profile>();

		reset();
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != VIEW_TYPE_SEPARATOR;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		if (getItem(position).isFriend()) {
			return VIEW_TYPE_USER;
		}
		if (getItem(position).isSeparator()) {
			return VIEW_TYPE_SEPARATOR;
		}
		throw new RuntimeException("Unknown view type");
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (getItemViewType(position) == VIEW_TYPE_SEPARATOR) {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.user_list_item_separator, null);
			}

			UserSeparatorListItemViewController controller = UserSeparatorListItemViewController.bind(convertView);

			controller.setText(getItem(position).getSeparatorText());
		}

		if (getItemViewType(position) == VIEW_TYPE_USER) {
			if (convertView == null) {
				UserListItemView newView = new UserListItemView(mContext);

				newView.setImageLoader(mImageLoader);

				convertView = newView;
			}

			((UserListItemView)convertView).setUser(getItem(position).getFriend());
		}

		return convertView;
	}

	@Override
	public void onModelChanged(FriendsModel model) {
		Log.debug(TAG, "Friend model changed, reseting all friends and filter again");

		synchronized (mAllFriends) {
			mAllFriends.clear();
			mAllFriends.addAll(mModel.getFriends());	

			Log.debug(TAG, "total friends count = " + mAllFriends.size());
		}

		filter();
	}

	public void reset() {
		synchronized (mAllFriends) {
			mAllFriends.clear();
			mAllFriends.addAll(mModel.getFriends());
		}

		if (mFilter.onlineOnly) {
			mFriends = new ArrayList<Profile>(mAllFriends.size() / 5);

			for (Profile user : mAllFriends) {
				if (user.availability == Availability.ONLINE) {
					mFriends.add(user);
				}
			}
		} else {
			mFriends = mAllFriends;
		}

		resetItems();
	}

	private void resetItems() {
		List<FriendListItem> items = getList();

		items.clear();

		List<Profile> friends = mFriends;

		if (mFilter.isEmpty() && mFriends.size() > 0) {
			for (Profile importantFriend : mModel.getFriendsTop()) {
				items.add(FriendListItem.newFriend(importantFriend));
			}
		}

		Profile prevFriend = null;

		for (int i = 0; i < friends.size(); i++) {
			Profile friend = friends.get(i);

			if (Strings.isNullOrEmpty(mFilter.query)) {
				boolean separatorNeeded = prevFriend == null;

				if (prevFriend != null){
					String currName = friend.getFullname().toUpperCase();
					String prevName = prevFriend.getFullname().toUpperCase();

					separatorNeeded = prevName.charAt(0) != currName.charAt(0);
				}

				if (separatorNeeded) {
					items.add(FriendListItem.newSeparator(friend));
				}
			}

			items.add(FriendListItem.newFriend(friend));

			prevFriend = friend;
		}

		notifyDataSetChanged();
	}

	public void filterByAvailability(boolean onlineOnly) {
		mFilter.onlineOnly = onlineOnly;

		filter();
	}

	public void filterByQuery(String query) {
		Log.debug(TAG, "filterByQuery(" + query + ")");

		mFilter.query = query;

		filter();
	}

	public void filter() {
		mFilter.filter(mFilter.query);
	}

	private class PeopleFilter extends Filter {
		public volatile boolean onlineOnly;
		public volatile String query;

		public boolean isEmpty() {
			return Strings.isNullOrEmpty(query) && !onlineOnly;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			Log.debug(TAG, "publishing filter results");

			if (results == null) {
				mFriends = mAllFriends;
			} else {
				mFriends = (List<Profile>)results.values;
			}

			resetItems();

			Log.debug(TAG, "results published, you see " + mFriends.size() + " friends");
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			Log.debug(TAG, "performing filtering with " + constraint + " constraint in background");

			String query = "";

			if (!onlineOnly) {
				if (constraint == null) return null;

				query = constraint.toString();

				if (query.length() == 0) return null;
			}

			if (constraint != null) {
				query = constraint.toString();
			}

			List<Profile> friends;

			synchronized (mAllFriends) {
				friends = new ArrayList<Profile>(mAllFriends.size());
				friends.addAll(mAllFriends);
			}

			Log.debug(TAG, "total friends count to filter " + friends.size());

			List<Profile> filteredByQuery = UserSearch.filterByQuery(friends, query);
			List<Profile> filteredByAvailability = filteredByQuery;

			if (onlineOnly) {
				filteredByAvailability = new ArrayList<Profile>(filteredByQuery.size());

				for (Profile user : filteredByQuery) {
					if (user.availability == Availability.ONLINE) {
						filteredByAvailability.add(user);
					}
				}
			}

			Log.debug(TAG, "filtered friends count " + filteredByAvailability.size());

			FilterResults results = new FilterResults();

			results.values = filteredByAvailability;
			results.count = filteredByAvailability.size();

			Log.debug(TAG, "filtering in background completed");

			return results;
		}
	}
}
