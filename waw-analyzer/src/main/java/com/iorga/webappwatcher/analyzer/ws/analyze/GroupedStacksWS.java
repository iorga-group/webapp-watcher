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

	@GET
	@Path("/computeDetails/{requestId}/{requestIndex}")
	public StreamingOutput computeDetails(@PathParam("requestId") final String requestId, @PathParam("requestIndex") final int requestIndex) {
		final List<TreeNode<StackStatElement>> stacks = requestsTimesAndStacks.computeGroupedStacksForRequestIdAndRequestIndex(requestId, requestIndex);

		return jsonWriter.writeIterableWithTemplate(StackStatElementTreeTemplate.class, stacks);
	}
}
