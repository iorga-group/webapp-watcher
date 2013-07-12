package com.iorga.webappwatcher.analyzer.model.session;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Qualifier;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iorga.webappwatcher.analyzer.model.session.DurationPerPrincipalStats.TimeSlice.StatsPerPrincipal;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FileMetadataReader;
import com.iorga.webappwatcher.analyzer.model.session.UploadedFiles.FilesChanged;
import com.iorga.webappwatcher.analyzer.util.JSF21AndRichFaces4RequestActionFilter;
import com.iorga.webappwatcher.analyzer.util.RequestActionFilter;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

@SessionScoped
public class DurationPerPrincipalStats implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final long NULL_AFTER_PROCESS_DATE_DURATION_MILLIS = 45 * 1000;

	/// Dependencies ///
	@Inject
	private UploadedFiles uploadedFiles;

	@Inject
	private Configurations configurations;

	@Inject
	private @Changed Event<DurationPerPrincipalStats> changedEvent;

	public static class TimeSlice implements Serializable {
		private static final long serialVersionUID = 1L;

		private final Date startDate;
		private final Date endDate;
		private final DescriptiveStatistics durationsBetween2clicks = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1click = new DescriptiveStatistics();
		private final DescriptiveStatistics cpuUsage = new DescriptiveStatistics();
		private final DescriptiveStatistics memoryUsage = new DescriptiveStatistics();

		public static class StatsPerPrincipal implements Serializable {
			private static final long serialVersionUID = 1L;

			private final DescriptiveStatistics durationsBetween2clicks = new DescriptiveStatistics();
			private final DescriptiveStatistics durationsFor1click = new DescriptiveStatistics();
		}
		private final Map<String, StatsPerPrincipal> statsPerPrincipal = Maps.newHashMap();

		public TimeSlice(final Date startDate, final long timeSliceDurationMillis) {
			this.startDate = startDate;
			this.endDate = new Date(startDate.getTime() + timeSliceDurationMillis);
		}

		public Date getStartDate() {
			return startDate;
		}
		public Date getEndDate() {
			return endDate;
		}
		public DescriptiveStatistics getDurationsFor1click() {
			return durationsFor1click;
		}
		public Map<String, StatsPerPrincipal> getStatsPerPrincipal() {
			return statsPerPrincipal;
		}
		public DescriptiveStatistics getCpuUsage() {
			return cpuUsage;
		}
		public DescriptiveStatistics getMemoryUsage() {
			return memoryUsage;
		}
	}

	public static class PrincipalContext implements Serializable {
		private static final long serialVersionUID = 1L;
		private Date lastClick;
	}

	public static class DayStatistic implements Serializable {
		private static final long serialVersionUID = 1L;
		private final Date startDate;
		private Date endDate;
		private final DescriptiveStatistics distinctUsers = new DescriptiveStatistics();
		private final DescriptiveStatistics numberOfRequests = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1clickSum = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1clickMean = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1clickMedian = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1click90c = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1clickMin = new DescriptiveStatistics();
		private final DescriptiveStatistics durationsFor1clickMax = new DescriptiveStatistics();

		public DayStatistic(final Date startDate) {
			this.startDate = startDate;
		}

		public Date getStartDate() {
			return startDate;
		}
		public Date getEndDate() {
			return endDate;
		}
		public DescriptiveStatistics getDistinctUsers() {
			return distinctUsers;
		}
		public DescriptiveStatistics getNumberOfRequests() {
			return numberOfRequests;
		}
		public DescriptiveStatistics getDurationsFor1clickSum() {
			return durationsFor1clickSum;
		}
		public DescriptiveStatistics getDurationsFor1clickMean() {
			return durationsFor1clickMean;
		}
		public DescriptiveStatistics getDurationsFor1clickMedian() {
			return durationsFor1clickMedian;
		}
		public DescriptiveStatistics getDurationsFor1click90c() {
			return durationsFor1click90c;
		}
		public DescriptiveStatistics getDurationsFor1clickMin() {
			return durationsFor1clickMin;
		}
		public DescriptiveStatistics getDurationsFor1clickMax() {
			return durationsFor1clickMax;
		}
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
	public static @interface Changed {}

	private final List<TimeSlice> timeSlices = Lists.newArrayList();	// to search by dichotomy
	private int lastAccessedTimeSliceIndex = -1;
	private final Map<String, PrincipalContext> principalContexts = Maps.newHashMap();
	private final DescriptiveStatistics totalDurationsBetween2clicks = new DescriptiveStatistics();
	private final DescriptiveStatistics totalDurationsFor1click = new DescriptiveStatistics();

	private List<DayStatistic> dayStatistics = null;

	private boolean computed;
	private long timeSliceDurationMillis = -1;

	private final RequestActionFilter requestActionFilter = new JSF21AndRichFaces4RequestActionFilter();

	/// Actions ///
	//////////////
	public List<DayStatistic> computeDayStatistics() throws ClassNotFoundException, IOException {
		compute();

		if (dayStatistics == null && timeSlices != null) {
			dayStatistics = Lists.newLinkedList();
			DayStatistic currentDayStatistic = null;

			for (final TimeSlice timeSlice : timeSlices) {
				if (currentDayStatistic == null || !DateUtils.isSameDay(currentDayStatistic.startDate, timeSlice.startDate)) {
					// New day statistic if there is no current day statistic
					// or if the currentDayStatistic is not the same day than the timeSlice we are, let's create a new day statistic
					currentDayStatistic = new DayStatistic(timeSlice.startDate);
					dayStatistics.add(currentDayStatistic);
				}
				// expand the day statistic to that timeSlice
				currentDayStatistic.endDate = timeSlice.endDate;
				// compute each value if the timeSlice contains a data
				if (timeSlice.durationsFor1click.getN() > 0) {
					currentDayStatistic.distinctUsers.addValue(timeSlice.statsPerPrincipal.size());
					currentDayStatistic.numberOfRequests.addValue(timeSlice.durationsFor1click.getN());
					currentDayStatistic.durationsFor1clickSum.addValue(timeSlice.durationsFor1click.getSum());
					currentDayStatistic.durationsFor1clickMean.addValue(timeSlice.durationsFor1click.getMean());
					currentDayStatistic.durationsFor1clickMedian.addValue(timeSlice.durationsFor1click.getPercentile(50));
					currentDayStatistic.durationsFor1click90c.addValue(timeSlice.durationsFor1click.getPercentile(90));
					currentDayStatistic.durationsFor1clickMin.addValue(timeSlice.durationsFor1click.getMin());
					currentDayStatistic.durationsFor1clickMax.addValue(timeSlice.durationsFor1click.getMax());
				}
			}
		}
		return dayStatistics;
	}

	public List<TimeSlice> computeTimeSliceList() throws ClassNotFoundException, IOException {
		compute();

		return timeSlices;
	}

	public DescriptiveStatistics computeTotalDurationsFor1click() throws ClassNotFoundException, IOException {
		compute();

		return totalDurationsFor1click;
	}

	/// Events ///
	/////////////
	public void onUploadedFilesChanged(@Observes @FilesChanged final UploadedFiles uploadedFiles) {
		resetComputation();
	}

	/// Utils ///
	////////////
	private void compute() throws ClassNotFoundException, IOException {
		compute(configurations.getTimeSliceDurationMillis());
	}

	private synchronized void compute(final long timeSliceDurationMillis) throws ClassNotFoundException, IOException {
		if (!computed || this.timeSliceDurationMillis != timeSliceDurationMillis) {

			uploadedFiles.readFiles(new FileMetadataReader() {
				@Override
				protected void handleEventLog(final EventLog eventLog) throws IOException {
					RequestEventLog requestEventLog = null;
					final Date eventDate = eventLog.getDate();

					if (eventLog instanceof SystemEventLog) {
						final TimeSlice timeSlice = getTimeSlice(eventDate, timeSliceDurationMillis);
						final SystemEventLog systemEventLog = (SystemEventLog) eventLog;
						timeSlice.cpuUsage.addValue(systemEventLog.getCpuUsage());
						timeSlice.memoryUsage.addValue(systemEventLog.getNonHeapMemoryUsed() + systemEventLog.getHeapMemoryUsed());
					} else if (eventLog instanceof RequestEventLog && requestActionFilter.isAnActionRequest(requestEventLog = (RequestEventLog) eventLog)) {
						final String principal = requestEventLog.getPrincipal();
						// Compute duration for 1 click
						final TimeSlice timeSlice = getTimeSlice(eventDate, timeSliceDurationMillis);
						final StatsPerPrincipal statsPerPrincipal = getStatsPerPrincipal(timeSlice, principal);
						Long durationMillis = requestEventLog.getDurationMillis();
						if (durationMillis == null) {
							System.err.println("Null duration, will treat it as "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+"ms");
							durationMillis = NULL_AFTER_PROCESS_DATE_DURATION_MILLIS;
						}
						statsPerPrincipal.durationsFor1click.addValue(durationMillis);
						timeSlice.durationsFor1click.addValue(durationMillis);
						totalDurationsFor1click.addValue(durationMillis);
						// Compute duration between 2 clicks
						final PrincipalContext principalContext = getPrincipalContext(principal);
						final Date lastClick = principalContext.lastClick;
						if (lastClick != null) {
							// Last click, we can compute the duration between the two clicks
							final TimeSlice lastClickTimeSlice = getTimeSlice(lastClick, timeSliceDurationMillis);
							final StatsPerPrincipal lastClickStatsPerPrincipal = getStatsPerPrincipal(lastClickTimeSlice, principal);
							final long duration = eventDate.getTime() - lastClick.getTime();
							lastClickStatsPerPrincipal.durationsBetween2clicks.addValue(duration);
							timeSlice.durationsBetween2clicks.addValue(duration);
							totalDurationsBetween2clicks.addValue(duration);
						}
						principalContext.lastClick = eventDate;
					}
				}
			});

			this.timeSliceDurationMillis = timeSliceDurationMillis;
			this.computed = true;

			changedEvent.fire(this);
		}
	}

	private TimeSlice getTimeSlice(final Date date, final long timeSliceDurationMillis) {
		final int index = lastAccessedTimeSliceIndex;
		if (index == -1) {
			// no last accessed time slice, we must found the slice by dichotomy
			return findOrCreateTimeSlice(date, timeSliceDurationMillis);
		} else {
			// let's see if the date fits in current time slice
			final TimeSlice currentTimeSlice = timeSlices.get(index);
			if (isDateInTimeSlice(date, currentTimeSlice)) {
				return returnTimeSlice(currentTimeSlice, index);
			} else if (date.before(currentTimeSlice.startDate)) {
				// the date is before, let's check the previous index
				assert index > 0;	// should never append because the time slice should already have been created
				final int previousIndex = index - 1;
				final TimeSlice previousTimeSlice = timeSlices.get(previousIndex);
				if (isDateInTimeSlice(date, previousTimeSlice)) {
					return returnTimeSlice(previousTimeSlice, previousIndex);
				} else {
					// does not fit in the previous time slice, must find it or create it by dichotomy
					return findOrCreateTimeSlice(date, timeSliceDurationMillis);
				}
			} else {
				// we are after, let's check if the next slice exists and create it other wise
				final int nextIndex = index + 1;
				if (timeSlices.size() <= nextIndex) {
					// let's create the new time slice
					final TimeSlice timeSlice = createNewTimeSlice(date, timeSliceDurationMillis);
					return returnTimeSlice(timeSlice, nextIndex);
				} else {
					final TimeSlice nextTimeSlice = timeSlices.get(nextIndex);
					if (isDateInTimeSlice(date, nextTimeSlice)) {
						return returnTimeSlice(nextTimeSlice, nextIndex);
					} else {
						// does not fit in the next time slice, must find it or create it by dichotomy
						return findOrCreateTimeSlice(date, timeSliceDurationMillis);
					}
				}
			}
		}
	}

	private TimeSlice findOrCreateTimeSlice(final Date date, final long timeSliceDurationMillis) {
		// will binary search the time slice
		int low = 0;
		int high = timeSlices.size() - 1;
		int mid;
		while (low <= high) {
			mid = (low + high) / 2;
			final TimeSlice midTimeSlice = timeSlices.get(mid);
			if (date.after(midTimeSlice.endDate)) {
				low = mid + 1;
			} else if (date.before(midTimeSlice.startDate)) {
				high = mid - 1;
			} else {
				return midTimeSlice;
			}
		}
		// not found in the existing time slices, must create a new one
		return createNewTimeSlice(date, timeSliceDurationMillis);
	}

	private boolean isDateInTimeSlice(final Date date, final TimeSlice currentTimeSlice) {
		return date.after(currentTimeSlice.startDate) && date.before(currentTimeSlice.endDate);
	}

	private TimeSlice returnTimeSlice(final TimeSlice timeSlice, final int index) {
		lastAccessedTimeSliceIndex = index;
		return timeSlice;
	}

	private TimeSlice createNewTimeSlice(final Date date, final long timeSliceDurationMillis) {
		// As the time slices are created in a sorted way, we can create it just by looking the last slice, check if the date fits inside a new slice
		// which would be just after, and if not, add it a new one which begins with that date
		final int lastIndex = timeSlices.size() - 1;
		final TimeSlice newTimeSlice;
		if (lastIndex >= 0) {
			final TimeSlice lastTimeSlice = timeSlices.get(lastIndex);
			final Date endDate = lastTimeSlice.endDate;
			final TimeSlice newTimeSliceJustAfterLast = new TimeSlice(endDate, timeSliceDurationMillis);
			if (isDateInTimeSlice(date, newTimeSliceJustAfterLast)) {
				newTimeSlice = newTimeSliceJustAfterLast;
			} else {
				// the given date doesn't fit in the next time slice, let's create a new one for it
				newTimeSlice = new TimeSlice(date, timeSliceDurationMillis);
			}
		} else {
			// Create a new time slice because there is no last time slice
			newTimeSlice = new TimeSlice(date, timeSliceDurationMillis);
		}
		timeSlices.add(newTimeSlice);
		return newTimeSlice;
	}

	private StatsPerPrincipal getStatsPerPrincipal(final TimeSlice timeSlice, final String principal) {
		StatsPerPrincipal statsPerPrincipal = timeSlice.statsPerPrincipal.get(principal);
		if (statsPerPrincipal == null) {
			statsPerPrincipal = new StatsPerPrincipal();
			timeSlice.statsPerPrincipal.put(principal, statsPerPrincipal);
		}
		return statsPerPrincipal;
	}

	private PrincipalContext getPrincipalContext(final String principal) {
		PrincipalContext principalContext = principalContexts.get(principal);
		if (principalContext == null) {
			principalContext = new PrincipalContext();
			principalContexts.put(principal, principalContext);
		}
		return principalContext;
	}

	private void resetComputation() {
		timeSlices.clear();
		lastAccessedTimeSliceIndex = -1;
		principalContexts.clear();
		totalDurationsBetween2clicks.clear();
		totalDurationsFor1click.clear();

		computed = false;
		timeSliceDurationMillis = -1;
		dayStatistics = null;
	}
}
