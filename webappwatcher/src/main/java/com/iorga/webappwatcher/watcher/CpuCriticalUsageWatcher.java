package com.iorga.webappwatcher.watcher;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.eventlog.DeadLockedThreadsEventLog;
import com.iorga.webappwatcher.eventlog.DeadLockedThreadsEventLog.Thread;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

public class CpuCriticalUsageWatcher {
	private static final Logger log = LoggerFactory.getLogger(CpuCriticalUsageWatcher.class);

	private float criticalCpuUsage = Math.min(70f, 100f / ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() * 1.5f);	// Default to over 1.5 processors
	private long deadLockThreadsSearchDeltaMillis = 5 * 60 * 1000; // Default every 5mn
	private Date lastDeadLockThreadsSearchDate = new Date(0);
	private final long infoMessagesDeltaMillis = 5 * 60 * 1000; // Displays info messages every 5mn at least
	private Date lastInfoMessageDate = new Date(0);


	@Subscribe
	public void onEvent(final SystemEventLog eventLog) {
		if (eventLog.getCpuUsage() > criticalCpuUsage) {
			try {
				final Date currentDate = new Date();
				final EventLogManager eventLogManager = EventLogManager.getInstance();
				if (currentDate.getTime() - lastDeadLockThreadsSearchDate.getTime() > deadLockThreadsSearchDeltaMillis) {
					// Search for deadLockedThread and log them
					final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
					final long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
					if (deadlockedThreadIds != null) {
						final DeadLockedThreadsEventLog deadLockedThreadsEventLog = eventLogManager.addEventLog(DeadLockedThreadsEventLog.class);
						final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
						final Thread[] deadLockedThreads = new Thread[threadInfos.length];
						for (int i = 0; i < threadInfos.length; i++) {
							deadLockedThreads[i] = new Thread(threadInfos[i]);
						}
						deadLockedThreadsEventLog.setDeadLockedThreads(deadLockedThreads);
						eventLogManager.fire(deadLockedThreadsEventLog);
					}
					lastDeadLockThreadsSearchDate = currentDate;
				}

				if (currentDate.getTime() - lastInfoMessageDate.getTime() > infoMessagesDeltaMillis) {
					log.info("CPU usage "+eventLog.getCpuUsage()+" over "+criticalCpuUsage+", writing log. (That message will not be repeated for "+infoMessagesDeltaMillis+" millis)");
					lastInfoMessageDate = currentDate;
				}
				eventLogManager.writeRetentionLog();
			} catch (final IOException e) {
				log.warn("Couldn't write retention log", e);
			}
		}
	}

	public float getCriticalCpuUsage() {
		return criticalCpuUsage;
	}

	public void setCriticalCpuUsage(final float criticalCpuUsage) {
		this.criticalCpuUsage = criticalCpuUsage;
	}

	public long getDeadLockThreadsSearchDeltaMillis() {
		return deadLockThreadsSearchDeltaMillis;
	}

	public void setDeadLockThreadsSearchDeltaMillis(final long deadLockThreadsSearchDeltaMillis) {
		this.deadLockThreadsSearchDeltaMillis = deadLockThreadsSearchDeltaMillis;
	}

}
