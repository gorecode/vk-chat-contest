package com.gorecode.vk.phonesync;

import java.io.ByteArrayOutputStream;
import java.util.List;

import roboguice.RoboGuice;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.BuildInfo;
import com.gorecode.vk.data.ImageUrls;
import com.gorecode.vk.data.Profile;
import com.gorecode.vk.imageloader.ImageLoader;
import com.gorecode.vk.sync.Session;

@Singleton
public class SyncManager {
	public static interface Callbacks {
		public static final int STATE_GET_FRIENDS = 0x0;
		public static final int STATE_SYNC_CONTACTS = 0x1;
		public static final int STATE_SYNC_PHOTOS = 0x2;

		public void onStateChanged(int state);

		public void onProgress(int current, int total);
	}

	public static final String PROVIDER_EXPECTED_VALUE = "com.gorecode.vk.account";

	private static final boolean DEBUG = false && BuildInfo.IS_DEBUG_BUILD;

	private static final String TAG = SyncManager.class.getSimpleName();

	private final Context mContext;

	private final Session mSession;
	private final VkModel mVk;

	private final ImageLoader mImageLoader;

	@Inject
	public SyncManager(Context context, ImageLoader imageLoader) {
		mContext = context;
		mImageLoader = imageLoader;

		Injector serviceLocator = RoboGuice.getInjector(context);

		mSession = serviceLocator.getInstance(Session.class);
		mVk = RoboGuice.getInjector(context).getInstance(VkModel.class);
	}

	public SyncResult syncContacts(Callbacks callbacks) throws Exception {
		callbacks.onStateChanged(Callbacks.STATE_GET_FRIENDS);

		List<Profile> friends = Profile.fromVkUsers(mVk.getFriends(mSession.getContext().getUserId(), VkModel.REQUIRED_USER_FIELDS, null));

		if (DEBUG) {
			dumpRawContacts();
		}

		return syncContacts(friends, callbacks);
	}

	public synchronized SyncResult syncContacts(List<Profile> users, Callbacks callbacks) {
		callbacks.onStateChanged(Callbacks.STATE_SYNC_CONTACTS);

		SyncResult syncResult = new SyncResult();

		Context context = mContext;

		long userId;
		long rawContactId = 0;
		final ContentResolver resolver = context.getContentResolver();
		final BatchOperation batchOperation = new BatchOperation(context, resolver);
		Log.d(TAG, "In SyncContacts");

		for (int i = 0; i < users.size(); i++) {
			Profile user = users.get(i);

			Log.d(TAG, "sync user " + i + " of " + users.size());

			try {
				userId = user.getUid();

				rawContactId = lookupRawContact(resolver, userId);

				if (rawContactId != 0) {
					Log.d(TAG, "In updateContact");

					updateContact(context, resolver, user, rawContactId, batchOperation);
				} else {
					Log.d(TAG, "In addContact");

					addContact(context, user, batchOperation);
				}

				batchOperation.execute();

				callbacks.onProgress(i + 1, users.size());
			} catch (final Exception e) {
				syncResult.stats.numParseExceptions++;

				Log.e(TAG, "error running sync adapter", e);
			}
		}

		callbacks.onStateChanged(Callbacks.STATE_SYNC_PHOTOS);

		for (int i = 0; i < users.size(); i++) {
			Log.d(TAG, "updating photo, " + i + " of " + users.size());

			Profile user = users.get(i);

			try {
				if (user.avatarUrls == null) {
					continue;
				}

				userId = user.getUid();

				rawContactId = lookupRawContact(resolver, userId);

				if (rawContactId == 0) {
					continue;
				} else {
					updatePhotoThumb(rawContactId, user.avatarUrls);
				}

				callbacks.onProgress(i + 1, users.size());
			} catch (final Exception e) {
				syncResult.stats.numParseExceptions++;

				Log.e(TAG, "error updating contact photo", e);
			}
		}

		return syncResult;
	}

	private void updatePhotoThumb(long rawContactId, ImageUrls photoUrls) throws Exception {
		String photoUrl = photoUrls.fullsizeUrl;

		ContentResolver resolver = mContext.getContentResolver();

		Cursor c = resolver.query(Data.CONTENT_URI, PhotoQuery.PROJECTION, PhotoQuery.SELECTION, new String[] { String.valueOf(rawContactId) }, null);

		try {
			Bitmap image = mImageLoader.loadImage(photoUrl, null, null).get();

			ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
			image.compress(CompressFormat.PNG, 75, imageStream);
			imageStream.flush();

			ContentValues values = new ContentValues();

			values.put(Photo.PHOTO, imageStream.toByteArray());

			Uri dataUri = null;

			if (!c.moveToFirst()) {
				if (DEBUG) {
					Log.v(TAG, "insert photo thumb");
				}

				values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
				values.put(Data.IS_PRIMARY, 1);
				values.put(Data.IS_SUPER_PRIMARY, 1);
				values.put(Data.RAW_CONTACT_ID, rawContactId);

				dataUri = resolver.insert(Data.CONTENT_URI, values);
			} else {
				if (DEBUG) {
					Log.v(TAG, "update photo thumb");
				}

				dataUri = ContentUris.withAppendedId(Data.CONTENT_URI, c.getLong(PhotoQuery.COLUMN_ID));

				resolver.update(dataUri, values, null, null);
			}

			resolver.notifyChange(dataUri, null);
		} finally {
			c.close();
		}
	}

	private void dumpRawContacts() {
		Cursor c = mContext.getContentResolver().query(RawContacts.CONTENT_URI, new String[] { RawContacts._ID, RawContacts.SOURCE_ID, RawContacts.SYNC1 }, null, null, null);

		try {
			while (c.moveToNext()) {
				String sync1 = c.getString(c.getColumnIndex(RawContacts.SYNC1));
				String sourceId = c.getString(c.getColumnIndexOrThrow(RawContacts.SOURCE_ID));
				String _id = c.getString(c.getColumnIndexOrThrow(RawContacts._ID));

				Log.d(TAG, String.format("_id = %s, sync1 = %s, source_id = %s", _id, sync1, sourceId));
			}
		} finally {
			c.close();
		}
	}

	private static void addContact(Context context, Profile user, BatchOperation batchOperation) {
		final ContactOperations contactOp = ContactOperations.createNewContact(context, user.getUid(), batchOperation);

		contactOp

		.addName(user.getFirstName(), user.getLastName())
		.addEmail(user.getEmail())
		.addPhone(user.getCellPhone(), Phone.TYPE_MOBILE)
		.addPhone(user.getHomePhone(), Phone.TYPE_OTHER);
	}

	private static void updateContact(Context context, ContentResolver resolver, Profile user, long rawContactId, BatchOperation batchOperation) {
		Uri uri;
		String cellPhone = null;
		String otherPhone = null;
		String email = null;

		final Cursor c = resolver.query(Data.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION, new String[] {String.valueOf(rawContactId)}, null);

		final ContactOperations contactOp = ContactOperations.updateExistingContact(context, rawContactId, batchOperation);

		try {
			while (c.moveToNext()) {
				final long id = c.getLong(DataQuery.COLUMN_ID);
				final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
				uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);

				if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
					final String lastName = c.getString(DataQuery.COLUMN_FAMILY_NAME);
					final String firstName = c.getString(DataQuery.COLUMN_GIVEN_NAME);

					contactOp.updateName(uri, firstName, lastName, user.getFirstName(), user.getLastName());
				} else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
					final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);

					if (type == Phone.TYPE_MOBILE) {
						cellPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);

						contactOp.updatePhone(cellPhone, user.getCellPhone(), uri);
					} else if (type == Phone.TYPE_OTHER) {
						otherPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);

						contactOp.updatePhone(otherPhone, user.getHomePhone(), uri);
					}
				} else if (Data.MIMETYPE.equals(Email.CONTENT_ITEM_TYPE)) {
					email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);

					contactOp.updateEmail(user.getEmail(), email, uri);
				}
			} // while
		} finally {
			c.close();
		}

		// Add the cell phone, if present and not updated above
		if (cellPhone == null) {
			contactOp.addPhone(user.getCellPhone(), Phone.TYPE_MOBILE);
		}

		// Add the other phone, if present and not updated above
		if (otherPhone == null) {
			contactOp.addPhone(user.getHomePhone(), Phone.TYPE_OTHER);
		}

		// Add the email address, if present and not updated above
		if (email == null) {
			contactOp.addEmail(user.getEmail());
		}
	}

	private static long lookupRawContact(ContentResolver resolver, long userId) {
		long authorId = 0;
		final Cursor c =
				resolver.query(RawContacts.CONTENT_URI, UserIdQuery.PROJECTION,
						UserIdQuery.SELECTION, new String[] {String.valueOf(userId)},
						null);
		try {
			if (c.moveToFirst()) {
				authorId = c.getLong(UserIdQuery.COLUMN_ID);
			}
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return authorId;
	}

	private interface PhotoQuery {
		public static final String[] PROJECTION = new String[] { Data._ID };

		public static final int COLUMN_ID = 0;

		public static final String SELECTION = Data.MIMETYPE + "='" + Photo.CONTENT_ITEM_TYPE + "' AND " + Data.RAW_CONTACT_ID + "=?";
	}

	private interface UserIdQuery {
		public final static String[] PROJECTION =
				new String[] {RawContacts._ID};

		public final static int COLUMN_ID = 0;

		public static final String SELECTION = SyncColumns.PROVIDER + "='" + SyncManager.PROVIDER_EXPECTED_VALUE + "' AND " + SyncColumns.USER_ID + "=?";
	}

	private interface DataQuery {
		public static final String[] PROJECTION =
				new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
			Data.DATA3,};

		public static final int COLUMN_ID = 0;
		public static final int COLUMN_MIMETYPE = 1;
		public static final int COLUMN_DATA1 = 2;
		public static final int COLUMN_DATA2 = 3;
		public static final int COLUMN_DATA3 = 4;
		public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
		public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
		public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
		public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
		public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;

		public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
	}
}
