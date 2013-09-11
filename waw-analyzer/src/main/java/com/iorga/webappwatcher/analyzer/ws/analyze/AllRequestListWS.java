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
package com.iorga.webappwatcher.analyzer.ws.analyze;

import java.io.IOException;
import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.StreamingOutput;

import org.jboss.resteasy.annotations.GZIP;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.RequestContainer;
import com.iorga.webappwatcher.analyzer.ws.template.RequestDetailsTemplate;

@Path("/analyze/allRequestList")
public class AllRequestListWS {
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;
	@Inject
	private JsonWriter jsonWriter;

	@ContextParam(RequestContainer.class)
	public static class RequestContainerTemplate {
		String url;
		@ContextPath("requestEventLog.principal")
		String principal;
		@ContextPath("requestEventLog.durationMillis")
		Long duration;
		public static int getNbStacks(final RequestContainer requestContainer) {
			return requestContainer.getSystemEventLogList().size();
		}
		@ContextPath("requestEventLog.date")
		Date startDate;
		@ContextPath("requestEventLog.afterProcessedDate")
		Date endDate;
	}
	@GET
	@GZIP
	@Path("/compute")
	public StreamingOutput compute() throws ClassNotFoundException, IOException {
		return jsonWriter.writeIterableWithTemplate(RequestContainerTemplate.class, requestsTimesAndStacks.computeRequestContainers());
	}

	@GET
	@Path("/requestDetails/{requestIndex}")
	public StreamingOutput requestDetails(@PathParam("requestIndex") final int requestIndex) throws ClassNotFoundException, IOException {
		return jsonWriter.writeWithTemplate(RequestDetailsTemplate.class, requestsTimesAndStacks.computeRequestContainers().get(requestIndex).getRequestEventLog());
	}
}
