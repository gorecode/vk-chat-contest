package com.uva.net;

import java.io.IOException;
import java.io.InputStream;

import com.uva.io.StreamUtilities;
import com.uva.log.Log;
import com.uva.log.Message;

/**
 * A tool for downloading files.
 *
 * @author enikey.
 */
public class Downloader {
	private static final String TAG = "Downloader";

	/**
	 * Downloads a file with a specified url using GET request method.
	 * @param url the file url.
	 * @return downloaded file bytes.
	 * @throws IOException if io-error occurs or if Http response contains error.
	 */
	public static byte[] download(String url, HttpConnector connector) throws IOException {
		if (connector == null) {
			throw new IllegalArgumentException("HttpConnection cannot be null");
		}

		HttpConnection connection = connector.openHttpConnection(url, HttpConnector.GET_METHOD);

		byte[] responseBytes = null;

		try {
			InputStream is = connection.openInputStream();
			try {
				responseBytes = StreamUtilities.readUntilEnd(is);
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					Log.exception(TAG, Message.WARNING, "Failed to close http connection's input stream", e);
				}
			}
		} finally {
			try {
				connection.close();
			} catch (IOException e) {
				Log.exception(TAG, Message.WARNING, "Failed to close http connection stream", e);
			}
		}

		return responseBytes;
	}

	private Downloader() {
	}
}
