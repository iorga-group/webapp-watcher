package com.iorga.webappwatcher.web;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.tukaani.xz.CorruptedInputException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;
import com.iorga.webappwatcher.web.StatisticsAction.DurationPerPrincipalStats.PrincipalContext;
import com.iorga.webappwatcher.web.StatisticsAction.DurationPerPrincipalStats.TimeSlice;
import com.iorga.webappwatcher.web.StatisticsAction.DurationPerPrincipalStats.TimeSlice.StatsPerPrincipal;

@ManagedBean
@ViewScoped
public class StatisticsAction implements Serializable {
	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final long AGGLOMERATION_DURATION_MILLIS = 10 * 60 * 1000;
	private static final long NULL_AFTER_PROCESS_DATE_DURATION_MILLIS = 45 * 1000;
	private static final long RELEVENT_REQUEST_DURATION_THRESHOLD = 500;
	private static final long TIME_SLICE_DURATION_MILLIS = 30 * 60 * 1000; // 30mn
	private static final int MAX_ITEMS_FOR_DISPERSION_TABLES = 12;

	private List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();

	private static class CSVDurationStatsLine {
		private Date startDate = null;
		private final Set<String> principals = new HashSet<String>();
		private final DescriptiveStatistics durations = new DescriptiveStatistics();
		private final DescriptiveStatistics releventDurations = new DescriptiveStatistics();
	}

	public static class DurationPerPrincipalStats {
		public static class TimeSlice {
			private final Date startDate;
			private final Date endDate;
			private final DescriptiveStatistics durationsBetween2clicks = new DescriptiveStatistics();
			private final DescriptiveStatistics durationsFor1click = new DescriptiveStatistics();

			public static class StatsPerPrincipal {
				private final DescriptiveStatistics durationsBetween2clicks = new DescriptiveStatistics();
				private final DescriptiveStatistics durationsFor1click = new DescriptiveStatistics();
			}
			private final Map<String, StatsPerPrincipal> statsPerPrincipal = Maps.newHashMap();

			public TimeSlice(final Date startDate) {
				this.startDate = startDate;
				this.endDate = new Date(startDate.getTime() + TIME_SLICE_DURATION_MILLIS);
			}
		}
		public static class PrincipalContext {
			private Date lastClick;
		}
		private final List<TimeSlice> timeSlices = Lists.newArrayList();	// to search by dichotomy
		private int lastAccessedTimeSliceIndex = -1;
		private final Map<String, PrincipalContext> principalContexts = Maps.newHashMap();
		private final DescriptiveStatistics totalDurationsBetween2clicks = new DescriptiveStatistics();
		private final DescriptiveStatistics totalDurationsFor1click = new DescriptiveStatistics();
	}

	public static interface RequestActionFilter {
		public boolean isAnActionRequest(final RequestEventLog requestEventLog);
	}

	public static class JSF21AndRichFaces4RequestActionFilter implements RequestActionFilter {
		@Override
		public boolean isAnActionRequest(final RequestEventLog requestEventLog) {
			final String requestURI = requestEventLog.getRequestURI();
			return !requestURI.contains("/javax.faces.resource/") && !requestURI.contains("/rfRes/")
				&& (requestEventLog.getMethod().equals("GET") || isNotAnAjaxPostOrIsNotAPollerRequest(requestEventLog));
		}

		private boolean isNotAnAjaxPostOrIsNotAPollerRequest(final RequestEventLog requestEventLog) {
//			String javaxEvent = null;
			String javaxSource = null;
			boolean ajaxEvent = false;
			for (final Parameter parameter : requestEventLog.getParameters()) {
				final String parameterName = parameter.getName();
				if (parameterName.equals("AJAX:EVENTS_COUNT")) {
					ajaxEvent = true;
//				} else if (parameterName.equals("javax.faces.partial.event")) {
//					javaxEvent = parameter.getValues()[0];
				} else if (parameterName.equals("javax.faces.source")) {
					javaxSource = parameter.getValues()[0];
				}
			}
//			return !ajaxEvent || ("click".equals(javaxEvent) || "change".equals(javaxEvent) || "keydown".equals(ajaxEvent) || "rich:datascroller:onscroll".equals(ajaxEvent));
			return !ajaxEvent || !javaxSource.contains("poller");
		}
	}

	/// Actions ///
	//////////////
	public void extractDurationStats() throws IOException, ClassNotFoundException {
		// based on http://stackoverflow.com/a/9394237/535203
		final FacesContext facesContext = FacesContext.getCurrentInstance();

		final HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		response.reset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
		response.setContentType("text/csv"); // Check http://www.w3schools.com/media/media_mimeref.asp for all types. Use if necessary ServletContext#getMimeType() for auto-detection based on filename.
//		response.setContentLength(contentLength); // Set it with the file size. This header is optional. It will work if it's omitted, but the download progress will be unknown.
		response.setHeader("Content-Disposition", "attachment; filename=\"extract.csv\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.

		final ServletOutputStream outputStream = response.getOutputStream();

		outputStream.println("Start date;End date;Distinct users;Number of requests;Duration;Average;Median;90c;Relevent number of requests;Relevent duration;Relevent Average;Relevent Median;Relevent 90c");

		for (final UploadedFile uploadedFile : uploadedFiles) {
			readEventLogsForDurationStats(outputStream, uploadedFile.getInputstream(), uploadedFile.getFileName(), new JSF21AndRichFaces4RequestActionFilter());
		}

		facesContext.responseComplete();
	}

	public void extractDurationPerPrincipalStats() throws IOException, ClassNotFoundException {
		final FacesContext facesContext = FacesContext.getCurrentInstance();

		final HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
		response.reset(); // Some JSF component library or some Filter might have set some headers in the buffer beforehand. We want to get rid of them, else it may collide.
		response.setContentType("text/csv"); // Check http://www.w3schools.com/media/media_mimeref.asp for all types. Use if necessary ServletContext#getMimeType() for auto-detection based on filename.
		response.setHeader("Content-Disposition", "attachment; filename=\"extract.csv\""); // The Save As popup magic is done here. You can give it any file name you want, this only won't work in MSIE, it will use current request URL as file name instead.


		final DurationPerPrincipalStats durationPerPrincipalStats = new DurationPerPrincipalStats();

		for (final UploadedFile uploadedFile : uploadedFiles) {
			readEventLogsForDurationPerPrincipalStats(uploadedFile.getInputstream(), uploadedFile.getFileName(), durationPerPrincipalStats, new JSF21AndRichFaces4RequestActionFilter());
		}

		final ServletOutputStream outputStream = response.getOutputStream();
		writeDurationPerPrincpalStats(durationPerPrincipalStats, outputStream);

		facesContext.responseComplete();
	}

	/// Events ///
	/////////////
	public void handleFileUpload(final FileUploadEvent event) throws IOException, ClassNotFoundException {
		System.out.println("Handling "+event.getFile().getFileName());
		uploadedFiles.add(event.getFile());
	}

	/// Utils ///
	////////////
	private void readEventLogsForDurationStats(final ServletOutputStream outputStream, final InputStream inputstream, final String fileName, final RequestActionFilter requestActionFilter) throws IOException, ClassNotFoundException {
		final ObjectInputStream objectInputStream = EventLogManager.readLog(inputstream, fileName);
		try {
			CSVDurationStatsLine csvLine = new CSVDurationStatsLine();

			EventLog eventLog;
			RequestEventLog requestEventLog = null;
			try {
				while ((eventLog = readEventLog(objectInputStream)) != null) {
					if (eventLog instanceof RequestEventLog) {
						requestEventLog = (RequestEventLog) eventLog;
						if (requestActionFilter.isAnActionRequest(requestEventLog)) {
							csvLine = readRequestEventLogForDurationStats(requestEventLog, csvLine, outputStream);
						}
					}
				}
			} catch (final EOFException e) {
				// Normal end of the read file
			}
			writeCsvDurationStatsLine(csvLine, requestEventLog, outputStream);
		} finally {
			objectInputStream.close();
		}
	}

	private CSVDurationStatsLine readRequestEventLogForDurationStats(final RequestEventLog requestEventLog, CSVDurationStatsLine csvLine, final ServletOutputStream outputStream) throws IOException {
		final Date date = requestEventLog.getDate();
		if (csvLine.startDate == null) {
			csvLine.startDate = date;
		} else if(date.getTime() - csvLine.startDate.getTime() > AGGLOMERATION_DURATION_MILLIS) {
			writeCsvDurationStatsLine(csvLine, requestEventLog, outputStream);
			csvLine = new CSVDurationStatsLine();
			csvLine.startDate = date;
		}
		// adding a point in the csv line
		Long durationMillis = requestEventLog.getDurationMillis();
		if (durationMillis == null) {
			System.err.println("Null duration, will treat it as "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+"ms");
			durationMillis = NULL_AFTER_PROCESS_DATE_DURATION_MILLIS;
		}
		csvLine.durations.addValue(durationMillis);
		if (durationMillis > RELEVENT_REQUEST_DURATION_THRESHOLD) {
			csvLine.releventDurations.addValue(durationMillis);
		}
		csvLine.principals.add(requestEventLog.getPrincipal());

		return csvLine;
	}

	private void writeCsvDurationStatsLine(final CSVDurationStatsLine csvLine, final RequestEventLog currentRequestEventLog, final ServletOutputStream outputStream) throws IOException {
		if (csvLine.startDate != null) { // else the csvLine has never been filled
			final DescriptiveStatistics durations = csvLine.durations;
			final DescriptiveStatistics releventDurations = csvLine.releventDurations;
			final StringBuilder line = new StringBuilder();
			// Processing "Start date;End date;Distinct users;Number of requests;Duration;Average;Median;90c;Relevent number of requests;Relevent duration;Relevent Average;Relevent Median;Relevent 90c"
			final Date currentRequestDate = currentRequestEventLog.getDate();
			line.append(dateFormatter.format(csvLine.startDate)).append(";")
				.append(dateFormatter.format(currentRequestDate != null ? currentRequestDate : csvLine.startDate)).append(";")
				.append(csvLine.principals.size()).append(";")
				.append(durations.getN()).append(";")
				.append((int)durations.getSum()).append(";")
				.append((int)durations.getMean()).append(";")
				.append((int)durations.getPercentile(50)).append(";")
				.append((int)durations.getPercentile(90)).append(";")
				.append(releventDurations.getN()).append(";")
				.append((int)releventDurations.getSum()).append(";")
				.append((int)releventDurations.getMean()).append(";")
				.append((int)releventDurations.getPercentile(50)).append(";")
				.append((int)releventDurations.getPercentile(90)).append(";");

			outputStream.println(line.toString());
		}
	}

	private void readEventLogsForDurationPerPrincipalStats(final InputStream inputstream, final String fileName, final DurationPerPrincipalStats durationPerPrincipalStats, final RequestActionFilter requestActionFilter) throws FileNotFoundException, IOException, ClassNotFoundException {
		final ObjectInputStream objectInputStream = EventLogManager.readLog(inputstream, fileName);
		try {
			EventLog eventLog;
			RequestEventLog requestEventLog = null;
			try {
				while ((eventLog = readEventLog(objectInputStream)) != null) {
					if (eventLog instanceof RequestEventLog) {
						requestEventLog = (RequestEventLog) eventLog;

						readRequestEventLogForDurationPerPrincipalStats(requestEventLog, durationPerPrincipalStats, requestActionFilter);
					}
				}
			} catch (final EOFException e) {
				// Normal end of the read file
			}
		} finally {
			objectInputStream.close();
		}
	}

	private void readRequestEventLogForDurationPerPrincipalStats(final RequestEventLog requestEventLog, final DurationPerPrincipalStats stats, final RequestActionFilter requestActionFilter) {
		if (requestActionFilter.isAnActionRequest(requestEventLog)) {
			final String principal = requestEventLog.getPrincipal();
			final Date requestDate = requestEventLog.getDate();
			// Compute duration for 1 click
			final TimeSlice timeSlice = getTimeSlice(requestDate, stats);
			final StatsPerPrincipal statsPerPrincipal = getStatsPerPrincipal(timeSlice, principal);
			Long durationMillis = requestEventLog.getDurationMillis();
			if (durationMillis == null) {
				System.err.println("Null duration, will treat it as "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+"ms");
				durationMillis = NULL_AFTER_PROCESS_DATE_DURATION_MILLIS;
			}
			statsPerPrincipal.durationsFor1click.addValue(durationMillis);
			timeSlice.durationsFor1click.addValue(durationMillis);
			stats.totalDurationsFor1click.addValue(durationMillis);
			// Compute duration between 2 clicks
			final PrincipalContext principalContext = getPrincipalContext(stats, principal);
			final Date lastClick = principalContext.lastClick;
			if (lastClick != null) {
				// Last click, we can compute the duration between the two clicks
				final TimeSlice lastClickTimeSlice = getTimeSlice(lastClick, stats);
				final StatsPerPrincipal lastClickStatsPerPrincipal = getStatsPerPrincipal(lastClickTimeSlice, principal);
				final long duration = requestDate.getTime() - lastClick.getTime();
				lastClickStatsPerPrincipal.durationsBetween2clicks.addValue(duration);
				timeSlice.durationsBetween2clicks.addValue(duration);
				stats.totalDurationsBetween2clicks.addValue(duration);
			}
			principalContext.lastClick = requestDate;
		}
	}

	private TimeSlice getTimeSlice(final Date date, final DurationPerPrincipalStats stats) {
		final int index = stats.lastAccessedTimeSliceIndex;
		if (index == -1) {
			// no last accessed time slice, we must found the slice by dichotomy
			return findOrCreateTimeSlice(date, stats);
		} else {
			// let's see if the date fits in current time slice
			final TimeSlice currentTimeSlice = stats.timeSlices.get(index);
			if (isDateInTimeSlice(date, currentTimeSlice)) {
				return returnTimeSlice(currentTimeSlice, stats, index);
			} else if (date.before(currentTimeSlice.startDate)) {
				// the date is before, let's check the previous index
				assert index > 0;	// should never append because the time slice should already have been created
				final int previousIndex = index - 1;
				final TimeSlice previousTimeSlice = stats.timeSlices.get(previousIndex);
				if (isDateInTimeSlice(date, previousTimeSlice)) {
					return returnTimeSlice(previousTimeSlice, stats, previousIndex);
				} else {
					// does not fit in the previous time slice, must find it or create it by dichotomy
					return findOrCreateTimeSlice(date, stats);
				}
			} else {
				// we are after, let's check if the next slice exists and create it other wise
				final int nextIndex = index + 1;
				if (stats.timeSlices.size() <= nextIndex) {
					// let's create the new time slice
					final TimeSlice timeSlice = createNewTimeSlice(date, stats);
					return returnTimeSlice(timeSlice, stats, nextIndex);
				} else {
					final TimeSlice nextTimeSlice = stats.timeSlices.get(nextIndex);
					if (isDateInTimeSlice(date, nextTimeSlice)) {
						return returnTimeSlice(nextTimeSlice, stats, nextIndex);
					} else {
						// does not fit in the next time slice, must find it or create it by dichotomy
						return findOrCreateTimeSlice(date, stats);
					}
				}
			}
		}
	}

	private TimeSlice findOrCreateTimeSlice(final Date date, final DurationPerPrincipalStats stats) {
		final List<TimeSlice> timeSlices = stats.timeSlices;
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
		return createNewTimeSlice(date, stats);
	}

	private boolean isDateInTimeSlice(final Date date, final TimeSlice currentTimeSlice) {
		return date.after(currentTimeSlice.startDate) && date.before(currentTimeSlice.endDate);
	}

	private TimeSlice returnTimeSlice(final TimeSlice timeSlice, final DurationPerPrincipalStats stats, final int index) {
		stats.lastAccessedTimeSliceIndex = index;
		return timeSlice;
	}

	private TimeSlice createNewTimeSlice(final Date date, final DurationPerPrincipalStats stats) {
		// As the time slices are created in a sorted way, we can create it just by looking the last slice, check if the date fits inside a new slice
		// which would be just after, and if not, add it a new one which begins with that date
		final List<TimeSlice> timeSlices = stats.timeSlices;
		final int lastIndex = timeSlices.size() - 1;
		final TimeSlice newTimeSlice;
		if (lastIndex >= 0) {
			final TimeSlice lastTimeSlice = timeSlices.get(lastIndex);
			final Date endDate = lastTimeSlice.endDate;
			final TimeSlice newTimeSliceJustAfterLast = new TimeSlice(endDate);
			if (isDateInTimeSlice(date, newTimeSliceJustAfterLast)) {
				newTimeSlice = newTimeSliceJustAfterLast;
			} else {
				// the given date doesn't fit in the next time slice, let's create a new one for it
				newTimeSlice = new TimeSlice(date);
			}
		} else {
			// Create a new time slice because there is no last time slice
			newTimeSlice = new TimeSlice(date);
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

	private PrincipalContext getPrincipalContext(final DurationPerPrincipalStats stats, final String principal) {
		PrincipalContext principalContext = stats.principalContexts.get(principal);
		if (principalContext == null) {
			principalContext = new PrincipalContext();
			stats.principalContexts.put(principal, principalContext);
		}
		return principalContext;
	}

	private EventLog readEventLog(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		try {
			return (EventLog) objectInputStream.readObject();
		} catch (final CorruptedInputException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static abstract class DurationPerPrincpalStatsWriter {
		public abstract String getTitle();
		public abstract DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(StatsPerPrincipal statsPerPrincipal);
		public abstract DescriptiveStatistics getTotalDescriptiveStatistics(DurationPerPrincipalStats stats);
		public abstract double getStatValue(DescriptiveStatistics descriptiveStatistics);

		public void write(final DurationPerPrincipalStats stats, final ServletOutputStream out) throws IOException {
			/// Waiting time between 2 clicks - Mean
			out.println(getTitle());
			// List principals
			out.print(";Principals;");
			final List<String> principals = Lists.newLinkedList(stats.principalContexts.keySet());
			for (final String principal : principals) {
				out.print(principal + ";");
			}
			out.println();
			// for each time slice
			for(final TimeSlice timeSlice : stats.timeSlices) {
				final StringBuilder line = new StringBuilder();
				line.append(dateFormatter.format(timeSlice.startDate)).append(';')
					.append(dateFormatter.format(timeSlice.endDate)).append(';');
				for (final String principal : principals) {
					final StatsPerPrincipal statsPerPrincipal = timeSlice.statsPerPrincipal.get(principal);
					if (statsPerPrincipal != null) {
						line.append((int)getStatValue(getDescriptiveStatisticsForStatsPerPrincipal(statsPerPrincipal)));
					}
					line.append(';');
				}
				out.println(line.toString());
			}
			out.println("Total:;"+(int)getStatValue(getTotalDescriptiveStatistics(stats)));
			out.println();
		}
	}

	public static abstract class DurationPerDispersionStatsWriter {
		public abstract String getTitle();
		public abstract String getYTitle();
		public abstract DescriptiveStatistics getTotalDescriptiveStatistics(DurationPerPrincipalStats stats);
		public abstract DescriptiveStatistics getDescriptiveStatisticsPerTimeSlice(TimeSlice timeSlice);
		public abstract Object getValueForN(int n, DescriptiveStatistics descriptiveStatisticsPerTimeSlice);

		public void write(final DurationPerPrincipalStats stats, final ServletOutputStream out) throws IOException {
			/// Waiting time between 2 clicks - Mean
			out.println(getTitle());
			// List principals
			out.print(";"+getYTitle()+";");

			final DescriptiveStatistics totalDescriptiveStatistics = getTotalDescriptiveStatistics(stats);

			final double[] yValues = new double[MAX_ITEMS_FOR_DISPERSION_TABLES];
			// Create the list of different Y values
			for (int i = 0; i < yValues.length; i++) {
				yValues[i] = totalDescriptiveStatistics.getPercentile((i+1d)/MAX_ITEMS_FOR_DISPERSION_TABLES*100d);
			}
			// Print it as header
			for (int i = 0; i < yValues.length; i++) {
				final double yValue = yValues[i];
				if (i == 0) {
					out.print(0);
				} else {
					out.print((int)yValues[i - 1]);
				}
				out.print(" - "+(int)yValue + ";");
			}
			out.println("Total;");
			// for each time slice
			for(final TimeSlice timeSlice : stats.timeSlices) {
				final StringBuilder line = new StringBuilder();
				line.append(dateFormatter.format(timeSlice.startDate)).append(';')
					.append(dateFormatter.format(timeSlice.endDate)).append(';');

				final DescriptiveStatistics descriptiveStatisticsPerTimeSlice = getDescriptiveStatisticsPerTimeSlice(timeSlice);
				for (int i = 0; i < yValues.length; i++) {
					final double maxInclude = yValues[i];
					final double minExclude = i == 0 ? 0d : yValues[i - 1];
					final double[] values = descriptiveStatisticsPerTimeSlice.getValues();
					int n = 0;
					for (final double value : values) {
						if (minExclude < value && value <= maxInclude) {
							n++;
						}
					}
					line.append(getValueForN(n, descriptiveStatisticsPerTimeSlice)).append(';');
				}
				line.append(getValueForN((int) descriptiveStatisticsPerTimeSlice.getN(), descriptiveStatisticsPerTimeSlice)).append(';');
				out.println(line.toString());
			}
			out.println();
		}
	}

	private void writeDurationPerPrincpalStats(final DurationPerPrincipalStats stats, final ServletOutputStream out) throws IOException {
		//// Waiting time between 2 clicks ////
		/// Waiting time between 2 clicks - Mean
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Thinking time (Mean)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsBetween2clicks;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsBetween2clicks;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getMean();
			}
		}.write(stats, out);
		/// Waiting time between 2 clicks - Number of elements
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Thinking time (Number of elements)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsBetween2clicks;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsBetween2clicks;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getN();
			}
		}.write(stats, out);
		/// Waiting time between 2 clicks - Median
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Thinking time (Median)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsBetween2clicks;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsBetween2clicks;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getPercentile(50);
			}
		}.write(stats, out);
		/// Waiting time between 2 clicks - Total Dispersion number of element
		new DurationPerDispersionStatsWriter() {
			@Override
			public String getTitle() {
				return "Thinking time (Dispersion - Number of elements)";
			}
			@Override
			public String getYTitle() {
				return "Dispersion slice";
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsBetween2clicks;
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsPerTimeSlice(final TimeSlice timeSlice) {
				return timeSlice.durationsBetween2clicks;
			}
			@Override
			public Object getValueForN(final int n, final DescriptiveStatistics descriptiveStatisticsPerTimeSlice) {
				return n;
			}
		}.write(stats, out);
		/// Waiting time between 2 clicks - Total Dispersion %
		new DurationPerDispersionStatsWriter() {
			@Override
			public String getTitle() {
				return "Thinking time (Dispersion %)";
			}
			@Override
			public String getYTitle() {
				return "Dispersion slice";
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsBetween2clicks;
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsPerTimeSlice(final TimeSlice timeSlice) {
				return timeSlice.durationsBetween2clicks;
			}
			@Override
			public Object getValueForN(final int n, final DescriptiveStatistics descriptiveStatisticsPerTimeSlice) {
				return (((double)n/(double)descriptiveStatisticsPerTimeSlice.getN())*100d);
			}
		}.write(stats, out);

		//// Waiting time for one click ////
		/// Waiting time for one click - Mean
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Request duration (Mean)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsFor1click;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsFor1click;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getMean();
			}
		}.write(stats, out);
		/// Waiting time for one click - Number of elements
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Request duration (Number of elements)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsFor1click;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsFor1click;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getN();
			}
		}.write(stats, out);
		/// Waiting time for one click - Median
		new DurationPerPrincpalStatsWriter() {
			@Override
			public String getTitle() {
				return "Request duration (Median)";
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsForStatsPerPrincipal(final StatsPerPrincipal statsPerPrincipal) {
				return statsPerPrincipal.durationsFor1click;
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsFor1click;
			}
			@Override
			public double getStatValue(final DescriptiveStatistics descriptiveStatistics) {
				return descriptiveStatistics.getPercentile(50);
			}
		}.write(stats, out);
		/// Waiting time for one click - Total Dispersion number of element
		new DurationPerDispersionStatsWriter() {
			@Override
			public String getTitle() {
				return "Request duration (Dispersion - Number of elements)";
			}
			@Override
			public String getYTitle() {
				return "Dispersion slice";
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsFor1click;
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsPerTimeSlice(final TimeSlice timeSlice) {
				return timeSlice.durationsFor1click;
			}
			@Override
			public Object getValueForN(final int n, final DescriptiveStatistics descriptiveStatisticsPerTimeSlice) {
				return n;
			}
		}.write(stats, out);
		/// Waiting time for one click - Total Dispersion %
		new DurationPerDispersionStatsWriter() {
			@Override
			public String getTitle() {
				return "Request duration (Dispersion %)";
			}
			@Override
			public String getYTitle() {
				return "Dispersion slice";
			}
			@Override
			public DescriptiveStatistics getTotalDescriptiveStatistics(final DurationPerPrincipalStats stats) {
				return stats.totalDurationsFor1click;
			}
			@Override
			public DescriptiveStatistics getDescriptiveStatisticsPerTimeSlice(final TimeSlice timeSlice) {
				return timeSlice.durationsFor1click;
			}
			@Override
			public Object getValueForN(final int n, final DescriptiveStatistics descriptiveStatisticsPerTimeSlice) {
				return (int)(((double)n/(double)descriptiveStatisticsPerTimeSlice.getN())*100d);
			}
		}.write(stats, out);
	}

	/// Getters & Setters ///
	public List<UploadedFile> getUploadedFiles() {
		return uploadedFiles;
	}

	public void setUploadedFiles(final List<UploadedFile> uploadedFiles) {
		this.uploadedFiles = uploadedFiles;
	}
}
