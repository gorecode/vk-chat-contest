package com.gorecode.vk.utilities;

import android.content.Context;
import android.widget.Toast;

public class ErrorHandlingUtilities {
	public static void displayErrorSoftly(Context context, Throwable error, ErrorAnalyzer analyzer) {
		displayErrorSoftly(context, context.getString(analyzer.getMessageResId(error)));
	}

	public static void displayErrorSoftly(Context context, Throwable cause) {
		displayErrorSoftly(context, cause, GenericErrorAnalyzer.getInstance());
	}

	public static void displayErrorSoftly(Context context, String message) {
		displayErrorSoftly(context, message, null);
	}
	
	public static void displayErrorSoftly(Context context, int messageId) {
		displayErrorSoftly(context, context.getString(messageId));
	}

	public static void displayErrorSoftly(Context context, int messageId, Throwable cause) {
		displayErrorSoftly(context, context.getString(messageId), cause);
	}
	
	public static void displayErrorSoftly(Context context, String message, Throwable cause) {
		int toastDuration = Toasts.getDurationForMessage(message);

		Toast.makeText(context, message, toastDuration).show();
	}

	private ErrorHandlingUtilities() {
		;
	}
}
