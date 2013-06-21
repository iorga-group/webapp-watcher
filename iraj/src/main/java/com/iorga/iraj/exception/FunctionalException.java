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
