package com.uva.concurrent;

public interface AbstractScheduler {
	public void schedule(Runnable target);
	public void scheduleDelayed(Runnable target, final long delay);
	public void scheduleAt(Runnable target, final long atTime);
	public boolean cancel(Runnable target);
}
