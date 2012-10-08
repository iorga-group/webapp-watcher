package com.iorga.webappwatcher.watcher;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.iorga.webappwatcher.EventLogFilter;
import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.event.EventLogWillBeDeletedEvent;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;

public class WriteAllRequestsWatcher {
	private final static Logger log = LoggerFactory.getLogger(WriteAllRequestsWatcher.class);

	private final static EventLogFilter onlyRequestLog = new EventLogFilter() {
		@Override
		public boolean apply(final EventLog event) {
			return event instanceof RequestEventLog;
		}

		@Override
		public int getPrecedence() {
			return Precedence.LOW;
		}
	};

	@Subscribe
	public void onEvent(final EventLogWillBeDeletedEvent event) {
		// There is an eventLog which will be deleted, it's time to write all the request event logs
		try {
			EventLogManager.getInstance().writeRetentionLog(onlyRequestLog);
		} catch (final IOException e) {
			log.error("Problem while writing retention log", e);
		}
	}

}
