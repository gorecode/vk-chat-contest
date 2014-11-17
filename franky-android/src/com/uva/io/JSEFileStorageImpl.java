package com.uva.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.uva.utilities.AssertCompat;

public class JSEFileStorageImpl implements FileStorage.Impl {
	private final File mDir;

	public JSEFileStorageImpl(File dir) {
		AssertCompat.notNull(dir, "Directory");

		mDir = dir;
	}

	public boolean isFileExists(String name) throws IOException {
		File file = new File(mDir, name);

		return file.exists();
	}

	public InputStream openFileInput(String name) throws IOException {
		return new FileInputStream(childFile(name));
	}

	public OutputStream openFileOutput(String name, boolean append) throws IOException {
		return new FileOutputStream(childFile(name), append);
	}

	public void deleteFile(String name) throws IOException {
		File file = childFile(name);

		if (!file.delete()) {
			throw new IOException("Failed to delete " + file.getAbsolutePath() + " file");
		}
	}

	private File childFile(String name) {
		return new File(mDir, name);
	}
}
