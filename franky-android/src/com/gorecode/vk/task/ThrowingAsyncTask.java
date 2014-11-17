package com.gorecode.vk.task;

import android.os.AsyncTask;
import android.util.Pair;

public abstract class ThrowingAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Pair<Result, Exception>> {
	@Override
	final protected Pair<Result, Exception> doInBackground(Params... params) {
		try {
			return Pair.create(doInBackgroundOrThrow(params), (Exception)null);
		} catch (Exception e) {
			return Pair.create((Result)null, e);
		}
	}

	abstract protected Result doInBackgroundOrThrow(Params... params) throws Exception;
}
