package com.uva.log;

/**
 * An abstract formatter of exception.
 *
 * @author enikey.
 */
public interface ExceptionFormatter {
	/**
	 * Formats given exception to string.
	 * @param exception exception to format.
	 * @return formatted string with message description. 
	 */
	public String format(Throwable exception);
}
