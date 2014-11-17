package com.gorecode.vk.task;

import java.util.WeakHashMap;

import android.os.AsyncTask;

public class AutoCancelPool {
	// FIXME: In this collection, key is weak! not value!
	private final WeakHashMap<Integer, AsyncTask<?, ?, ?>> mAsyncTasks = new WeakHashMap<Integer, AsyncTask<?, ?, ?>>();
	private final WeakHashMap<Integer, LongAction<?, ?>> mFrankyTasks = new WeakHashMap<Integer, LongAction<?, ?>>();

	public void add(LongAction<?, ?> action) {
		mFrankyTasks.put(action.hashCode(), action);
	}

	public void add(AsyncTask<?, ?, ?> task) {
		mAsyncTasks.put(task.hashCode(), task);
	}

	public void drain() {
		for (AsyncTask<?, ?, ?> each : mAsyncTasks.values()) {
			each.cancel(true);
		}

		mAsyncTasks.clear();

		for (LongAction<?, ?> each : mFrankyTasks.values()) {
			each.abort();
		}

		mFrankyTasks.clear();
	}
}
