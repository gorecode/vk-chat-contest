package com.uva.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.uva.log.Log;
import com.uva.log.Message;

public class FileStorage {
	public static interface Impl {
		public boolean isFileExists(String name) throws IOException;
		public InputStream openFileInput(String name) throws IOException;
		public OutputStream openFileOutput(String name, boolean append) throws IOException;
		public void deleteFile(String name) throws IOException;		
	}

	private static final String LOG_TAG = "FileStorage";

	private final Impl mImpl;

	public FileStorage(Impl impl) {
		if (impl == null) throw new IllegalArgumentException("File storage implementation cannot be null");

		mImpl = impl;
	}

	public JSONObject readJSONObject(String filename) throws IOException, JSONException {
		byte[] fileContent = readFileContent(filename);

		String jsonString = new String(fileContent, "UTF-8");

		return new JSONObject(jsonString);
	}

	public void writeJSONObject(String filename, JSONObject json) throws IOException, JSONException {
		if (json == null) throw new JSONException("JSON object to write cannot be null");

		byte[] fileContent = json.toString().getBytes("UTF-8");

		writeFileContent(filename, fileContent);
	}

	public boolean isFileExists(String name) throws IOException {
		assertNotNull(name);

		return mImpl.isFileExists(name);
	}

	public InputStream openFileInput(String name) throws IOException {
		assertNotNull(name);

		InputStream is = mImpl.openFileInput(name);

		if (is == null) {
			throw new IllegalStateException("Implementation of openFileInput cannot return null");
		}

		return is;
	}

	public OutputStream openFileOutput(String name) throws IOException {
	  return openFileOutput(name, false);
	}

	public OutputStream openFileOutput(String name, boolean append) throws IOException {
		assertNotNull(name);

		OutputStream os = mImpl.openFileOutput(name, append);

		if (os == null) {
			throw new IllegalStateException("Implementation of openFileOutput cannot return null");
		}

		return os;		
	}

	public void deleteFile(String name) throws IOException {
		assertNotNull(name);

		if (isFileExists(name)) {
			mImpl.deleteFile(name);
		}
	}

	public byte[] readFileContent(String name) throws IOException {
		InputStream in = openFileInput(name);

		try {
			return StreamUtilities.readUntilEnd(in);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				Log.exception(LOG_TAG, Message.WARNING, e);
			}
		}
	}

	public void writeFileContent(String name, byte[] data) throws IOException {
		OutputStream out = openFileOutput(name);

		try {
			out.write(data);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				Log.exception(LOG_TAG, Message.WARNING, e);
			}
		}
	}

	private static void assertNotNull(String name) {
		if (name == null) {
			throw new IllegalArgumentException("File name cannot be null");
		}
	}
}
