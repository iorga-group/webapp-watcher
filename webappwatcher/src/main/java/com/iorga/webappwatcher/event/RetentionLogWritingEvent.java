package com.iorga.webappwatcher.event;

import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class RetentionLogWritingEvent {
	private final Class<?> source;
	private final String reason;
	private final Map<String, Object> context;


	public RetentionLogWritingEvent(final Class<?> source, final String reason, final Map<String, Object> context) {
		this.source = source;
		this.reason = reason;
		this.context = context;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, new ToStringStyle() {
			private static final long serialVersionUID = 1L;
			{
				this.setUseIdentityHashCode(false);
	            this.setUseClassName(false);
			}
		});
	}


	public Class<?> getSource() {
		return source;
	}
	public String getReason() {
		return reason;
	}
	public Map<String, Object> getContext() {
		return context;
	}
}
