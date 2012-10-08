package com.iorga.webappwatcher.event;

import com.iorga.webappwatcher.eventlog.EventLog;

/**
 * Indicates that the event will be deleted if it's still into the retention queue after the event is handled
 */
public class EventLogWillBeIgnoredUncompletedEvent {

	private EventLog eventLog;

	public EventLogWillBeIgnoredUncompletedEvent(final EventLog eventLog) {
		this.eventLog = eventLog;
	}


	public EventLog getEventLog() {
		return eventLog;
	}

	public void setEventLog(final EventLog eventLog) {
		this.eventLog = eventLog;
	}

}
