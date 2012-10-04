package com.iorga.webappwatcher.util;


public abstract class StartableRunnable implements Runnable {
	private boolean running = false;

	protected synchronized void begin() {
		running = true;
	}

	protected synchronized void end() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void start() {
		if (!running) {
			new Thread(this, getClass().getName()).start();
		}
	}
}
