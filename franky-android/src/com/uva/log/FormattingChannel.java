//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

/**
 * Filter channel, that routes message to formatter before passing it on to the destination channel.
 *
 * @author enikey.
 */
public class FormattingChannel implements Channel {
	/**
	 * Creates new formatter channel.
	 * @param formatter message formatter.
	 * @param channel destination channel.
	 * @throws IllegalArgumentException when formatter or channel is null.
	 */
	public FormattingChannel(Formatter formatter, Channel channel) {
		if (formatter == null) {
			throw new IllegalArgumentException("Formatter is null");
		}
		if (channel == null) {
			throw new IllegalArgumentException("Channel is null");
		}
		_formatter = formatter;
		_channel = channel;
	}

	/**
	 * {@inheritDoc}
	 */
	public void log(Message msg) {
		_channel.log(new Message(msg.who(), _formatter.format(msg), msg.severity(), msg.threadName()));
	}

	/**
	 * Closes destination channel.
	 */
	public void close() {
		_channel.close();
	}

	/** Message formatter. */
	private Formatter _formatter;
	/** Destination channel. */
	private Channel _channel;
}
