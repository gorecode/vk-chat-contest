package com.uva.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.uva.utilities.AssertCompat;

public class JSESocketConnection implements AbstractSocketConnection {
	private final Socket mSocket;

	public JSESocketConnection(Socket connectedSocket) {
		AssertCompat.notNull(connectedSocket, "Socket");

		mSocket = connectedSocket;
	}

	public void setSoTimeout(int timeout) throws IOException {
		mSocket.setSoTimeout(timeout);
	}

	public int getSoTimeout() throws IOException {
		return mSocket.getSoTimeout();
	}

	public InputStream openInputStream() throws IOException {
		return mSocket.getInputStream();
	}

	public OutputStream openOutputStream() throws IOException {
		return mSocket.getOutputStream();
	}

	public void close() throws IOException {
		try {
			mSocket.shutdownInput();
		} catch (IOException e) {
			;
		}
		try {
			mSocket.shutdownOutput();
		} catch (IOException e) {
			;
		}
		mSocket.close();
	}
}
