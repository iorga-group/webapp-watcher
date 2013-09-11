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
package com.iorga.webappwatcher.analyzer.util;

import java.io.Serializable;

import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;

public class JSF21AndRichFaces4RequestActionFilter implements RequestActionFilter, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean isAnActionRequest(final RequestEventLog requestEventLog) {
		final String requestURI = requestEventLog.getRequestURI();
		return !requestURI.contains("/javax.faces.resource/") && !requestURI.contains("/rfRes/")
			&& (requestEventLog.getMethod().equals("GET") || isNotAnAjaxPostOrIsNotAPollerRequest(requestEventLog));
	}

	private boolean isNotAnAjaxPostOrIsNotAPollerRequest(final RequestEventLog requestEventLog) {
//		String javaxEvent = null;
		String javaxSource = null;
		boolean ajaxEvent = false;
		for (final Parameter parameter : requestEventLog.getParameters()) {
			final String parameterName = parameter.getName();
			if (parameterName.equals("AJAX:EVENTS_COUNT")) {
				ajaxEvent = true;
//			} else if (parameterName.equals("javax.faces.partial.event")) {
//				javaxEvent = parameter.getValues()[0];
			} else if (parameterName.equals("javax.faces.source")) {
				javaxSource = parameter.getValues()[0];
			}
		}
//		return !ajaxEvent || ("click".equals(javaxEvent) || "change".equals(javaxEvent) || "keydown".equals(ajaxEvent) || "rich:datascroller:onscroll".equals(ajaxEvent));
		return !ajaxEvent || !javaxSource.contains("poller");
	}
}