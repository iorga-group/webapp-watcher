package com.iorga.iraj.security;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.util.DateUtil;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

public class AbstractSecurityFilterTest {
//	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public static class SecurityFilter extends AbstractSecurityFilter<SimpleSecurityContext> {
		private final boolean handleDateShift;
		private final String accessKeyId;
		private final SimpleSecurityContext simpleSecurityContext;

		public SecurityFilter(final String accessKeyId, final String secretAccessKey, final boolean handleDateShift) {
			this.accessKeyId = accessKeyId;
			this.simpleSecurityContext = new SimpleSecurityContext(secretAccessKey);
			this.handleDateShift = handleDateShift;
		}

		@Override
		protected boolean handleParsedDate(final Date parsedDate, final SimpleSecurityContext simpleSecurityContext, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws IOException {
			return handleDateShift ? super.handleParsedDate(parsedDate, simpleSecurityContext, httpRequest, httpResponse) : true;
		}

		@Override
		protected SimpleSecurityContext findSecurityContext(final String accessKeyId) {
			Assert.assertEquals(this.accessKeyId, accessKeyId);
			return simpleSecurityContext;
		}
	}

	public static class FakeServletInputStream extends ServletInputStream {
		private final InputStream inputStream;

		public FakeServletInputStream(final InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public int read() throws IOException {
			return inputStream.read();
		}
	}

	public static class HttpServletRequestMockBuilder {
		private final Map<String, String> headers = new HashMap<String, String>();
		private InputStream body;

		public HttpServletRequestMockBuilder body(final InputStream body) {
			this.body = body;
			return this;
		}

		public HttpServletRequestMockBuilder addHeader(final String name, final String value) {
			headers.put(name, value);
			return this;
		}

		public HttpServletRequest build() {
			// Adding the "Date" header
			headers.put("Date", DateUtil.formatDate(new Date()));

			final HttpServletRequest requestMock = mock(HttpServletRequest.class);
			// Mocking body inputStream
			try {
				when(requestMock.getInputStream()).thenReturn(new FakeServletInputStream(body));
			} catch (final IOException e) {
				// Can't throw exception
			}
			// Mocking headers
			when(requestMock.getHeader(Mockito.anyString())).thenAnswer(new Answer<String>() {
				@Override
				public String answer(final InvocationOnMock invocation) throws Throwable {
					final String headerName = (String) invocation.getArguments()[0];
					return headers.get(headerName);
				}
			});
			when(requestMock.getHeaders(Mockito.anyString())).thenAnswer(new Answer<Enumeration<String>>() {
				@Override
				public Enumeration<String> answer(final InvocationOnMock invocation) throws Throwable {
					final String headerName = (String) invocation.getArguments()[0];
					return Collections.enumeration(Lists.newArrayList(headers.get(headerName)));
				}
			});
			when(requestMock.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
			when(requestMock.getMethod()).thenReturn("GET");
			when(requestMock.getContentType()).thenReturn("text/plain");
			when(requestMock.getQueryString()).thenReturn(null);
			when(requestMock.getPathInfo()).thenReturn("/");
			return requestMock;
		}
	}

	@Test
	public void testStaticSignature() throws IOException, ServletException, NoSuchAlgorithmException, InvalidKeyException {
		// Create the HTTP request mock
		final Date currentAdditionalDate = new Date(0);	// "Thu, 01 Jan 1970 00:00:00 GMT" with HTTP date format
		final String requestBody = "Body Test";	// MD5 : 2f7fb2f31c94658d3ac47fc1f4a6cde9
		final String accessKeyId = SecurityUtils.generateAccessKeyId();
		final String secretAccessKey = "iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0";
		// Computing the string to sign
		/* Using java apis
		final String stringToSign =
			"GET\n" +	// HTTP method
			"2f7fb2f31c94658d3ac47fc1f4a6cde9\n" + // Body MD5
			"text/plain\n" +	// content type
			"Thu, 01 Jan 1970 00:00:00 GMT\n" +	// reference date (our forged ADDITIONAL_DATE_HEADER)
			"x-iraj-date:Thu, 01 Jan 1970 00:00:00 GMT\n" +	// all the "x-iraj" headers
			"/\n";	// the resource pathinfo + params
		// Compute the authorization Header
		final SecretKeySpec secretKeySpec = new SecretKeySpec(secretAccessKey.getBytes(UTF8_CHARSET), "HmacSHA1");
		final Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKeySpec);
		final String signature = StringUtils.chomp(Base64.encodeBase64String(mac.doFinal(stringToSign.getBytes(UTF8_CHARSET))));
		*/

		/* computed from bash with (thanks to http://stackoverflow.com/questions/7285059/hmac-sha1-in-bash and http://stackoverflow.com/questions/4583967/how-to-encode-md5-sum-into-base64-in-bash) :
			echo -n "GET
			> 2f7fb2f31c94658d3ac47fc1f4a6cde9
			> text/plain
			> Thu, 01 Jan 1970 00:00:00 GMT
			> x-iraj-date:Thu, 01 Jan 1970 00:00:00 GMT
			> /
			> " | openssl sha1 -binary -hmac "iLKJ8zhzU/5eEZFKeQ5bP+piXQ/JQr4+QKbORZP0" | base64
			fWUiX2xF1+oSDIv7m+3cbo8Ve88=
		 */

		final String signature = "fWUiX2xF1+oSDIv7m+3cbo8Ve88=";

		final String authorizationHeader = "IWS "+accessKeyId+":"+signature;

		final HttpServletRequest requestMock = new HttpServletRequestMockBuilder()
			.body(new ByteArrayInputStream(requestBody.getBytes()))
			.addHeader("Authorization", authorizationHeader)
			.addHeader("X-IRAJ-Date", DateUtil.formatDate(currentAdditionalDate))
			.build();
		final HttpServletResponse responseMock = mock(HttpServletResponse.class);
		final FilterChain filterChainMock = mock(FilterChain.class);

		final SecurityFilter securityFilter = new SecurityFilter(accessKeyId, secretAccessKey, false);
		securityFilter.doFilter(requestMock, responseMock, filterChainMock);

		verify(filterChainMock).doFilter(any(ServletRequest.class), any(ServletResponse.class));
	}

}
