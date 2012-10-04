package com.iorga.webappwatcher;

import com.google.common.base.Predicate;
import com.iorga.webappwatcher.eventlog.EventLog;

public interface EventLogFilter extends Predicate<EventLog> {
	public interface Precedence {
		public static final int HIGH = 10;
		public static final int MEDIUM = 5;
		public static final int LOW = 1;
	}

	public int getPrecedence();
}
