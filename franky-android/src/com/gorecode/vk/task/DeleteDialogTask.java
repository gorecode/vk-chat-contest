package com.gorecode.vk.task;

import roboguice.RoboGuice;
import android.content.Context;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.gorecode.vk.activity.VkFragment;
import com.gorecode.vk.api.VkModel;
import com.gorecode.vk.application.VkApplication;
import com.gorecode.vk.data.Dialog;
import com.gorecode.vk.task.LongAction;

public class DeleteDialogTask extends LongAction<Void, Void> {
	@Inject
	private VkModel mModel;

	private Dialog mDialog;

	public DeleteDialogTask(Context context, VkFragment fragment, Dialog dialog) {
		super(context, fragment);

		init(dialog);
	}

	public DeleteDialogTask(Context context, Dialog dialog) {
		super(context);

		init(dialog);
	}

	@Override
	protected Void doInBackgroundOrThrow(Void args) throws Exception {
		mModel.deleteDialog(mDialog);

		return null;
	}

	private void init(Dialog dialog) {
		mDialog = dialog;

		Injector injector = RoboGuice.getBaseApplicationInjector(VkApplication.from(getContext()));
		injector.injectMembers(this);

		wrapWithProgress(false);
	}
}
