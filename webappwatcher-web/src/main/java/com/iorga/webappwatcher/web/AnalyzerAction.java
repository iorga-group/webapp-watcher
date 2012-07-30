package com.iorga.webappwatcher.web;

import java.awt.Color;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang.StringUtils;

import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

@ManagedBean
@ViewScoped
public class AnalyzerAction implements Serializable {
	private static final long serialVersionUID = 1L;

//	private Map<String, List<RequestEventLog>> requestEventLogsByPrincipal;

	private String cpuUsageJsonValues;

	private String memoryUsedJsonValues;

	private String markingsJson;

	private List<RequestEventLog> requestEventLogsSortedByDate;
	private List<SystemEventLog> systemEventLogsSortedByDate;

	private long selectedTime;
	private SystemEventLog selectedSystemEventLog;
	private List<RequestEventLog> selectedRequestEventLogs;

	@PostConstruct
	public void readEventLogs() throws IOException, ClassNotFoundException {
//		final FileInputStream inputStream = new FileInputStream("/home/aogier/Applications/JBoss/5.1.0.GA/bin/webappwatcherlog.3.ser");
//		final FileInputStream inputStream = new FileInputStream("/home/aogier/Applications/JBoss/7.1.1.Final/bin/webappwatcherlog.ser");
		final FileInputStream inputStream = new FileInputStream("/tmp/webappwatcherlog.ser");

		final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		try {
			final StringBuilder cpuUsageJsonValuesBuilder = new StringBuilder();
			final StringBuilder memoryUsedJsonValuesBuilder = new StringBuilder();
			final Map<String, List<RequestEventLog>> requestEventLogsByPrincipal = new HashMap<String, List<RequestEventLog>>();
			final LinkedList<SystemEventLog> systemEventLogsSortedByDate = new LinkedList<SystemEventLog>();
			try {
				boolean first = true;
				requestEventLogsSortedByDate = new LinkedList<RequestEventLog>();
				EventLog eventLog;
				while ((eventLog = (EventLog) objectInputStream.readObject()) != null) {
					final Date eventLogDate = eventLog.getDate();
					if (eventLog instanceof SystemEventLog) {
						final SystemEventLog systemEventLog = (SystemEventLog) eventLog;
						addData(eventLogDate, systemEventLog.getCpuUsage(), cpuUsageJsonValuesBuilder, first);
						addData(eventLogDate, systemEventLog.getHeapMemoryUsed()+systemEventLog.getNonHeapMemoryUsed(), memoryUsedJsonValuesBuilder, first);
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
				}
			} catch (final EOFException e) {
				// Normal end of the read file
			}
			this.cpuUsageJsonValues = cpuUsageJsonValuesBuilder.toString();
			this.memoryUsedJsonValues = memoryUsedJsonValuesBuilder.toString();
			// Compute the requests
			final int nbPrincipals = requestEventLogsByPrincipal.size();
			int currentPrincipal = 0;
			final StringBuilder markingsJsonBuilder = new StringBuilder();
			boolean first = true;
			for (final List<RequestEventLog> requestsForOnePrincipal : requestEventLogsByPrincipal.values()) {
				final float currentPrincipalFraction = ((float)currentPrincipal) / ((float)nbPrincipals);
				final String color = Integer.toHexString(Color.HSBtoRGB(currentPrincipalFraction, 0.5f, 0.7f)).substring(2);
				// Spec to write = { color: '#121212', lineWidth: 2, xaxis: { from: 1343292820699, to: 1343292895085 }, yaxis: { from: -1, to: -1} }
				final float y = -19f * (currentPrincipal+1f) / (nbPrincipals+1f) - 0.5f; // 19 = 20% (y axis) - 0.5 margin top & bottom
				for (final RequestEventLog requestEventLog : requestsForOnePrincipal) {
					if (!first) {
						markingsJsonBuilder.append(",");
					} else {
						first = false;
					}
					markingsJsonBuilder.append("{color:'#").append(color).append("',")
						.append("xaxis:{from:").append(requestEventLog.getDate().getTime()).append(",to:").append(requestEventLog.getAfterProcessedDate().getTime()).append("},")
						.append("yaxis:{from:").append(y).append(",to:").append(y).append("}}");
				}
				currentPrincipal++;
			}
			this.markingsJson = markingsJsonBuilder.toString();
			// putting the systemEventLogsSortedByDate in an array list in order to binary search on it later
			this.systemEventLogsSortedByDate = new ArrayList<SystemEventLog>(systemEventLogsSortedByDate);
		} finally {
			objectInputStream.close();
		}
	}

	public void updateInfoPanel() {
		// Binary search on systemEventLogsSortedByDate to get the nearer systemEventLog
		final Date selectedDate = new Date(getSelectedTime());
		final SystemEventLog systemEventLogToFind = new SystemEventLog(selectedDate);
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
			final SystemEventLog n0 = systemEventLogsSortedByDate.get(insertionPoint - 1);
			if (insertionPoint == systemEventLogsSortedByDate.size()) {
				// get the last entry because the insertion point is at the end
				this.selectedSystemEventLog = n0;
			} else  {
				final SystemEventLog n = systemEventLogsSortedByDate.get(insertionPoint);
				if (insertionPoint == 0) {
					this.selectedSystemEventLog = n;
				} else if (n0.getDate().getTime() + n.getDate().getTime() <= getSelectedTime() * 2) {
					// the selected date is near n0 than n
					this.selectedSystemEventLog = n0;
				} else {
					this.selectedSystemEventLog = n;
				}
			}
		}
		// Now search for selected request logs
		this.selectedRequestEventLogs = new LinkedList<RequestEventLog>();
		final Iterator<RequestEventLog> iterator = requestEventLogsSortedByDate.iterator();
		RequestEventLog request = null;
		while (iterator.hasNext() && (request = iterator.next()).getDate().getTime() <= getSelectedTime()) {
			if (request.getAfterProcessedDate().getTime() >= getSelectedTime()) {
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

}
