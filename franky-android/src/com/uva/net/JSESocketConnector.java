package com.uva.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.uva.net.AbstractSocketConnection;
import com.uva.net.PlatformSocketConnector;

public class JSESocketConnector extends PlatformSocketConnector {
	public AbstractSocketConnection connect(String host, int port, int timeout) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host, port), timeout);
		return new JSESocketConnection(socket);
	}
}
