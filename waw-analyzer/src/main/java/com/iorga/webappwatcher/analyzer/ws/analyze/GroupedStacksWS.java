package com.iorga.webappwatcher.analyzer.ws.analyze;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.StreamingOutput;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.StackStatElement;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.TreeNode;

@Path("/analyze/groupedStacks")
public class GroupedStacksWS {
	@Inject
	private JsonWriter jsonWriter;
	@Inject
	private RequestsTimesAndStacks requestsTimesAndStacks;

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
	@Path("/compute/{requestId}")
	public StreamingOutput computeGroupedStacks(@PathParam("requestId") final String requestId) {
		final List<TreeNode<StackStatElement>> stacks = requestsTimesAndStacks.computeGroupedStacksForRequestId(requestId);

		return jsonWriter.writeIterableWithTemplate(StackStatElementTreeTemplate.class, stacks);
	}
}
