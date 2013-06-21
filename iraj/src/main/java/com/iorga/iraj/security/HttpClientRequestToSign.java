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
