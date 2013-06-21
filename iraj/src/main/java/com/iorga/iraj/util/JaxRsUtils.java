package com.iorga.iraj.util;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.iorga.iraj.json.JsonWriter;

public class JaxRsUtils {
	private static final JsonWriter JSON_WRITER = new JsonWriter();

	public static final String HEADER_PREFIX = "X-IRAJ-";
	public static final String EXCEPTION_HEADER = HEADER_PREFIX+"Exception";

	public static Response throwableToIrajResponse(final Class<?> templateClass, final Throwable throwable) {
		return Response.status(Response.Status.BAD_REQUEST)
			.header(EXCEPTION_HEADER, throwable.getClass().getName())
			.entity(JSON_WRITER.writeWithTemplate(templateClass, throwable))
			.type(MediaType.APPLICATION_JSON_TYPE)
			.build();
	}
}
