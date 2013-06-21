package com.iorga.iraj.jaxrs;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.map.JsonMappingException;

import com.iorga.iraj.util.JaxRsUtils;

@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    @Override
    public Response toResponse(JsonMappingException exception) {
    	return JaxRsUtils.throwableToIrajResponse(ThrowableTemplate.class, exception);
    }
}
