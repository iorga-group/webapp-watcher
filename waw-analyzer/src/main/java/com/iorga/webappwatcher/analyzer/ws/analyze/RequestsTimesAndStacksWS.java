package com.iorga.webappwatcher.analyzer.ws.analyze;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.StreamingOutput;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.RequestTimes;

@Path("/analyze/requestsTimesAndStacks")
public class RequestsTimesAndStacksWS {
	@Inject
	private JsonWriter jsonWriter;
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;

	@ContextParam(value = RequestTimes.class)
	public static class RequestTimesTemplate {
		String id;
		String url;

		public static double getN(final RequestTimes requestTimes) {
			return requestTimes.getStatistics().getN();
		}
		public static double getMin(final RequestTimes requestTimes) {
			return requestTimes.getStatistics().getMin();
		}
		public static double getMax(final RequestTimes requestTimes) {
			return requestTimes.getStatistics().getMax();
		}
		public static double getMean(final RequestTimes requestTimes) {
			return requestTimes.getStatistics().getMean();
		}
		public static double getMedian(final RequestTimes requestTimes) {
			return requestTimes.getStatistics().getPercentile(50d);
		}
	}
	@GET
	@Path("/compute/{minMillisToLog}")
	public StreamingOutput compute(@PathParam("minMillisToLog") final int minMillisToLog) throws IOException, ClassNotFoundException {
		requestsTimesAndStacks.compute(minMillisToLog);

		final List<RequestTimes> requests = requestsTimesAndStacks.createSortedRequestByDescendantMeanList();

		return jsonWriter.writeIterableWithTemplate(RequestTimesTemplate.class, requests);
	}
}
