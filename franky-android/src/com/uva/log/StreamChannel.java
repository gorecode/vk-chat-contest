package com.uva.log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StreamChannel implements Channel {
	private DataOutputStream mOutput;

	public StreamChannel(OutputStream output) {
		if (output == null) {
			throw new IllegalArgumentException("Output stream cannot be null");
		}

		mOutput = new DataOutputStream(output);
	}

	public void log(Message msg) {
		try {
			mOutput.write(msg.text().getBytes());
			mOutput.write("\n".getBytes());
			mOutput.flush();
		} catch (IOException e) {
			;
		}
	}

	public void close() {
		try {
			mOutput.close();
		} catch (IOException e) {
			;
		}
	}
}
