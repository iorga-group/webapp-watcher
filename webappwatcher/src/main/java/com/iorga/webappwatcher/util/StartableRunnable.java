/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
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
