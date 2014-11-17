package com.uva.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

public class MemoryFileStorageImpl implements FileStorage.Impl {
	private final Hashtable mStorage = new Hashtable();

	public boolean isFileExists(String name) {
		return mStorage.containsKey(name);
	}

	public InputStream openFileInput(String name) throws IOException {
		ByteArrayOutputStream ostream = (ByteArrayOutputStream)mStorage.get(name);

		if (ostream == null) {
			throw new IOException("Entry not found in memory");
		}

		byte[] data = ostream.toByteArray();

		return new ByteArrayInputStream(data);
	}

	public OutputStream openFileOutput(String name, boolean append) throws IOException {
	  if (append) {
	    // XXX: Spike.
	    throw new IOException("Append mode not supported");
	  }

		ByteArrayOutputStream ostream = new ByteArrayOutputStream(); 
		mStorage.put(name, ostream);
		return ostream;
	}

	public void deleteFile(String name) throws IOException {
		mStorage.remove(name);
	}
}
