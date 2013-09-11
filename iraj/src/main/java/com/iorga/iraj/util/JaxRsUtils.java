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
