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
package com.iorga.iraj.exception;

import com.iorga.iraj.message.Messages;

public class ExceptionWithMessages extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private final Messages messages;

	public ExceptionWithMessages(final Messages messages) {
		super();
		this.messages = messages;
	}

	public ExceptionWithMessages(final String message, final Throwable cause, final Messages messages) {
		super(message, cause);
		this.messages = messages;
	}

	public ExceptionWithMessages(final String message, final Messages messages) {
		super(message);
		this.messages = messages;
	}

	public ExceptionWithMessages(final Throwable cause, final Messages messages) {
		super(cause);
		this.messages = messages;
	}

	public Messages getMessages() {
		return messages;
	}
}
