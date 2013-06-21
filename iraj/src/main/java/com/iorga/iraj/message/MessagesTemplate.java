package com.iorga.iraj.message;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;

@SuppressWarnings("unused")
@ContextParam(Messages.class)
public class MessagesTemplate {
	@ContextPath("fieldMessages")
	private Iterable<FieldMessageTemplate> irajFieldMessages;
	@ContextPath("messages")
	private Iterable<MessageTemplate> irajMessages;
}
