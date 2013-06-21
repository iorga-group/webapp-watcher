package com.iorga.iraj.security;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractSecurityWithWebServiceFilter<S extends SecurityContext> extends AbstractSecurityFilter<S> {
	@Override
	protected boolean handleParsedDate(final Date parsedDate, final S securityContext, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws IOException {
		// If the target webservice is the "/api/security/getTime" webservice, do not handle the date check
		if (httpRequest.getPathInfo().equals(SecurityWebService.SECURITY_WEB_SERVICE_PATH_PREFIX+SecurityWebService.GET_TIME_METHOD_PATH)) {
			return true;
		} else {
			return super.handleParsedDate(parsedDate, securityContext, httpRequest, httpResponse);
		}
	}
}
