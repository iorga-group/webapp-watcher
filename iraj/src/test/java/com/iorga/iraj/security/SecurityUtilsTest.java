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
