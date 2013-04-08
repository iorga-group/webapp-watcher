package com.iorga.webappwatcher.web;

import java.awt.Color;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.tukaani.xz.CorruptedInputException;

import com.iorga.webappwatcher.EventLogManager;
import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

@ManagedBean
@ViewScoped
public class AnalyzerAction implements Serializable {
	private static final float PRINCIPALS_PANEL_MARGIN = 0.5f;
	private static final float PRINCIPALS_PANEL_HEIGHT = 19f;
	private static final String ALL_PRINCIPAL_VALUE = "All";

	private static final int NULL_AFTER_PROCESS_DATE_DURATION_MILLIS = 45 * 1000;


	private static final long serialVersionUID = 1L;

	private String allCpuUsageJsonValues;
	private String allMemoryUsedJsonValues;
	private String allMarkingsJson;

	private String cpuUsageJsonValues;
	private String memoryUsedJsonValues;
	private String markingsJson;

	private List<RequestEventLog> requestEventLogsSortedByDate;
	private List<SystemEventLog> systemEventLogsSortedByDate;

	private long selectedTime;
	private SystemEventLog selectedSystemEventLog;
	private List<RequestEventLog> selectedRequestEventLogs;
	private Map<String, List<RequestEventLog>> requestEventLogsByPrincipal;
	private Map<String, String> colorsByPrincipal;

	private Date allFirstEventLogDate;
	private Date allLastEventLogDate;
	private long allMaxMemoryUsed;

	private Date firstEventLogDate;
	private Date lastEventLogDate;
	private long maxMemoryUsed;

	private String selectedSystemEventLogTimeColor;
	private String selectedPrincipal;

	private boolean retentionLogLoaded = false;

	public void handleFileUpload(final FileUploadEvent event) throws IOException, ClassNotFoundException {
		readEventLogs(event.getFile().getInputstream(), event.getFile().getFileName());
	}

	public void readEventLogs(final InputStream inputstream, final String fileName) throws IOException, ClassNotFoundException {
		// Init values
		allFirstEventLogDate = null;
		allLastEventLogDate = null;
		allMaxMemoryUsed = 0;

		final ObjectInputStream objectInputStream = EventLogManager.readLog(inputstream, fileName);
		try {
			final StringBuilder cpuUsageJsonValuesBuilder = new StringBuilder();
			final StringBuilder memoryUsedJsonValuesBuilder = new StringBuilder();
			requestEventLogsByPrincipal = new HashMap<String, List<RequestEventLog>>();
			final LinkedList<SystemEventLog> systemEventLogsSortedByDate = new LinkedList<SystemEventLog>();
			EventLog lastEventLog = null;
			try {
				boolean first = true;
				requestEventLogsSortedByDate = new LinkedList<RequestEventLog>();
				EventLog eventLog;
				while ((eventLog = readEventLog(objectInputStream)) != null) {
					final Date eventLogDate = eventLog.getDate();
					if (allFirstEventLogDate == null) {
						allFirstEventLogDate = eventLogDate;
					}
					if (eventLog instanceof SystemEventLog) {
						final SystemEventLog systemEventLog = (SystemEventLog) eventLog;
						addData(eventLogDate, systemEventLog.getCpuUsage(), cpuUsageJsonValuesBuilder, first);
						final long memoryUsed = systemEventLog.getHeapMemoryUsed()+systemEventLog.getNonHeapMemoryUsed();
						if (memoryUsed > allMaxMemoryUsed) {
							// remember max memory used, for flot max y axis
							allMaxMemoryUsed = memoryUsed;
						}
						addData(eventLogDate, memoryUsed, memoryUsedJsonValuesBuilder, first);
						systemEventLogsSortedByDate.add(systemEventLog);
					}
					if (eventLog instanceof RequestEventLog) {
						final RequestEventLog requestEventLog = (RequestEventLog) eventLog;
						String principal = requestEventLog.getPrincipal();
						if (StringUtils.isEmpty(principal)) {
							principal = "";
						}
						List<RequestEventLog> requestsForThisPrincipal = requestEventLogsByPrincipal.get(principal);
						if (requestsForThisPrincipal == null) {
							requestsForThisPrincipal = new LinkedList<RequestEventLog>();
							requestEventLogsByPrincipal.put(principal, requestsForThisPrincipal);
						}
						requestsForThisPrincipal.add(requestEventLog);
						requestEventLogsSortedByDate.add(requestEventLog);
						// now search
					}
					first = false;
					lastEventLog = eventLog;
				}
			} catch (final EOFException e) {
				// Normal end of the read file
			}
			this.allCpuUsageJsonValues = cpuUsageJsonValuesBuilder.toString();
			this.allMemoryUsedJsonValues = memoryUsedJsonValuesBuilder.toString();
			// Compute the requests
			final int nbPrincipals = requestEventLogsByPrincipal.size();
			int currentPrincipal = 0;
			final StringBuilder markingsJsonBuilder = new StringBuilder();
			boolean first = true;
			colorsByPrincipal = new HashMap<String, String>();
			for (final Entry<String, List<RequestEventLog>> entry : requestEventLogsByPrincipal.entrySet()) {
				final List<RequestEventLog> requestsForOnePrincipal = entry.getValue();
				final String principal = entry.getKey();
				final float currentPrincipalFraction = ((float)currentPrincipal) / ((float)nbPrincipals);
				final String color = Integer.toHexString(Color.HSBtoRGB(currentPrincipalFraction, 0.5f, 0.7f)).substring(2);
				colorsByPrincipal.put(principal, color);
				// Spec to write = { color: '#121212', lineWidth: 2, xaxis: { from: 1343292820699, to: 1343292895085 }, yaxis: { from: -1, to: -1} }
				final float y = -PRINCIPALS_PANEL_HEIGHT * (currentPrincipal+1f) / (nbPrincipals+1f) - PRINCIPALS_PANEL_MARGIN; // 19 = 20% (y axis) - 0.5 margin top & bottom
				for (final RequestEventLog requestEventLog : requestsForOnePrincipal) {
					Date afterProcessedDate = requestEventLog.getAfterProcessedDate();
					if (afterProcessedDate == null) {
						System.err.println("AfterProcessedDate null, render date + "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+" millis for "+requestEventLog);
						afterProcessedDate = new Date(requestEventLog.getDate().getTime() + NULL_AFTER_PROCESS_DATE_DURATION_MILLIS);
					}
					if (!first) {
						markingsJsonBuilder.append(",");
					} else {
						first = false;
					}
					markingsJsonBuilder.append("{color:'#").append(color).append("',")
						.append("xaxis:{from:").append(requestEventLog.getDate().getTime()).append(",to:").append(afterProcessedDate.getTime()).append("},")
						.append("yaxis:{from:").append(y).append(",to:").append(y).append("}}");
				}
				currentPrincipal++;
			}
			this.allMarkingsJson = markingsJsonBuilder.toString();
			// putting the systemEventLogsSortedByDate in an array list in order to binary search on it later
			this.systemEventLogsSortedByDate = new ArrayList<SystemEventLog>(systemEventLogsSortedByDate);
			this.allLastEventLogDate = lastEventLog.getDate();

			this.selectedPrincipal = ALL_PRINCIPAL_VALUE;
			changeSelectedPrincipal();
		} finally {
			objectInputStream.close();
		}
		retentionLogLoaded = true;
	}

	private EventLog readEventLog(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		try {
			return (EventLog) objectInputStream.readObject();
		} catch (final CorruptedInputException e) {
			e.printStackTrace(System.err);
			return null;
		}
	}

	public void updateInfoPanel() {
		// Binary search on systemEventLogsSortedByDate to get the nearer systemEventLog
		final Date selectedDate = new Date(getSelectedTime());
		final SystemEventLog systemEventLogToFind = new SystemEventLog(selectedDate);
		if (systemEventLogsSortedByDate.size() > 0) {
			final int binaryIndex = Collections.binarySearch(systemEventLogsSortedByDate, systemEventLogToFind, new Comparator<SystemEventLog>() {
				@Override
				public int compare(final SystemEventLog o1, final SystemEventLog o2) {
					return o1.getDate().compareTo(o2.getDate());
				}
			});
			if (binaryIndex > 0) {
				// binary search found the date
				this.selectedSystemEventLog = systemEventLogsSortedByDate.get(binaryIndex);
			} else {
				final int insertionPoint = -binaryIndex - 1;
				if (insertionPoint == 0) {
					// would be inserted before the 1st element, so the selected is the 1st element
					this.selectedSystemEventLog = systemEventLogsSortedByDate.get(insertionPoint);
				} else if (insertionPoint == systemEventLogsSortedByDate.size()) {
					// would be inserted at the end of the list, so the selected is the last element
					this.selectedSystemEventLog = systemEventLogsSortedByDate.get(insertionPoint - 1);
				} else {
					// must know the eventLog date closer from the selected time
					final SystemEventLog n = systemEventLogsSortedByDate.get(insertionPoint);
					final SystemEventLog n0 = systemEventLogsSortedByDate.get(insertionPoint - 1);
					if (getSelectedTime() - n0.getDate().getTime() <= n.getDate().getTime() - getSelectedTime()) {
						// the selected date is nearer n0 than n
						this.selectedSystemEventLog = n0;
					} else {
						this.selectedSystemEventLog = n;
					}
				}
			}
			// Compute the color which symbolizes the difference between the selected time & the selected systemEventLog time : the more it's far, the redder it will be
			this.selectedSystemEventLogTimeColor = Integer.toHexString(Color.HSBtoRGB(0 /* red hue */, Math.min(1f, Math.abs(getSelectedTime() - this.selectedSystemEventLog.getDate().getTime()) / 4000f), 1f)).substring(2);
		}
		// Now search for selected request logs
		this.selectedRequestEventLogs = new LinkedList<RequestEventLog>();
		final Iterator<RequestEventLog> iterator = requestEventLogsSortedByDate.iterator();
		RequestEventLog request = null;
		while (iterator.hasNext() && (request = iterator.next()).getDate().getTime() <= getSelectedTime()) {
			Date afterProcessedDate = request.getAfterProcessedDate();
			if (afterProcessedDate == null) {
				afterProcessedDate = new Date(request.getDate().getTime() + NULL_AFTER_PROCESS_DATE_DURATION_MILLIS);
			}
			if (afterProcessedDate.getTime() >= getSelectedTime() && (ALL_PRINCIPAL_VALUE.equals(selectedPrincipal) || request.getPrincipal().equals(selectedPrincipal))) {
				// End date is after the selected time, the request is selected
				selectedRequestEventLogs.add(request);
			}
		}
	}

	private void addData(final Date date, final Number value, final StringBuilder stringBuilder, final boolean first) {
		if (!first) {
			stringBuilder.append(",");
		}
		stringBuilder.append("[").append(date.getTime()).append(",").append(value).append("]");
	}

	public void changeSelectedPrincipalListener(final ValueChangeEvent valueChangeEvent) {
		selectedPrincipal = (String) valueChangeEvent.getNewValue();
		changeSelectedPrincipal();
	}

	public void changeSelectedPrincipal() {
		if (ALL_PRINCIPAL_VALUE.equals(selectedPrincipal)) {
			firstEventLogDate = allFirstEventLogDate;
			lastEventLogDate = allLastEventLogDate;
			maxMemoryUsed = allMaxMemoryUsed;

			cpuUsageJsonValues = allCpuUsageJsonValues;
			memoryUsedJsonValues = allMemoryUsedJsonValues;
			markingsJson = allMarkingsJson;
		} else {
			final String selectedColor = colorsByPrincipal.get(selectedPrincipal);
			final float selectedPrincipalPanelY = -PRINCIPALS_PANEL_HEIGHT/2f - PRINCIPALS_PANEL_MARGIN;

			firstEventLogDate = null;
			lastEventLogDate = null;
			maxMemoryUsed = 0;

			final StringBuilder cpuUsageJsonValuesBuilder = new StringBuilder();
			final StringBuilder memoryUsedJsonValuesBuilder = new StringBuilder();
			final StringBuilder markingsJsonBuilder = new StringBuilder();

			final List<RequestEventLog> requestEventLogs = requestEventLogsByPrincipal.get(selectedPrincipal);
			Iterator<SystemEventLog> systemEventLogIterator = null;
			SystemEventLog currentSystemLog = null;
			boolean firstSystemLog = true;
			boolean firstRequestEventLog = true;

			for (final RequestEventLog requestEventLog : requestEventLogs) {
				if (firstEventLogDate == null) {
					firstEventLogDate = requestEventLog.getDate();
					if (systemEventLogsSortedByDate.size() > 0) {
						// Search for the system log, discarding previous logs
						systemEventLogIterator = systemEventLogsSortedByDate.iterator();
						while (systemEventLogIterator.hasNext() && (currentSystemLog = systemEventLogIterator.next()).getDate().getTime() < firstEventLogDate.getTime()) {}
						firstSystemLog = handleSystemEventLog(currentSystemLog, cpuUsageJsonValuesBuilder, memoryUsedJsonValuesBuilder, firstSystemLog);
					}
				}
				if (systemEventLogsSortedByDate.size() > 0 && lastEventLogDate != null) {
					// Search for the system log until the end of the request
					while (systemEventLogIterator.hasNext() && (currentSystemLog = systemEventLogIterator.next()).getDate().getTime() < lastEventLogDate.getTime()) {
						firstSystemLog = handleSystemEventLog(currentSystemLog, cpuUsageJsonValuesBuilder, memoryUsedJsonValuesBuilder, firstSystemLog);
					}
				}
				Date afterProcessedDate = requestEventLog.getAfterProcessedDate();
				if (afterProcessedDate == null) {
					System.err.println("AfterProcessedDate null, render date + "+NULL_AFTER_PROCESS_DATE_DURATION_MILLIS+" millis for "+requestEventLog);
					afterProcessedDate = new Date(requestEventLog.getDate().getTime() + NULL_AFTER_PROCESS_DATE_DURATION_MILLIS);
				}
				// Appending the current request
				if (!firstRequestEventLog) {
					markingsJsonBuilder.append(",");
				} else {
					firstRequestEventLog = false;
				}
				markingsJsonBuilder.append("{color:'#").append(selectedColor).append("',")
					.append("xaxis:{from:").append(requestEventLog.getDate().getTime()).append(",to:").append(afterProcessedDate.getTime()).append("},")
					.append("yaxis:{from:").append(selectedPrincipalPanelY).append(",to:").append(selectedPrincipalPanelY).append("}}");
				lastEventLogDate = afterProcessedDate;
			}

			cpuUsageJsonValues = cpuUsageJsonValuesBuilder.toString();
			memoryUsedJsonValues = memoryUsedJsonValuesBuilder.toString();
			markingsJson = markingsJsonBuilder.toString();
		}
	}

	private boolean handleSystemEventLog(final SystemEventLog systemEventLog, final StringBuilder cpuUsageJsonValuesBuilder, final StringBuilder memoryUsedJsonValuesBuilder, final boolean firstSystemLog) {
		if (systemEventLog != null && systemEventLog.getDate().getTime() >= firstEventLogDate.getTime()) {
			final Date eventLogDate = systemEventLog.getDate();
			addData(eventLogDate, systemEventLog.getCpuUsage(), cpuUsageJsonValuesBuilder, firstSystemLog);
			final long memoryUsed = systemEventLog.getHeapMemoryUsed()+systemEventLog.getNonHeapMemoryUsed();
			if (memoryUsed > maxMemoryUsed) {
				// remember max memory used, for flot max y axis
				maxMemoryUsed = memoryUsed;
			}
			addData(eventLogDate, memoryUsed, memoryUsedJsonValuesBuilder, firstSystemLog);
			return false;
		}
		return firstSystemLog;
	}

	/// Specials getters ///
	///////////////////////
	public Collection<String> getPrincipals() {
		return requestEventLogsByPrincipal.keySet();
	}

	/// Getters & Setters ///
	////////////////////////

	public String getCpuUsageJsonValues() {
		return cpuUsageJsonValues;
	}

	public String getMemoryUsedJsonValues() {
		return memoryUsedJsonValues;
	}

	public String getMarkingsJson() {
		return markingsJson;
	}

	public long getSelectedTime() {
		return selectedTime;
	}

	public void setSelectedTime(final long clickedTime) {
		this.selectedTime = clickedTime;
	}

	public SystemEventLog getSelectedSystemEventLog() {
		return selectedSystemEventLog;
	}

	public List<RequestEventLog> getSelectedRequestEventLogs() {
		return selectedRequestEventLogs;
	}

	public Date getFirstEventLogDate() {
		return firstEventLogDate;
	}

	public Date getLastEventLogDate() {
		return lastEventLogDate;
	}

	public long getMaxMemoryUsed() {
		return maxMemoryUsed;
	}

	public String getSelectedSystemEventLogTimeColor() {
		return selectedSystemEventLogTimeColor;
	}

	public boolean isRetentionLogLoaded() {
		return retentionLogLoaded;
	}

	public String getSelectedPrincipal() {
		return selectedPrincipal;
	}

	public void setSelectedPrincipal(final String selectedPrincipal) {
		this.selectedPrincipal = selectedPrincipal;
	}

	public String getAllPrincipalValue() {
		return ALL_PRINCIPAL_VALUE;
	}

	public Map<String, String> getColorsByPrincipal() {
		return colorsByPrincipal;
	}

}
