//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

import java.util.Vector;

/**
 * A log channel that allows to write message into multiply channels.
 *
 * @author enikey, vvs.
 */
public class SplitterChannel implements Channel {
	/**
	 * Creates splitter channel.
	 */
	public SplitterChannel() {
		_channels = new Vector();
	}

	/**
	 * Initializes splitter with collection of channels.
	 * @param channels Collection of channels, will be added to this splitter.
	 */
	public SplitterChannel(Channel[] channels) {
		this();
		if (channels == null) throw new IllegalArgumentException();	  

		// add all the channels
		for (int i = 0; i < channels.length; i++) {
			addChannel(channels[i]);
		}
	}

	/**
	 * Adds channel to chain.
	 * @param channel log channel.
	 */
	public void addChannel(Channel channel) {
		_channels.addElement(channel);
	}

	/**
	 * Removes channel from chain.
	 * @param channel log chain to remove.
	 */
	public void removeChannel(Channel channel) {
		_channels.removeElement(channel);
	}

	/**
	 * {@inheritDoc}
	 */
	public void log(Message msg) {
		for (int i = 0; i < _channels.size(); i++) {
			((Channel)_channels.elementAt(i)).log(msg);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		for (int i = 0; i < _channels.size(); i++) {
			((Channel)_channels.elementAt(i)).close();
		}
	}

	/** Underlying channels. */
	private Vector _channels;
}
