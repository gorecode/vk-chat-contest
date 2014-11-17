package com.gorecode.vk.task;

import java.io.File;
import java.util.List;

import com.gorecode.vk.api.VkModel;
import com.perm.kate.api.UploadedObject;

public class SaveProfilePhotoTask extends UploadImageTask<String> {
	public SaveProfilePhotoTask(VkModel model) {
		super(model);
	}

	@Override
	protected String getUploadServer() throws Exception {
		return mModel.photosGetProfileUploadServer();
	}

	@Override
	protected String registerUpload(UploadedObject upload) throws Exception {
		return mModel.saveProfilePhoto(upload.server, upload.photo, upload.hash)[0];
	}
}
