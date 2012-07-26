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

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		final String includesParam = filterConfig.getInitParameter("includes");
		if (includesParam != null) {
			this.includes = new ArrayList<Pattern>();
			final String[] includes = includesParam.split(",");
			for (final String include : includes) {
				if (StringUtils.isNotBlank(include)) {
					this.includes.add(Pattern.compile(include));
				}
			}
		}
		final String excludesParam = filterConfig.getInitParameter("excludes");
		if (excludesParam != null) {
			this.excludes = new ArrayList<Pattern>();
			final String[] excludes = excludesParam.split(",");
			for (final String exclude : excludes) {
				if (StringUtils.isNotBlank(exclude)) {
					this.excludes.add(Pattern.compile(exclude));
				}
			}
		}
		// Initializing system event logger
		EventLogManager.getInstance().addEventLogListener(new CpuCriticalUsageWatcher());
		systemEventLogger = new SystemEventLogger();
		systemEventLogger.start();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest httpRequest = (HttpServletRequest)request;
		final String requestURI = httpRequest.getRequestURI();
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

		chain.doFilter(request, response);

		if (matches) {
			logRequest.setAfterProcessedDate(new Date());
			EventLogManager.getInstance().fire(logRequest);
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
		systemEventLogger.stop();
		try {
			EventLogManager.getInstance().closeLog();
		} catch (final IOException e) {
			log.error("Cannot close EventLog's log", e);
		}
	}

}
