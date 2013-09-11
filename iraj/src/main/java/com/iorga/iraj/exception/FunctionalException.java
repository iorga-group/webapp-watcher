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

import com.iorga.iraj.message.MessageType;
import com.iorga.iraj.message.Messages;
import com.iorga.iraj.message.MessagesBuilder;

public class FunctionalException extends ExceptionWithMessages {
	private static final long serialVersionUID = 1L;

	public FunctionalException(final Messages messages) {
		super(messages);
	}

	public FunctionalException(final Messages messages, final Throwable cause) {
		super(cause, messages);
	}

	public FunctionalException(final String message, final MessageType type) {
		super(message, new MessagesBuilder().appendMessage(message, type).build());
	}

	public FunctionalException(final String message) {
		this(message, MessageType.ERROR);
	}

	public FunctionalException(final String message, final MessageType type, final Throwable cause) {
		super(message, cause, new MessagesBuilder().appendMessage(message, type).build());
	}

	public FunctionalException(final String message, final Throwable cause) {
		this(message, MessageType.ERROR, cause);
	}

}
