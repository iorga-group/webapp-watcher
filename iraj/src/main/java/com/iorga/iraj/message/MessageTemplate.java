package com.iorga.iraj.message;

import com.iorga.iraj.annotation.ContextParam;

@SuppressWarnings("unused")
@ContextParam(Message.class)
public class MessageTemplate {
	private String message;

	public static String getType(final Message message) {
		return message.getType().jsonValue();
	}
}
