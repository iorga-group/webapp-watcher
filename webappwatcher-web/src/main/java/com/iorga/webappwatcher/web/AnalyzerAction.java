package com.iorga.webappwatcher.web;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.iorga.webappwatcher.eventlog.EventLog;
import com.iorga.webappwatcher.eventlog.SystemEventLog;

@ManagedBean
@ViewScoped
public class AnalyzerAction implements Serializable {
	private static final long serialVersionUID = 1L;

	//	private CartesianChartModel cpuUsageModel;
	private Date firstDate;
	private Date lastDate;

	private List<SystemEventLog> systemEventLogs;

	@PostConstruct
	public void readEventLogs() throws IOException, ClassNotFoundException {
		final FileInputStream inputStream = new FileInputStream("/home/aogier/Applications/JBoss/5.1.0.GA/bin/webappwatcherlog.3.ser");
		final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		try {
//			final ChartSeries cpuSeries = new ChartSeries("CPU");
			EventLog eventLog = null;
			try {
				boolean first = true;
				systemEventLogs = new LinkedList<SystemEventLog>();
				while ((eventLog = (EventLog) objectInputStream.readObject()) != null) {
					final Date eventLogDate = eventLog.getDate();
					if (first) {
						firstDate = eventLogDate;
						first = false;
					}
					if (eventLog instanceof SystemEventLog) {
						final SystemEventLog systemEventLog = (SystemEventLog) eventLog;
//						cpuSeries.set(eventLogDate, systemEventLog.getCpuUsage());
						systemEventLogs.add(systemEventLog);
					}
//					if (eventLog instanceof RequestEventLog) {
//						RequestEventLog requestEventLog = (RequestEventLog) eventLog;
//
//					}
				}
			} catch (final EOFException e) {
				// Normal end of the read file
				objectInputStream.close();
				lastDate = eventLog.getDate();
			}
//			cpuUsageModel = new CartesianChartModel();
//			cpuUsageModel.addSeries(cpuSeries);
		} finally {
			objectInputStream.close();
		}
	}

//	public CartesianChartModel getCpuUsageModel() {
//		return cpuUsageModel;
//	}

	public Object getFirstDate() {
		return firstDate;
	}

	public Date getLastDate() {
		return lastDate;
	}

	public List<SystemEventLog> getSystemEventLogs() {
		return systemEventLogs;
	}
}
