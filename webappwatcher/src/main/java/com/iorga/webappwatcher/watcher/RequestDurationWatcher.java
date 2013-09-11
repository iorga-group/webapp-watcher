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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.event.EventLogWillBeDeletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeIgnoredUncompletedEvent;
import com.iorga.webappwatcher.event.EventLogWillBeWrittenUncompletedEvent;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.util.PatternDuration;

public class RequestDurationWatcher {
	private List<PatternDuration> requestDurationLimits = new ArrayList<PatternDuration>();

	{
		requestDurationLimits.add(new PatternDuration(Pattern.compile(".*\\.xhtml"), 30 * 1000));
	}

	@Subscribe
	public void onEvent(final RequestEventLog eventLog) throws IOException {
		checkForTooLongRequest(eventLog);
	}

	@Subscribe
	public void onEvent(final EventLogWillBeDeletedEvent event) throws IOException {
		checkForTooLongRequest(event.getEventLog());
	}

	@Subscribe
	public void onEvent(final EventLogWillBeIgnoredUncompletedEvent event) throws IOException {
		checkForTooLongRequest(event.getEventLog());
	}

	@Subscribe
	public void onEvent(final EventLogWillBeWrittenUncompletedEvent event) throws IOException {
		checkForTooLongRequest(event.getEventLog());
	}

	private void checkForTooLongRequest(final EventLog eventLog) throws IOException {
		if (eventLog instanceof RequestEventLog) {
			final RequestEventLog requestEventLog = (RequestEventLog)eventLog;
			// first, check if the request match one of the patterns
			for (final PatternDuration patternDuration : getRequestDurationLimits()) {
				if (patternDuration.getPattern().matcher(requestEventLog.getRequestURI()).matches()) {
					// the pattern matches, let's test the duration
					final Date afterProcessedDate = requestEventLog.getAfterProcessedDate();
					final Map<String, Object> context = Maps.newHashMap();
					context.put("requestEventLog", requestEventLog);
					if (afterProcessedDate == null) {
						// no end date, it's a peak, let's write all the logs
						EventLogManager.getInstance().writeRetentionLog(this.getClass(), "requestWithoutEndDate", context);
					} else {
						final Integer patternDurationDuration = patternDuration.getDuration();
						if (afterProcessedDate.getTime() - requestEventLog.getDate().getTime() >= patternDurationDuration) {
							// the request was too long, let's write all the logs
							context.put("patternDurationDuration", patternDurationDuration);
							EventLogManager.getInstance().writeRetentionLog(this.getClass(), "requestTooLong", context);
						}
					}
					break;	// stops on first match
				}
			}
		}
	}

	/// Getter & Setters ///
	///////////////////////
	public List<PatternDuration> getRequestDurationLimits() {
		return requestDurationLimits;
	}

	public void setRequestDurationLimits(final List<PatternDuration> patternDurations) {
		this.requestDurationLimits = patternDurations;
	}

}
