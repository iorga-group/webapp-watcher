package com.iorga.webappwatcher.watcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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
					if (afterProcessedDate == null) {
						// no end date, it's a peak, let's write all the logs
						EventLogManager.getInstance().writeRetentionLog();
					} else if (afterProcessedDate.getTime() - requestEventLog.getDate().getTime() >= patternDuration.getDuration()) {
						// the request was too long, let's write all the logs
						EventLogManager.getInstance().writeRetentionLog();
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
