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
package com.iorga.webappwatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.iorga.webappwatcher.eventlog.ExcludedRequestsEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Header;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;
import com.iorga.webappwatcher.util.BasicCommandHandler;
import com.iorga.webappwatcher.util.BasicParameterHandler;
import com.iorga.webappwatcher.util.CommandHandler;
import com.iorga.webappwatcher.util.InstanceSetParameterHandler;
import com.iorga.webappwatcher.util.ParameterHandler;
import com.iorga.webappwatcher.util.PatternDurationParameterHandler;
import com.iorga.webappwatcher.util.PatternListParameterHandler;
import com.iorga.webappwatcher.util.PatternUtils;
import com.iorga.webappwatcher.watcher.CpuCriticalUsageWatcher;
import com.iorga.webappwatcher.watcher.RequestDurationWatcher;
import com.iorga.webappwatcher.watcher.RetentionLogWritingWatcher;
import com.iorga.webappwatcher.watcher.WriteAllRequestsWatcher;



public class RequestLogFilter implements Filter {

	private static Map<String, ParameterHandler<?, ?>> parameterHandlers = new LinkedHashMap<String, ParameterHandler<?,?>>();
	private static final Map<Class<?>, Object> parametersContext = new HashMap<Class<?>, Object>();

	static {
		// initParameter for RequestLogFilter
		addParameterHandler("cmdRequestName", RequestLogFilter.class);
		addPatternListParameterHandler("requestNameIncludes", RequestLogFilter.class);
		addPatternListParameterHandler("requestNameExcludes", RequestLogFilter.class);
		// initParameter for EventLogManager
		addParameterHandler("waitForEventLogToCompleteMillis", EventLogManager.class);
		addParameterHandler("logPath", EventLogManager.class);
		addParameterHandler("eventLogRetentionMillis", EventLogManager.class);
		addParameterHandler("maxLogFileSizeMo", EventLogManager.class);
		addInstanceSetParameterHandler("eventLogWatchers", EventLogManager.class, Object.class);
		// initParameter for CpuCriticalUsageWatcher
		addParameterHandler("criticalCpuUsage", CpuCriticalUsageWatcher.class);
		addParameterHandler("deadLockThreadsSearchDeltaMillis", CpuCriticalUsageWatcher.class);
		// initParameter for RetentionLogWritingWatcher
		addPatternDurationListParameterHandler("writingEventsCooldown", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpHost", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpPort", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpAuth", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpUsername", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpPassword", RetentionLogWritingWatcher.class);
		addParameterHandler("mailSmtpSecurityType", RetentionLogWritingWatcher.class);
		addParameterHandler("mailFrom", RetentionLogWritingWatcher.class);
		addParameterHandler("mailTo", RetentionLogWritingWatcher.class);

		// initParameter for RequestDurationWatcher
		addPatternDurationListParameterHandler("requestDurationLimits", RequestDurationWatcher.class);
		// initParameter for SystemEventLogger
		addParameterHandler("cpuComputationDeltaMillis", SystemEventLogger.class);
		addPatternListParameterHandler("threadNameIncludes", SystemEventLogger.class);
		addPatternListParameterHandler("threadNameExcludes", SystemEventLogger.class);
	}

	// initParameter for RequestLogFilter
	private static final String DEFAULT_CMD_REQUEST_NAME = "RequestLogFilterCmd";

	private static final Map<String, CommandHandler> commandHandlers = new LinkedHashMap<String, CommandHandler>();
	private static CommandHandler defaultCommandHandler;
	// Commands available
	static {
		addCommandHandler(new BasicCommandHandler("stopAll") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) {
				getRequestLogFilter(commandContext).stopServices();
				return false;
			}
		});
		addCommandHandler(new BasicCommandHandler("startAll") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) {
				getRequestLogFilter(commandContext).startServices();
				return false;
			}
		});
		addCommandHandler(new BasicCommandHandler("writeRetentionLog") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				EventLogManager.getInstance().writeRetentionLog(RequestLogFilter.class, "writeRetentionLogRequest", null);
				return false;
			}
		});
		addCommandHandler(new BasicCommandHandler("closeRetentionLog") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				EventLogManager.getInstance().closeLog();
				return false;
			}
		});
		addCommandHandler(new BasicCommandHandler("changeParameters") {
			@SuppressWarnings("unchecked")
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				final HttpServletRequest request = getHttpServletRequest(commandContext);
				final RequestLogFilter logFilter = getRequestLogFilter(commandContext);
				for(final String parameterName : (List<String>)Collections.list(request.getParameterNames())) {
					logFilter.setParameter(parameterName, request.getParameter(parameterName));
				}
				return false;
			}
			@Override
			protected String toHtmlInForm(final Map<Class<?>, Object> commandContext) {
				final StringBuilder htmlBuilder = new StringBuilder();
				final RequestLogFilter logFilter = getRequestLogFilter(commandContext);
				for (final Entry<String, ParameterHandler<?, ?>> parameterHandlerEntry : parameterHandlers.entrySet()) {
					final String parameterName = parameterHandlerEntry.getKey();
					final ParameterHandler<?, ?> parameterHandler = parameterHandlerEntry.getValue();
					htmlBuilder.append(parameterName)
						.append(": <input type=\"text\" name=\"").append(parameterName)
							.append("\" value=\"").append(logFilter.getParameterStringValue(parameterHandler)).append("\" /><br />");
				}
				return htmlBuilder.append(super.toHtmlInForm(commandContext)).toString();
			}
		});
		addCommandHandler(new BasicCommandHandler("printParameters") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				final HttpServletResponse response = getHttpServletResponse(commandContext);
				final RequestLogFilter logFilter = getRequestLogFilter(commandContext);
				response.setStatus(HttpServletResponse.SC_OK);
				final PrintWriter writer = response.getWriter();
				for (final Entry<String, ParameterHandler<?, ?>> parameterHandlerEntry : parameterHandlers.entrySet()) {
					logFilter.writeParameter(writer, parameterHandlerEntry.getKey(), parameterHandlerEntry.getValue());
				}
				return true;
			}
		});
		addCommandHandler(new BasicCommandHandler("downloadEventLogs") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws Exception {
				final HttpServletRequest request = getHttpServletRequest(commandContext);
				final HttpServletResponse response = getHttpServletResponse(commandContext);
				final String[] files = request.getParameterValues("files");
				EventLogManager.getInstance().writeEventLogsToHttpServletResponse(response, Lists.newArrayList(files));
				return true;
			}
			@Override
			protected String toHtmlInForm(final Map<Class<?>, Object> commandContext) {
				final StringBuilder htmlBuilder = new StringBuilder();
				// List the files with a checkbox each time
				final List<File> eventLogsInThePath = Ordering.from(new Comparator<File>() {
					@Override
					public int compare(final File o1, final File o2) {
						return o1.getName().compareTo(o2.getName());
					}
				}).sortedCopy(EventLogManager.getInstance().listEventLogsInThePath());
				for (final File eventLog : eventLogsInThePath) {
					final String eventLogName = eventLog.getName();
					htmlBuilder.append("<input type=\"checkbox\" name=\"files\" value=\"").append(eventLogName).append("\" />");
					final long eventLogLength = eventLog.length();
					htmlBuilder.append(eventLogName).append(" (").append(FileUtils.byteCountToDisplaySize(eventLogLength)).append(")<br />");
				}
				return htmlBuilder.append(super.toHtmlInForm(commandContext)).toString();
			}
		});
		addCommandHandler(new BasicCommandHandler("downloadEventLog") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				final HttpServletResponse response = getHttpServletResponse(commandContext);
				EventLogManager.getInstance().writeEventLogToHttpServletResponse(response);
				return true;
			}
		});
		addCommandHandler(new BasicCommandHandler("printInfos") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				final HttpServletResponse response = getHttpServletResponse(commandContext);
				response.setStatus(HttpServletResponse.SC_OK);
				final PrintWriter writer = response.getWriter();
				writer.println(" * eventLogLength = "+EventLogManager.getInstance().getEventLogLength());
				return true;
			}
		});
		addCommandHandler(new BasicCommandHandler("printHtmlCommands") {
			@Override
			public boolean execute(final Map<Class<?>, Object> commandContext) throws IOException {
				final HttpServletResponse response = getHttpServletResponse(commandContext);
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("text/html");
				final PrintWriter writer = response.getWriter();
				writer.print("<html><head><title>"+RequestLogFilter.class.getSimpleName()+" - All Commands</title></head><body>");
				for (final CommandHandler commandHandler : commandHandlers.values()) {
					writer.print(commandHandler.toHtml(commandContext));
				}
				writer.print("</body></html>");
				return true;
			}
		}, true);
	}


	private static <T> void addPatternListParameterHandler(final String parameterName, final Class<T> ownerClass) {
		parameterHandlers.put(parameterName, new PatternListParameterHandler<T>(ownerClass, parameterName));
	}

	private static <T> void addPatternDurationListParameterHandler(final String parameterName, final Class<T> ownerClass) {
		parameterHandlers.put(parameterName, new PatternDurationParameterHandler<T>(ownerClass, parameterName));
	}

	private static <T, I> void addInstanceSetParameterHandler(final String parameterName, final Class<T> ownerClass, final Class<I> instancesClass) {
		parameterHandlers.put(parameterName, new InstanceSetParameterHandler<T, I>(ownerClass, parameterName, parametersContext));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T, V> void addParameterHandler(final String parameterName, final Class<T> ownerClass) {
		parameterHandlers.put(parameterName, new BasicParameterHandler(ownerClass, parameterName));
	}

	private static void addCommandHandler(final CommandHandler commandHandler) {
		commandHandlers.put(commandHandler.getCommandName(), commandHandler);
	}

	private static void addCommandHandler(final CommandHandler commandHandler, final boolean defaultCommand) {
		commandHandlers.put(commandHandler.getCommandName(), commandHandler);
		if (defaultCommand) {
			defaultCommandHandler = commandHandler;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	private List<Pattern> requestNameIncludes;
	private List<Pattern> requestNameExcludes;
	{
		// By default, we only log .seam
		requestNameIncludes = new ArrayList<Pattern>();
		requestNameIncludes.add(Pattern.compile(".*\\.xhtml"));
	}
	private String cmdRequestName = DEFAULT_CMD_REQUEST_NAME;

	private SystemEventLogger systemEventLogger;

	private int nbExcludedRequests = 0;

	@SuppressWarnings("unchecked")
	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// Initializing context
		final CpuCriticalUsageWatcher cpuCriticalUsageWatcher = new CpuCriticalUsageWatcher();
		parametersContext.put(CpuCriticalUsageWatcher.class, cpuCriticalUsageWatcher);
		final WriteAllRequestsWatcher writeAllRequestsWatcher = new WriteAllRequestsWatcher();
		parametersContext.put(WriteAllRequestsWatcher.class, writeAllRequestsWatcher);
		final RequestDurationWatcher requestDurationWatcher = new RequestDurationWatcher();
		parametersContext.put(RequestDurationWatcher.class, requestDurationWatcher);
		final RetentionLogWritingWatcher retentionLogWritingWatcher = createRetentionLogWritingWatcher();
		parametersContext.put(RetentionLogWritingWatcher.class, retentionLogWritingWatcher);
		final EventLogManager eventLogManager = EventLogManager.getInstance();
		parametersContext.put(EventLogManager.class, eventLogManager);
		systemEventLogger = new SystemEventLogger();
		parametersContext.put(SystemEventLogger.class, systemEventLogger);
		parametersContext.put(RequestLogFilter.class, this);
		// by default, watch the CPU peaks, the request duration & write all requests to the log
		eventLogManager.setEventLogWatchers(Sets.newHashSet(cpuCriticalUsageWatcher, requestDurationWatcher, writeAllRequestsWatcher, retentionLogWritingWatcher));

		// Reading web.xml filterConfig init-params
		for (final String parameterName : (List<String>)Collections.list(filterConfig.getInitParameterNames())) {
			final String value = filterConfig.getInitParameter(parameterName);
			setParameter(parameterName, value);
		}
		// Reading "webappwatcher.properties" parameters
		try {
			final Properties properties = new Properties();
			final InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream("webappwatcher.properties");
			if (propertiesStream != null) {
				properties.load(propertiesStream);
				for (final Entry<Object, Object> property : properties.entrySet()) {
					setParameter((String)property.getKey(), (String)property.getValue());
				}
			}
		} catch (final IOException e) {
			throw new ServletException("Problem while reading webappwatcher.properties file", e);
		}

		startServices();
	}

	protected RetentionLogWritingWatcher createRetentionLogWritingWatcher() {
		return new RetentionLogWritingWatcher();
	}

	private void startServices() {
		systemEventLogger.start();
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest)request;
		final String requestURI = httpRequest.getRequestURI();

		final boolean matches = PatternUtils.matches(requestURI, requestNameIncludes, requestNameExcludes);
		final RequestEventLog logRequest;
		if (matches) {
			// log previous excluded requests
			logNbExcludedRequests();
			// create a log request
			logRequest = createRequestEventLog(httpRequest, requestURI);
		} else {
			logRequest = null;
			incrementNbExcludedRequests();
		}

		try {
			if (httpRequest.getServletPath().startsWith("/"+cmdRequestName)) {
				// this is a command request, let's handle it
				handleFilterCommandRequest(requestURI, httpRequest, response);
			} else {
				chain.doFilter(request, response);
			}
		} catch (final Throwable t) {
			if (matches) {
				try {
					logRequest.setThrowableStackTraceAsString(Throwables.getStackTraceAsString(t));
				} catch (final Throwable t2) {
					logRequest.setThrowableStackTraceAsString("Exception in thread \""+Thread.currentThread().getName()+"\" "+t.getClass().getName()+" couldn't be got as string due to "+t2.getClass().getName());
				}
			}
			if (t instanceof IOException) {
				throw (IOException)t;
			}
			if (t instanceof ServletException) {
				throw (ServletException)t;
			}
			throw (RuntimeException)t;
		} finally {
			if (matches) {
				logRequest.setAfterProcessedDate(new Date());
				EventLogManager.getInstance().fire(logRequest);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private RequestEventLog createRequestEventLog(final HttpServletRequest httpRequest, final String requestURI) {
		final RequestEventLog logRequest = EventLogManager.getInstance().addEventLog(RequestEventLog.class);
		logRequest.setRequestURI(requestURI);
		logRequest.setMethod(httpRequest.getMethod());
		final Enumeration<String> parameterNames = httpRequest.getParameterNames();
		final List<Parameter> parameters = new LinkedList<Parameter>();
		while (parameterNames.hasMoreElements()) {
			final String parameterName = parameterNames.nextElement();
			parameters.add(new Parameter(parameterName, httpRequest.getParameterValues(parameterName)));
		}
		logRequest.setParameters(parameters.toArray(new Parameter[parameters.size()]));
		final Enumeration<String> headerNames = httpRequest.getHeaderNames();
		final List<Header> headers = new LinkedList<Header>();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			headers.add(new Header(headerName, httpRequest.getHeader(headerName)));
		}
		logRequest.setHeaders(headers.toArray(new Header[headers.size()]));
		final Principal userPrincipal = httpRequest.getUserPrincipal();
		if (userPrincipal != null) {
			logRequest.setPrincipal(userPrincipal.getName());
		}
		final Thread currentThread = Thread.currentThread();
		logRequest.setThreadName(currentThread.getName());
		logRequest.setThreadId(currentThread.getId());
		return logRequest;
	}

	private void handleFilterCommandRequest(final String requestURI, final HttpServletRequest httpRequest, final ServletResponse response) throws Exception {
		// This is a Command Request, let's execute it
		final HttpServletResponse httpResponse = ((HttpServletResponse)response);
		final HashMap<Class<?>, Object> commandContext = new HashMap<Class<?>, Object>(parametersContext);
		commandContext.put(HttpServletRequest.class, httpRequest);
		commandContext.put(HttpServletResponse.class, httpResponse);
		final String commandName = StringUtils.substringAfterLast(requestURI, "/");
		final boolean commandHandledResponse;
		if (StringUtils.isEmpty(commandName) || commandName.equals(getCmdRequestName())) {
			// Here is the default command : nothing after last /, or no last /
			commandHandledResponse = defaultCommandHandler.execute(commandContext);
		} else {
			final CommandHandler commandHandler = commandHandlers.get(commandName);
			if (commandHandler != null) {
				commandHandledResponse = commandHandler.execute(commandContext);
			} else {
				commandHandledResponse = true;
				httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				httpResponse.getWriter().write("ERROR : Command not understood");
			}
		}
		if (!commandHandledResponse) {
			httpResponse.setStatus(HttpServletResponse.SC_OK);
			httpResponse.getWriter().write("OK : Command successfully completed");
		}
	}

	private synchronized void logNbExcludedRequests() {
		if (nbExcludedRequests > 0) {
			final EventLogManager eventLogManager = EventLogManager.getInstance();
			final ExcludedRequestsEventLog eventLog = eventLogManager.addEventLog(ExcludedRequestsEventLog.class);
			eventLog.setNbExcludedRequests(this.nbExcludedRequests);
			eventLogManager.fire(eventLog);
			this.nbExcludedRequests = 0;
		}
	}

	private synchronized void incrementNbExcludedRequests() {
		nbExcludedRequests++;
	}

	@Override
	public void destroy() {
		stopServices();
	}

	private void stopServices() {
		systemEventLogger.stop();
		try {
			EventLogManager.getInstance().closeLog();
		} catch (final IOException e) {
			log.error("Cannot close EventLog's log", e);
		}
	}

	@SuppressWarnings("unchecked")
	<T, V> void setParameter(final String parameterName, final String value) {
		final ParameterHandler<T, V> parameterSetter = (ParameterHandler<T, V>) parameterHandlers.get(parameterName);
		if (parameterSetter == null) {
			log.warn("init-parameter "+parameterName+" is not handled. Ignoring.");
		} else {
			parameterSetter.setFieldStringValue((T) parametersContext.get(parameterSetter.getOwnerClass()), value);
		}
	}

	private <T, V> void writeParameter(final PrintWriter writer, final String parameterName, final ParameterHandler<T, V> parameterHandler) {
		writer.println(" * "+parameterName+" : "+getParameterStringValue(parameterHandler));
	}

	@SuppressWarnings("unchecked")
	private <T, V> String getParameterStringValue(final ParameterHandler<T, V> parameterHandler) {
		return parameterHandler.getFieldStringValue((T) parametersContext.get(parameterHandler.getOwnerClass()));
	}


	public List<Pattern> getRequestNameIncludes() {
		return requestNameIncludes;
	}

	public void setRequestNameIncludes(final List<Pattern> requestNameIncludes) {
		this.requestNameIncludes = requestNameIncludes;
	}

	public List<Pattern> getRequestNameExcludes() {
		return requestNameExcludes;
	}

	public void setRequestNameExcludes(final List<Pattern> requestNameExcludes) {
		this.requestNameExcludes = requestNameExcludes;
	}

	public String getCmdRequestName() {
		return cmdRequestName;
	}

	public void setCmdRequestName(final String cmdRequestName) {
		this.cmdRequestName = cmdRequestName;
	}
}
