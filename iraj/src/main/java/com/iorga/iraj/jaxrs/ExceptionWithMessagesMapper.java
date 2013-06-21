package com.iorga.iraj.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.iraj.exception.ExceptionWithMessages;
import com.iorga.iraj.message.FieldMessageTemplate;
import com.iorga.iraj.message.MessageTemplate;
import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class ExceptionWithMessagesMapper implements ExceptionMapper<ExceptionWithMessages> {

	@SuppressWarnings("unused")
	@ContextParam(ExceptionWithMessages.class)
	public static class ExceptionWithMessagesTemplate {
		@ContextPath("messages.fieldMessages")
		private Iterable<FieldMessageTemplate> irajFieldMessages;
		@ContextPath("messages.messages")
		private Iterable<MessageTemplate> irajMessages;
	}

	@Override
	public Response toResponse(final ExceptionWithMessages exception) {
		return JaxRsUtils.throwableToIrajResponse(ExceptionWithMessagesTemplate.class, exception);
	}

}
