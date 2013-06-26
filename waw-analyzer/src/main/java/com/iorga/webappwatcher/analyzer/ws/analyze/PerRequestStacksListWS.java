package com.iorga.webappwatcher.analyzer.ws.analyze;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.google.common.collect.Lists;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.RequestTimes;
import com.iorga.webappwatcher.analyzer.ws.analyze.PerRequestStacksListWS.PerRequestStacksListTemplate.RequestTemplate;
import com.iorga.webappwatcher.eventlog.RequestEventLog;

@Path("/analyze/perRequestStacksList")
public class PerRequestStacksListWS {
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;

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
}
