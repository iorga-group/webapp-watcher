package com.iorga.webappwatcher.analyzer.util;

import com.iorga.webappwatcher.eventlog.RequestEventLog;

public interface RequestActionFilter {
	public boolean isAnActionRequest(final RequestEventLog requestEventLog);
}
