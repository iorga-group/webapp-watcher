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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tukaani.xz.XZInputStream;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.iorga.webappwatcher.event.EventLogWillBeDeletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeIgnoredUncompletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeWrittenUncompletedEvent;
import com.iorga.webappwatcher.event.RetentionLogWritingEvent;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.util.StartableRunnable;

public class EventLogManager {
	private static final String EVENT_LOG_FILE_EXTENSION = ".ser";

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
	private RandomAccessFile rafLog;
	private final Object objectOutputStreamLogLock = new Object();

	private static final long SLEEP_BETWEEN_TEST_FOR_EVENT_LOG_TO_COMPLETE_MILLIS = 300;
	private static final long SLEEP_BEFORE_CLEANING_EVENT_LOGS_QUEUE_MILLIS = 1000;

	private long waitForEventLogToCompleteMillis = 5 * 60 * 1000;	// 5mn by default

	private long maxLogFileSizeMo = 100;
	private long eventsWritten = 0;

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
					eventBus.post(new EventLogWillBeDeletedEvent(firstEventLog));	// TODO : l'instance n'est pas typée !!
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
	public void writeRetentionLog(final Class<?> source, final String reason, final Map<String, Object> context) throws IOException {
		writeRetentionLog(acceptAll, source, reason, context);
	}

	/**
	 * Write the retetion log, but only the eventLogs accepted by the given filter
	 * @param filter
	 * @throws IOException
	 */
	public void writeRetentionLog(final EventLogFilter filter, final Class<?> source, final String reason, final Map<String, Object> context) throws IOException {
		// TODO : le filter peut être gardé si le writer tourne encore
		synchronized (eventLogsQueueLock) {
			synchronized (eventLogsQueueToWriteLock) {
				eventLogsQueueToWrite.addAll(eventLogsQueue);
				eventLogsQueue.clear();
			}
		}
		retentionLogWriter.start(filter);
		eventBus.post(new RetentionLogWritingEvent(source, reason, context));
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
			eventsWritten++;
			if (eventsWritten % 1000 == 0) {
				// every 1000 events, reset the outputstream in order to read it without requiring plenty of memory
				objectOutputStream.reset();
			}
			if (eventsWritten % 50 == 0) {
				// every 100 events written, let's flush & check if the file size is more than the max authorized
				objectOutputStream.flush();
				if (getEventLogLength() > maxLogFileSizeMo * 1024 * 1024) {
					closeLog();
				}
			}
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
				rafLog = new RandomAccessFile(logFile, "rw");
				objectOutputStreamLog = new ObjectOutputStream(new FileOutputStream(rafLog.getFD()));
			}
			return objectOutputStreamLog;
		}
	}

	private void flushLog() throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				objectOutputStreamLog.flush();
			}
		}
	}

	private File generateLogFile() {
		return new File(logPath+"."+DateFormatUtils.format(new Date(), "yyyyMMdd-HHmmss-SSS")+EVENT_LOG_FILE_EXTENSION);
	}

	private String generateLogFileNameWithoutExtension() {
		return new File(logPath).getName()+"."+DateFormatUtils.format(new Date(), "yyyyMMdd-HHmmss-SSS");
	}

	private static class GZIPer implements Runnable {
		private final File fileToGzip;

		public GZIPer(final File fileToGzip) {
			this.fileToGzip = fileToGzip;
		}

		@Override
		public void run() {
			final File gzFile = new File(fileToGzip.getAbsolutePath()+".gz");
			OutputStream outputStream;
			try {
				outputStream = new GZIPOutputStream(new FileOutputStream(gzFile));
				final InputStream inputStream = new FileInputStream(fileToGzip);
				IOUtils.copy(inputStream, outputStream);
				inputStream.close();
				outputStream.close();
				fileToGzip.delete();
			} catch (final Exception e) {
				log.error("Problem while gzipping "+fileToGzip, e);
			}
		}
	}

	public void closeLog() throws IOException {
		synchronized (objectOutputStreamLogLock) {
			if (objectOutputStreamLog != null) {
				try {
					objectOutputStreamLog.close();
					rafLog.close();
					eventsWritten = 0;
					// launch the zipping process in another thread
					new Thread(new GZIPer(logFile), GZIPer.class.getName()).start();
				} catch (final IOException e) {
					throw new IOException("Problem while closing the event log", e);
				} finally {
					// Reset fields to null even if there was a problem while closing the log, in order to write to a new one next time
					rafLog = null;
					objectOutputStreamLog = null;
					logFile = null;
				}
			}
		}
	}

	public static ObjectInputStream readLog(InputStream inputStream, String fileName) throws FileNotFoundException, IOException {
		if (fileName.endsWith(".gz")) {
			inputStream = new GZIPInputStream(inputStream);
			fileName = StringUtils.substringBeforeLast(fileName, ".gz");
		} else if (fileName.endsWith(".xz")) {
			inputStream = new XZInputStream(inputStream);
			fileName = StringUtils.substringBeforeLast(fileName, ".xz");
		}
		if (fileName.endsWith(".ser")) {
			return new ObjectInputStream(inputStream);
		} else {
			throw new IllegalArgumentException("Filename must end with .ser, .ser.gz or .ser.xz");
		}
	}

	public static ObjectInputStream readLog(final String filePath) throws FileNotFoundException, IOException {
		final File file = new File(filePath);
		return readLog(new FileInputStream(file), file.getName());
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
				httpResponse.setContentType("application/x-gzip");
				httpResponse.setHeader("Content-Disposition", "attachment; filename=\""+logFile.getName()+".gz\"");

				final FileInputStream inputStream = new FileInputStream(logFile);
				final OutputStream outputStream = new GZIPOutputStream(httpResponse.getOutputStream());
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
			}
		}
	}

	public Iterable<String> listEventLogsNameInThePath() {
		return Iterables.transform(listEventLogsInThePath(), new Function<File, String>() {
			@Override
			public String apply(final File file) {
				return file.getName();
			}
		});
	}

	public List<File> listEventLogsInThePath() {
		final List<File> eventLogs = Lists.newArrayList();

		final File[] files = getLogPathDirectory().listFiles();
		for (final File file : files) {
			final String fileName = file.getName();
			if (fileName.endsWith(EVENT_LOG_FILE_EXTENSION) || fileName.endsWith(EVENT_LOG_FILE_EXTENSION+".gz")) {
				eventLogs.add(file);
			}
		}

		return eventLogs;
	}

	public void writeEventLogsToHttpServletResponse(final HttpServletResponse httpResponse, final Iterable<String> fileNames) throws IOException {
		// First, we check if the given paths are in the log path
		final Set<String> eventLogs = Sets.newHashSet(listEventLogsNameInThePath());
		ArchiveOutputStream outputStream = null;
		try {
			for (final String fileName : fileNames) {
				if (eventLogs.contains(fileName) && new File(getLogPathDirectory(), fileName).exists()) {
					outputStream = writeEventLogToHttpServletResponse(httpResponse, fileName, outputStream);
				}
			}
		} finally {
			if (outputStream != null) {
				outputStream.close();
			}
		}

		if (outputStream == null) {
			// No file were appended
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
			final PrintWriter writer = httpResponse.getWriter();
			writer.write("No log available at the moment.");
		}
	}

	private ArchiveOutputStream writeEventLogToHttpServletResponse(final HttpServletResponse httpResponse, String fileName, ArchiveOutputStream outputStream) throws IOException {
		if (outputStream == null) {
			// First, let's open the outputStream
			httpResponse.setStatus(HttpServletResponse.SC_OK);
			httpResponse.setContentType("application/zip");
			httpResponse.setHeader("Content-Disposition", "attachment; filename=\""+generateLogFileNameWithoutExtension()+".zip\"");

			outputStream = new ZipArchiveOutputStream(httpResponse.getOutputStream());
		}

		final File file = new File(getLogPathDirectory(), fileName);
		InputStream inputStream = new FileInputStream(file);
		if (fileName.endsWith(".gz")) {
			// it's a GZIP, let's decompress it
			inputStream = new GZIPInputStream(inputStream);
			fileName = StringUtils.substringBeforeLast(fileName, ".gz");
		} else if (logFile != null && logFile.getName().equals(fileName)) {
			// this is the current log file, we must lock the logFile
			synchronized (objectOutputStreamLogLock) {
				// before trying to download the log, we must first flush it
				flushLog();
				writeEventLogToHttpServletResponse(outputStream, inputStream, file, fileName);
			}
			return outputStream;
		}

		writeEventLogToHttpServletResponse(outputStream, inputStream, file, fileName);

		return outputStream;
	}

	private void writeEventLogToHttpServletResponse(final ArchiveOutputStream outputStream, final InputStream inputStream, final File file, final String fileName) throws IOException {
		ArchiveEntry archiveEntry = outputStream.createArchiveEntry(file, fileName);
		if (!file.getName().equals(fileName)) {
			// the original file is not the same as the final file, i.e. an original .gz which is uncompressed on the fly, we can't know the file size
			archiveEntry = new ZipArchiveEntry(fileName);
		}
		outputStream.putArchiveEntry(archiveEntry);
		try {
			IOUtils.copy(inputStream, outputStream);
		} catch (final Exception e) {
			log.warn("Problem while copying file "+fileName, e);
		} finally {
			outputStream.closeArchiveEntry();
			inputStream.close();
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

	public File getLogPathDirectory() {
		return new File(getLogPath()).getAbsoluteFile().getParentFile();
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

	public long getMaxLogFileSizeMo() {
		return maxLogFileSizeMo;
	}

	public void setMaxLogFileSizeMo(final long maxLogFileSizeMo) {
		this.maxLogFileSizeMo = maxLogFileSizeMo;
	}

}
