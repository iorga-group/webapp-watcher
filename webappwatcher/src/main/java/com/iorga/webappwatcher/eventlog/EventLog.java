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
	private boolean completed = false;

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
