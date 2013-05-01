package com.iorga.webappwatcher.eventlog;

import java.io.Serializable;
import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;


public class SystemEventLog extends EventLog {
	private static final long serialVersionUID = 1L;

	public static class Thread implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String name;
		private final long id;
		private final StackTraceElement[] stackTrace;
		private final State state;
		private long userTime;
		private long cpuTime;

		public Thread(final ThreadInfo threadInfo, final long userTime, final long cpuTime) {
			name = threadInfo.getThreadName();
			id = threadInfo.getThreadId();
			stackTrace = threadInfo.getStackTrace();
			state = threadInfo.getThreadState();
			this.userTime = userTime;
			this.cpuTime = cpuTime;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public String getName() {
			return name;
		}
		public long getId() {
			return id;
		}
		public StackTraceElement[] getStackTrace() {
			return stackTrace;
		}
		public State getState() {
			return state;
		}
		public long getUserTime() {
			return userTime;
		}
		public void setUserTime(final long userTime) {
			this.userTime = userTime;
		}
		public long getCpuTime() {
			return cpuTime;
		}
		public void setCpuTime(final long cpuTime) {
			this.cpuTime = cpuTime;
		}
	}

	protected SystemEventLog() {
		super();
	}

	public SystemEventLog(final Date date) {
		super(date);
	}

	private float cpuUsage;
	private long heapMemoryUsed;
	private long nonHeapMemoryUsed;
	private Thread[] blockedOrRunningThreads;
	private int threadCount;
	private int peakThreadCount;
	private long uptime;
	private long processCpuTime;
	private int availableProcessors;

	public float getCpuUsage() {
		return cpuUsage;
	}
	public void setCpuUsage(final float cpuUsage) {
		this.cpuUsage = cpuUsage;
	}
	public long getHeapMemoryUsed() {
		return heapMemoryUsed;
	}
	public void setHeapMemoryUsed(final long heapMemoryUsed) {
		this.heapMemoryUsed = heapMemoryUsed;
	}
	public long getNonHeapMemoryUsed() {
		return nonHeapMemoryUsed;
	}
	public void setNonHeapMemoryUsed(final long nonHeapMemoryUsed) {
		this.nonHeapMemoryUsed = nonHeapMemoryUsed;
	}
	public Thread[] getBlockedOrRunningThreads() {
		return blockedOrRunningThreads;
	}
	public void setBlockedOrRunningThreads(final Thread[] blockedOrRunningThreads) {
		this.blockedOrRunningThreads = blockedOrRunningThreads;
	}
	public int getThreadCount() {
		return threadCount;
	}
	public void setThreadCount(final int threadCount) {
		this.threadCount = threadCount;
	}
	public int getPeakThreadCount() {
		return peakThreadCount;
	}
	public void setPeakThreadCount(final int peakThreadCount) {
		this.peakThreadCount = peakThreadCount;
	}
	public long getUptime() {
		return uptime;
	}
	public void setUptime(final long uptime) {
		this.uptime = uptime;
	}
	public long getProcessCpuTime() {
		return processCpuTime;
	}
	public void setProcessCpuTime(final long processCpuTime) {
		this.processCpuTime = processCpuTime;
	}
	public int getAvailableProcessors() {
		return availableProcessors;
	}
	public void setAvailableProcessors(final int availableProcessors) {
		this.availableProcessors = availableProcessors;
	}
}
