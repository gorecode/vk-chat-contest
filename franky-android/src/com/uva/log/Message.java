//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

/**
 * A class represents single Log Message.
 *
 * @author enikey.
 */
final public class Message {
	/**
	 * Represents SDK critical error.
	 */
	public static final int CRITICAL_ERROR = 0x0;
	/**
	 * Represents SDK error.
	 */
	public static final int ERROR = 0x1;
	/**
	 * Represents warning.
	 */
	public static final int WARNING = 0x2;
	/**
	 * Represents message.
	 */
	public static final int INFORMATION = 0x3;
	/**
	 * Represents debug message.
	 */
	public static final int DEBUG = 0x4;
	/**
	 * Represents debug trace message.
	 */
	public static final int TRACE = 0x5;
	/**
	 * Represents debug dump.
	 */
	public static final int DUMP = 0x6;
	/**
	 * Creates message with given parameters.
	 * @param who name of object who is creating this message.
	 * @param text message text.
	 * @param severity severity of message.
	 * @param threadName current thread name.
	 */
	public Message(String who, String text, int severity, String threadName) {
		_who = who;
		_text = text;
		_severity = severity;
		_timestamp = System.currentTimeMillis();
		_threadName = threadName;
	}
	/**
	 * Returns message severity.
	 * @return message severity.
	 */
	public int severity() {
		return _severity;
	}
	/**
	 * Returns message text.
	 * @return message text.
	 */
	public String text() {
		return _text;
	}
	/**
	 * Returns name of object who added message.
	 * @return name of object who added message
	 */
	public String who() {
		return _who;
	}
	/**
	 * Returns timestamp of message.
	 * @return time of message creation in milliseconds since unix epoch.
	 */
	public long timestamp() {
		return _timestamp;
	}
	/**
	 * Returns name of thread created this message.
	 */
	public String threadName() {
		return _threadName;
	}
	private long _timestamp;
	private int _severity;
	private String _who;
	private String _text;
	private String _threadName;
}
