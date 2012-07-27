package com.iorga.webappwatcher.web;

import java.awt.Color;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
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

//	private List<SystemEventLog> systemEventLogs;
//	private Map<String, List<RequestEventLog>> requestEventLogsByPrincipal;

	private String cpuUsageJsonValues;

	private String memoryUsedJsonValues;

	private String markingsJson;

	@PostConstruct
	public void readEventLogs() throws IOException, ClassNotFoundException {
		final FileInputStream inputStream = new FileInputStream("/home/aogier/Applications/JBoss/5.1.0.GA/bin/webappwatcherlog.3.ser");
		final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		try {
			final StringBuilder cpuUsageJsonValuesBuilder = new StringBuilder();
			final StringBuilder memoryUsedJsonValuesBuilder = new StringBuilder();
			final Map<String, List<RequestEventLog>> requestEventLogsByPrincipal = new HashMap<String, List<RequestEventLog>>();
			try {
				boolean first = true;
//				systemEventLogs = new LinkedList<SystemEventLog>();
				EventLog eventLog;
				while ((eventLog = (EventLog) objectInputStream.readObject()) != null) {
					final Date eventLogDate = eventLog.getDate();
					if (eventLog instanceof SystemEventLog) {
						final SystemEventLog systemEventLog = (SystemEventLog) eventLog;
//						systemEventLogs.add(systemEventLog);
						addData(eventLogDate, systemEventLog.getCpuUsage(), cpuUsageJsonValuesBuilder, first);
						addData(eventLogDate, systemEventLog.getHeapMemoryUsed()+systemEventLog.getNonHeapMemoryUsed(), memoryUsedJsonValuesBuilder, first);
					}
					if (eventLog instanceof RequestEventLog) {
						final RequestEventLog requestEventLog = (RequestEventLog) eventLog;
						String principal = requestEventLog.getPrincipal();
						if (StringUtils.isEmpty(principal)) {
							principal = "";
						}
						List<RequestEventLog> requests = requestEventLogsByPrincipal.get(principal);
						if (requests == null) {
							requests = new LinkedList<RequestEventLog>();
							requestEventLogsByPrincipal.put(principal, requests);
						}
						requests.add(requestEventLog);
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
		} finally {
			objectInputStream.close();
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
}
