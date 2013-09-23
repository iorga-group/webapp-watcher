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
import org.codehaus.jackson.annotate.JsonIgnore;

import com.iorga.webappwatcher.analyzer.util.JSF21AndRichFaces4RequestActionFilter;
import com.iorga.webappwatcher.analyzer.util.JSF21RequestActionKeyComputer;
import com.iorga.webappwatcher.analyzer.util.RequestActionFilter;
import com.iorga.webappwatcher.analyzer.util.RequestActionKeyComputer;
import com.iorga.webappwatcher.analyzer.util.RichFaces3RequestActionFilter;
import com.iorga.webappwatcher.analyzer.util.RichFaces3RequestActionKeyComputer;

@SessionScoped
@JsonAutoDetect(getterVisibility = Visibility.PUBLIC_ONLY)
public class Configurations implements Serializable {
	public enum RequestActionFilterType {
		JSF21_AND_RICHFACES_4(new JSF21AndRichFaces4RequestActionFilter(), new JSF21RequestActionKeyComputer()),
		RICHFACES_3(new RichFaces3RequestActionFilter(), new RichFaces3RequestActionKeyComputer());

		private final RequestActionFilter requestActionFilter;
		private final RequestActionKeyComputer requestActionKeyComputer;

		RequestActionFilterType(final RequestActionFilter requestActionFilter, final RequestActionKeyComputer requestActionKeyComputer) {
			this.requestActionFilter = requestActionFilter;
			this.requestActionKeyComputer = requestActionKeyComputer;
		}

		public RequestActionFilter getRequestActionFilter() {
			return requestActionFilter;
		}
		public RequestActionKeyComputer getRequestActionKeyComputer() {
			return requestActionKeyComputer;
		}
	}

	private static final long serialVersionUID = 1L;

	private long timeSliceDurationMillis = 10 * 60 * 1000;

	private int minMillisToLog = 3000;

	private RequestActionFilterType requestActionFilterType = RequestActionFilterType.JSF21_AND_RICHFACES_4;

	@JsonIgnore
	public RequestActionFilter getRequestActionFilter() {
		return requestActionFilterType.getRequestActionFilter();
	}
	@JsonIgnore
	public RequestActionKeyComputer getRequestActionKeyComputer() {
		return requestActionFilterType.getRequestActionKeyComputer();
	}

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
	public RequestActionFilterType getRequestActionFilterType() {
		return requestActionFilterType;
	}
	public void setRequestActionFilterType(final RequestActionFilterType requestActionFilterType) {
		this.requestActionFilterType = requestActionFilterType;
	}

}
