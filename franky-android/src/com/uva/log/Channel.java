//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

/**
 * Logging channel.
 *
 * @author enikey.
 */
public interface Channel {
	/**
	 * Processes log message.
	 * @param msg log message.
	 */
	public void log(Message msg);
	/**
	 * Closes logging channel.
	 */
	public void close();
}
