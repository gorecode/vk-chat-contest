/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gorecode.vk.phonesync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;


/**
 * Helper class for storing data in the platform content providers.
 */
public class ContactOperations {

	private final ContentValues mValues;
	private ContentProviderOperation.Builder mBuilder;
	private final BatchOperation mBatchOperation;
	private boolean mYield;
	private long mRawContactId;
	private int mBackReference;
	private boolean mIsNewContact;

	public static ContactOperations createNewContact(Context context,
			long userId, BatchOperation batchOperation) {
		return new ContactOperations(context, userId, batchOperation, true);
	}

	/**
	 * Returns an instance of ContactOperations for updating existing contact in
	 * the platform contacts provider.
	 * 
	 * @param context the Authenticator Activity context
	 * @param rawContactId the unique Id of the existing rawContact
	 * @return instance of ContactOperations
	 */
	public static ContactOperations updateExistingContact(Context context,
			long rawContactId, BatchOperation batchOperation) {
		return new ContactOperations(context, rawContactId, batchOperation, false);
	}

	public ContactOperations(Context context, long userId, BatchOperation batchOperation, boolean isNewContact) {
		mValues = new ContentValues();
		mYield = true;
		mBatchOperation = batchOperation;

		mBackReference = mBatchOperation.size();
		mIsNewContact = isNewContact;
		mValues.put(SyncColumns.USER_ID, String.valueOf(userId));
		mValues.put(SyncColumns.PROVIDER, SyncManager.PROVIDER_EXPECTED_VALUE);

		if (mIsNewContact) {
			mBuilder = newInsertCpo(RawContacts.CONTENT_URI, true).withValues(mValues);
			mBatchOperation.add(mBuilder.build());
		}
	}

	/**
	 * Adds a contact name
	 * 
	 * @param name Name of contact
	 * @param nameType type of name: family name, given name, etc.
	 * @return instance of ContactOperations
	 */
	public ContactOperations addName(String firstName, String lastName) {
		mValues.clear();
		if (!TextUtils.isEmpty(firstName)) {
			mValues.put(StructuredName.GIVEN_NAME, firstName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (!TextUtils.isEmpty(lastName)) {
			mValues.put(StructuredName.FAMILY_NAME, lastName);
			mValues.put(StructuredName.MIMETYPE,
					StructuredName.CONTENT_ITEM_TYPE);
		}
		if (mValues.size() > 0) {
			addInsertOp();
		}
		return this;
	}

	/**
	 * Adds an email
	 * 
	 * @param new email for user
	 * @return instance of ContactOperations
	 */
	public ContactOperations addEmail(String email) {
		mValues.clear();
		if (!TextUtils.isEmpty(email)) {
			mValues.put(Email.DATA, email);
			mValues.put(Email.TYPE, Email.TYPE_OTHER);
			mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	/**
	 * Adds a phone number
	 * 
	 * @param phone new phone number for the contact
	 * @param phoneType the type: cell, home, etc.
	 * @return instance of ContactOperations
	 */
	public ContactOperations addPhone(String phone, int phoneType) {
		mValues.clear();
		if (!TextUtils.isEmpty(phone)) {
			mValues.put(Phone.NUMBER, phone);
			mValues.put(Phone.TYPE, phoneType);
			mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			addInsertOp();
		}
		return this;
	}

	/**
	 * Updates contact's email
	 * 
	 * @param email email id of the sample SyncAdapter user
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of ContactOperations
	 */
	public ContactOperations updateEmail(String email, String existingEmail,
			Uri uri) {
		if (!TextUtils.equals(existingEmail, email)) {
			mValues.clear();
			mValues.put(Email.DATA, email);
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contact's name
	 * 
	 * @param name Name of contact
	 * @param existingName Name of contact stored in provider
	 * @param nameType type of name: family name, given name, etc.
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of ContactOperations
	 */
	public ContactOperations updateName(Uri uri, String existingFirstName,
			String existingLastName, String firstName, String lastName) {
		Log.i("ContactOperations", "ef=" + existingFirstName + "el="
				+ existingLastName + "f=" + firstName + "l=" + lastName);
		mValues.clear();
		if (!TextUtils.equals(existingFirstName, firstName)) {
			mValues.put(StructuredName.GIVEN_NAME, firstName);
		}
		if (!TextUtils.equals(existingLastName, lastName)) {
			mValues.put(StructuredName.FAMILY_NAME, lastName);
		}
		if (mValues.size() > 0) {
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Updates contact's phone
	 * 
	 * @param existingNumber phone number stored in contacts provider
	 * @param phone new phone number for the contact
	 * @param uri Uri for the existing raw contact to be updated
	 * @return instance of ContactOperations
	 */
	public ContactOperations updatePhone(String existingNumber, String phone,
			Uri uri) {
		if (!TextUtils.equals(phone, existingNumber)) {
			mValues.clear();
			mValues.put(Phone.NUMBER, phone);
			addUpdateOp(uri);
		}
		return this;
	}

	/**
	 * Adds an insert operation into the batch
	 */
	private void addInsertOp() {
		if (!mIsNewContact) {
			mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
		}
		mBuilder =
				newInsertCpo(Data.CONTENT_URI, mYield);
		mBuilder.withValues(mValues);
		if (mIsNewContact) {
			mBuilder
			.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
		}
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	/**
	 * Adds an update operation into the batch
	 */
	private void addUpdateOp(Uri uri) {
		mBuilder = newUpdateCpo(uri, mYield).withValues(mValues);
		mYield = false;
		mBatchOperation.add(mBuilder.build());
	}

	public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
			boolean yield) {
		return ContentProviderOperation.newInsert(uri).withYieldAllowed(yield);
	}

	public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
			boolean yield) {
		return ContentProviderOperation.newUpdate(uri).withYieldAllowed(yield);
	}

	public static ContentProviderOperation.Builder newDeleteCpo(Uri uri, boolean yield) {
		return ContentProviderOperation.newDelete(uri).withYieldAllowed(yield);
	}
}
