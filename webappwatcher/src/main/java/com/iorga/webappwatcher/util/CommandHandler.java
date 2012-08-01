package com.iorga.webappwatcher.util;

import java.util.Map;

public interface CommandHandler {
	String getCommandName();
	/**
	 * @param commandContext
	 * @return <code>true</code> if the command handled the response, <code>false</code> otherwise
	 * @throws Exception
	 */
	boolean execute(Map<Class<?>, Object> commandContext) throws Exception;
	String toHtml(Map<Class<?>, Object> commandContext);
}
