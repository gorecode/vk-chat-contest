//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

/**
 * A log channel that prints log message to console.
 *
 * @author enikey.
 */
public class ConsoleChannel implements Channel {
	/**
	 * Creates console channel.
	 */
	public ConsoleChannel() {
		_closed = false;
	}

	/**
	 * {@inheritDoc}
	 */
	public void log(Message msg) {
		if (!_closed) {
			System.out.println(msg.text());
		}
	}

	/**
	 * Closes logging channel.
	 */
	public void close() {
		_closed = true;
	}

	/** Determies if channel is closed. */
	private boolean _closed;
}
