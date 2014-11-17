package com.uva.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class JSEExceptionFormatter implements ExceptionFormatter {
	@Override
	public String format(Throwable t) {		
		if (t == null) return "null";

		return getStackTrace(t);
	}

	public static String getStackTrace(Throwable t) {
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    t.printStackTrace(printWriter);
    return result.toString();
  }
}
