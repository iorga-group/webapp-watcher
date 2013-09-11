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
