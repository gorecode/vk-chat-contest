package com.gorecode.vk.utilities;

import com.google.common.base.Preconditions;

import android.os.Handler;
import android.os.Looper;

public class UiThreadRunner {
	private final Thread mUiThread = Thread.currentThread();
	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private static volatile UiThreadRunner sRunner;

	public static void setGlobalRunner(UiThreadRunner runner) {
		Preconditions.checkState(sRunner == null);
		Preconditions.checkNotNull(runner);

		sRunner = runner;
	}

	public static UiThreadRunner get() {
		Preconditions.checkNotNull(sRunner);

		return sRunner;
	}

	public void runOnUiThread(Runnable action) {
		if (Thread.currentThread() != mUiThread) {
			mHandler.post(action);
		} else {
			action.run();
		}
	}
}
