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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.ClientRequest;

public class HttpClientRequestToSign implements HttpRequestToSign {
	private final ClientRequest clientRequest;
	private final String webApplicationPathPrefix;

	public HttpClientRequestToSign(final ClientRequest clientRequest, final String webApplicationPathPrefix) {
		this.clientRequest = clientRequest;
		this.webApplicationPathPrefix = webApplicationPathPrefix;
	}

	@Override
	public String getMethod() {
		return clientRequest.getHttpMethod();
	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		clientRequest.writeRequestBody(clientRequest.getHeadersAsObjects(), outputStream);
		return outputStream.toByteArray();
	}

	@Override
	public String getContentType() {
		final MediaType bodyContentType = clientRequest.getBodyContentType();
		return bodyContentType != null ? bodyContentType.toString() : null;
	}

	@Override
	public Iterable<String> getHeaderNames() {
		return clientRequest.getHeaders().keySet();
	}

	@Override
	public Iterable<String> getHeaders(final String name) {
		return clientRequest.getHeaders().get(name);
	}

	@Override
	public String getHeader(final String name) {
		return clientRequest.getHeaders().getFirst(name);
	}

	@Override
	public String getResource() {
		try {
			return StringUtils.substringAfter(clientRequest.getUri(), webApplicationPathPrefix);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

}
