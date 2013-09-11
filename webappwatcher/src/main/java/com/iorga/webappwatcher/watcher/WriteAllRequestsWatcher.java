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
package com.iorga.webappwatcher.watcher;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
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
			final Map<String, Object> context = Maps.newHashMap();
			context.put("eventLogWillBeDeletedEvent", event);
			EventLogManager.getInstance().writeRetentionLog(onlyRequestLog, this.getClass(), "avoidEventLogToBeDeleted", context);
		} catch (final IOException e) {
			log.error("Problem while writing retention log", e);
		}
	}

}
