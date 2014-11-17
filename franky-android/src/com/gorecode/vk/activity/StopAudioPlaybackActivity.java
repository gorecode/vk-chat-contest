package com.gorecode.vk.activity;

import com.gorecode.vk.R;
import com.google.inject.Inject;
import com.gorecode.vk.audio.AudioPlayer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class StopAudioPlaybackActivity extends VkActivity {
	private static final int STOP_PLAYBACK_QUESTION_DIALOG = 0x0;

	@Inject
	private AudioPlayer mPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		showDialog(STOP_PLAYBACK_QUESTION_DIALOG);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id != STOP_PLAYBACK_QUESTION_DIALOG) return super.onCreateDialog(id);

		final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
				case DialogInterface.BUTTON_POSITIVE:
					onYesClicked();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					onNoClicked();
					break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.stop_plackback_dialog_message);
		builder.setPositiveButton(R.string.yes, dialogClickListener);
		builder.setNegativeButton(R.string.no, dialogClickListener);
		return builder.create();
	}

	private void onYesClicked() {
		mPlayer.reset();

		finish();
	}

	private void onNoClicked() {
		finish();
	}
}
