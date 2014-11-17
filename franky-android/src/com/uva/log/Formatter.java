//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

/**
 * Basic message formatter.
 *
 * A Formatter takes Message object and formattes it into string.
 *
 * @author enikey.
 */
public interface Formatter {
	/**
	 * Formats given message into string.
	 * @param message the message.
	 * @return formatter string created from given message.
	 */
	public String format(final Message message);
}
