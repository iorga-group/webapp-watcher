package com.iorga.webappwatcher.analyzer.ws.template;

import java.util.Date;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.webappwatcher.analyzer.model.session.RequestsTimesAndStacks.RequestContainer;

@ContextParam(RequestContainer.class)
public class RequestContainerTemplate {
	@ContextPath("requestIndex")
	int index;
	String url;
	@ContextPath("requestEventLog.principal")
	String principal;
	@ContextPath("requestEventLog.durationMillis")
	Long duration;
	public static int getNbStacks(final RequestContainer requestContainer) {
		return requestContainer.getSystemEventLogList().size();
	}
	@ContextPath("requestEventLog.date")
	Date startDate;
	@ContextPath("requestEventLog.afterProcessedDate")
	Date endDate;
}
