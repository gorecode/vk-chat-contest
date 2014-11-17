//
// Copyright 2010 by UVaResearch Corp.
// Do not redistribute.
//

package com.uva.log;

import java.util.Calendar;
import java.util.Date;

import com.uva.lang.StringBufferUtilities;

/**
 * Formatter that formats Message into string in PPF log message format.
 * PPF message format is: DD:MM hh:mm:ss:iii threadName severity facility text
 * @author enikey.
 */
public class PPFFormatter implements Formatter {
	/**
	 * Creates PPF formatter.
	 */
	public PPFFormatter() {
	}

	/**
	 * {@inheritDoc}
	 */
	public String format(final Message message) {
		StringBuffer buffer = new StringBuffer(1024);

		buffer.append(formatDate(message.timestamp()));
		buffer.append(" ");
		buffer.append(message.threadName());
		buffer.append(" ");
		buffer.append(severityToString(message.severity()));
		buffer.append(" ");
		buffer.append(message.who());
		buffer.append(" ");
		buffer.append(message.text());

		return buffer.toString();
	}

	private String severityToString(int severity) {
		String severityString = null;

		switch (severity) {
		case Message.CRITICAL_ERROR:
			severityString = "CRIT";
			break;
		case Message.ERROR:
			severityString = "ERRR";
			break;
		case Message.WARNING:
			severityString = "WARN";
			break;
		case Message.INFORMATION:
			severityString = "INFO";
			break;
		case Message.DEBUG:
			severityString = "DEBG";
			break;
		case Message.TRACE:
			severityString = "TRAC";
			break;
		case Message.DUMP:
			severityString = "DUMP";
			break;
		}

		return severityString;
	}

	private String formatDate(long timeMillis) {
		_optNow.setTime(timeMillis);

		_optCalendar.setTime(_optNow);

		int month = _optCalendar.get(Calendar.MONTH);
		int day =  _optCalendar.get(Calendar.DAY_OF_MONTH);
		int hour = _optCalendar.get(Calendar.HOUR);
		int minute = _optCalendar.get(Calendar.MINUTE);
		int second = _optCalendar.get(Calendar.SECOND);
		int millisecond = _optCalendar.get(Calendar.MILLISECOND);

		// Date in MM:DD hh:mm:ss:SSS format.
		StringBuffer dateBuffer = new StringBuffer(18);

		StringBufferUtilities.append(dateBuffer, month + 1, 2);
		dateBuffer.append(":");
		StringBufferUtilities.append(dateBuffer, day, 2);
		dateBuffer.append(" ");
		StringBufferUtilities.append(dateBuffer, hour, 2);
		dateBuffer.append(":");
		StringBufferUtilities.append(dateBuffer, minute, 2);
		dateBuffer.append(":");
		StringBufferUtilities.append(dateBuffer, second, 2);
		dateBuffer.append(":");
		StringBufferUtilities.append(dateBuffer, millisecond, 3);

		return dateBuffer.toString(); 
	}

	/** Members for optimizing. */

	private final Date _optNow = new Date();

	private final Calendar _optCalendar = Calendar.getInstance();
}
