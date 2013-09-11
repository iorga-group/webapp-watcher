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
package com.iorga.webappwatcher.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.iorga.webappwatcher.RequestLogFilter;

public abstract class BasicCommandHandler implements CommandHandler {
	protected String commandName;


	public BasicCommandHandler(final String commandName) {
		this.commandName = commandName;
	}

	@Override
	public String toHtml(final Map<Class<?>, Object> commandContext) {
		final HttpServletRequest request = getHttpServletRequest(commandContext);
		final RequestLogFilter logFilter = getRequestLogFilter(commandContext);

		final String commandName = getCommandName();
		return "<form action=\"" + request.getContextPath() + "/" + logFilter.getCmdRequestName() + "/" + commandName + "\">"
			+ "<fieldset><legend>"+commandName+"</legend>"
			+ toHtmlInForm(commandContext)
			+ "</fieldset></form>";
	}

	protected String toHtmlInForm(final Map<Class<?>, Object> commandContext) {
		return "<input type=\"submit\" value=\""+getCommandName()+"\" />";
	}

	@SuppressWarnings("unchecked")
	protected <T> T get(final Map<Class<?>, Object> commandContext, final Class<T> contextObjectClass) {
		return (T) commandContext.get(contextObjectClass);
	}

	protected RequestLogFilter getRequestLogFilter(final Map<Class<?>, Object> commandContext) {
		return get(commandContext, RequestLogFilter.class);
	}

	protected HttpServletRequest getHttpServletRequest(final Map<Class<?>, Object> commandContext) {
		return get(commandContext, HttpServletRequest.class);
	}

	protected HttpServletResponse getHttpServletResponse(final Map<Class<?>, Object> commandContext) {
		return get(commandContext, HttpServletResponse.class);
	}

	@Override
	public String getCommandName() {
		return commandName;
	}

}
