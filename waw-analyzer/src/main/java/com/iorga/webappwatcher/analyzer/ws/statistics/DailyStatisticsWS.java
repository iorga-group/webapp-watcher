package com.iorga.webappwatcher.analyzer.ws.statistics;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;

import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats.DayStatistic;

@Path("/statistics/dailyStatistics")
public class DailyStatisticsWS {
	private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Inject
	private DurationPerPrincipalStats durationPerPrincipalStats;

	@GET
	@Path("/compute")
	public StreamingOutput compute() throws ClassNotFoundException, IOException {
		final List<DayStatistic> computeDayStatistics = durationPerPrincipalStats.computeDayStatistics();
		return new StreamingOutput() {
			@Override
			public void write(final OutputStream output) throws IOException, WebApplicationException {
				final JsonGenerator generator = OBJECT_MAPPER.getJsonFactory().createJsonGenerator(output, JsonEncoding.UTF8);
				generator.writeStartArray();
				for (final DayStatistic dayStatistic : computeDayStatistics) {
					generator.writeStartObject();
					generator.writeFieldName("startDate");
					OBJECT_MAPPER.writeValue(generator, dayStatistic.getStartDate());
					generator.writeFieldName("endDate");
					OBJECT_MAPPER.writeValue(generator, dayStatistic.getEndDate());
					generator.writeFieldName("statistics");
					generator.writeStartArray();
					// write each statistic
					for (final String statisticType : new String[] {"distinctUsers", "numberOfRequests", "durationsFor1clickSum", "durationsFor1clickMean", "durationsFor1clickMedian", "durationsFor1click90c", "durationsFor1clickMin", "durationsFor1clickMax"}) {
//						generator.writeFieldName(statisticType);
						generator.writeStartObject();
						generator.writeStringField("type", statisticType);
						// get the statistics from the dayStatistic
						DescriptiveStatistics descriptiveStatistics;
						try {
							descriptiveStatistics = (DescriptiveStatistics) dayStatistic.getClass().getMethod("get"+StringUtils.capitalize(statisticType)).invoke(dayStatistic);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
							throw new IOException("Problem while invoking getter for statisticType "+statisticType, e);
						}
						generator.writeNumberField("min", descriptiveStatistics.getMin());
						generator.writeNumberField("max", descriptiveStatistics.getMax());
						generator.writeNumberField("mean", descriptiveStatistics.getMean());
						generator.writeNumberField("median", descriptiveStatistics.getPercentile(50));
						generator.writeEndObject();
					}
					generator.writeEndArray();
					generator.writeEndObject();
				}
				generator.writeEndArray();

				generator.flush(); // required else all the stream is not sent
			}
		};
	}
}
