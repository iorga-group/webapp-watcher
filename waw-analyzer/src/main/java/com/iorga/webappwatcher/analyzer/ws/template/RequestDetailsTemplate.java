package com.iorga.webappwatcher.analyzer.ws.template;

import java.util.Date;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Header;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;

@ContextParam(RequestEventLog.class)
public class RequestDetailsTemplate {
	String method;
	String requestURI;
	String principal;
	Parameter[] parameters;
	Header[] headers;
	@ContextPath("date")
	Date startDate;
	@ContextPath("afterProcessedDate")
	Date endDate;
	Long durationMillis;
	boolean completed;
	String throwableStackTraceAsString;
}
