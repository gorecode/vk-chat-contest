package com.uva.net;

import java.io.IOException;

import com.uva.utilities.AssertCompat;

public abstract class PlatformSocketConnector {
	private static PlatformSocketConnector sConnector;

	public abstract AbstractSocketConnection connect(String host, int port, int timeout) throws IOException;

	public synchronized static void define(PlatformSocketConnector connector) {
		AssertCompat.notNull(connector, "Socket connector");

		if (sConnector != null) {
			throw new IllegalStateException("Socket connector already defined");
		}

		sConnector = connector;
	}

	public static PlatformSocketConnector getDefined() {
		if (sConnector == null) {
			throw new IllegalStateException("Socket connector not defined");
		}

		return sConnector;
	}
}
