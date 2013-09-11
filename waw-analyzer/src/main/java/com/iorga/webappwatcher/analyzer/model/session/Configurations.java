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
package com.iorga.webappwatcher.analyzer.model.session;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

@SessionScoped
@JsonAutoDetect(getterVisibility = Visibility.PUBLIC_ONLY)
public class Configurations implements Serializable {
	private static final long serialVersionUID = 1L;

	private long timeSliceDurationMillis = 10 * 60 * 1000;

	private int minMillisToLog = 3000;

	public long getTimeSliceDurationMillis() {
		return timeSliceDurationMillis;
	}
	public void setTimeSliceDurationMillis(final long timeSliceDurationMillis) {
		this.timeSliceDurationMillis = timeSliceDurationMillis;
	}
	public int getMinMillisToLog() {
		return minMillisToLog;
	}
	public void setMinMillisToLog(final int minMillisToLog) {
		this.minMillisToLog = minMillisToLog;
	}
}
