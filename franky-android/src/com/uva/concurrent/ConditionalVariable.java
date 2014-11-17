package com.uva.concurrent;

public class ConditionalVariable {
	private final Object awaitingMonitor = new Object();

	private boolean value;

	public ConditionalVariable(boolean value) {
		this.value = value;
	}

	public synchronized void set(boolean value) {
		this.value = value;

		synchronized (awaitingMonitor) {
			awaitingMonitor.notify();
		}
	}

	public synchronized boolean get() {
		return value;
	}

	public boolean waitFor(boolean expected, long timeout) throws InterruptedException {
		long sleepStartTime = System.currentTimeMillis();

		synchronized (awaitingMonitor) {
			while (get() != expected) {
				final long timeInSleep = System.currentTimeMillis() - sleepStartTime;
				final long timeToWait = Math.max(0, timeout - timeInSleep);

				if (timeToWait == 0) {
					break;
				}

				awaitingMonitor.wait(timeToWait);
			}
		}
		return get() == expected;
	}

	public void waitFor(boolean expected) throws InterruptedException {
		synchronized (awaitingMonitor) {
			while (get() != expected) {
				awaitingMonitor.wait();
			}
		}
	}
}
