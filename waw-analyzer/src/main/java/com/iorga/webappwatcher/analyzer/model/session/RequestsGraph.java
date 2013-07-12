package com.iorga.webappwatcher.analyzer.model.session;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats.Changed;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats.TimeSlice;

@SessionScoped
public class RequestsGraph implements Serializable {
	private static final long serialVersionUID = 1L;

	public static enum GraphMode {AUTO, STATIC};
	private static List<Double> staticDispersionTable = Lists.newArrayList(1000d, 2000d, 3000d, 5000d, 10000d, 20000d);

	public static class DateDoubleValue implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Date date;
		private final Double doubleValue;

		public DateDoubleValue(final Date date, final Double doubleValue) {
			this.date = date;
			this.doubleValue = doubleValue;
		}
		public Date getDate() {
			return date;
		}
		public Double getDoubleValue() {
			return doubleValue;
		}
	}

	public static class Serie implements Serializable {
		private static final long serialVersionUID = 1L;

		private final int min;
		private final int max;
		private final List<DateDoubleValue> data = Lists.newLinkedList();

		public Serie(final int min, final int max) {
			this.min = min;
			this.max = max;
		}
		public int getMin() {
			return min;
		}
		public int getMax() {
			return max;
		}
		public List<DateDoubleValue> getData() {
			return data;
		}
	}

	public static class Graph implements Serializable {
		private static final long serialVersionUID = 1L;

		private final List<DateDoubleValue> cpuUsageMeans = Lists.newLinkedList();
		private final List<DateDoubleValue> memoryUsageMeans = Lists.newLinkedList();
		private final List<DateDoubleValue> nbUsersMax = Lists.newLinkedList();
		private final List<DateDoubleValue> durationsFor1clickMedians = Lists.newLinkedList();
		private List<Serie> durationsFor1clickDispersionSeries;

		public List<DateDoubleValue> getCpuUsageMeans() {
			return cpuUsageMeans;
		}
		public List<DateDoubleValue> getMemoryUsageMeans() {
			return memoryUsageMeans;
		}
		public List<DateDoubleValue> getNbUsersMax() {
			return nbUsersMax;
		}
		public List<DateDoubleValue> getDurationsFor1clickMedians() {
			return durationsFor1clickMedians;
		}
		public List<Serie> getDurationsFor1clickDispersionSeries() {
			return durationsFor1clickDispersionSeries;
		}
	}

	/// Dependencies ///
	@Inject
	private DurationPerPrincipalStats durationPerPrincipalStats;

	/// Parameters ///
	private GraphMode graphMode;
	private int nbItemsForDispersionTables = 6;

	/// Variables structure ///
	private Graph graph;


	/// Actions ///
	//////////////
	public synchronized Graph compute(final GraphMode graphMode, int nbItemsForDispersionTables) throws ClassNotFoundException, IOException {
		if (graph == null || this.nbItemsForDispersionTables != nbItemsForDispersionTables || this.graphMode != graphMode) {
			final Graph graph = new Graph();

			/// now let's build the json series ///

			// first, we must create the list of different Y values

			final boolean isStaticMode = graphMode == GraphMode.STATIC;
			if (isStaticMode) {
				nbItemsForDispersionTables = staticDispersionTable.size() + 2; // +2 because we will add the median, and the max
			} else {
				nbItemsForDispersionTables = this.nbItemsForDispersionTables;
			}
			final double[] yValues = new double[nbItemsForDispersionTables];

			graph.durationsFor1clickDispersionSeries = new ArrayList<Serie>(nbItemsForDispersionTables);

			final DescriptiveStatistics totalDurationsFor1click = durationPerPrincipalStats.computeTotalDurationsFor1click();
			if (isStaticMode) {
				// static mode : median / 1s / 2s / 3s / 5s / 10s / 20s / max (median should be ordered)
				final List<Double> yValuesList = Lists.newArrayList(staticDispersionTable);
				yValuesList.add(totalDurationsFor1click.getPercentile(50)); // Add the median
				yValuesList.add(totalDurationsFor1click.getMax()); // Add max
				final List<Double> sortedYValuesList = Ordering.natural().sortedCopy(yValuesList);
				int i = 0;
				for (final Double yValue : sortedYValuesList) {
					yValues[i++] = yValue;
				}
			} else {
				for (int i = 0; i < yValues.length; i++) {
					yValues[i] = totalDurationsFor1click.getPercentile((i+1d)/nbItemsForDispersionTables*100d);
				}
			}
			// compute the labels
			for (int i = 0; i < yValues.length; i++) {
				graph.durationsFor1clickDispersionSeries.add(new Serie(i == 0 ? 0 : (int)yValues[i - 1], (int)yValues[i]));
			}

			// Now let's compute the datas for each Y values by slice

			TimeSlice previousTimeSlice = null;
			for(final TimeSlice timeSlice : durationPerPrincipalStats.computeTimeSliceList()) {
				final long endDateTime = timeSlice.getEndDate().getTime();
				final long startDateTime = timeSlice.getStartDate().getTime();
				//TODO : améliorer cet algorithme en itérant sur chaque value de totalDurationsFor1click et pour chacune d'elle aller chercher par dichotomie l'entier à incrémenter correspondant à la bonne tranche des yValues
				final Date middleTimeSliceDate = new Date((endDateTime+startDateTime) / 2);	// the data should be displayed in the middle of the slice
				final boolean mustAppendNullForPrevious = previousTimeSlice != null && previousTimeSlice.getEndDate().getTime() != startDateTime;
				for (int i = 0; i < yValues.length; i++) {
					final Serie serie = graph.durationsFor1clickDispersionSeries.get(i);
					final double maxInclude = serie.max;
					final double minExclude = serie.min;
					final double[] values = timeSlice.getDurationsFor1click().getValues();
					int n = 0;
					for (final double value : values) {
						if (minExclude < value && value <= maxInclude) {
							n++;
						}
					}
					addNewDateDoubleValueAndNullForPreviousIfNecessary(serie.data, middleTimeSliceDate, n, mustAppendNullForPrevious, previousTimeSlice);
				}
				// adding cpu & memory info
				addNewDateDoubleValueAndNullForPreviousIfNecessary(graph.cpuUsageMeans, middleTimeSliceDate, timeSlice.getCpuUsage().getMean(), mustAppendNullForPrevious, previousTimeSlice);
				addNewDateDoubleValueAndNullForPreviousIfNecessary(graph.memoryUsageMeans, middleTimeSliceDate, timeSlice.getMemoryUsage().getMean(), mustAppendNullForPrevious, previousTimeSlice);
				addNewDateDoubleValueAndNullForPreviousIfNecessary(graph.nbUsersMax, middleTimeSliceDate, timeSlice.getStatsPerPrincipal().size(), mustAppendNullForPrevious, previousTimeSlice);
				addNewDateDoubleValueAndNullForPreviousIfNecessary(graph.durationsFor1clickMedians, middleTimeSliceDate, timeSlice.getDurationsFor1click().getPercentile(50), mustAppendNullForPrevious, previousTimeSlice);

				previousTimeSlice = timeSlice;
			}

			this.nbItemsForDispersionTables = nbItemsForDispersionTables;
			this.graphMode = graphMode;
			this.graph = graph; // because the changement of the statistics will reset the graph here
		}

		return graph;
	}

	/// Events ///
	/////////////
	public void onDurationPerPrincipalStatsChanged(@Observes @Changed final DurationPerPrincipalStats durationPerPrincipalStats) {
		resetComputation();
	}

	/// Utils ///
	////////////
	private void addNewDateDoubleValueAndNullForPreviousIfNecessary(final List<DateDoubleValue> list, final Date date, final double doubleValue, final boolean mustAppendNullForPrevious, final TimeSlice previousTimeSlice) {
		if (mustAppendNullForPrevious) {
			list.add(new DateDoubleValue(previousTimeSlice.getEndDate(), null));
		}
		list.add(new DateDoubleValue(date, doubleValue));
	}

	private void resetComputation() {
		graph = null;
	}
}
