package com.gorecode.vk.config;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.uva.io.FileStorage;
import com.uva.utilities.AssertCompat;

public class FileConfigStorage implements ConfigStorage {
	private final FileStorage mFileStorage;
	private final String mFilename;

	public FileConfigStorage(FileStorage fileStorage, String configFilename) {
		AssertCompat.notNull(fileStorage, "File storage");
		AssertCompat.notNull(configFilename, "Config filename");

		mFileStorage = fileStorage;
		mFilename = configFilename;
	}

	public void restoreConfigState(Config config) throws IOException {
		byte[] bytes = mFileStorage.readFileContent(mFilename);

		try {
			JSONObject state = new JSONObject(new String(bytes));

			config.importValues(state);
		} catch (JSONException e) {
			throw new IOException(e.getMessage());
		}
	}

	public void saveConfigState(Config config) throws IOException {
		JSONObject state = config.exportValues();

		String jsonString = state.toString();

		mFileStorage.writeFileContent(mFilename, jsonString.getBytes());
	}
}
