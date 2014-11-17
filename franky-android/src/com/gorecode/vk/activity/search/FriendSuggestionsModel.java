package com.gorecode.vk.activity.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.cache.OffersCache;
import com.gorecode.vk.cache.SQLiteSelectionCommand;
import com.gorecode.vk.cache.SuggestionsCache;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UsersTable;
import com.gorecode.vk.event.AllFriendshipOffersRejectedEvent;
import com.gorecode.vk.event.FriendAddedEvent;
import com.gorecode.vk.event.FriendshipOfferedEvent;
import com.gorecode.vk.event.FriendshipRejectedEvent;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.task.LongActionContext;
import com.uva.log.Log;
import com.uva.utilities.ObserverCollection;

@Singleton
public class FriendSuggestionsModel {
	public static interface OnModelChangedListener {
		public void onModelChanged(FriendSuggestionsModel model);
	}

	public static class SyncResult {
		public Collection<Profile> offers;
		public Collection<Profile> suggestions;
	}

	private static final boolean DEBUG = false;

	private static final String TAG = FriendSuggestionsModel.class.getSimpleName();

	private static final String PREFERENCE_PHONEBOOK_UPLOAD_TIME = "phonebookUploadTime";

	private static final long PHONEBOOK_UPLOAD_INTERVAL = DEBUG ? 30000 : 7 * 24 * 60 * 60 * 1000;

	private final Context mContext;

	private final VkModel mVk;
	private final OffersCache mOffersCache;
	private final SuggestionsCache mSuggestionsCache;

	private final UsersTable mOffers;
	private final UsersTable mSuggestions;

	private final SharedPreferences mPreferences;

	private final ObserverCollection<OnModelChangedListener> mOnModelChangedListeners = new ObserverCollection<FriendSuggestionsModel.OnModelChangedListener>();

	private LongAction<Void, SyncResult> mPendingSync;

	@Inject
	public FriendSuggestionsModel(Context context, VkModel vk, OffersCache offersCache, SuggestionsCache suggestionsCache) {
		mContext = context;
		mPreferences = mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE);
		mVk = vk;
		mOffersCache = offersCache;
		mSuggestionsCache = suggestionsCache;
		mOffers = new UsersTable();
		mSuggestions = new UsersTable();
		mOffers.putAll(mOffersCache.findAll());
		mSuggestions.putAll(mSuggestionsCache.findAll());
	}

	public void addOnModelChangedListener(OnModelChangedListener listener) {
		mOnModelChangedListeners.add(listener);
	}

	public void removeOnModelChangedListener(OnModelChangedListener listener) {
		mOnModelChangedListeners.remove(listener);
	}

	public List<Profile> getOffers() {
		return mOffers.asList();
	}

	public List<Profile> getSuggestions() {
		return mSuggestions.asList();
	}

	public Future<SyncResult> sync(Context context) {
		Log.debug(TAG, "synchronizing friends offers and suggestions");

		if (mPendingSync != null) {
			Log.debug(TAG, "sync action is pending, return existing action");

			return mPendingSync.asFuture();
		}

		final boolean shouldUploadPhonebook = System.currentTimeMillis() - getLastPhonebookUploadTime() > PHONEBOOK_UPLOAD_INTERVAL;

		mPendingSync = new LongAction<Void, SyncResult>(context) {
			@Override
			public void displayError(Throwable error) {
				Log.exception(TAG, "Error synchronizing offers and suggestions", error);
			}

			@Override
			protected SyncResult doInBackgroundOrThrow(Void params) throws Exception {
				if (shouldUploadPhonebook) {
					Log.debug(TAG, "uploading phonebook to vk server");

					ArrayList<String> contacts = new ArrayList<String>();

					try {
						contacts.addAll(selectEmailFromPhonebook());
					} catch (Exception e) {
						Log.exception(TAG, "error selecting emails from phonebook", e);
					}

					try {
						contacts.addAll(selectPhonesFromPhonebook());
					} catch (Exception e) {
						Log.exception(TAG, "error selecting phones from phonebook", e);
					}

					try {
						/// XXX: VK restriction.
						while (contacts.size() > 1000) {
							contacts.remove(contacts.size() - 1);
						}

						if (contacts.size() > 0) {
							mVk.importContacts(contacts);

							Log.debug(TAG, "phonebook is uploaded");

							setLastPhonebookUploadTime(System.currentTimeMillis());
						} else {
							Log.debug(TAG, "phonebook is empty, nothing to upload");
						}
					} catch (Exception e) {
						Log.exception(TAG, "error uploading phonebook", e);
					}
				}

				SyncResult syncResult = new SyncResult();

				syncResult.offers = Collections.emptyList();
				syncResult.suggestions = Collections.emptyList();

				String code =
						"var offers_uid = API.friends.getRequests({need_messages:1,count:1000});\n" + 
								"var offers = API.users.get({uids:offers_uid@.uid, fields:\"%1$s\"});\n" +
								"var suggestions_uid = API.friends.getSuggestions({});\n" +
								"var suggestions = API.users.get({uids:suggestions_uid@.uid, fields:\"%1$s\"});\n" +
								"return {offers:offers, suggestions:suggestions};\n";

				code = String.format(code, VkModel.REQUIRED_USER_FIELDS);

				JSONObject responseJson = mVk.execute(code);

				JSONArray offersJson = responseJson.optJSONArray("offers");

				if (offersJson != null) {
					syncResult.offers = Profile.fromVkUsers(VkModel.parseUsers(offersJson));
				}

				JSONArray suggestionsJson = responseJson.optJSONArray("suggestions");

				if (suggestionsJson != null) {
					syncResult.suggestions = Profile.fromVkUsers(VkModel.parseUsers(suggestionsJson));
				}

				if (!isCancelled()) {
					mOffersCache.deleteAll();
					mOffersCache.saveAsync(syncResult.offers);

					mSuggestionsCache.deleteAll();
					mSuggestionsCache.saveAsync(syncResult.suggestions);
				}

				return syncResult;
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				mPendingSync = null;

				return super.cancel(mayInterruptIfRunning);
			}

			@Override
			public void onComplete(LongActionContext<Void, SyncResult> result) {
				mPendingSync = null;
			}

			@Override
			public void onSuccess(SyncResult syncResult) {
				Log.message(TAG, String.format("friendship offers and suggestions are synchronized, got %d offers, %d suggestions", syncResult.offers.size(), syncResult.suggestions.size()));

				mOffers.clear();
				mOffers.putAll(syncResult.offers);

				mSuggestions.clear();
				mSuggestions.putAll(syncResult.suggestions);

				notifyChanged();
			}
		};

		mPendingSync.execute();

		return mPendingSync.asFuture();
	}

	@Subscribe
	public void onAllOffersRejected(AllFriendshipOffersRejectedEvent event) {
		mOffersCache.deleteAll();
		mOffers.clear();

		notifyChanged();
	}

	@Subscribe
	public void onNewOffer(FriendshipOfferedEvent event) {
		Profile user = event.getSender();

		mOffersCache.saveAsync(user);
		mOffers.put(user);

		notifyChanged();
	}

	@Subscribe
	public void onNewFriend(FriendAddedEvent event) {
		removeOffer(event.getFriend());
	}

	@Subscribe
	public void onOfferRejected(FriendshipRejectedEvent event) {
		removeOffer(event.getSender());
	}

	public void removeOffer(Profile offer) {
		removeOffer(offer.id);
	}

	public void removeOffer(long uid) {
		mOffersCache.deleteOne(String.valueOf(uid));
		mOffers.removeById(uid);

		notifyChanged();
	}

	public void cleanup() {
		mPreferences.edit().clear().commit();
		mOffers.clear();
		mOffersCache.deleteAll();
		mSuggestions.clear();
		mSuggestionsCache.deleteAll();
	}

	private long getLastPhonebookUploadTime() {
		return mPreferences.getLong(PREFERENCE_PHONEBOOK_UPLOAD_TIME, 0);
	}

	private void setLastPhonebookUploadTime(long ts) {
		mPreferences.edit().putLong(PREFERENCE_PHONEBOOK_UPLOAD_TIME, ts).commit();
	}

	private Collection<String> selectEmailFromPhonebook() throws Exception {
		ArrayList<String> emails = new ArrayList<String>();

		Cursor cursor = new SQLiteSelectionCommand().select(Email.DATA).from(Email.CONTENT_URI).execute(mContext.getContentResolver());

		try {
			while (cursor.moveToNext()) {
				emails.add(cursor.getString(cursor.getColumnIndexOrThrow(Email.DATA)));
			}
		} finally {
			cursor.close();
		}

		return emails;		
	}

	private Collection<String> selectPhonesFromPhonebook() throws Exception {
		ArrayList<String> phones = new ArrayList<String>();

		Cursor cursor = new SQLiteSelectionCommand().select(Phone.NUMBER).from(Phone.CONTENT_URI).execute(mContext.getContentResolver());

		try {
			while (cursor.moveToNext()) {
				phones.add(cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)));
			}
		} finally {
			cursor.close();
		}

		return phones;
	}

	private void notifyChanged() {
		for (OnModelChangedListener listener : mOnModelChangedListeners) {
			listener.onModelChanged(this);
		}
	}
}
