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
