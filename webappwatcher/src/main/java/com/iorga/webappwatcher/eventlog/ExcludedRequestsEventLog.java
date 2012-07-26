package com.iorga.webappwatcher.eventlog;


public class ExcludedRequestsEventLog extends EventLog {
	private static final long serialVersionUID = 1L;

	protected ExcludedRequestsEventLog() {
		super();
	}

	private int nbExcludedRequests;


	public int getNbExcludedRequests() {
		return nbExcludedRequests;
	}
	public void setNbExcludedRequests(final int nbExcludedRequests) {
		this.nbExcludedRequests = nbExcludedRequests;
	}
}
