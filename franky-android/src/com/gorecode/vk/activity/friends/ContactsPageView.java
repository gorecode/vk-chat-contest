package com.gorecode.vk.activity.friends;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import roboguice.RoboGuice;
import roboguice.inject.RoboInjector;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.gorecode.vk.activity.InviteUserActivity;
import com.gorecode.vk.activity.UserActivity;
import com.gorecode.vk.adapter.EmptyViewVisibilityController;
import com.gorecode.vk.adapter.ListAdapter;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.cache.SQLiteSelectionCommand;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.data.UserSearch;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.imageloader.ListImageLoader;
import com.gorecode.vk.phonesync.SyncManager;
import com.gorecode.vk.phonesync.SyncManager.Callbacks;
import com.gorecode.vk.task.LongAction;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.Toasts;
import com.gorecode.vk.view.LoaderLayout;
import com.gorecode.vk.view.SearchView;
import com.gorecode.vk.view.UserSeparatorListItemViewController;
import com.gorecode.vk.view.WebImageView;
import com.uva.lang.StringUtilities;
import com.uva.log.Log;

public class ContactsPageView extends FrameLayout implements OnItemClickListener, SearchView.OnQueryTextListener {
	private static final String TAG = ContactsPageView.class.getSimpleName();

	private static final String PREF_SYNC_DONE = "syncDone";

	private ViewGroup mContactSyncLayout;
	private LoaderLayout mContactListLayout;

	@Inject
	private VkModel mVk;
	@Inject
	private ListImageLoader mImageLoader;

	private ListView mContactListView;
	private SearchView mContactListSearchView;
	private ContactListAdapter mContactListAdapter;

	private ContactListGet mPendingContactListUpdate;

	private TextView mSyncContactsStateTitleView;
	private TextView mSyncContactsProgressTextView;

	private volatile boolean mIsDetachedFromWindow;

	private SharedPreferences mPrefs;

	public ContactsPageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		inflate(context, R.layout.contacts_fragment, this);

		if (isInEditMode()) return;
	}

	public void init() {
		RoboInjector injector = RoboGuice.getInjector(getContext());

		injector.injectMembersWithoutViews(this);

		mPrefs = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE);

		mImageLoader.registerFetcher(new PhotosFetcher(getContentResolver()));

		setUpViews();

		final boolean isSyncDone = mPrefs.getBoolean(PREF_SYNC_DONE, false);

		mContactListLayout.setVisibility(isSyncDone ? VISIBLE : GONE);
		mContactSyncLayout.setVisibility(isSyncDone ? GONE : VISIBLE);

		if (isSyncDone) {
			updateContactListAsync();	
		}
	}

	public void destroy() {
		mImageLoader.stop();

		abortContactListUpdate();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		mIsDetachedFromWindow = true;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Object item = mContactListView.getAdapter().getItem(position);

		if (item instanceof ContactListItem) {
			Contact contact = ((ContactListItem)item).contact;

			Intent intent = null;

			if (contact.vkUser != null) {
				intent = UserActivity.getDisplayIntent(getContext(), contact.vkUser, UserActivity.USER_CATEGORY_ALMOST_FRIEND);
			} else {
				intent = InviteUserActivity.getDisplayIntent(getContext(), contact.fullName, contact.phones.get(0), contact.photoId);
			}

			getContext().startActivity(intent);
		}
	}

	@Override
	public boolean onQueryTextChange(String query) {
		mContactListAdapter.getFilter().filter(query);

		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		mContactListAdapter.getFilter().filter(query);

		return false;
	}

	protected void onInviteFriendButtonClicked(View v) {
		// FIXME: Copy-paste from InviteUserActivity.
		try {
			String smsText = String.format(getContext().getString(R.string.sms_invitation_text_format), getContext().getString(R.string.application_name));
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
			intent.putExtra("sms_body", smsText); 
			((Activity)getContext()).startActivity(intent);
		} catch (Exception e) {
			Log.exception(TAG, "error performing send sms action", e);
		}
	}

	protected void onRetryContactsGetButtonClick(View v) {
		abortContactListUpdate();

		updateContactListAsync();
	}

	@InjectOnClickListener(R.id.sync_contacts_button)
	protected void onSyncButtonClick(View view) {
		final SyncManager.Callbacks progressPublisher = new SyncManager.Callbacks() {
			@Override
			public void onStateChanged(final int state) {
				if (mIsDetachedFromWindow) return;

				post(new Runnable() {
					@Override
					public void run() {
						if (mIsDetachedFromWindow) return;

						if (state == Callbacks.STATE_GET_FRIENDS) {
							mSyncContactsStateTitleView.setText(R.string.sync_state_get_friends);
						}
						if (state == Callbacks.STATE_SYNC_CONTACTS) {
							mSyncContactsStateTitleView.setText(R.string.sync_state_update_contacts);
						}
						if (state == Callbacks.STATE_SYNC_PHOTOS) {
							mSyncContactsStateTitleView.setText(R.string.sync_state_update_photos);
						}
					}
				});
			}
			
			@Override
			public void onProgress(final int current, final int total) {
				if (mIsDetachedFromWindow) return;

				post(new Runnable() {
					@Override
					public void run() {
						if (mIsDetachedFromWindow) return;

						mSyncContactsProgressTextView.setVisibility(View.VISIBLE);
						mSyncContactsProgressTextView.setText(getContext().getString(R.string.sync_progress, current, total));
					}
				});
			}
		};

		LongAction<Void, Void> action = new LongAction<Void, Void>(getContext()) {
			@Override
			public void blockUi() {
				mSyncContactsProgressTextView.setVisibility(View.INVISIBLE);

				mSyncContactsStateTitleView.setText(R.string.sync_state_get_friends);

				mContactSyncLayout.findViewById(R.id.contact_sync_offer_layout).setVisibility(View.GONE);
				mContactSyncLayout.findViewById(R.id.contact_sync_progress_layout).setVisibility(View.VISIBLE);
			}

			@Override
			public void unblockUi() {
				mContactSyncLayout.findViewById(R.id.contact_sync_progress_layout).setVisibility(View.GONE);				
			}

			@Override
			protected Void doInBackgroundOrThrow(Void params) throws Exception {
				SyncManager syncManager = RoboGuice.getInjector(getContext()).getInstance(SyncManager.class);

				try {
					syncManager.syncContacts(progressPublisher);
				} catch (Exception e) {
					Log.exception(TAG, "error synchronizing contacts", e);
					throw e;
				}

				return null;
			}

			@Override
			public void onError(Exception e) {
				ErrorHandlingUtilities.displayErrorSoftly(getContext(), e);

				mContactSyncLayout.findViewById(R.id.contact_sync_offer_layout).setVisibility(View.VISIBLE);
			}

			@Override
			public void onSuccess(Void unused) {
				mPrefs.edit().putBoolean(PREF_SYNC_DONE, true).commit();

				Toasts.makeText(getContext(), R.string.toast_contacts_sync_complete).show();

				mContactListLayout.setVisibility(VISIBLE);
				mContactSyncLayout.setVisibility(GONE);

				updateContactListAsync();
			}
		};
		action.execute();
	}

	private void abortContactListUpdate() {
		if (mPendingContactListUpdate != null) {
			mPendingContactListUpdate.abort();
			mPendingContactListUpdate = null;
		}
	}

	private void updateContactListAsync() {
		mPendingContactListUpdate = new ContactListGet();
		mPendingContactListUpdate.wrapWithSpinner(mContactListLayout);
		mPendingContactListUpdate.execute();
	}

	private List<Contact> selectContacts() throws Exception {
		String[] projection = new String[] { Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME, Contacts.PHOTO_ID, ContactsContract.Contacts.HAS_PHONE_NUMBER  };

		String selection = Contacts.HAS_PHONE_NUMBER + "=?";

		Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI, projection, selection, new String[] { "1" } , null);

		Map<String, Contact> contacts = new HashMap<String, Contact>();

		final int lookupColumn = cursor.getColumnIndexOrThrow(Contacts.LOOKUP_KEY);
		final int nameColumn = cursor.getColumnIndexOrThrow(Contacts.DISPLAY_NAME);
		final int photoColumn = cursor.getColumnIndexOrThrow(Contacts.PHOTO_ID);

		List<String> lookups = new ArrayList<String>();

		while (cursor.moveToNext()) {
			if (cursor.getInt(cursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER)) == 0) {
				continue;
			}

			Contact contact = new Contact();

			contact.lookupKey = cursor.getString(lookupColumn);
			contact.fullName = cursor.getString(nameColumn);
			contact.photoId = cursor.isNull(photoColumn) ? null : cursor.getLong(photoColumn);

			contacts.put(contact.lookupKey, contact);

			lookups.add(contact.lookupKey);
		}

		Map<String, List<String>> photosOfContacts = selectPhonesOfContacts(lookups);

		for (String lookupKey : photosOfContacts.keySet()) {
			Contact contact = contacts.get(lookupKey);

			contact.phones.addAll(photosOfContacts.get(lookupKey));
		}

		ArrayList<Contact> results = new ArrayList<Contact>();

		for (String lookupKey : contacts.keySet()) {
			Contact contact = contacts.get(lookupKey);

			if (contact.phones.size() > 0) {
				results.add(contact);
			}
		}

		return results;
	}

	private Map<String, List<String>> selectPhonesOfContacts(List<String> lookups) throws Exception{
		lookups = Lists.transform(lookups, new Function<String, String>() {
			@Override
			public String apply(String arg) {
				return "\'" + arg + "\'";
			}
		});

		Cursor c = new SQLiteSelectionCommand()
		.select(Phone.NUMBER, Contacts.LOOKUP_KEY)
		.from(Phone.CONTENT_URI)
		.where(PhoneLookup.LOOKUP_KEY + " IN(" + StringUtilities.join(", ", lookups) + ")")
		.execute(getContentResolver());

		try {
			final HashMap<String, List<String>> phonesOfContacts = new HashMap<String, List<String>>();

			while (c.moveToNext()) {
				final String lookupKey = c.getString(1);
				final String phone = c.getString(0);

				List<String> phones = phonesOfContacts.get(lookupKey);

				if (phones == null) {
					phones = new ArrayList<String>();
					phonesOfContacts.put(lookupKey, phones);
				}
				phones.add(phone);
			}

			return phonesOfContacts;
		} finally {
			c.close();
		}
	}

	private ContentResolver getContentResolver() {
		return getContext().getContentResolver();
	}

	private void setUpViews() {
		View contactListHeaderView = inflate(getContext(), R.layout.contact_list_header, null);

		View inviteFriendHeaderView = inflate(getContext(), R.layout.header_invite_friend_button, null);

		inviteFriendHeaderView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onInviteFriendButtonClicked(v);
			}
		});

		mContactSyncLayout = (ViewGroup)findViewById(R.id.contact_sync_layout);

		mSyncContactsStateTitleView = (TextView)mContactSyncLayout.findViewById(R.id.state_text);
		mSyncContactsProgressTextView = (TextView)mContactSyncLayout.findViewById(R.id.progress_text);

		mContactListLayout = (LoaderLayout)findViewById(R.id.contact_list_layout);

		mContactListSearchView = (SearchView)contactListHeaderView.findViewById(R.id.searchView);
		mContactListSearchView.setOnQueryTextListener(this);

		mContactListView = (ListView)findViewById(android.R.id.list);
		mContactListView.setDrawingCacheEnabled(false);
		mContactListView.addHeaderView(contactListHeaderView);
		mContactListView.addHeaderView(inviteFriendHeaderView);
		mContactListView.setOnScrollListener(mImageLoader);
		mContactListView.setOnItemClickListener(this);

		findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onRetryContactsGetButtonClick(v);
			}
		});

		Aibolit.doInjections(this);
	}

	private static class PhotosFetcher implements ImageLoader.ImageFetcher {
		private static final String URI_PREFIX = "contacts://photos/";

		private final ContentResolver mResolver;

		public static String makeUri(long photoId) {
			return URI_PREFIX + photoId;
		}

		public PhotosFetcher(ContentResolver resolver) {
			mResolver = resolver;
		}

		@Override
		public boolean canFetch(String url) {
			return url.startsWith(URI_PREFIX);
		}

		@Override
		public void fetchImage(String url, File outFile) throws Exception {
			long photoId = Long.parseLong(url.substring(URI_PREFIX.length()));

			Cursor c = new SQLiteSelectionCommand().select(Photo.PHOTO).from(Data.CONTENT_URI).where(Data._ID + "=?").withValues(photoId).execute(mResolver);

			if (!c.moveToFirst()) throw new Exception("No photo with id = " + photoId + " found");

			byte[] blob = c.getBlob(0);

			FileOutputStream fos = new FileOutputStream(outFile);

			try {
				fos.write(blob);
				fos.flush();
			} finally {
				fos.close();
			}
		}
	}

	private static class ContactListItem {
		public final Contact contact;
		public final String separatorText;

		public boolean isContact() {
			return contact != null;
		}

		public boolean isSeparator() {
			return separatorText != null;
		}

		public ContactListItem(Contact contact) {
			this.contact = contact;
			this.separatorText = null;
		}

		public ContactListItem(String separatorText) {
			this.contact = null;
			this.separatorText = separatorText.substring(0, 1);
		}
	}

	private class ContactListAdapter extends ListAdapter<ContactListItem> implements Filterable {
		private final int VIEW_TYPE_CONTACT = 0x0;
		private final int VIEW_TYPE_SEPARATOR = 0x1;

		private final List<Contact> mAllContacts;

		private final Context mContext;

		public ContactListAdapter(Context context, List<Contact> contacts) {
			super(new ArrayList<ContactListItem>());

			mContext = context;

			mAllContacts = contacts;

			addAll(convertContactsIntoItems(mAllContacts));
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			ContactListItem item = getItem(position);

			if (item.isContact()) {
				return VIEW_TYPE_CONTACT;
			}

			return VIEW_TYPE_SEPARATOR;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			return getItem(position).isContact();
		}

		@Override
		public long getItemId(int position) {
			ContactListItem item = getItem(position);

			if (item.isSeparator()) {
				return IGNORE_ITEM_VIEW_TYPE;
			}

			return getItem(position).contact.lookupKey.hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ContactListItem item = getItem(position);

			if (item.isContact()) {
				if (convertView == null) {
					convertView = inflate(mContext, R.layout.item_contact_list, null);

					((WebImageView)convertView.findViewById(R.id.item_image)).setImageLoader(mImageLoader);
				}

				ContactListItemViewHolder.bind(convertView).setContact(getItem(position).contact);
			}

			if (item.isSeparator()) {
				if (convertView == null) {
					convertView = LayoutInflater.from(mContext).inflate(R.layout.user_list_item_separator, null);
				}

				UserSeparatorListItemViewController controller = UserSeparatorListItemViewController.bind(convertView);

				controller.setText(getItem(position).separatorText);
			}

			return convertView;
		}

		private Collection<ContactListItem> convertContactsIntoItems(Collection<Contact> contacts) {
			List<ContactListItem> items = new ArrayList<ContactListItem>(contacts.size());

			items.clear();

			Contact prev = null;

			for (Contact current : contacts) {
				boolean separatorNeeded = prev == null;

				String currName = current.fullName.toUpperCase();

				if (prev != null) {
					String prevName = prev.fullName.toUpperCase();

					separatorNeeded = prevName.charAt(0) != currName.charAt(0);
				}

				if (separatorNeeded) {
					items.add(new ContactListItem(currName));
				}

				items.add(new ContactListItem(current));

				prev = current;
			}

			return items;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					if (results == null) return;

					getList().clear();
					getList().addAll((Collection<ContactListItem>)results.values);

					notifyDataSetChanged();
				}

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					String query = null;

					if (constraint != null) {
						query = constraint.toString();
					}

					Collection<ContactListItem> items = convertContactsIntoItems(UserSearch.filterByQuery(mAllContacts, query, FUNCTION_GET_CONTACT_NAME));

					FilterResults results = new FilterResults();
					results.values = items;
					results.count = items.size();
					return results;
				}
			};
		}
	}

	private static final Function<Contact, String> FUNCTION_GET_CONTACT_NAME = new Function<ContactsPageView.Contact, String>() {
		@Override
		public String apply(Contact contact) {
			return contact.fullName;
		}
	};

	private static class ContactListItemViewHolder {
		private final WebImageView mContactPhotoView;
		private final TextView mContactNameView;
		private final TextView mUserNameView;

		private Contact mContact;

		public static ContactListItemViewHolder bind(View view) {
			ContactListItemViewHolder holder = (ContactListItemViewHolder)view.getTag();

			if (holder == null) {
				holder = new ContactListItemViewHolder(view);

				view.setTag(holder);
			}

			return holder;
		}

		public ContactListItemViewHolder(View view) {
			mContactPhotoView = (WebImageView)view.findViewById(R.id.item_image);
			mContactNameView = (TextView)view.findViewById(R.id.item_text);
			mUserNameView = (TextView)view.findViewById(R.id.item_subtext);
		}

		public void setContact(Contact contact) {
			mContact = contact;

			updateViews();
		}

		private void updateViews() {
			mContactPhotoView.setImageUrl(mContact.photoId == null ? null : PhotosFetcher.makeUri(mContact.photoId));
			mContactNameView.setText(mContact.fullName);
			mUserNameView.setVisibility(mContact.vkUser != null ? VISIBLE : GONE);
			if (mContact.vkUser != null) {
				mUserNameView.setText(mContact.vkUser.getFullname());
			}
		}
	}

	private class ContactListGet extends LongAction<Void, List<Contact>> {
		public ContactListGet() {
			super(ContactsPageView.this.getContext());
		}

		@Override
		protected List<Contact> doInBackgroundOrThrow(Void params) throws Exception {
			List<Contact> contacts = selectContacts();

			List<String> phonesOfContacts = new ArrayList<String>();

			for (Contact contact : contacts) {
				phonesOfContacts.addAll(contact.phones);
			}

			Collection<Pair<String, Profile>> entries = mVk.getFriendsByPhones(phonesOfContacts);

			for (Pair<String, Profile> entry : entries) {
				for (Contact contact : contacts) {
					if (contact.phones.contains(entry.first)) {
						contact.vkUser = entry.second;
						break;
					}
				}				
			}

			Collections.sort(contacts, new java.util.Comparator<Contact>() {
				@Override
				public int compare(Contact c1, Contact c2) {
					return c1.fullName.compareTo(c2.fullName);
				}
			});

			return contacts;
		}

		@Override
		public void onSuccess(List<Contact> contacts) {
			mContactListAdapter = new ContactListAdapter(getContext(), contacts);

			EmptyViewVisibilityController.bind(mContactListAdapter, findViewById(android.R.id.empty));

			mContactListView.setAdapter(mContactListAdapter);
		}
	}

	private static class Contact {
		public String lookupKey;
		public String fullName;
		public Long photoId;

		public final List<String> phones = new ArrayList<String>();

		public Profile vkUser;
	}
}
