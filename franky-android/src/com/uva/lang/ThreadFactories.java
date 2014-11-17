package com.uva.lang;

import java.util.concurrent.ThreadFactory;

public class ThreadFactories {
	public static ThreadFactory WITH_LOWEST_PRIORITY = new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	};

	private ThreadFactories() {
		;
	}
}
