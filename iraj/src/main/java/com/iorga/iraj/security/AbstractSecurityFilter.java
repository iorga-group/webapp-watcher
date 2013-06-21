package com.iorga.iraj.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.resteasy.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.iraj.message.MessagesBuilder;


/**
 * Security filter (for example for webservices), based on http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html and
 * http://docs.amazonwebservices.com/AmazonCloudFront/latest/DeveloperGuide/RESTAuthentication.html
 */
public abstract class AbstractSecurityFilter<S extends SecurityContext> implements Filter {

	private static final Logger log = LoggerFactory.getLogger(AbstractSecurityFilter.class);

	private final static String BASE64_REGEXP = "(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?";	// Read on http://stackoverflow.com/a/475217/535203
	private final static Pattern AUTHORIZATION_HEADER_PATTERN = Pattern.compile("^"+SecurityUtils.AUTHORIZATION_HEADER_VALUE_PREFIX+" (\\w+):("+BASE64_REGEXP+")$");

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		// Extraction of the authentication header
		final HttpServletRequest httpRequest = (HttpServletRequest)request;
		final HttpServletResponse httpResponse = (HttpServletResponse)response;
		final String authorizationHeader = httpRequest.getHeader(SecurityUtils.AUTHORIZATION_HEADER_NAME);
		if (authorizationHeader == null) {
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Need "+SecurityUtils.AUTHORIZATION_HEADER_NAME+" header", httpResponse);
		} else {
			final Matcher matcher = AUTHORIZATION_HEADER_PATTERN.matcher(authorizationHeader);
			if (matcher.find()) {
				final String accessKeyId = matcher.group(1);
				final String signature = matcher.group(2);
				String date = httpRequest.getHeader("Date");
				// Handle the additional date header
				final String additionalDate = httpRequest.getHeader(SecurityUtils.ADDITIONAL_DATE_HEADER_NAME);
				if (additionalDate != null) {
					date = additionalDate;
				}
				try {
					final S securityContext = findSecurityContext(accessKeyId);
					if (securityContext != null) {
						if (handleParsedDate(DateUtil.parseDate(date), securityContext, httpRequest, httpResponse)) {
							// Let's process the signature in order to compare it
							final String secretAccessKey = securityContext.getSecretAccessKey();
							try {
								final MultiReadHttpServletRequest multiReadHttpRequest = new MultiReadHttpServletRequest(httpRequest);
								final String serverSignature = SecurityUtils.computeSignature(secretAccessKey, new HttpServletRequestToSign(multiReadHttpRequest));
								if (serverSignature.equalsIgnoreCase(signature)) {
									doFilterWhenSecurityOK(httpRequest, httpResponse, chain, multiReadHttpRequest, accessKeyId);
								} else {
									rejectSignature(signature, serverSignature, httpResponse);
								}
							} catch (final NoSuchAlgorithmException e) {
								throw new ServletException(e);
							} catch (final InvalidKeyException e) {
								sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid key", httpResponse, e);
							}
						}
					} else {
						rejectAccessKeyId(accessKeyId, httpResponse);
					}
				} catch (final ParseException e) {
					sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid date", httpResponse, "Have to parse '"+date+"'", e);
				} finally {
					doFinallyAfterFindSecurityContext();
				}
			} else {
				sendError(HttpServletResponse.SC_BAD_REQUEST, "Request incorrectly formated", httpResponse, "Got "+authorizationHeader);
			}
		}
	}

	/**
	 * Method which is called in the finally part after the findSecurityContext method has been called
	 */
	protected void doFinallyAfterFindSecurityContext() {
	}

	/**
	 * @param accessKeyId
	 * @return the {@link SecurityContext} for the given <code>accessKeyId</code>, <code>null</code> if not
	 * found or the accessKeyId is invalid
	 */
	protected abstract S findSecurityContext(String accessKeyId);

	protected void doFilterWhenSecurityOK(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse, final FilterChain chain, final MultiReadHttpServletRequest multiReadHttpRequest, final String accessKeyId) throws IOException, ServletException {
		// By default, security OK, forward to next filter
		chain.doFilter(multiReadHttpRequest, httpResponse);
	}

	protected boolean handleParsedDate(final Date parsedDate, final S securityContext, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws IOException {
		final Date localDate = new Date();
		// By default, we check that the time shifting is less than 15mn
		if (Math.abs(parsedDate.getTime() - localDate.getTime()) > 15 * 60 * 1000) {
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Date too far from local time", httpResponse, "Got "+parsedDate+", local date is "+localDate);
			return false;
		} else {
			return true;
		}
	}

	protected void rejectAccessKeyId(final String accessKeyId, final HttpServletResponse httpResponse) throws IOException {
		rejectGenericInvalidAccessKeyIdOrSignature("Couldn't find SecurityContext for "+accessKeyId, httpResponse);
//		sendError(HttpServletResponse.SC_UNAUTHORIZED, "AccessKeyId unknown or invalid", httpResponse, "Couldn't find SecurityContext for "+accessKeyId);
	}

	protected void rejectSignature(final String signature, final String serverSignature, final HttpServletResponse httpResponse) throws IOException {
		rejectGenericInvalidAccessKeyIdOrSignature("Got "+signature+", was expecting "+serverSignature, httpResponse);
//		sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unvalid signature", httpResponse, "Got "+signature+", was expecting "+serverSignature);
	}

	protected void rejectGenericInvalidAccessKeyIdOrSignature(final String debugMessage, final HttpServletResponse httpResponse) throws IOException {
		sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid accessKeyId or signature", httpResponse, debugMessage);
	}

	protected static void sendError(final int sc, final String message, final HttpServletResponse resp) throws IOException {
		sendError(sc, message, resp, null, null);
	}

	protected static void sendError(final int sc, final String message, final HttpServletResponse resp, final String debugMessage) throws IOException {
		sendError(sc, message, resp, debugMessage, null);
	}

	protected static void sendError(final int sc, final String message, final HttpServletResponse resp, final Throwable debugThrowableCause) throws IOException {
		sendError(sc, message, resp, null, debugThrowableCause);
	}

	protected static void sendError(final int sc, final String message, final HttpServletResponse resp, final String debugMessage, final Throwable debugThrowableCause) throws IOException {
		if (log.isDebugEnabled()) {
			final String logMessage = "["+sc+":"+message+"]"+(debugMessage != null ? " "+debugMessage : "");
			if (debugThrowableCause != null) {
				log.debug(logMessage, debugThrowableCause);
			} else {
				log.debug(logMessage);
			}
		}

		resp.setStatus(sc);
		new MessagesBuilder().appendError(message).build().writeToOutputStream(resp.getOutputStream());
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
