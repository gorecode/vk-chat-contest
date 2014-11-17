package com.uva.concurrent;

import java.util.concurrent.ExecutorService;

import com.uva.utilities.AssertCompat;

public class ExecutorServiceAdapter implements Executor {
	private final ExecutorService service;

	public ExecutorServiceAdapter(ExecutorService service) {
		AssertCompat.notNull(service, "Executor service");

		this.service = service;
	}

	public void execute(Runnable target) {
		service.execute(target);
	}
}
