package com.iorga.webappwatcher;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.webappwatcher.eventlog.DeadLockedThreadsEventLog;
import com.iorga.webappwatcher.eventlog.DeadLockedThreadsEventLog.Thread;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

public class CpuCriticalUsageWatcher implements EventLogListener<SystemEventLog> {
	private static final Logger log = LoggerFactory.getLogger(CpuCriticalUsageWatcher.class);

	private final float criticalCpuUsage = 1F;	//TODO mettre en paramètre
	private final long deadLockThreadsSearchDeltaMillis = 5 * 60 * 1000; // Default every 5mn	//TODO mettre en paramètre
	private Date lastDeadLockThreadsSearch = new Date(0);

	@Override
	public Class<SystemEventLog> getListenedEventLogType() {
		return SystemEventLog.class;
	}

	@Override
	public void onFire(final SystemEventLog eventLog) {
		if (eventLog.getCpuUsage() > criticalCpuUsage) {
			try {
				final Date currentDate = new Date();
				final EventLogManager eventLogManager = EventLogManager.getInstance();
				if (currentDate.getTime() - lastDeadLockThreadsSearch.getTime() > deadLockThreadsSearchDeltaMillis) {
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
					lastDeadLockThreadsSearch = currentDate;
				}

				log.info("CPU usage "+eventLog.getCpuUsage()+" over "+criticalCpuUsage+", writing log.");
				eventLogManager.writeRetentionLog();
			} catch (final IOException e) {
				log.warn("Couldn't write retention log", e);
			}
		}
	}

}
