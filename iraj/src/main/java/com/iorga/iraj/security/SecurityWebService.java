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
