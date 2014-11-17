package com.uva.io;

import com.uva.utilities.AssertCompat;

public class PlatformFileStorage {
	private static FileStorage sDefined;

	public synchronized static void define(FileStorage.Impl platformFileStorageImp) {
		define(new FileStorage(platformFileStorageImp));
	}

	public synchronized static void define(FileStorage platformFileStorage) {
		AssertCompat.notNull(platformFileStorage, "File storage");

		if (sDefined != null) {
			throw new IllegalStateException("Already defined");
		}

		sDefined = platformFileStorage;
	}

	public synchronized static FileStorage getDefined() {
		if (sDefined == null) {
			throw new IllegalStateException("Platform file storage not defined");
		}
		return sDefined;
	}

	private PlatformFileStorage() {
		;
	}
}
