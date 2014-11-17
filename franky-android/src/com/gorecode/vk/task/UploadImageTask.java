package com.gorecode.vk.task;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.inject.Inject;
import com.gorecode.vk.api.VkModel;
import com.perm.kate.api.KException;
import com.perm.kate.api.UploadedObject;
import com.uva.io.StreamUtilities;
import com.uva.log.Log;

public abstract class UploadImageTask<UploadItem> extends ThrowingAsyncTask<File, Integer, List<UploadItem>> {
	private static final String TAG = AttachPhotosTask.class.getSimpleName();

	protected final VkModel mModel;

	@Inject
	public UploadImageTask(VkModel model) {
		mModel = model;
	}

	abstract protected String getUploadServer() throws Exception;
	abstract protected UploadItem registerUpload(UploadedObject upload) throws Exception;

	@Override
	protected List<UploadItem> doInBackgroundOrThrow(File... params) throws Exception {
		String uploadServer = getUploadServer();

		ArrayList<UploadItem> items = new ArrayList<UploadItem>();

		int totalFilesSizeInBytes = 0;

		for (File file : params) {
			totalFilesSizeInBytes += file.length();
		}

		final int toUpload = totalFilesSizeInBytes;

		UploadCallbacks progressPublisher = new UploadCallbacks() {
			private int mTotalBytesWritten = 0;
			private int mProgressInPercents = 0;

			@Override
			public void onBytesWritten(int size) {
				int lastProgress = mProgressInPercents;

				mTotalBytesWritten += size;

				mProgressInPercents = mTotalBytesWritten * 100 / toUpload;

				if (mProgressInPercents != lastProgress) {
					publishProgress(mProgressInPercents);
				}
			}
		};

		for (File file : params) {
			InputStream is = new FileInputStream(file);

			if (isCancelled()) throw new Exception("Cancelled");

			try { 
				String response = uploadFile(new URL(uploadServer), is, progressPublisher);

				Log.message(TAG, "Upload server response = " + response);

				JSONObject json = new JSONObject(response);

				Log.debug(TAG, "Upload server response (as json) = " + response);

				if (json.has("error")) {
					throw new KException(-1, json.getString("error"));
				}

				items.add(registerUpload(UploadedObject.parseJson(json)));
			} finally {
				is.close();
			}
		}

		return items;
	}

	private String uploadFile(URL uploadServerUrl, InputStream inputStream, UploadCallbacks callbacks) throws Exception {
		final String lineEnd = "\r\n";
		final String twoHyphens = "--";
		final String boundary = "*****";

		HttpURLConnection conn = (HttpURLConnection)uploadServerUrl.openConnection();

		try {
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

			try {
				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"photo.png\"" + lineEnd);
				dos.writeBytes(lineEnd);

				int bytesAvailable = inputStream.available();
				int maxBufferSize = 1024;
				int bufferSize = Math.min(bytesAvailable, maxBufferSize);
				byte[] buffer = new byte[bufferSize];
				int bytesRead = inputStream.read(buffer, 0, bufferSize);
				while (bytesRead > 0) {
		        	if (Thread.currentThread().isInterrupted()) {
		        		throw new InterruptedIOException();
		        	}
					dos.write(buffer, 0, bufferSize);
					callbacks.onBytesWritten(bufferSize);
					bytesAvailable = inputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = inputStream.read(buffer, 0, bufferSize);
				}
				dos.writeBytes(lineEnd);
				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

				inputStream.close();
				dos.flush();

				String response = new String(StreamUtilities.readUntilEnd(conn.getInputStream()), "UTF-8");

				return response;
			} finally {
				dos.close();
			}
		} finally {
			conn.disconnect();
		}
	}

	private static interface UploadCallbacks {
		public void onBytesWritten(int size);
	}
}
