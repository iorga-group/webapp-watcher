package com.iorga.webappwatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

import com.google.common.eventbus.EventBus;
import com.iorga.webappwatcher.event.EventLogWillBeDeletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeIgnoredUncompletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeWrittenUncompletedEvent;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.util.StartableRunnable;

public class EventLogManager {
	private static final String EVENT_LOG_FILE_EXTENSION = ".ser.xz";

	private static final Logger log = LoggerFactory.getLogger(EventLogManager.class);

	private static EventLogManager instance;

	/**
	 * Firsts are older than lasts
	 */
	private final Deque<EventLog> eventLogsQueue = new LinkedList<EventLog>();
	private final Object eventLogsQueueLock = new Object();
	/**
	 * Firsts are older than lasts
	 */
	private final Deque<EventLog> eventLogsQueueToWrite = new LinkedList<EventLog>();
	private final Object eventLogsQueueToWriteLock = new Object();

	private long eventLogRetentionMillis = 5 * 60 * 1000;	// 5mn log by default
	private String logPath = "webappwatcherlog";

	private File logFile;
	private ObjectOutputStream objectOutputStreamLog;
	private XZOutputStream xzOutputStream;
	private final Object objectOutputStreamLogLock = new Object();

	private static final long SLEEP_BETWEEN_TEST_FOR_EVENT_LOG_TO_COMPLETE_MILLIS = 300;
	private static final long SLEEP_BEFORE_CLEANING_EVENT_LOGS_QUEUE_MILLIS = 1000;

	private long waitForEventLogToCompleteMillis = 5 * 60 * 1000;	// 5mn by default

	private final EventBus eventBus = new EventBus();
//	private final Map<Class<? extends EventLogListener>, EventLogListener<?>> eventLogListeners = Maps.newHashMap();
	private Set<?> eventLogWatchers;

	private EventLogManager() {
	}

	public static EventLogManager getInstance() {
		if (instance == null) {
			synchronized (EventLogManager.class) {
				if (instance == null) {
					instance = new EventLogManager();
				}
			}
		}
		return instance;
	}

	public <E extends EventLog> E addEventLog(final Class<E> eventLogClass) {
		final E eventLog;
		synchronized (eventLogsQueueLock) {
			try {
				final Constructor<E> constructor = eventLogClass.getDeclaredConstructor();
				constructor.setAccessible(true);
				eventLog = constructor.newInstance();
			} catch (final Exception e) {
				throw new IllegalArgumentException("Can't create a new "+eventLogClass.getName(), e);
			}

			eventLogsQueue.addLast(eventLog);

			eventLogsQueueCleaner.start(); // start the cleaner to check the potentially too old eventLogs to remove
		}
		return eventLog;
	}

	public <E extends EventLog> void fire(final E systemEventLog) {
		final boolean alreadyCompleted;
		synchronized (systemEventLog) {
			// check if it was already completed or not. Synchronized because multiple threads could want to fire that event
			alreadyCompleted = systemEventLog.isCompleted();
			if (!alreadyCompleted) {
				systemEventLog.setCompleted(true);
			}
		}
		if (!alreadyCompleted) {
			eventBus.post(systemEventLog);
		}
	}

	private class EventLogsQueueCleaner extends StartableRunnable {
		@Override
		public void run() {
			begin();
			try {
				// Wait a small amount of time before trying to clean the eventLogsQueue
				Thread.sleep(SLEEP_BEFORE_CLEANING_EVENT_LOGS_QUEUE_MILLIS);
				// Remove all logs which are too old for retention
				EventLog firstEventLog = eventLogsQueue.peekFirst();
				while (firstEventLog != null && (new Date().getTime() - firstEventLog.getDate().getTime()) > eventLogRetentionMillis) {
					// There is one log too old, throw an event which indicates that this eventLog is too old and could be erased if not handled
					eventBus.post(new EventLogWillBeDeletedEvent(firstEventLog));	// FIXME : l'instance n'est pas typée !!
					// now, lock the eventLogsQueue & remove if it's still the first one
					synchronized (eventLogsQueueLock) {
						final EventLog newFirstEventLog = eventLogsQueue.peekFirst();
						if (newFirstEventLog == firstEventLog) {
							// it's still the same, delete it
							eventLogsQueue.removeFirst();
						}
					}
					firstEventLog = eventLogsQueue.peekFirst();
				}
			} catch (final Exception e) {
				log.error("Exception while trying to clean the eventLogsQueue", e);
			} finally {
				end();
			}
		}
	}

	private final EventLogsQueueCleaner eventLogsQueueCleaner = new EventLogsQueueCleaner();

	private class RetentionLogWriter extends StartableRunnable {
		private EventLogFilter filter;

		@Override
		public void run() {
			begin();
			try {
				while (!eventLogsQueueToWrite.isEmpty()) {
					final EventLog eventLog;
					synchronized (eventLogsQueueToWriteLock) {
						if (!eventLogsQueueToWrite.isEmpty()) {
							eventLog = eventLogsQueueToWrite.removeFirst();
						} else {
							eventLog = null;
						}
					}
					if (eventLog != null) {
						if (eventLog.isCompleted()) {
							if (filter.apply(eventLog)) {
								// completed event log, accepted by the filter, let's write it
								writeEventLog(eventLog);
							} else {
								// not accepted, let's ignore it
							}
						} else {
							if (filter.apply(eventLog)) {
								// uncompleted event log, accepted, we have to wait for it & finally write it
								waitAndWriteEventLog(eventLog);
							} else {
								// uncompleted event log, rejected by filter, let's alert watchers
								eventBus.post(new EventLogWillBeIgnoredUncompletedEvent(eventLog));
								// perhaps some watchers changed the filter with that event, so let's test it again
								if (filter.apply(eventLog)) {
									// uncompleted event log, accepted, we have to wait for it & finally write it
									waitAndWriteEventLog(eventLog);
								} else {
									// not accepted, let's ignore it
								}
							}
						}
					}
				}
			} catch (final Exception e) {
				log.error("Exception while trying to write to EventLog's file", e);
			} finally {
				end();
			}
		}

		@Override
		protected synchronized void end() {
			super.end();
			this.filter = null; // reset the filter so that the next "start" could replace it
		}

		public synchronized void start(final EventLogFilter filter) {
			if (!isRunning() || (isRunning() && this.filter.getPrecedence() < filter.getPrecedence())) {
				// Only replace the filter if the given one is more important in precedence, or if the logWriter is not running
				this.filter = filter;
			}
			start();
		}
	}

	private final RetentionLogWriter retentionLogWriter = new RetentionLogWriter();

	private static final EventLogFilter acceptAll = new EventLogFilter() {
		@Override
		public boolean apply(final EventLog event) {
			return true;
		}

		@Override
		public int getPrecedence() {
			return Precedence.HIGH;
		}
	};

	/**
	 * Write all the retetion log.
	 * @throws IOException
	 */
	public void writeRetentionLog() throws IOException {
		writeRetentionLog(acceptAll);
	}

	/**
	 * Write the retetion log, but only the eventLogs accepted by the given filter
	 * @param filter
	 * @throws IOException
	 */
	public void writeRetentionLog(final EventLogFilter filter) throws IOException {
		// TODO : le filter peut être gardé si le writer tourne encore
		synchronized (eventLogsQueueLock) {
			synchronized (eventLogsQueueToWriteLock) {
				eventLogsQueueToWrite.addAll(eventLogsQueue);
				eventLogsQueue.clear();
			}
		}
		retentionLogWriter.start(filter);
	}

	private void waitAndWriteEventLog(final EventLog eventLog) throws InterruptedException, FileNotFoundException, IOException {
		// wait for the eventLog to complete as the filter accepts it
		final Date beginWaitIsComplete = new Date();
		while (!eventLog.isCompleted() && (new Date().getTime() - beginWaitIsComplete.getTime()) < waitForEventLogToCompleteMillis) {
			Thread.sleep(SLEEP_BETWEEN_TEST_FOR_EVENT_LOG_TO_COMPLETE_MILLIS);
		}
		if (!eventLog.isCompleted()) {
			eventBus.post(new EventLogWillBeWrittenUncompletedEvent(eventLog));
		}
		writeEventLog(eventLog);
	}

	private void writeEventLog(final EventLog eventLog) throws FileNotFoundException, IOException, InterruptedException {
		if (!eventLog.isCompleted()) {
			log.info("Writing not completed "+eventLog.getClass().getName()+"#"+eventLog.getDate().getTime());
		}
		if (log.isDebugEnabled()) {
			log.debug("Writing "+eventLog.toString());
		}
		synchronized (objectOutputStreamLogLock) {
			final ObjectOutputStream objectOutputStream = getOrOpenLog();
			objectOutputStream.writeObject(eventLog);
		}
	}

	private ObjectOutputStream getOrOpenLog() throws FileNotFoundException, IOException, InterruptedException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog == null) {
				logFile = generateLogFile();
				while (logFile.exists()) {
					Thread.sleep(1);
					logFile = generateLogFile(); // Never re-write on a previous log because of "AC" header in an objectOutputStream, see http://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream/1195078#1195078
				}
				xzOutputStream = new XZOutputStream(new FileOutputStream(logFile), new LZMA2Options());
				objectOutputStreamLog = new ObjectOutputStream(xzOutputStream);
			}
			return objectOutputStreamLog;
		}
	}

	private void flushLog() throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				objectOutputStreamLog.flush();
				// In order for the stream to be correctly
				xzOutputStream.endBlock();
				xzOutputStream.flush();
			}
		}
	}

	private File generateLogFile() {
		return new File(logPath+"."+DateFormatUtils.format(new Date(), "yyyyMMdd-HHmmss-SSS")+EVENT_LOG_FILE_EXTENSION);
	}

	public void closeLog() throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				try {
					objectOutputStreamLog.close();
				} catch (final IOException e) {
					throw new IOException("Problem while closing the event log", e);
				} finally {
					// Reset fields to null even if there was a problem while closing the log, in order to write to a new one next time
					objectOutputStreamLog = null;
					xzOutputStream = null;
					logFile = null;
				}
			}
		}
	}

	public static ObjectInputStream readLog(final String file) throws FileNotFoundException, IOException {
		return new ObjectInputStream(new XZInputStream(new FileInputStream(file)));
	}

	public void writeEventLogToHttpServletResponse(final HttpServletResponse httpResponse) throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				// before trying to download the log, we must first flush it
				// warning : as the footer of the xz will never be written, that file will be inconsistent for properly reading it
				flushLog();
			}
			if (logFile != null && logFile.exists()) {
				httpResponse.setStatus(HttpServletResponse.SC_OK);
				httpResponse.setContentType("application/x-xz");
				httpResponse.setHeader("Content-Disposition", "attachment; filename=\""+logFile.getName()+"\"");
				httpResponse.setContentLength((int) logFile.length());

				final FileInputStream inputStream = new FileInputStream(logFile);
				final ServletOutputStream outputStream = httpResponse.getOutputStream();
				try {
					IOUtils.copy(inputStream, outputStream);
				} finally {
					inputStream.close();
					outputStream.close();
				}
			} else {
				httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
				final PrintWriter writer = httpResponse.getWriter();
				writer.write("No log available at the moment.");
				writer.flush();
			}
		}
	}

	/// Getters / Setters ///
	////////////////////////
	public long getEventLogRetentionMillis() {
		return eventLogRetentionMillis;
	}

	public void setEventLogRetentionMillis(final long eventLogRetentionMillis) {
		this.eventLogRetentionMillis = eventLogRetentionMillis;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(final String logPath) throws IOException {
		closeLog();
		this.logPath = logPath;
	}

	public long getWaitForEventLogToCompleteMillis() {
		return waitForEventLogToCompleteMillis;
	}

	public void setWaitForEventLogToCompleteMillis(final long waitForEventLogToCompleteMillis) {
		this.waitForEventLogToCompleteMillis = waitForEventLogToCompleteMillis;
	}

	public long getEventLogLength() {
		if (logFile != null && logFile.exists()) {
			return logFile.length();
		} else {
			return -1;
		}
	}

	public void setEventLogWatchers(final Set<?> eventLogWatchers) {
		// first remove the old ones
		if (this.eventLogWatchers != null) {
			for (final Object eventLogWatcher : this.eventLogWatchers) {
				eventBus.unregister(eventLogWatcher);
			}
		}
		this.eventLogWatchers = eventLogWatchers;
		// and now register the new ones
		for (final Object eventLogWatcher : eventLogWatchers) {
			eventBus.register(eventLogWatcher);
		}
	}

	public Set<?> getEventLogWatchers() {
		return eventLogWatchers;
	}

}
