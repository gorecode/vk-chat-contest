package com.gorecode.vk.application;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import com.uva.log.Log;

public class EpicFailHandler implements UncaughtExceptionHandler {
	private static final String TAG = "LoggingUncaughtExceptionHandler";

	private final UncaughtExceptionHandler mSystemHandler;

	public EpicFailHandler() {
		this(null);
	}

	public EpicFailHandler(UncaughtExceptionHandler systemHandler) {
		mSystemHandler = systemHandler;
	}

	private static String getExceptionStackTrace(Throwable exception) {
		StringWriter writer = new StringWriter();		
		exception.printStackTrace(new PrintWriter(writer));		
		return writer.toString();
	}

	public void uncaughtException(Thread thread, Throwable ex) {
		Log.critical(TAG, "Unhandled exception: \n" + getExceptionStackTrace(ex));

		if (mSystemHandler != null) {
			mSystemHandler.uncaughtException(thread, ex);
		}
	}
}
