package com.uva.log;

import com.uva.log.Channel;
import com.uva.log.Message;

import android.util.Log;

/**
 * Uses android.util.Log for log output.<p>
 * <i>Created for debugging purposes.</i>
 * @author enikey.
 * @category debug.
 */
public class AndroidNativeLogChannel implements Channel {
	/**
	 * {@inheritDoc}
	 */
	public void log(Message msg) {
		switch (msg.severity()) {
		case Message.CRITICAL_ERROR:
		case Message.ERROR:
			Log.e(msg.who(), msg.text());
			break;
		case Message.WARNING:
			Log.w(msg.who(), msg.text());
			break;
		case Message.INFORMATION:
			Log.i(msg.who(), msg.text());
			break;
		case Message.DEBUG:
			Log.d(msg.who(), msg.text());
			break;
		default:
			Log.v(msg.who(), msg.text());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void close() {
		; // Do nothing.
	}
}
