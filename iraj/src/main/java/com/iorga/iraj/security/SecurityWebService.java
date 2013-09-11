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

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path(SecurityWebService.SECURITY_WEB_SERVICE_PATH_PREFIX)
public class SecurityWebService {
	public static final String SECURITY_WEB_SERVICE_PATH_PREFIX = "/security";
	public static final String GET_TIME_METHOD_PATH = "/getTime";

	@GET
	@Path(GET_TIME_METHOD_PATH)
	public long getTime() {
		return new Date().getTime();
	}
}
