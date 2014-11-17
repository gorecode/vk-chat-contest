package com.uva.concurrent;

public class ObjectHolder {
	public static final Object NO_VALUE = new Object();

	private Object value = NO_VALUE;

	public ObjectHolder() {
		;
	}

	public ObjectHolder(Object value) {
		setValue(value);
	}

	public boolean hasValue() {
		return getValue() != NO_VALUE;
	}

	public Object waitForValue() throws InterruptedException {
		synchronized (this) {
			while (NO_VALUE == value) {
				wait();
			}
			return value;
		}
	}

	public Object getValue() {
		synchronized (this) {
			return value;
		}
	}

	public void setValue(Object value) {
		synchronized (this) {
			this.value = value;

			notifyAll();
		}
	}
}
