package com.iorga.webappwatcher;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.webappwatcher.eventlog.ExcludedRequestsEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Header;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;
import com.iorga.webappwatcher.util.BasicParameterHandler;
import com.iorga.webappwatcher.util.ParameterHandler;
import com.iorga.webappwatcher.util.PatternListParameterHandler;
import com.iorga.webappwatcher.util.PatternUtils;



/**
 * Paramètres du filtre : <ul>
 * <li>requestNameIncludes : Valeurs séparées par des "," qui définissent les fichiers qui doivent être inclus. Par défaut, vaut ".*\.seam"</li>
 * <li>requestNameExcludes : Valeurs séparées par des "," qui définissent les fichiers qui doivent être exclus.</li>
 * </ul>
 *
 * @author aogier
 *
 */
public class RequestLogFilter implements Filter {

	private static Map<String, ParameterHandler<?, ?>> parameterHandlers = new HashMap<String, ParameterHandler<?,?>>();
	static {
		// initParameter for RequestLogFilter
		addPatternListParameterHandler("requestNameExcludes", RequestLogFilter.class);
		addPatternListParameterHandler("requestNameIncludes", RequestLogFilter.class);
		addParameterHandler("cmdRequestName", RequestLogFilter.class);
		// initParameter for EventLogManager
		addParameterHandler("waitForEventLogToCompleteMillis", EventLogManager.class);
		addParameterHandler("logPath", EventLogManager.class);
		addParameterHandler("eventLogRetentionMillis", EventLogManager.class);
		// initParameter for CpuCriticalUsageWatcher
		addParameterHandler("criticalCpuUsage", CpuCriticalUsageWatcher.class);
		addParameterHandler("deadLockThreadsSearchDeltaMillis", CpuCriticalUsageWatcher.class);
		// initParameter for SystemEventLogger
		addParameterHandler("cpuComputationDeltaMillis", SystemEventLogger.class);
		addPatternListParameterHandler("threadNameIncludes", SystemEventLogger.class);
		addPatternListParameterHandler("threadNameExcludes", SystemEventLogger.class);
	}

	// initParameter for RequestLogFilter
	private static final String DEFAULT_CMD_REQUEST_NAME = "RequestLogFilterCmd";
	// Commands available
	private static final String CMD_STOP_ALL = "stopAll";
	private static final String CMD_START_ALL = "startAll";
	private static final String CMD_WRITE_RETENTION_LOG = "writeRetentionLog";
	private static final String CMD_CHANGE_PARAMETERS = "changeParameters";
	private static final String CMD_PRINT_PARAMETERS = "printParameters";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> void addPatternListParameterHandler(final String parameterName, final Class<T> ownerClass) {
		parameterHandlers.put(parameterName, new PatternListParameterHandler(ownerClass, parameterName));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T, V> void addParameterHandler(final String parameterName, final Class<T> ownerClass) {
		parameterHandlers.put(parameterName, new BasicParameterHandler(ownerClass, parameterName));
	}

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	private List<Pattern> requestNameIncludes;
	private List<Pattern> requestNameExcludes;
	{
		// By default, we only log .seam
		requestNameIncludes = new ArrayList<Pattern>();
		requestNameIncludes.add(Pattern.compile(".*\\.seam"));
	}
	private String cmdRequestName = DEFAULT_CMD_REQUEST_NAME;

	private SystemEventLogger systemEventLogger;
	private final Map<Class<?>, Object> parametersContext = new HashMap<Class<?>, Object>();

	private int nbExcludedRequests = 0;

	@SuppressWarnings("unchecked")
	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// Initializing context
		final CpuCriticalUsageWatcher cpuCriticalUsageWatcher = new CpuCriticalUsageWatcher();
		parametersContext.put(CpuCriticalUsageWatcher.class, cpuCriticalUsageWatcher);
		final EventLogManager eventLogManager = EventLogManager.getInstance();
		parametersContext.put(EventLogManager.class, eventLogManager);
		eventLogManager.addEventLogListener(cpuCriticalUsageWatcher);
		systemEventLogger = new SystemEventLogger();
		parametersContext.put(SystemEventLogger.class, systemEventLogger);
		parametersContext.put(RequestLogFilter.class, this);

		for (final String parameterName : (List<String>)Collections.list(filterConfig.getInitParameterNames())) {
			final String value = filterConfig.getInitParameter(parameterName);
			setParameter(parameterName, value);
		}

		startServices();
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
				logRequest.setThrowable(t);
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

	@SuppressWarnings("unchecked")
	private void handleFilterCommandRequest(final String requestURI, final HttpServletRequest httpRequest, final ServletResponse response) throws IOException {
		// This is a Command Request, let's execute it
		final HttpServletResponse httpResponse = ((HttpServletResponse)response);
		if (requestURI.endsWith(CMD_START_ALL)) {
			startServices();
		} else if (requestURI.endsWith(CMD_STOP_ALL)) {
			stopServices();
		} else if (requestURI.endsWith(CMD_WRITE_RETENTION_LOG)) {
			EventLogManager.getInstance().writeRetentionLog();
		} else if (requestURI.endsWith(CMD_CHANGE_PARAMETERS)) {
			for(final String parameterName : (List<String>)Collections.list(httpRequest.getParameterNames())) {
				setParameter(parameterName, httpRequest.getParameter(parameterName));
			}
		} else if (requestURI.endsWith(CMD_PRINT_PARAMETERS)) {
			httpResponse.setStatus(200);
			final PrintWriter writer = httpResponse.getWriter();
			for (final Entry<String, ParameterHandler<?, ?>> parameterHandlerEntry : parameterHandlers.entrySet()) {
				writeParameter(writer, parameterHandlerEntry.getKey(), parameterHandlerEntry.getValue());
			}
			return;
		} else {
			httpResponse.setStatus(400);
			httpResponse.getWriter().write("ERROR : Command not understood");
			return;
		}
		httpResponse.setStatus(200);
		httpResponse.getWriter().write("OK : Command successfully completed");
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
	private <T, V> void setParameter(final String parameterName, final String value) {
		final ParameterHandler<T, V> parameterSetter = (ParameterHandler<T, V>) parameterHandlers.get(parameterName);
		if (parameterSetter == null) {
			log.warn("init-parameter "+parameterName+" is not handled. Ignoring.");
		} else {
			parameterSetter.setFieldStringValue((T) parametersContext.get(parameterSetter.getOwnerClass()), value);
		}
	}

	@SuppressWarnings("unchecked")
	private <T, V> void writeParameter(final PrintWriter writer, final String parameterName, final ParameterHandler<T, V> parameterHandler) {
		writer.println(" * "+parameterName+" : "+parameterHandler.getFieldStringValue((T) parametersContext.get(parameterHandler.getOwnerClass())));
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
