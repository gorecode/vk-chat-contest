package com.gorecode.vk.dialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

import com.gorecode.vk.R;
import com.gorecode.vk.activity.PickImageActivity;
import com.gorecode.vk.utilities.BitmapUtilities;
import com.uva.io.StreamUtilities;
import com.uva.log.Log;

public class PickImageDialog extends DialogFragment {
	private static final String TAG = PickImageDialog.class.getSimpleName();
	private static final int REQUEST_PICK_AVATAR = 65501;

	public interface ImagePickListener {
		void onImagePick(Result result);
	}

	public static class Result {

		private File file;

		public Result(File avatarFile) {
			this.file = avatarFile;
		}

		public File getFile() {
			return file;
		}

		public Bitmap getImagePreview(int width, int height) {
			if (file == null) return null;
			try {
				return BitmapUtilities.loadPreview(file.getAbsolutePath(), width, height);
			} catch (Exception e) {
				Log.error(TAG, e.getMessage());
				return null;
			}
		}

		public byte[] getImageBytes() {
			if (file == null) return null;
			try {
				return StreamUtilities.readUntilEnd(new FileInputStream(file));
			} catch (IOException e) {
				Log.error(TAG, e.getMessage());
				return null;
			}
		}
	}

	private static class State extends Fragment {
		public boolean hasImage;		
		public String title;
		public ImagePickListener onPickListener;

		private boolean isDetachRequested;

		public State() {
			setRetainInstance(true);
		}

		@Override
		public void onResume() {			
			super.onResume();
			if (isDetachRequested) {
				Log.trace(TAG, "detach State fragment");
				getFragmentManager().beginTransaction().detach(this).commit();
			}
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			if (requestCode == REQUEST_PICK_AVATAR) {
				isDetachRequested = true;
				if (resultCode == Activity.RESULT_OK) {				
					notifyImagePicked(PickImageActivity.getImageFile(data));
				}
			} else {
				super.onActivityResult(requestCode, resultCode, data);
			}
		}

		public void removeImage() {
			isDetachRequested = true;
			notifyImagePicked(null);
		}

		private void notifyImagePicked(File imageFile) {
			if (onPickListener != null) {
				onPickListener.onImagePick(new Result(imageFile));
			}
		}
	}

	private State state = new State(); 

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction().add(state, State.class.getName()).commit();
		} else {
			state = (State)getFragmentManager().findFragmentByTag(State.class.getName());
		}
	}

	/**
	 * Generally Activities store images in two ways: image Url in Profile and selected on device Image.
	 * This function allows to deal with both and decide, do we have avatar selected or not. 
	 * @param previousResult - previously selected image on device, if so.
	 * @param profileImage - some image data, stored in profile to indicate do we have image (only checked for null).
	 */
	public void setHasImage(Result previousResult, Object profileImage) {
		if (previousResult == null) {
			setHasImage(profileImage != null);
		} else {
			setHasImage(previousResult.getFile() != null);
		}
	}

	public void setHasImage(boolean hasImage) {
		state.hasImage = hasImage;
	}

	public void setTitle(String title) {
		state.title = title;
	}

	public void setOnPickImageListener(ImagePickListener l) {
		state.onPickListener = l;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String[] items;

		if (state.hasImage) {
			items = new String[] {
					getString(R.string.photo_dialog_from_gallery),
					getString(R.string.photo_dialog_from_camera) };
		} else {
			items = new String[] {
					getString(R.string.photo_dialog_from_gallery),
					getString(R.string.photo_dialog_from_camera) };
		}	

		final Activity activity = getActivity();
		assert (activity != null);

		DialogInterface.OnClickListener callback = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {					
				switch (item) {
				case 0:
					Log.debug(TAG, "pick image from gallery");
					state.startActivityForResult(PickImageActivity.getDisplayIntent(activity, PickImageActivity.SOURCE_GALLERY), REQUEST_PICK_AVATAR);
					dialog.dismiss();
					return;
				case 1:
					Log.debug(TAG, "get image from camera");
					state.startActivityForResult(PickImageActivity.getDisplayIntent(activity, PickImageActivity.SOURCE_CAMERA), REQUEST_PICK_AVATAR);
					dialog.dismiss();
					return;
				case 2:
					Log.debug(TAG, "remove image");
					state.removeImage();
					dialog.dismiss();
					return;
				}
			}
		};

		AlertDialog dialog = new AlertDialog.Builder(activity).setSingleChoiceItems(items, -1, callback).setTitle(state.title).create();

		return dialog;
	}
}
