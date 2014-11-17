package com.gorecode.vk.activity;

import java.io.File;
import java.io.IOException;

import roboguice.activity.RoboActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.utilities.FileCache;
import com.gorecode.vk.utilities.FileUtilities;
import com.uva.log.Log;

public class PickImageActivity extends RoboActivity {
	public static final int SOURCE_GALLERY = 0x0;
	public static final int SOURCE_CAMERA = 0x1;

	private static final String TAG = PickImageActivity.class.getSimpleName();

	private static final String EXTRA_SOURCE = "source";

	private static final String STATE_CAPTURED_IMAGE_URI = "state:capturedImageUri";

	private static final int REQUEST_CODE_GALLERY_ACTIVITY = 0x6655441;
	private static final int REQUEST_CODE_CAMERA_ACTIVITY = 0x6655442;

	@Inject
	private FileCache fileCache;

	private int source;

	private Uri capturedImageUri;
	
	public static void display(Activity context, int requestCode, int source) {
		context.startActivityForResult(getDisplayIntent(context, source), requestCode);
	}
	
	public static Intent getDisplayIntent(Activity context, int source) {
		return new Intent(context, PickImageActivity.class).putExtra(EXTRA_SOURCE, source);
	}

	public static File getImageFile(Intent intent) {
		if (intent == null) return null;

		return FileUtilities.fromFileUri(intent.getData());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		try {
			if (!unpackExtras(getIntent())) {
				throw new Exception("Unable to unpack extras from intent");
			}

			if (savedInstanceState == null) {
				startPickerActivity();
			}
		} catch (Exception e) {
			Log.exception(TAG, "Unable to pick image", e);

			ErrorHandlingUtilities.displayErrorSoftly(this, e);

			finish();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (capturedImageUri != null) {
			outState.putParcelable(STATE_CAPTURED_IMAGE_URI, capturedImageUri);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		if (savedInstanceState.containsKey(STATE_CAPTURED_IMAGE_URI)) {
			capturedImageUri = savedInstanceState.getParcelable(STATE_CAPTURED_IMAGE_URI);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			Log.debug(TAG, "Got bad result from child acitivities, request = " + requestCode + " result = " + resultCode);

			finish();

			return;
		}

		switch (requestCode) {
			case REQUEST_CODE_CAMERA_ACTIVITY:
			case REQUEST_CODE_GALLERY_ACTIVITY: {
				Log.debug(TAG, "Got result from picker, intent = " + data);

				Uri pickedImageUri = null;

				if (requestCode == REQUEST_CODE_CAMERA_ACTIVITY) {
					Log.debug(TAG, "Result is from camera");

					// XXX: Workaround Android Issue, http://code.google.com/p/android/issues/detail?id=1480

					if (data != null && data.getData() != null) {
						pickedImageUri = data.getData();
					} else {
						pickedImageUri = capturedImageUri;
					}

					Log.debug(TAG, "Delete file onDestroy(): " + pickedImageUri);
				} else {
					Log.debug(TAG, "Result is from gallery");

					pickedImageUri = data.getData();
				}

				Log.debug(TAG, "Picked image uri = " + pickedImageUri);

				pickedImageUri = Uri.fromFile(FileUtilities.fromUri(this, pickedImageUri));

				Log.debug(TAG, "Picked image file uri = " + pickedImageUri);

				Intent resultData = new Intent();
				resultData.setData(pickedImageUri);
				setResult(RESULT_OK, resultData);
				finish();

				break;
			}
		}
	}

	private void startPickerActivity() {
		switch (source) {
			case SOURCE_GALLERY:
				try {
					Log.debug(TAG, "Starting gallery");

					Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					intent.putExtra("return-data", false);
					startActivityForResult(intent, REQUEST_CODE_GALLERY_ACTIVITY);
				} catch (RuntimeException e) {
					throw new RuntimeException("Unable to start gallery activity", e);
				}
				break;
			case SOURCE_CAMERA:
				try {
					capturedImageUri = newTempFileUri();

					Log.debug(TAG, "Starting camera, output file = " + capturedImageUri);

					Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					intent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
					startActivityForResult(intent, REQUEST_CODE_CAMERA_ACTIVITY);
				} catch (IOException e) {
					Log.exception(TAG, "Unable to create temp file for camera capture output", e);

					finish();
				} catch (RuntimeException e) {
					throw new RuntimeException("Unable to start camera activity", e);
				}
				break;
		}
	}

	private Uri newTempFileUri() throws IOException {
		return Uri.fromFile(newTempFile());
	}

	private File newTempFile() throws IOException {
		return fileCache.addFileToCacheOrThrow(String.valueOf(System.currentTimeMillis()));
	}

	private boolean unpackExtras(Intent intent) {
		if (intent == null) return false;

		Bundle extras = intent.getExtras();

		if (extras == null) return false;

		source = extras.getInt(EXTRA_SOURCE, SOURCE_GALLERY);

		return true;
	}
}
