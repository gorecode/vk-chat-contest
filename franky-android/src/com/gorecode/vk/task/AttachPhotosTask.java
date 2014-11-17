package com.gorecode.vk.task;

import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.perm.kate.api.UploadedObject;
import com.perm.kate.api.VkPhoto;

public class AttachPhotosTask extends UploadImageTask<VkPhoto> {
	@Inject
	public AttachPhotosTask(VkModel model) {
		super(model);
	}

	@Override
	protected String getUploadServer() throws Exception {
		return mModel.getMessagesUploadServer();
	}

	@Override
	protected VkPhoto registerUpload(UploadedObject upload) throws Exception {
		return mModel.saveMessagesPhoto(upload.server, upload.photo, upload.hash).get(0);
	}
}
