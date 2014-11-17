package com.uva.log;

/**
 * Formats exception using exception.getClassName():exception.getMessage().
 *
 * @author vvs.
 */
public class SimpleExceptionFormatter implements ExceptionFormatter {
	public static String formatThrowable(Throwable exception) {
		if (exception == null) return "Exception: null";
		
		// get simple file name
		String name = exception.getClass().getName();
		name = name.substring(name.lastIndexOf('.') + 1);
		
		return name + ": \"" + exception.getMessage() + "\"";
	}

	/**
	 * {@inheritDoc}
	 */
	public String format(Throwable exception) {
		return formatThrowable(exception);
	}
}
