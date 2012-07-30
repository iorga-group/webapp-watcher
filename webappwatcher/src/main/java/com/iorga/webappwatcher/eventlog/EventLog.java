package com.iorga.webappwatcher.eventlog;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public abstract class EventLog implements Serializable {
	private static final long serialVersionUID = 1L;

	protected static final ToStringStyle TO_STRING_STYLE = new ToStringStyle() {
		private static final long serialVersionUID = 1L;
		{
			this.setUseIdentityHashCode(false);
            this.setUseClassName(false);
		}
	};

	protected final Date date;
	private transient boolean completed = false;

	protected EventLog() {
		date = new Date();
	}

	protected EventLog(final Date date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE);
	}

	public Date getDate() {
		return date;
	}


	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(final boolean completed) {
		this.completed = completed;
	}
}
