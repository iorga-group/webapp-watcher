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
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.StackStatElement;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.TreeNode;

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

	@ContextParam(value = TreeNode.class, parameterizedArguments = StackStatElement.class)
	public static class StackStatElementTreeTemplate {
		List<StackStatElementTreeTemplate> children;

		public static String getStackTraceElement(final TreeNode<StackStatElement> node) {
			return node.getData().getStackTraceElement().toString();
		}
		public static int getNb(final TreeNode<StackStatElement> node) {
			return node.getData().getNb();
		}
	}
	@GET
	@Path("/computeGroupedStacks/{requestId}")
	public StreamingOutput computeGroupedStacks(@PathParam("requestId") final String requestId) {
		final List<TreeNode<StackStatElement>> stacks = requestsTimesAndStacks.computeGroupedStacksForRequestId(requestId);

		return jsonWriter.writeIterableWithTemplate(StackStatElementTreeTemplate.class, stacks);
	}
}
