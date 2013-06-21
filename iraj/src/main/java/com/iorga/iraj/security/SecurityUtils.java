package com.iorga.iraj.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.iraj.util.JaxRsUtils;


public class SecurityUtils {
	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String ADDITIONAL_HEADER_PREFIX = JaxRsUtils.HEADER_PREFIX;
	public static final String ADDITIONAL_DATE_HEADER_NAME = ADDITIONAL_HEADER_PREFIX+"Date";
	public static final String AUTHORIZATION_HEADER_VALUE_PREFIX = "IWS";

	private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final char[] SYMBOLS = new char[36];
	static {
		for (int i = 0; i < 10; i++) {
			SYMBOLS[i] = (char) ('0' + i);
		}
		for (int i = 10; i < 36; i++) {
			SYMBOLS[i] = (char) ('A' + i - 10);
		}
	}
	private static final int ACCESS_KEY_ID_LENGTH = 25;

	public static String computeSignature(final String secretAccessKey, final HttpRequestToSign httpRequest) throws IOException, InvalidKeyException, NoSuchAlgorithmException, ParseException {
		return computeDataSignature(secretAccessKey, computeData(httpRequest));
	}

	private static String computeData(final HttpRequestToSign httpRequest) throws IOException, ParseException {
		final StringBuilder dataBuilder = new StringBuilder();
		// HTTP method
		final String method = httpRequest.getMethod();
		if (log.isDebugEnabled()) {
			log.debug("method = "+method);
		}
		dataBuilder.append(method).append("\n");
		// body MD5
		final byte[] bodyBytes = httpRequest.getBodyBytes();
		if (bodyBytes.length > 0) {
			final String bodyMd5 = DigestUtils.md5Hex(bodyBytes);
			if (log.isDebugEnabled()) {
				log.debug("bodyMd5 = "+bodyMd5);
			}
			dataBuilder.append(bodyMd5);
		}
		dataBuilder.append("\n");
		// Content type
		final String contentType = httpRequest.getContentType();
		if (StringUtils.isNotEmpty(contentType)) {
			if (log.isDebugEnabled()) {
				log.debug("contentType = "+contentType);
			}
			dataBuilder.append(contentType.toLowerCase());
		}
		dataBuilder.append("\n");
		/// Date
		String date = httpRequest.getHeader("Date");
		if (log.isDebugEnabled()) {
			log.debug("date = "+date);
		}
		// Handle additional date header
		final String additionalDate = httpRequest.getHeader(SecurityUtils.ADDITIONAL_DATE_HEADER_NAME);
		if (additionalDate != null) {
			if (log.isDebugEnabled()) {
				log.debug("additionalDate = "+additionalDate);
			}
			date = additionalDate;
		}
		// Adding date
		DateUtil.parseDate(date);
		dataBuilder.append(date).append("\n");
		// Handling security additional headers
		final List<String> canonicalizedHeaderNames = new ArrayList<String>();
		final Map<String, String> canonicalizedHeaderNamesMap = new HashMap<String, String>();
		for (final String headerName : httpRequest.getHeaderNames()) {
			if (StringUtils.startsWithIgnoreCase(headerName, SecurityUtils.ADDITIONAL_HEADER_PREFIX)) {
				// This is a security additional header
				final String lowerCasedHeaderName = headerName.toLowerCase();
				canonicalizedHeaderNames.add(lowerCasedHeaderName);
				canonicalizedHeaderNamesMap.put(lowerCasedHeaderName, headerName);
			}
		}
		// Sort of the headers
		Collections.sort(canonicalizedHeaderNames);
		for (final String canonicalizedHeaderName : canonicalizedHeaderNames) {
			// Collect values
			final StringBuilder headerDataBuilder = new StringBuilder();
			headerDataBuilder.append(canonicalizedHeaderName).append(":");
			boolean firstValue = true;
			for (final String headerValue : httpRequest.getHeaders(canonicalizedHeaderNamesMap.get(canonicalizedHeaderName))) {
				if (firstValue) {
					firstValue = false;
				} else {
					headerDataBuilder.append(",");
				}
				headerDataBuilder.append(headerValue);
			}
			if (log.isDebugEnabled()) {
				log.debug("header ["+headerDataBuilder.toString()+"]");
			}
			dataBuilder.append(headerDataBuilder).append("\n");
			//TODO implement "4	"Unfold" long headers that span multiple lines (as allowed by RFC 2616, section 4.2) by replacing the folding white-space (including new-line) by a single space." http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#d0e3869
		}
		// Adding request
		final String resource = httpRequest.getResource();
		if (log.isDebugEnabled()) {
			log.debug("resource = "+resource);
		}
		dataBuilder.append(resource).append("\n");
		return dataBuilder.toString();
	}

	public static String computeDataSignature(final String secretAccessKey, final String data) throws NoSuchAlgorithmException, InvalidKeyException {
		final SecretKeySpec secretKeySpec = new SecretKeySpec(secretAccessKey.getBytes(UTF8_CHARSET), "HmacSHA1");
		final Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKeySpec);
		final String signature = Base64.encodeBase64String(mac.doFinal(data.getBytes(UTF8_CHARSET)));
		return StringUtils.chomp(signature);
	}

	public static String computeAuthorizationHeaderValue(final String accessKeyId, final String secretAccessKey, final HttpRequestToSign httpRequest) throws NoSuchAlgorithmException, InvalidKeyException, IOException, ParseException {
		return computeAuthorizationHeaderValue(accessKeyId, secretAccessKey, computeData(httpRequest));
	}

	public static String computeAuthorizationHeaderValue(final String accessKeyId, final String secretAccessKey, final String data) throws NoSuchAlgorithmException, InvalidKeyException {
		return AUTHORIZATION_HEADER_VALUE_PREFIX+" "+accessKeyId+":"+computeDataSignature(secretAccessKey, data);
	}

	/**
	 * Generates a 25 length String access key id
	 * @return a 25 length String access key id
	 */
	public static String generateAccessKeyId() {
		// Based on http://stackoverflow.com/a/41156/535203
		final char[] accessKeyId = new char[ACCESS_KEY_ID_LENGTH];
		for (int i = 0; i < accessKeyId.length; i++) {
			accessKeyId[i] = SYMBOLS[SECURE_RANDOM.nextInt(SYMBOLS.length)];
		}
		return new String(accessKeyId);
	}

	/**
	 * Generates a 40 length String secret access key
	 * @return a 40 length Base64 encoded String
	 */
	public static String generateSecretAccessKey() {
		final byte[] secretAccessKeyBytes = new byte[30];
		SECURE_RANDOM.nextBytes(secretAccessKeyBytes);
		return StringUtils.chomp(Base64.encodeBase64String(secretAccessKeyBytes));
	}
}
