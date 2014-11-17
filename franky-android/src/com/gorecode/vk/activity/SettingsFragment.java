package com.gorecode.vk.activity;

import java.io.File;
import java.util.List;

import roboguice.inject.InjectView;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.danikula.aibolit.Aibolit;
import com.danikula.aibolit.VisibleInjectionContext;
import com.danikula.aibolit.annotation.InjectOnClickListener;
import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.data.ImageUrls;
import com.gorecode.vk.dialog.PickImageDialog;
import com.gorecode.vk.dialog.PickImageDialog.ImagePickListener;
import com.gorecode.vk.dialog.PickImageDialog.Result;
import com.gorecode.vk.imageloader.ImageProcessors;
import com.gorecode.vk.sync.Session;
import com.gorecode.vk.task.SaveProfilePhotoTask;
import com.gorecode.vk.utilities.ErrorHandlingUtilities;
import com.gorecode.vk.view.WebImageView;
import com.uva.log.Log;

public class SettingsFragment extends VkFragment implements ImagePickListener {
	private static final String TAG = SettingsFragment.class.getSimpleName();

	@InjectView(R.id.photo)
	private WebImageView mPhotoView;

	@Inject
	private VkModel mVk;
	@Inject
	private Session mSession;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.settings_fragment, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setUpViews();

		updateViews();
	}

	@Override
	public void onImagePick(Result result) {
		new SaveProfilePhotoTask(mVk) {
			private final ProgressDialog mProgressDialog = new ProgressDialog(getActivity());

			@Override
			public void onPreExecute() {
				Log.debug(TAG, "Start uploading new profile photo");

				if (isCancelled()) {
					return;
				}

				mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				mProgressDialog.setMessage(getActivity().getText(R.string.message_uploading_images));
				mProgressDialog.setOnCancelListener(new ProgressDialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
				});

				blockUi();
			}

			@Override
			protected List<String> doInBackgroundOrThrow(File... params) throws Exception {
				List<String> urls = super.doInBackgroundOrThrow(params);

				mSession.syncUserNow();

				return urls;
			}

			@Override
			public void onProgressUpdate(Integer... percentsComplete) {
				if (isCancelled()) return;

				int percent = percentsComplete[0];

				mProgressDialog.setProgress(percent);
			}

			@Override
			public void onPostExecute(Pair<List<String>, Exception> result) {
				if (!shouldNotTouchViews()) {
					unblockUi();
				}

				if (isCancelled()) {
					return;
				}

				if (result.second != null) {
					Log.exception(TAG, "Unable to change profile photo", result.second);

					ErrorHandlingUtilities.displayErrorSoftly(getActivity(), result.second);
				} else {
					Log.debug(TAG, "Photo have been changed");

					if (!shouldNotTouchViews()) {
						updateViews();
					}
				}
			}

			@Override
			public void onCancelled() {
				if (!shouldNotTouchViews()) {
					unblockUi();
				}					
			}

			private void blockUi() {
				mPhotoView.setEnabled(false);

				mProgressDialog.show();
			}

			private void unblockUi() {
				mPhotoView.setEnabled(true);

				mProgressDialog.dismiss();
			}
		}.execute(result.getFile());
	}

	@InjectOnClickListener(R.id.photo)
	public void onPhotoClicked(View v) {
		ImageUrls photoUrls = mSession.getContext().getUser().avatarUrls;

		if (photoUrls != null) {
			ViewImageActivity.displayWebImage(getActivity(), photoUrls.fullsizeUrl);
		}
	}

	@InjectOnClickListener(R.id.change_profile_photo_button)
	public void onChangeProfilePhotoButtonClicked(View v) {
		offerProfilePhotoChange();
	}

	@InjectOnClickListener(R.id.logout_button)
	public void onLogoutButtonClicked(View v) {
		mSession.logout();

		LoginActivity.display(getActivity());
		
		getActivity().finish();
	}

	@InjectOnClickListener(R.id.preferences_button)
	public void onPreferencesButtonClicked(View v) {
		EditConfigActivity.display(getActivity(), R.xml.preferences);
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		Log.debug(TAG, "onHidden(" + hidden + ")");

		if (!hidden) {
			updateViews();
		}
	}

	private void offerProfilePhotoChange() {
		PickImageDialog dialog = new PickImageDialog();
		dialog.setOnPickImageListener(this);		
		dialog.setTitle(getString(R.string.photo_dialog_title));
		dialog.show(getFragmentManager(), null);
	}

	private void setUpViews() {
		Aibolit.doInjections(this, new VisibleInjectionContext(getView()));
	}

	private void updateViews() {
		mPhotoView.setImageUrls(mSession.getContext().getUser().avatarUrls, ImageProcessors.PHOTO_ROUNDER);
	}
}
