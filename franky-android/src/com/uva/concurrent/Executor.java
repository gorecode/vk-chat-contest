package com.uva.concurrent;

public interface Executor {
	public void execute(Runnable target);
}
