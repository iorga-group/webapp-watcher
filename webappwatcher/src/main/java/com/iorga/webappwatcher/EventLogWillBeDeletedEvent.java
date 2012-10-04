package com.iorga.webappwatcher;

import com.iorga.webappwatcher.eventlog.EventLog;

public class EventLogWillBeDeletedEvent {

	private EventLog eventLog;

	public EventLogWillBeDeletedEvent(final EventLog eventLog) {
		this.eventLog = eventLog;
	}


	public EventLog getEventLog() {
		return eventLog;
	}

	public void setEventLog(final EventLog eventLog) {
		this.eventLog = eventLog;
	}

}
