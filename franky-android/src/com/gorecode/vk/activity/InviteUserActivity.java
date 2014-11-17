package com.gorecode.vk.activity;

import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.gorecode.vk.cache.SQLiteSelectionCommand;
import com.uva.log.Log;
import com.uva.log.Message;

@ContentView(R.layout.invite_user_activity)
public class InviteUserActivity extends VkActivity {
	private static final String TAG = InviteUserActivity.class.getSimpleName();

	private static final String EXTRA_NAME = "name";
	private static final String EXTRA_PHOTO_ID = "photoId";
	private static final String EXTRA_PHONE = "phone";

	private static final long INVALID_ID = -1;

	@InjectView(R.id.photo)
	private ImageView mPhotoView;
	@InjectView(R.id.call_button)
	private Button mCallButton;

	private String mName;
	private String mPhone;
	private Bitmap mPhoto;

	public static Intent getDisplayIntent(Context context, String name, String phone, Long photoId) {
		Intent intent = new Intent(context, InviteUserActivity.class);

		intent.putExtra(EXTRA_NAME, name);
		intent.putExtra(EXTRA_PHONE, phone);

		if (photoId != null) {
			intent.putExtra(EXTRA_PHOTO_ID, photoId);
		}

		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setAnimations(ANIMATIONS_SLIDE_RIGHT);

		unpackArguments();

		setUpViews();
	}

	@InjectOnClickListener(R.id.send_invitation_button)
	protected void onSendInvitationButtonClicked(View v) {
		try {
			String smsText = String.format(getString(R.string.sms_invitation_text_format), getString(R.string.application_name));
			Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + mPhone));
			intent.putExtra("sms_body", smsText); 
			startActivity(intent);
		} catch (Exception e) {
			Log.exception(TAG, "error performing send sms action", e);
		}
	}

	@InjectOnClickListener(R.id.call_button)
	protected void onCallButtonClicked(View v) {
		try {
			Log.debug(TAG, "calling on phone = " + mPhone);
			Intent intent = new Intent(Intent.ACTION_CALL);
			intent.setData(Uri.parse("tel:" + mPhone));
			startActivity(intent);
		} catch (Exception e) {
			Log.exception(TAG, "Error starting phone activity", e);
		}
	}

	private void setUpViews() {
		if (mPhoto != null) {
			mPhotoView.setImageBitmap(mPhoto);
		}

		mCallButton.setText(String.format(getString(R.string.call_button_text_format), mPhone));

		getVkActionBar().setTitle(mName);

		Aibolit.doInjections(this);
	}

	private void unpackArguments() {
		Intent intent = getIntent();

		mName = intent.getStringExtra(EXTRA_NAME);
		mPhone = intent.getStringExtra(EXTRA_PHONE);

		long photoId = intent.getLongExtra(EXTRA_PHOTO_ID, INVALID_ID);

		if (photoId != INVALID_ID) {
			try {
				Cursor c = new SQLiteSelectionCommand().select(Photo.PHOTO).from(Data.CONTENT_URI).where(Data._ID + "=?").withValues(photoId).execute(getContentResolver());

				if (c.moveToFirst()) {
					byte[] blob = c.getBlob(0);

					mPhoto = BitmapFactory.decodeByteArray(blob, 0, blob.length);
				}
			} catch (Exception e) {
				Log.exception(TAG, Message.WARNING, "error loading contact photo", e);
			}
		}
	}
}
