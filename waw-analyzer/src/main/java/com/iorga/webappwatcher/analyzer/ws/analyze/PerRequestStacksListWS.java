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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.StreamingOutput;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.google.common.collect.Lists;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.RequestTimes;
import com.iorga.webappwatcher.analyzer.ws.analyze.PerRequestStacksListWS.PerRequestStacksListTemplate.RequestTemplate;
import com.iorga.webappwatcher.analyzer.ws.template.RequestDetailsTemplate;
import com.iorga.webappwatcher.eventlog.RequestEventLog;

@Path("/analyze/perRequestStacksList")
public class PerRequestStacksListWS {
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;
	@Inject
	private JsonWriter jsonWriter;

	@SuppressWarnings("unused")
	@JsonAutoDetect(fieldVisibility = Visibility.ANY)
	public static class PerRequestStacksListTemplate {
		private final String url;
		private final String id;
		private final List<RequestTemplate> slowRequests = Lists.newLinkedList();

		public PerRequestStacksListTemplate(final String url, final String id) {
			this.url = url;
			this.id = id;
		}

		@JsonAutoDetect(fieldVisibility = Visibility.ANY)
		public static class RequestTemplate {
			private final String principal;
			private final Date startDate;
			private final Date endDate;
			private final long duration;
			private final int nbStacks;

			public RequestTemplate(final String principal, final Date startDate, final Date endDate, final long duration, final int nbStacks) {
				this.principal = principal;
				this.startDate = startDate;
				this.endDate = endDate;
				this.duration = duration;
				this.nbStacks = nbStacks;
			}
		}
	}
	@GET
	@Path("/compute/{requestId}")
	public PerRequestStacksListTemplate compute(@PathParam("requestId") final String requestId) {
		final RequestTimes requestTimes = requestsTimesAndStacks.getRequestTimesForId(requestId);

		final PerRequestStacksListTemplate perRequestStacksListTemplate = new PerRequestStacksListTemplate(requestTimes.getUrl(), requestTimes.getId());

		for (final RequestEventLog requestEventLog : requestTimes.getSlowRequests()) {
			perRequestStacksListTemplate.slowRequests.add(new RequestTemplate(
				requestEventLog.getPrincipal(),
				requestEventLog.getDate(),
				requestEventLog.getAfterProcessedDate(),
				requestEventLog.getDurationMillis(),
				requestsTimesAndStacks.getNbStacksForRequestEventLog(requestEventLog)
			));
		}

		return perRequestStacksListTemplate;
	}

	@GET
	@Path("/requestDetails/{requestId}/{requestIndex}")
	public StreamingOutput requestDetails(@PathParam("requestId") final String requestId, @PathParam("requestIndex") final int requestIndex) {
		final RequestEventLog requestEventLog = requestsTimesAndStacks.getRequestEventLogForRequestIdAndRequestIndex(requestId, requestIndex);

		return jsonWriter.writeWithTemplate(RequestDetailsTemplate.class, requestEventLog);
	}
}
