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
package com.iorga.webappwatcher.analyzer.ws.statistics;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.StreamingOutput;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.json.JsonWriter;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats.TimeSlice;

@Path("/statistics/timeSliceList")
public class TimeSliceListWS {
	@Inject
	private DurationPerPrincipalStats durationPerPrincipalStats;

	@Inject
	private JsonWriter jsonWriter;

	@ContextParam(TimeSlice.class)
	public static class TimeSliceTemplate {
		Date startDate;
		Date endDate;
		public static int getDistinctUsers(final TimeSlice timeSlice) {
			return timeSlice.getStatsPerPrincipal().size();
		}
		public static long getNumberOfRequests(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getN();
		}
		public static double getDurationsFor1clickSum(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getSum();
		}
		public static double getDurationsFor1clickMean(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getMean();
		}
		public static double getDurationsFor1clickMedian(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getPercentile(50);
		}
		public static double getDurationsFor1click90c(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getPercentile(90);
		}
		public static double getDurationsFor1clickMin(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getMin();
		}
		public static double getDurationsFor1clickMax(final TimeSlice timeSlice) {
			return timeSlice.getDurationsFor1click().getMax();
		}
	}
	@GET
	@Path("/compute")
	public StreamingOutput compute() throws ClassNotFoundException, IOException {
		final List<TimeSlice> timeSliceList = durationPerPrincipalStats.computeTimeSliceList();
		return jsonWriter.writeIterableWithTemplate(TimeSliceTemplate.class, timeSliceList);
	}
}
