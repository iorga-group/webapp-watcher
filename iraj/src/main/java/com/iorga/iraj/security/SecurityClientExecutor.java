package com.iorga.iraj.security;

import java.util.Date;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;
import org.jboss.resteasy.util.DateUtil;

public class SecurityClientExecutor extends ApacheHttpClient4Executor {
	private final String accessKeyId;
	private final String secretAccessKey;
	private final String webApplicationPathPrefix;


	public SecurityClientExecutor(final String accessKeyId, final String secretAccessKey, final String webApplicationPathPrefix) {
		this.accessKeyId = accessKeyId;
		this.secretAccessKey = secretAccessKey;
		this.webApplicationPathPrefix = webApplicationPathPrefix;
	}


	@Override
	public ClientResponse<?> execute(final ClientRequest request) throws Exception {
		final String data = DateUtil.formatDate(createDateToAddToHeaders());
		request.header(SecurityUtils.ADDITIONAL_DATE_HEADER_NAME, data);
		request.header(SecurityUtils.AUTHORIZATION_HEADER_NAME, SecurityUtils.computeAuthorizationHeaderValue(accessKeyId, secretAccessKey, new HttpClientRequestToSign(request, webApplicationPathPrefix)));
		return super.execute(request);
	}

	protected Date createDateToAddToHeaders() {
		return new Date();
	}
}
