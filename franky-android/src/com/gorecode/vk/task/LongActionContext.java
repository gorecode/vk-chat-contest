package com.gorecode.vk.task;

public class LongActionContext<Params, Result> {
	public Params input;
	public Result result;
	public Exception error;

	public boolean isCompletedSuccessfuly() {
		return error == null;
	}
}
