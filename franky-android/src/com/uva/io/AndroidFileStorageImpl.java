package com.uva.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;

import com.uva.io.FileStorage;
import com.uva.lang.ArrayUtilities;

public class AndroidFileStorageImpl implements FileStorage.Impl {
	private final Context mContext;

	public AndroidFileStorageImpl(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context cannot be null");
		}
		this.mContext = context;
	}

	public boolean isFileExists(String name) throws IOException {
		return (ArrayUtilities.contains(mContext.fileList(), name));
	}

	public InputStream openFileInput(String name) throws IOException {
		return mContext.openFileInput(name);
	}

	public OutputStream openFileOutput(String name, boolean append) throws IOException {
		return mContext.openFileOutput(name, Context.MODE_PRIVATE | (append ? Context.MODE_APPEND : 0));
	}

	public void deleteFile(String name) throws IOException {
		if (!mContext.deleteFile(name)) {
			throw new IOException("Failed to delete file");
		}
	}
}
