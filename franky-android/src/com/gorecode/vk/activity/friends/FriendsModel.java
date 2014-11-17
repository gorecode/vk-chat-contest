package com.gorecode.vk.activity.friends;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.cache.FriendsCache;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UserSearch;
import com.gorecode.vk.data.UsersTable;
import com.gorecode.vk.event.AvailabilityChangedEvent;
import com.gorecode.vk.event.FriendAddedEvent;
import com.gorecode.vk.event.FriendRemovedEvent;
import com.gorecode.vk.event.LoggedInEvent;
import com.gorecode.vk.event.LoggedOutEvent;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.task.LongActionContext;
import com.perm.kate.api.VkUser;
import com.uva.log.Log;
import com.uva.utilities.ObserverCollection;

@Singleton
public class FriendsModel {
	public static interface OnModelChangedListener {
		public void onModelChanged(FriendsModel model);
	}

	public static class SyncResult {
		public Collection<Profile> friends;
		public Set<Long> friendsTop;
	}

	private static final String TAG = FriendsModel.class.getSimpleName();

	private static final String FRIENDS_TOP_FILENAME = "friends_top.bin";

	private final ObserverCollection<OnModelChangedListener> mOnModelChangedListeners = new ObserverCollection<FriendsModel.OnModelChangedListener>();

	private final UsersTable mFriends = new UsersTable();
	private final FriendsCache mFriendsCache;
	private final Set<Long> mFriendsTop = new HashSet<Long>();

	private final VkModel mVk;

	private Future<SyncResult> mLastSync;

	private LongAction<Void, SyncResult> mPendingSync;

	@Inject
	public FriendsModel(VkModel vk, FriendsCache cache) {
		mVk = vk;
		mFriendsCache = cache;

		restoreState();
	}

	public void addOnModelChangedListener(OnModelChangedListener listener) {
		mOnModelChangedListeners.add(listener);
	}

	public void removeOnModelChangedListener(OnModelChangedListener listener) {
		mOnModelChangedListeners.remove(listener);
	}

	public UsersTable getFriendsSet() {
		return mFriends;
	}

	public List<Profile> getFriends() {
		return mFriends.asList();
	}

	public List<Profile> getFriendsFilteredByQuery(String expectedName) {
		while (true) {
			try {
				return UserSearch.filterByQuery(getFriends(), expectedName);
			} catch (ConcurrentModificationException e) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException interruption) {
					return Collections.emptyList();
				}
			}
		}
	}

	public Collection<Profile> getFriendsTop() {
		return Collections2.filter(getFriends(), mIsInFriendsTopPredicate);
	}

	public Future<SyncResult> sync(Context context) {
		if (mPendingSync != null) {
			return mPendingSync.asFuture();
		}

		LongAction<Void, SyncResult> action = new LongAction<Void, SyncResult>(context) {
			@Override
			public void displayError(Throwable error) {
				Log.exception(TAG, "Error synchronizing friends model", error);
			}

			@Override
			protected SyncResult doInBackgroundOrThrow(Void unused) throws Exception {
				Log.debug(TAG, "Synchronizing friends list");

				String code =
						"var friends=API.friends.get({\"fields\":\"%1$s\"});" +
								"var friendsTop=API.friends.get({\"order\":\"hints\",\"count\":\"5\"});" +
								"return {friendsTop:friendsTop,friends:friends};";

				code = String.format(code, VkModel.REQUIRED_USER_FIELDS);

				JSONObject responseJson = mVk.execute(code);

				SyncResult result = new SyncResult();

				result.friends = new ArrayList<Profile>();
				result.friendsTop = new HashSet<Long>();

				JSONArray friendsJson = responseJson.optJSONArray("friends");

				if (friendsJson != null) {
					for (VkUser vkUser : VkModel.parseUsers(friendsJson)) {
						result.friends.add(Profile.fromVkUser(vkUser));
					}
				}

				JSONArray jsonFriendsTop = responseJson.optJSONArray("friendsTop");

				if (jsonFriendsTop != null) {
					for (int i = 0; i < jsonFriendsTop.length(); i++) {
						result.friendsTop.add(jsonFriendsTop.getLong(i));
					}
				}

				mFriendsCache.deleteAll();
				mFriendsCache.save(result.friends);

				saveFriendsTop(result.friendsTop);

				return result;
			}

			@Override
			protected void onComplete(LongActionContext<Void, SyncResult> executionContext) {
				mPendingSync = null;
			}

			@Override
			protected void onSuccess(SyncResult result) {
				mFriends.clear();
				mFriends.putAll(result.friends);
				mFriendsTop.clear();
				mFriendsTop.addAll(result.friendsTop);

				Log.message(TAG, "Friends list synchronized, got " + result.friends.size() + " friends");

				notifyModelChanged();
			}
		};

		action.execute();

		mPendingSync = action;

		mLastSync = action.asFuture();

		return mLastSync;
	}

	public Future<SyncResult> getLastSync() {
		return mLastSync;
	}

	public void addFriend(Profile friend) {
		mFriends.put(friend);
		mFriendsCache.saveAsync(friend);

		notifyModelChanged();
	}

	public void removeFriend(Profile friend) {
		mFriends.removeById(friend.id);
		mFriendsTop.remove(friend.id);
		mFriendsCache.deleteEntity(friend);

		notifyModelChanged();
	}

	@Subscribe
	public void onFriendAdded(FriendAddedEvent event) {
		Log.debug(TAG, "Got friend added event, uid = " + event.getFriend().id);

		addFriend(event.getFriend());
	}

	@Subscribe
	public void onFriendRemoved(FriendRemovedEvent event) {
		Log.debug(TAG, "Got friend removed event, uid = " + event.getFriend().id);

		removeFriend(event.getFriend());
	}

	@Subscribe
	public void onAvailabilityChanged(AvailabilityChangedEvent event) {
		Profile friend = mFriends.getById(event.getUserId());

		if (friend != null) {
			friend.availability = event.getAvailability();

			mFriendsCache.saveAsync(friend);
		}

		notifyModelChanged();
	}

	public void cleanup() {
		mFriendsCache.deleteAll();
		mFriends.clear();
		mFriendsTop.clear();
		saveFriendsTop(mFriendsTop);
	}

	private void restoreState() {
		mFriends.putAll(mFriendsCache.findAll());
		mFriendsTop.addAll(loadFriendsTop());
	}

	private void notifyModelChanged() {
		Log.debug(TAG, "model changed");

		mOnModelChangedListeners.callForEach(new Function<FriendsModel.OnModelChangedListener, Void>() {
			@Override
			public Void apply(OnModelChangedListener observer) {
				observer.onModelChanged(FriendsModel.this);

				return null;
			}
		});
	}

	private synchronized void saveFriendsTop(Collection<Long> friendsTop) {
		try {
			DataOutputStream out = new DataOutputStream(VkApplication.getApplication().openFileOutput(FRIENDS_TOP_FILENAME, Context.MODE_PRIVATE));

			try {
				out.writeInt(friendsTop.size());
				for (Long id : friendsTop) {
					out.writeLong(id);
				}
			} finally {
				out.close();
			}
		} catch (Exception e) {
			Log.exception(TAG, "Error saving friends top", e);
		}
	}

	private synchronized Collection<Long> loadFriendsTop() {
		Collection<Long> friendsTop = new ArrayList<Long>();

		try {
			DataInputStream is = new DataInputStream(VkApplication.getApplication().openFileInput(FRIENDS_TOP_FILENAME));

			try {
				int size = is.readInt();

				for (int i = 0; i < size; i++) {
					friendsTop.add(is.readLong());
				}
			} finally {
				is.close();
			}
		} catch (Exception e) {
			Log.exception(TAG, "Error restoring friends top", e);
		}

		return friendsTop;
	}

	private final Predicate<Profile> mIsInFriendsTopPredicate = new Predicate<Profile>() {
		@Override
		public boolean apply(Profile arg) {
			return mFriendsTop.contains(arg.id);
		}
	};
}
