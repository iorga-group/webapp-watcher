package com.iorga.webappwatcher;

import com.iorga.webappwatcher.eventlog.EventLog;

public interface EventLogListener<E extends EventLog> {

	Class<E> getListenedEventLogType();	// TODO: detect it at runtime

	void onFire(E eventLog);
}
