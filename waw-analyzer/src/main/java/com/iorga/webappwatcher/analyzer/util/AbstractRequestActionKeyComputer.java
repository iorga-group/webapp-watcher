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

import com.google.common.collect.Maps;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;

public abstract class AbstractRequestActionKeyComputer implements RequestActionKeyComputer {

	@Override
	public String computeRequestKey(final RequestEventLog request) {
		final StringBuilder requestKeyBuilder = new StringBuilder();
		requestKeyBuilder.append(request.getMethod()).append(":").append(request.getRequestURI());
		// Put the parameters in a Map in order to check values easily
		final Map<String, String[]> parameters = Maps.newHashMap();
		for (final Parameter parameter : request.getParameters()) {
			parameters.put(parameter.getName(), parameter.getValues());
		}
		doComputeEndRequestKey(requestKeyBuilder, parameters, request);
		return requestKeyBuilder.toString();
	}

	protected abstract void doComputeEndRequestKey(StringBuilder requestKeyBuilder, Map<String, String[]> parameters, RequestEventLog request);

}
