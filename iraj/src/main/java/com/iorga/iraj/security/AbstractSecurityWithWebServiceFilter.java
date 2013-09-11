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
