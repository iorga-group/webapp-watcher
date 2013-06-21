package com.iorga.iraj.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonParseException;

import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(final JsonParseException exception) {
    	return JaxRsUtils.throwableToIrajResponse(ThrowableTemplate.class, exception);
    }
}