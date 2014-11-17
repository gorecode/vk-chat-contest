package com.uva.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

public class StreamUtilities {
	private StreamUtilities() {
		;
	}

	public static void copyStream(InputStream is, OutputStream os) throws IOException {
		final int buffer_size = 1024;
		byte[] bytes = new byte[buffer_size];
		while (true) {
			int count = is.read(bytes, 0, buffer_size);
			if (count == -1) {
				break;
			}
        	if (Thread.currentThread().isInterrupted()) {
        		throw new InterruptedIOException();
        	}
			os.write(bytes, 0, count);
		}
	}

	public static byte[] readUntilEnd(InputStream in) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		int bytesRead;
		byte[] buffer = new byte[1024];
		while ((bytesRead = in.read(buffer)) != -1) {
        	if (Thread.currentThread().isInterrupted()) {
        		throw new InterruptedIOException();
        	}
			result.write(buffer, 0, bytesRead);
		}
		return result.toByteArray();			
	}
}
