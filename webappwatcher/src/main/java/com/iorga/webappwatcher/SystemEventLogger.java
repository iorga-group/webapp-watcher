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
package com.iorga.webappwatcher;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.iorga.webappwatcher.eventlog.SystemEventLog;
import com.iorga.webappwatcher.util.PatternUtils;
import com.sun.management.OperatingSystemMXBean;

public class SystemEventLogger {
	private long cpuComputationDeltaMillis = 500;

	private long prevUptime = 0L;
	private long prevProcessCpuTime = 0L;

	private boolean runLogLoop = false;

	private List<Pattern> threadNameIncludes;
	private List<Pattern> threadNameExcludes;

	{
		// By default, we only log http*
		threadNameIncludes = new ArrayList<Pattern>();
		threadNameIncludes.add(Pattern.compile("http.*"));
	}

	private final Runnable loop = new Runnable() {
		@Override
		public void run() {
			try {
				while (runLogLoop) {
					try {
						final EventLogManager eventLogManager = EventLogManager.getInstance();
						// Code inspiré de http://knight76.blogspot.fr/2009/05/how-to-get-java-cpu-usage-jvm-instance.html et http://www.docjar.com/html/api/sun/tools/jconsole/SummaryTab$Result.java.html
						final SystemEventLog systemEventLog = eventLogManager.addEventLog(SystemEventLog.class);
						final OperatingSystemMXBean osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
						final long processCpuTime = osMXBean.getProcessCpuTime();
						final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
						final long uptime = runtimeMXBean.getUptime();
						final int availableProcessors = osMXBean.getAvailableProcessors();
						final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
						systemEventLog.setHeapMemoryUsed(memoryMXBean.getHeapMemoryUsage().getUsed());
						systemEventLog.setNonHeapMemoryUsed(memoryMXBean.getNonHeapMemoryUsage().getUsed());
						final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
						systemEventLog.setThreadCount(threadMXBean.getThreadCount());
						systemEventLog.setPeakThreadCount(threadMXBean.getPeakThreadCount());

						// Compute the cpuUsage
						if (prevUptime > 0L && uptime > prevUptime) {
							// elapsedCpu is in ns and elapsedTime is in ms.
							final long elapsedCpu = processCpuTime - prevProcessCpuTime;
							final long elapsedTime = uptime - prevUptime;
							// cpuUsage could go higher than 100% because elapsedTime
							// and elapsedCpu are not fetched simultaneously. Limit to
							// 99% to avoid Plotter showing a scale from 0% to 200%.
							systemEventLog.setCpuUsage(Math.min(100F, elapsedCpu / (elapsedTime * 10000F * availableProcessors))); // elapsedCpu / (elapsedTime * 1000F * 1000F * availableProcessors)) * 100 => pour l'avoir en %
						}
						systemEventLog.setUptime(uptime);
						systemEventLog.setProcessCpuTime(processCpuTime);
						systemEventLog.setAvailableProcessors(availableProcessors);

						// We now log all threads in RUNNABLE or BLOCKED state
						final ThreadInfo[] allThreads = threadMXBean.dumpAllThreads(false, false);
						final List<SystemEventLog.Thread> loggedThreads = new LinkedList<SystemEventLog.Thread>();
						for (final ThreadInfo threadInfo : allThreads) {
							if (threadInfo != null) {	// It seems that sometime (with JRockit) threadInfo is null
								final State threadState = threadInfo.getThreadState();
								if ((threadState == State.BLOCKED || threadState == State.RUNNABLE) && PatternUtils.matches(threadInfo.getThreadName(), threadNameIncludes, threadNameExcludes)) {
									final long threadId = threadInfo.getThreadId();
									loggedThreads.add(new SystemEventLog.Thread(threadInfo, threadMXBean.getThreadUserTime(threadId), threadMXBean.getThreadCpuTime(threadId)));
								}
							}
						}
						systemEventLog.setBlockedOrRunningThreads(loggedThreads.toArray(new SystemEventLog.Thread[loggedThreads.size()]));

						eventLogManager.fire(systemEventLog);
						SystemEventLogger.this.prevUptime = uptime;
						SystemEventLogger.this.prevProcessCpuTime = processCpuTime;
						Thread.sleep(cpuComputationDeltaMillis);
					} catch (final InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			} finally {
				stop();
			}
		}
	};

	public synchronized void start() {
		if (!runLogLoop) {
			runLogLoop = true;
			new Thread(loop, SystemEventLogger.class.getName()).start();
		}
	}

	public synchronized void stop() {
		runLogLoop = false;
	}


	public long getCpuComputationDeltaMillis() {
		return cpuComputationDeltaMillis;
	}

	public void setCpuComputationDeltaMillis(final long cpuComputationDeltaMillis) {
		this.cpuComputationDeltaMillis = cpuComputationDeltaMillis;
	}

	public List<Pattern> getThreadNameIncludes() {
		return threadNameIncludes;
	}

	public void setThreadNameIncludes(final List<Pattern> threadNameIncludes) {
		this.threadNameIncludes = threadNameIncludes;
	}

	public List<Pattern> getThreadNameExcludes() {
		return threadNameExcludes;
	}

	public void setThreadNameExcludes(final List<Pattern> threadNameExcludes) {
		this.threadNameExcludes = threadNameExcludes;
	}

}
