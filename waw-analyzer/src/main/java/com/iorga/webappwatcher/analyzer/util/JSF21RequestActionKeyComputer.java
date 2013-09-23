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

import java.util.Map;

import com.iorga.webappwatcher.eventlog.RequestEventLog;

public class JSF21RequestActionKeyComputer extends AbstractRequestActionKeyComputer {

	@Override
	protected void doComputeEndRequestKey(final StringBuilder requestKeyBuilder, final Map<String, String[]> parameters, final RequestEventLog request) {
		// Now check if it's an AJAX request, retrieve the source /!\ Specific JSF
		if (parameters.containsKey("AJAX:EVENTS_COUNT")) {
			// It's an ajax request, let's add the source to the key
			final String[] javaxFacesSource = parameters.get("javax.faces.source");
			if (javaxFacesSource != null) {
				// JSF 2.1
				requestKeyBuilder.append("?ajax.source=").append(javaxFacesSource[0]);
			}
		}
	}

}
