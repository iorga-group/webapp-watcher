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
