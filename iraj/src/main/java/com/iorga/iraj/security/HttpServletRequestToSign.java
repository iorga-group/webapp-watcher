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
import java.util.Iterator;

import org.apache.commons.collections.iterators.EnumerationIterator;

public class HttpServletRequestToSign implements HttpRequestToSign {
	private final MultiReadHttpServletRequest httpServletRequest;

	/**
	 * @param httpServletRequest MultiReadHttpServletRequest as it is preferable that the httpServlet be read multiple times
	 */
	public HttpServletRequestToSign(final MultiReadHttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	@Override
	public String getMethod() {
		return httpServletRequest.getMethod();
	}

	@Override
	public byte[] getBodyBytes() throws IOException {
		return httpServletRequest.getBodyBytes();
	}

	@Override
	public String getContentType() {
		return httpServletRequest.getContentType();
	}

	@Override
	public Iterable<String> getHeaderNames() {
		return new Iterable<String>() {
			@SuppressWarnings("unchecked")
			@Override
			public Iterator<String> iterator() {
				return new EnumerationIterator(httpServletRequest.getHeaderNames());
			}
		};
	}

	@Override
	public Iterable<String> getHeaders(final String name) {
		return new Iterable<String>() {
			@SuppressWarnings("unchecked")
			@Override
			public Iterator<String> iterator() {
				return new EnumerationIterator(httpServletRequest.getHeaders(name));
			}
		};
	}

	@Override
	public String getHeader(final String name) {
		return httpServletRequest.getHeader(name);
	}

	@Override
	public String getResource() {
		final String queryString = httpServletRequest.getQueryString();
		return httpServletRequest.getPathInfo()+(queryString != null ? "?"+queryString : "");
	}

}
