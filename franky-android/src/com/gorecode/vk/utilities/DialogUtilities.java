package com.gorecode.vk.utilities;

import android.app.Dialog;

public class DialogUtilities {
	public static void dismissSafely(Dialog dialog) {
		try {
			if (dialog != null && dialog.getWindow() != null) dialog.dismiss();
		} catch (IllegalArgumentException e) {
			// XXX: Just consume "View not attached to window manager" exception.
		}
	}
}
