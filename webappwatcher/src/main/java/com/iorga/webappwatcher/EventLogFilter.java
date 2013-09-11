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
