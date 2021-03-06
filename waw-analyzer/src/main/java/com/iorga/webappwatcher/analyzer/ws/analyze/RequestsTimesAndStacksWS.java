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
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
	@Path("/compute")
	public StreamingOutput compute() throws IOException, ClassNotFoundException {
		requestsTimesAndStacks.compute();

		final List<RequestTimes> requests = requestsTimesAndStacks.createSortedRequestByDescendantMeanList();

		return jsonWriter.writeIterableWithTemplate(RequestTimesTemplate.class, requests);
	}
}
