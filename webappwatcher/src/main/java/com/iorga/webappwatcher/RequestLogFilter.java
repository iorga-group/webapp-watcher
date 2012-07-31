package com.iorga.webappwatcher;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import com.iorga.webappwatcher.util.BasicParameterSetter;
import com.iorga.webappwatcher.util.ParameterSetter;
import com.iorga.webappwatcher.util.PatternListParameterSetter;



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

	private static Map<String, ParameterSetter<?, ?>> parameterSetters = new HashMap<String, ParameterSetter<?,?>>();
	static {
		// initParameter for RequestLogFilter
		addPatternListParameterSetter("requestNameExcludes", RequestLogFilter.class);
		addPatternListParameterSetter("requestNameIncludes", RequestLogFilter.class);
		addParameterSetter("cmdRequestName", RequestLogFilter.class);
		// initParameter for EventLogManager
		addParameterSetter("waitForEventLogToCompleteMillis", EventLogManager.class);
		addParameterSetter("logPath", EventLogManager.class);
		addParameterSetter("eventLogRetentionMillis", EventLogManager.class);
		// initParameter for CpuCriticalUsageWatcher
		addParameterSetter("criticalCpuUsage", CpuCriticalUsageWatcher.class);
		addParameterSetter("deadLockThreadsSearchDeltaMillis", CpuCriticalUsageWatcher.class);
		// initParameter for SystemEventLogger
		addParameterSetter("cpuComputationDeltaMillis", SystemEventLogger.class);
	}

	// initParameter for RequestLogFilter
	private static final String DEFAULT_CMD_REQUEST_NAME = "RequestLogFilterCmd";
	// Commands available
	private static final String CMD_STOP_ALL = "stopAll";
	private static final String CMD_START_ALL = "startAll";
	private static final String CMD_WRITE_RETENTION_LOG = "writeRetentionLog";
	private static final String CMD_CHANGE_PARAMETERS = "changeParameters";

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T> void addPatternListParameterSetter(final String parameterName, final Class<T> ownerClass) {
		parameterSetters.put(parameterName, new PatternListParameterSetter(ownerClass, parameterName));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static <T, V> void addParameterSetter(final String parameterName, final Class<T> ownerClass) {
		parameterSetters.put(parameterName, new BasicParameterSetter(ownerClass, parameterName));
	}

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	private List<Pattern> requestNameIncludes;
	{
		// Par défaut, on log seulement les fichiers .seam
		requestNameIncludes = new ArrayList<Pattern>();
		requestNameIncludes.add(Pattern.compile(".*\\.seam"));
	}
	private List<Pattern> requestNameExcludes;
	private String cmdRequestName = DEFAULT_CMD_REQUEST_NAME;

	private SystemEventLogger systemEventLogger;
	private final Map<Class<?>, Object> parametersContext = new HashMap<Class<?>, Object>();

	private int nbExcludedRequests = 0;

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

		@SuppressWarnings("unchecked")
		final
		List<String> parameterNames = Collections.list(filterConfig.getInitParameterNames());
		for (final String parameterName : parameterNames) {
			final String value = filterConfig.getInitParameter(parameterName);
			setParameter(parameterName, value);
		}

		startServices();
	}

	@SuppressWarnings("unchecked")
	private <T, V> void setParameter(final String parameterName, final String value) {
		final ParameterSetter<T, V> parameterSetter = (ParameterSetter<T, V>) parameterSetters.get(parameterName);
		if (parameterSetter == null) {
			log.warn("init-parameter "+parameterName+" is not handled. Ignoring.");
		} else {
			parameterSetter.setFieldFromString((T) parametersContext.get(parameterSetter.getOwnerClass()), value);
		}
	}

	private void startServices() {
		systemEventLogger.start();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest)request;
		final String requestURI = httpRequest.getRequestURI();
		if (httpRequest.getServletPath().startsWith("/"+cmdRequestName)) {
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
			} else {
				httpResponse.setStatus(400);
				httpResponse.getWriter().write("ERROR : Command not understood");
				return;
			}
			httpResponse.setStatus(200);
			httpResponse.getWriter().write("OK : Command successfully completed");
		} else {
			// Test des filtres d'inclusion / exclusion
			boolean matches = false;
			if (requestNameIncludes != null) {
				for (final Iterator<Pattern> iterator = requestNameIncludes.iterator(); iterator.hasNext() && !matches;) {
					final Pattern include = iterator.next();
					matches |= include.matcher(requestURI).matches();
				}
			}
			if (requestNameExcludes != null) {
				for (final Iterator<Pattern> iterator = requestNameExcludes.iterator(); iterator.hasNext() && matches;) {
					final Pattern exclude = iterator.next();
					matches &= !exclude.matcher(requestURI).matches();
				}
			}
			final RequestEventLog logRequest;
			if (matches) {
				logNbExcludedRequests();
				logRequest = EventLogManager.getInstance().addEventLog(RequestEventLog.class);
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
			} else {
				logRequest = null;
				incrementNbExcludedRequests();
			}

			try {
				chain.doFilter(request, response);
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
