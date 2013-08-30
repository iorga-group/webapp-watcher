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
