package com.iorga.webappwatcher.analyzer.model.session;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

@SessionScoped
@JsonAutoDetect(getterVisibility = Visibility.PUBLIC_ONLY)
public class Configurations implements Serializable {
	private static final long serialVersionUID = 1L;

	private long timeSliceDurationMillis = 10 * 60 * 1000;

	public long getTimeSliceDurationMillis() {
		return timeSliceDurationMillis;
	}

	public void setTimeSliceDurationMillis(final long timeSliceDurationMillis) {
		this.timeSliceDurationMillis = timeSliceDurationMillis;
	}
}
