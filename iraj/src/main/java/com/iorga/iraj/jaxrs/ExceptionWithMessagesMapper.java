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
