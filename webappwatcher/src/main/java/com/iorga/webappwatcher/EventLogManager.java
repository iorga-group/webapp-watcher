package com.iorga.webappwatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.webappwatcher.eventlog.EventLog;

public class EventLogManager {
	private static final Logger log = LoggerFactory.getLogger(EventLogManager.class);

	private static EventLogManager instance;

	private final List<EventLogListener<?>> eventLogListeners = new ArrayList<EventLogListener<?>>();
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

	private ObjectOutputStream objectOutputStreamLog;
	private final Object objectOutputStreamLogLock = new Object();

	private static final long SLEEP_BETWEEN_TEST_FOR_EVENT_LOG_TO_COMPLETE_MILLIS = 300;
	private final long waitForEventLogToCompleteMillis = 2000;

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

	public <E extends EventLog> void addEventLogListener(final EventLogListener<E> eventLogListener) {
		eventLogListeners.add(eventLogListener);	// TODO Quick & Dirty : should be a Map in order to filter with EventLog type
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
			// Remove all logs which are too old for retention
			final Date currentDate = new Date();
			for (EventLog firstEventLog = eventLogsQueue.peekFirst() ;
					firstEventLog != null && (currentDate.getTime() - firstEventLog.getDate().getTime()) > eventLogRetentionMillis;
					eventLogsQueue.removeFirst()) {
				firstEventLog = eventLogsQueue.peekFirst();
			}

		}
		return eventLog;
	}

	@SuppressWarnings("unchecked")
	public <E extends EventLog> void fire(final E systemEventLog) {
		systemEventLog.setCompleted(true);
		for (final EventLogListener<?> eventLogListener : eventLogListeners) {
			if (eventLogListener.getListenedEventLogType().isAssignableFrom(systemEventLog.getClass())) {
				((EventLogListener<E>)eventLogListener).onFire(systemEventLog);
			}
		}
	}

	private class RetentionLogWriter implements Runnable {
		private boolean running = false;

		@Override
		public void run() {
			begin();
			try {
				final ObjectOutputStream objectOutputStream = getOrOpenLog();
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
						// wait for the eventLog to complete
						final Date beginWaitIsComplete = new Date();
						while (!eventLog.isCompleted() && (new Date().getTime() - beginWaitIsComplete.getTime()) < waitForEventLogToCompleteMillis) {
							Thread.sleep(SLEEP_BETWEEN_TEST_FOR_EVENT_LOG_TO_COMPLETE_MILLIS);
						}
						if (eventLog.isCompleted()) {
							// that log is completed, let's write it in the log file
							if (log.isDebugEnabled()) {
								log.debug(eventLog.toString());
							}
							objectOutputStream.writeObject(eventLog);
						} // else, ignore it = it will never be written anywhere
					}
				}
				objectOutputStream.flush();
				end();
			} catch (final Exception e) {
				log.error("Exception while trying to write to EventLog's file", e);
			}
		}

		private synchronized void begin() {
			running  = true;
		}

		private synchronized void end() {
			running = false;
		}

		public synchronized void start() {
			if (!running) {
				new Thread(this).start();
			}
		}
	}

	RetentionLogWriter retentionLogWriter = new RetentionLogWriter();

	public void writeRetentionLog() throws IOException {
		// TODO écrire les logs avec protobuff https://developers.google.com/protocol-buffers/docs/javatutorial?hl=fr-FR plugin maven dispo ici http://igor-petruk.github.com/protobuf-maven-plugin/usage.html (voir http://stackoverflow.com/a/9358222/535203 )
		synchronized (eventLogsQueueLock) {
			synchronized (eventLogsQueueToWriteLock) {
				eventLogsQueueToWrite.addAll(eventLogsQueue);
				eventLogsQueue.clear();
			}
		}
		retentionLogWriter.start();
	}

	private ObjectOutputStream getOrOpenLog() throws FileNotFoundException, IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog == null) {
				File logFile = new File(logPath+".ser");
				for (int i = 1 ; logFile.exists() ; i++) {
					logFile = new File(logPath+"."+i+".ser");	// Never re-write on a previous log because of "AC" header in an objectOutputStream, see http://stackoverflow.com/questions/1194656/appending-to-an-objectoutputstream/1195078#1195078
				}
				objectOutputStreamLog = new ObjectOutputStream(new FileOutputStream(logFile));
			}
			return objectOutputStreamLog;
		}
	}

	public void closeLog() throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				objectOutputStreamLog.close();
				objectOutputStreamLog = null;
			}
		}
	}


	public long getEventLogRetentionMillis() {
		return eventLogRetentionMillis;
	}

	public void setEventLogRetentionMillis(final long eventLogRetentionMillis) {
		this.eventLogRetentionMillis = eventLogRetentionMillis;
	}

	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(final String logPath) {
		this.logPath = logPath;
	}

}
