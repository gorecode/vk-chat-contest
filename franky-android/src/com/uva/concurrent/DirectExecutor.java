package com.uva.concurrent;

public class DirectExecutor implements Executor {
	public void execute(Runnable target) {
		target.run();
	}
}
