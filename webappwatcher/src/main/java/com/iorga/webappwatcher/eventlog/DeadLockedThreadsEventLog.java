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
package com.iorga.webappwatcher.eventlog;

import java.io.Serializable;
import java.lang.management.ThreadInfo;

import org.apache.commons.lang.builder.ToStringBuilder;

public class DeadLockedThreadsEventLog extends EventLog {
	private static final long serialVersionUID = 1L;

	public static class LockInfo implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String className;
		private final int identityHashCode;

		public LockInfo(final java.lang.management.LockInfo lockInfo) {
			this.className = lockInfo.getClassName();
			this.identityHashCode = lockInfo.getIdentityHashCode();
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public String getClassName() {
			return className;
		}

		public int getIdentityHashCode() {
			return identityHashCode;
		}
	}

	public static class MonitorInfo extends LockInfo {
		private static final long serialVersionUID = 1L;

		private final int stackDepth;
		private final StackTraceElement stackFrame;

		public MonitorInfo(final java.lang.management.MonitorInfo monitorInfo) {
			super (monitorInfo);
			this.stackDepth = monitorInfo.getLockedStackDepth();
			this.stackFrame = monitorInfo.getLockedStackFrame();
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public int getStackDepth() {
			return stackDepth;
		}

		public StackTraceElement getStackFrame() {
			return stackFrame;
		}
	}

	public static class Thread extends SystemEventLog.Thread {
		private static final long serialVersionUID = 1L;

		private final MonitorInfo[] lockedMonitors;
		private final LockInfo[] lockedSynchronizers;

		public Thread(final ThreadInfo threadInfo, final long userTime, final long cpuTime) {
			super(threadInfo, userTime, cpuTime);
			final java.lang.management.MonitorInfo[] lockedMonitors = threadInfo.getLockedMonitors();
			if (lockedMonitors != null) {
				this.lockedMonitors = new MonitorInfo[lockedMonitors.length];
				for (int i = 0; i < lockedMonitors.length; i++) {
					this.lockedMonitors[i] = new MonitorInfo(lockedMonitors[i]);
				}
			} else {
				this.lockedMonitors = null;
			}
			final java.lang.management.LockInfo[] lockedSynchronizers = threadInfo.getLockedSynchronizers();
			if (lockedSynchronizers != null) {
				this.lockedSynchronizers = new LockInfo[lockedSynchronizers.length];
				for (int i = 0; i < lockedSynchronizers.length; i++) {
					this.lockedSynchronizers[i] = new LockInfo(lockedSynchronizers[i]);
				}
			} else {
				this.lockedSynchronizers = null;
			}
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public MonitorInfo[] getLockedMonitors() {
			return lockedMonitors;
		}

		public LockInfo[] getLockedSynchronizers() {
			return lockedSynchronizers;
		}
	}

	private DeadLockedThreadsEventLog() {
		super();
	}

	protected Thread[] deadLockedThreads;


	public Thread[] getDeadLockedThreads() {
		return deadLockedThreads;
	}

	public void setDeadLockedThreads(final Thread[] deadLockedThreads) {
		this.deadLockedThreads = deadLockedThreads;
	}
}
