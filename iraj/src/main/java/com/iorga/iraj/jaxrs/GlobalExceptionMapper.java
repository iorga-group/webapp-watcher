package com.iorga.iraj.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
	@Override
	public Response toResponse(final Throwable throwable) {
		return JaxRsUtils.throwableToIrajResponse(ThrowableTemplate.class, throwable);
	}
}
