package com.iorga.webappwatcher;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.webappwatcher.eventlog.ExcludedRequestsEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Header;
import com.iorga.webappwatcher.eventlog.RequestEventLog.Parameter;



/**
 * Paramètres du filtre : <ul>
 * <li>includes : Valeurs séparées par des "," qui définissent les fichiers qui doivent être inclus. Par défaut, vaut ".*\.seam"</li>
 * <li>excludes : Valeurs séparées par des "," qui définissent les fichiers qui doivent être exclus.</li>
 * </ul>
 *
 * @author aogier
 *
 */
public class RequestLogFilter implements Filter {

	private static final String WAIT_FOR_EVENT_LOG_TO_COMPLETE_MILLIS_INIT_PARAM = "waitForEventLogToCompleteMillis";
	// initParameter for RequestLogFilter
	private static final String REQUEST_NAME_EXCLUDES_INIT_PARAM = "requestNameExcludes";
	private static final String REQUEST_NAME_INCLUDES_INIT_PARAM = "requestNameIncludes";
	private static final String CMD_REQUEST_NAME_INIT_PARAM = "cmdRequestName";
	private static final String DEFAULT_CMD_REQUEST_NAME = "RequestLogFilterCmd";
	// initParameter for CpuCriticalUsageWatcher
	private static final String CRITICAL_CPU_USAGE_INIT_PARAM = "criticalCpuUsage";
	private static final String DEAD_LOCK_THREADS_SEARCH_DELTA_MILLIS_INIT_PARAM = "deadLockThreadsSearchDeltaMillis";
	// initParameter for SystemEventLogger
	private static final String CPU_COMPUTATION_DELTA_MILLIS_INIT_PARAM = "cpuComputationDeltaMillis";
	// initParameter for EventLogManager
	private static final String LOG_PATH_INIT_PARAM = "logPath";
	private static final String EVENT_LOG_RETENTION_MILLIS_INIT_PARAM = "eventLogRetentionMillis";
	// Commands available
	private static final String CMD_STOP_ALL = "stopAll";
	private static final String CMD_START_ALL = "startAll";
	private static final String CMD_WRITE_RETENTION_LOG = "writeRetentionLog";

	private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

	private List<Pattern> includes;
	{
		// Par défaut, on log seulement les fichiers .seam
		includes = new ArrayList<Pattern>();
		includes.add(Pattern.compile(".*\\.seam"));
	}
	private List<Pattern> excludes;

	private SystemEventLogger systemEventLogger;
	private int nbExcludedRequests = 0;
	private String cmdRequestName = DEFAULT_CMD_REQUEST_NAME;

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// Configure requestLogFilter
		final String includesParam = filterConfig.getInitParameter(REQUEST_NAME_INCLUDES_INIT_PARAM);
		if (includesParam != null) {
			this.includes = parsePatternList(includesParam);
		}
		final String excludesParam = filterConfig.getInitParameter(REQUEST_NAME_EXCLUDES_INIT_PARAM);
		if (excludesParam != null) {
			this.excludes = parsePatternList(excludesParam);
		}
		final String cmdRequestName = filterConfig.getInitParameter(CMD_REQUEST_NAME_INIT_PARAM);
		if (StringUtils.isNotBlank(cmdRequestName)) {
			this.cmdRequestName = cmdRequestName;
		}
		/// Initializing system event logger
		final CpuCriticalUsageWatcher cpuCriticalUsageWatcher = new CpuCriticalUsageWatcher();
		// Configure cpuCriticalUsageWatcher
		final String criticalCpuUsage = filterConfig.getInitParameter(CRITICAL_CPU_USAGE_INIT_PARAM);
		if (StringUtils.isNotBlank(criticalCpuUsage)) {
			cpuCriticalUsageWatcher.setCriticalCpuUsage(Float.parseFloat(criticalCpuUsage));
		}
		final String deadLockThreadsSearchDeltaMillis = filterConfig.getInitParameter(DEAD_LOCK_THREADS_SEARCH_DELTA_MILLIS_INIT_PARAM);
		if (StringUtils.isNotBlank(deadLockThreadsSearchDeltaMillis)) {
			cpuCriticalUsageWatcher.setDeadLockThreadsSearchDeltaMillis(Long.parseLong(deadLockThreadsSearchDeltaMillis));
		}

		final EventLogManager eventLogManager = EventLogManager.getInstance();
		// Configure the eventLogManager
		final String eventLogRetentionMillis = filterConfig.getInitParameter(EVENT_LOG_RETENTION_MILLIS_INIT_PARAM);
		if (StringUtils.isNotBlank(eventLogRetentionMillis)) {
			eventLogManager.setEventLogRetentionMillis(Long.parseLong(eventLogRetentionMillis));
		}
		final String logPath = filterConfig.getInitParameter(LOG_PATH_INIT_PARAM);
		if (StringUtils.isNotBlank(logPath)) {
			eventLogManager.setLogPath(logPath);
		}
		final String waitForEventLogToCompleteMillis = filterConfig.getInitParameter(WAIT_FOR_EVENT_LOG_TO_COMPLETE_MILLIS_INIT_PARAM);
		if (StringUtils.isNotBlank(waitForEventLogToCompleteMillis)) {
			eventLogManager.setWaitForEventLogToCompleteMillis(Long.parseLong(waitForEventLogToCompleteMillis));
		}
		eventLogManager.addEventLogListener(cpuCriticalUsageWatcher);

		systemEventLogger = new SystemEventLogger();
		// Configure systemEventLogger
		final String cpuComputationDeltaMillis = filterConfig.getInitParameter(CPU_COMPUTATION_DELTA_MILLIS_INIT_PARAM);
		if (StringUtils.isNotBlank(cpuComputationDeltaMillis)) {
			systemEventLogger.setCpuComputationDeltaMillis(Long.parseLong(cpuComputationDeltaMillis));
		}

		startServices();
	}

	private List<Pattern> parsePatternList(final String includesParam) {
		final List<Pattern> patterns = new ArrayList<Pattern>();
		final String[] includes = includesParam.split(",");
		for (final String include : includes) {
			if (StringUtils.isNotBlank(include)) {
				patterns.add(Pattern.compile(include));
			}
		}
		return patterns;
	}

	private void startServices() {
		systemEventLogger.start();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest)request;
		final String requestURI = httpRequest.getRequestURI();
		if (requestURI.endsWith(cmdRequestName)) {
			// This is a Command Request, let's execute it
			final HttpServletResponse httpResponse = ((HttpServletResponse)response);
			if (httpRequest.getParameter(CMD_START_ALL) != null) {
				startServices();
			} else if (httpRequest.getParameter(CMD_STOP_ALL) != null) {
				stopServices();
			} else if (httpRequest.getParameter(CMD_WRITE_RETENTION_LOG) != null) {
				EventLogManager.getInstance().writeRetentionLog();
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
			if (includes != null) {
				for (final Iterator<Pattern> iterator = includes.iterator(); iterator.hasNext() && !matches;) {
					final Pattern include = iterator.next();
					matches |= include.matcher(requestURI).matches();
				}
			}
			if (excludes != null) {
				for (final Iterator<Pattern> iterator = excludes.iterator(); iterator.hasNext() && matches;) {
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

}
