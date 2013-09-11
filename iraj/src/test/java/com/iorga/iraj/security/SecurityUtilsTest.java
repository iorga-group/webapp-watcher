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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;

import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;

public class SecurityUtilsTest {
	@Test
	public void handleContentTypeHeaderAsCaseInsensitive() throws InvalidKeyException, NoSuchAlgorithmException, IOException, ParseException {
		final Date date = new Date();

		final ClientRequest request1 = new ClientRequest("http://example.com/app/api")
			.header("Date", DateUtil.formatDate(date))
			.header("Content-Type", "application/json;charset=utf-8");
		final String authorization1 = SecurityUtils.computeAuthorizationHeaderValue("test", "test", new HttpClientRequestToSign(request1, "/app/api"));

		final ClientRequest request2 = new ClientRequest("http://example.com/app/api")
			.header("Date", DateUtil.formatDate(date))
			.header("Content-Type", "application/json;charset=UTF-8");
		final String authorization2 = SecurityUtils.computeAuthorizationHeaderValue("test", "test", new HttpClientRequestToSign(request2, "/app/api"));

		Assert.assertEquals(authorization1, authorization2);
	}
}
