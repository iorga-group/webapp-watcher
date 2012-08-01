package com.iorga.webappwatcher.util;

import java.util.Map;

public interface CommandHandler {
	String getCommandName();
	void execute(Map<Class<?>, Object> commandContext) throws Exception;
	String toHtml(Map<Class<?>, Object> commandContext);
}
