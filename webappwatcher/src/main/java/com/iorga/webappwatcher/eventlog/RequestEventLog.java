package com.iorga.webappwatcher.eventlog;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

public class RequestEventLog extends EventLog {
	private static final long serialVersionUID = 1L;

	public static class Parameter implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String name;
		private final String[] values;

		public Parameter(final String name, final String[] values) {
			this.name = name;
			this.values = values;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public String getName() {
			return name;
		}

		public String[] getValues() {
			return values;
		}
	}

	public static class Header implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String name;
		private final String value;

		public Header(final String name, final String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}
	}

	private String requestURI;
	private String principal;
	private String threadName;
	private String method;
	private Parameter[] parameters;
	private Header[] headers;
	private long threadId;
	private Date afterProcessedDate;

	protected RequestEventLog() {
		super();
	}

	public String getRequestURI() {
		return requestURI;
	}

	public void setRequestURI(final String requestURI) {
		this.requestURI = requestURI;
	}

	public String getPrincipal() {
		return principal;
	}

	public void setPrincipal(final String principal) {
		this.principal = principal;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setThreadName(final String threadName) {
		this.threadName = threadName;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(final String method) {
		this.method = method;
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public void setParameters(final Parameter[] parameters) {
		this.parameters = parameters;
	}

	public Header[] getHeaders() {
		return headers;
	}

	public void setHeaders(final Header[] headers) {
		this.headers = headers;
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(final long threadId) {
		this.threadId = threadId;
	}

	public Date getAfterProcessedDate() {
		return afterProcessedDate;
	}

	public void setAfterProcessedDate(final Date afterProcessedDate) {
		this.afterProcessedDate = afterProcessedDate;
	}
}