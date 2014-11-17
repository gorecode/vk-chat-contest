//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

import com.uva.utilities.AssertCompat;

/**
 * API for sending log output.
 * <p>
 * Generally, use the message(), debug(), trace(), dump(), warning(), error(), critical() methods.
 * <p>
 * <b>Tip:</b> A good convention is to declare a TAG constant in your class:<br>
 * <code>
 * private static final String TAG = "MyClass";
 * </code><br>
 * and use that in subsequent calls to the log methods.
 * <p>
 * <b>Advanced:</b> An application uses static methods of the Log class to generate its
 * log messages and send them on their way to their final destination,
 * their final destination is a channel, to which Log passes on its messages.
 * Furthermore, it has a log level, which is used for filtering messages based on their priority.
 * Only messages with a priority equal to or higher than the specified level are passed on.<br>
 * For example, if the level of a logger is set to WARNING, only messages with priority WARNING, ERROR, and CRITICAL will propagate.
 * <p>
 * <b>Configuration:</b><p>
 * Use the setChannel() and getChannel() methods to set and access the destination channel.<br>
 * Use the setLogLevel() and getLogLevel() methods to set and access the log level.<br>
 * Use the setMessageFactory() and getMessageFactory() methods to set and access a log message factory().<br>
 * <p>
 * <b><i>Default log configuration:</i></b><p>
 * <li>Log level: Message.DUMP.</li>
 * <li>Channel: new FormattingChannel(new PPFFormatter(), new ConsoleChannel()).</li>
 * <li>MessageFactory: new MessageFactory()</li>
 * @see com.zema.log.Channel
 * @see com.VkMessage.log.Message
 * @author enikey, vvs.
 */
final public class Log {
	private static Channel s_channel = new ConsoleChannel();
	private static ExceptionFormatter s_exceptionFormatter = new SimpleExceptionFormatter();
	private static int s_maxAcceptableSeverity = Message.ERROR;

	/**
	 * Initializes log with given channel and log level.
	 * Method must be called first before using Log.
	 * @param channel log channel, optional, if null ConsoleChannel will be used.
	 * @param level log level, can be one of Message.* severity constants.
	 * @throws IllegalArgumentException when arguments are invalid.
	 */ 
	public static synchronized void initialize(Channel channel, ExceptionFormatter exceptionFormatter, int level) {
		AssertCompat.notNull(channel, "Channel");
		AssertCompat.notNull(exceptionFormatter, "Exception formatter");

		s_channel = channel;
		s_exceptionFormatter = exceptionFormatter;
		s_maxAcceptableSeverity = level;
	}	

	public static synchronized void setSeverityFilter(int severity) {
		s_maxAcceptableSeverity = severity;
	}

	/**
	 * Sets specific formatter for exceptions. Used to supply more information 
	 * about exceptions under particular platform.
	 * @param formatter Formatter to set (override base formatter format 
	 * method). Should not be null. 
	 */
	public static void setExceptionFormatter(ExceptionFormatter formatter) {
		AssertCompat.notNull(formatter, "Exception formatter");

		s_exceptionFormatter = formatter;
	}

	public static synchronized boolean shouldBeLogged(int severity) {
		return (severity <= s_maxAcceptableSeverity);
	}

	/**
	 * Logs message.
	 * @param facility caller's facility.
	 * @param text message text.
	 */
	public static void message(String facility, String text) {
		log(facility, Message.INFORMATION, text);
	}

	/**
	 * Logs warning.
	 * @param facility caller's facility.
	 * @param text message text.
	 */
	public static void warning(String facility, String text) {
		log(facility, Message.WARNING, text);
	}

	/**
	 * Logs error.
	 * @param facility caller's facility.
	 * @param text message text.
	 */
	public static void error(String facility, String text) {
		log(facility, Message.ERROR, text);
	}

	/**
	 * Logs error.
	 * @param facility caller's facility.
	 * @param text message text.
	 */
	public static void critical(String facility, String text) {
		log(facility, Message.CRITICAL_ERROR, text);
	}

	/**
	 * Logs debug output.
	 * @param facility caller's facility.
	 * @param text message text.
	 */
	public static void debug(String facility, String text) {
		log(facility, Message.DEBUG, text);
	}

	/**
	 * Logs binary dump.
	 * @param facility caller's facility.
	 * @param name name of dump.
	 * @param bytes bytes to dump.
	 */
	public static void binaryDump(String facility, String name, byte[] bytes) {
		if (s_maxAcceptableSeverity < Message.DUMP) {
			return;
		}
		log(facility, Message.DUMP, name + " (" + bytes.length + ") = " + BytesFormatter.format(bytes));
	}

	/**
	 * Traces log message.
	 * @param facility caller facility.
	 * @param message the log message.
	 */
	public static void trace(String facility, String message) {
		log(facility, Message.TRACE, message);
	}

	/**
	 * Logs given message using Log channel or rejects if log level of message is more that log level of Log.
	 * @param facility facility of caller.
	 * @param severity message severity.
	 * @param text message text.
	 */
	public static void log(String facility, int severity, String text) {
		if (!shouldBeLogged(severity)) return;

		Message message = new Message(facility, text, severity, Thread.currentThread().getName());

		s_channel.log(message);
	}

	/**
	 * Prints detailed information about exception to log using given severity.
	 * 
	 * Also this method has sweet feature: it can print exception source, if it
	 * located in this package. Feature implementation uses following assumption:
	 *     Log package and error source package should differ no more than one 
	 * domain. For example: com.uva.logging and com.uva.logic - fine,
	 * com.uva.logging and com.google - not.
	 *     If feature doesn't work or work incorrect, remove it.
	 * 
	 * @param facility facility facility facility of caller.
	 * @param severity exception severity, can be on of constants from Message class.
	 * Message.Debug, for example.
	 * @param exception exception to print info about.
	 */
	public static void exception(String facility, int severity, Throwable error) {
		exception(facility, severity, null, error);
	}
	
	/**
	 * Prints detailed information about exception to log using Message.ERROR severity.
	 * @param facility facility facility of caller.
	 * @param exception exception to print info about.
	 */
	public static void exception(String facility, Throwable error) {
		exception(facility, error.getMessage(), error);
	}	

	public static void exception(String tag, int severity, String message, Throwable e) {
		if (message != null) {
			log(tag, severity, message + ", e = " + s_exceptionFormatter.format(e));
		} else {
			log(tag, severity, s_exceptionFormatter.format(e));
		}
	}

	public static void exception(String tag, String message, Throwable e) {
		exception(tag, Message.ERROR, message, e);
	}

	/** Disallow creating instances of class.*/
	private Log() { }
}
