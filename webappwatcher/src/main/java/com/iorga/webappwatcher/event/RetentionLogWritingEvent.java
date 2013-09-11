/*
 * Copyright (C) 2013 Iorga Group
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */
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
