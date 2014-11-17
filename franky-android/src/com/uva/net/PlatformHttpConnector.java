package com.uva.net;

import com.uva.utilities.AssertCompat;

public final class PlatformHttpConnector {
	private static HttpConnector s_connector;

	private PlatformHttpConnector() {
		;
	}

	public static void define(HttpConnector connector) {
		AssertCompat.notNull(connector, "Http connector");

		if (s_connector != null) {
			throw new IllegalStateException("Http connection already defined");
		}

		s_connector = connector;
	}

	public static HttpConnector getDefined() {
		if (s_connector == null) {
			throw new IllegalStateException("Http connection not defined");
		}
		return s_connector;
	}
}
