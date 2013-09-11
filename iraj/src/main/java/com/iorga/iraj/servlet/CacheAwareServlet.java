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
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iorga.iraj.servlet;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ResponseFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iorga.iraj.servlet.CacheAwareServlet.CacheEntry.Attributes;
import com.iorga.iraj.servlet.CacheAwareServlet.CacheEntry.Resource;

// based on http://grepcode.com/file/repo1.maven.org/maven2/org.apache.tomcat/tomcat-catalina/7.0.37/org/apache/catalina/servlets/DefaultServlet.java#DefaultServlet.checkIfHeaders%28javax.servlet.http.HttpServletRequest%2Cjavax.servlet.http.HttpServletResponse%2Corg.apache.naming.resources.ResourceAttributes%29
public abstract class CacheAwareServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger log = LoggerFactory.getLogger(CacheAwareServlet.class);

    /**
     * MIME multipart separation string
     */
    protected static final String mimeSeparation = "CATALINA_MIME_BOUNDARY";
    /**
     * Full range marker.
     */
    protected static final ArrayList<Range> FULL = new ArrayList<Range>();

	public static interface CacheEntry {

		boolean exists();

		Attributes getAttributes();

		public static interface Attributes {

			String getMimeType();

			void setMimeType(String contentType);

			String getETag();

			String getLastModifiedHttp();

			long getContentLength();

			long getLastModified();

		}

		Resource getResource();

		public static interface Resource {

			byte[] getContent();

			InputStream streamContent();

		}

		String getName();

	}

	protected static class Range {

		public long start;
		public long end;
		public long length;

		/**
		 * Validate range.
		 */
		public boolean validate() {
			if (end >= length)
				end = length - 1;
			return (start >= 0) && (end >= 0) && (start <= end) && (length > 0);
		}
	}


	protected boolean acceptsRanges() {
		return true;
	}

	protected abstract CacheEntry lookupCache(String path);

	protected int getOutputBufferSize() {
		return 2048;
	}

	protected int getInputBufferSize() {
		return 2048;
	}

	protected String getFileEncoding() {
		return "UTF-8";
	}


	/**
	 * Process a GET request for the specified resource.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

		// Serve the requested resource, including the data content
		serveResource(request, response, true);

	}

	/**
	 * Process a HEAD request for the specified resource.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

		// Serve the requested resource, without the data content
		serveResource(request, response, false);

	}

	/**
	 * Serve the specified resource, optionally including the data content.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param content
	 *            Should the content be included?
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet-specified error occurs
	 */
	protected void serveResource(final HttpServletRequest request, final HttpServletResponse response, final boolean content)
			throws IOException, ServletException {

		boolean serveContent = content;

		// Identify the requested resource path
		final String path = getRelativePath(request);
		if (log.isDebugEnabled()) {
			if (serveContent)
				log.debug("DefaultServlet.serveResource:  Serving resource '" + path + "' headers and data");
			else
				log.debug("DefaultServlet.serveResource:  Serving resource '" + path + "' headers only");
		}

		final CacheEntry cacheEntry = lookupCache(path);

		if (!cacheEntry.exists()) {
			// Check if we're included so we can return the appropriate
			// missing resource name in the error
			String requestUri = (String) request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
			if (requestUri == null) {
				requestUri = request.getRequestURI();
			} else {
				// We're included
				// SRV.9.3 says we must throw a FNFE
				throw new FileNotFoundException("Missing resource : " + requestUri);
			}

			response.sendError(HttpServletResponse.SC_NOT_FOUND, requestUri);
			return;
		}

		// If the resource is not a collection, and the resource path
		// ends with "/" or "\", return NOT FOUND
		if (path.endsWith("/") || (path.endsWith("\\"))) {
			// Check if we're included so we can return the appropriate
			// missing resource name in the error
			String requestUri = (String) request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
			if (requestUri == null) {
				requestUri = request.getRequestURI();
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND, requestUri);
			return;
		}

		final boolean isError = response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST;

		// Check if the conditions specified in the optional If headers are
		// satisfied.

		// Checking If headers
		final boolean included = (request.getAttribute(RequestDispatcher.INCLUDE_CONTEXT_PATH) != null);
		if (!included && !isError && !checkIfHeaders(request, response, cacheEntry.getAttributes())) {
			return;
		}

		// Find content type.
		String contentType = cacheEntry.getAttributes().getMimeType();
		if (contentType == null) {
			contentType = getServletContext().getMimeType(cacheEntry.getName());
			cacheEntry.getAttributes().setMimeType(contentType);
		}

		ArrayList<Range> ranges = null;
		long contentLength = -1L;

		if (!isError) {
			if (acceptsRanges()) {
				// Accept ranges header
				response.setHeader("Accept-Ranges", "bytes");
			}

			// Parse range specifier
			ranges = parseRange(request, response, cacheEntry.getAttributes());

			// ETag header
			response.setHeader("ETag", cacheEntry.getAttributes().getETag());

			// Last-Modified header
			response.setHeader("Last-Modified", cacheEntry.getAttributes().getLastModifiedHttp());
		}

		// Get content length
		contentLength = cacheEntry.getAttributes().getContentLength();
		// Special case for zero length files, which would cause a
		// (silent) ISE when setting the output buffer size
		if (contentLength == 0L) {
			serveContent = false;
		}

		ServletOutputStream ostream = null;
		PrintWriter writer = null;

		if (serveContent) {

			// Trying to retrieve the servlet output stream

			try {
				ostream = response.getOutputStream();
			} catch (final IllegalStateException e) {
				// If it fails, we try to get a Writer instead if we're
				// trying to serve a text file
				if ((contentType == null) || (contentType.startsWith("text")) || (contentType.endsWith("xml"))
						|| (contentType.contains("/javascript"))) {
					writer = response.getWriter();
					// Cannot reliably serve partial content with a Writer
					ranges = FULL;
				} else {
					throw e;
				}
			}

		}

		// Check to see if a Filter, Valve of wrapper has written some content.
		// If it has, disable range requests and setting of a content length
		// since neither can be done reliably.
		ServletResponse r = response;
		long contentWritten = 0;
		while (r instanceof ServletResponseWrapper) {
			r = ((ServletResponseWrapper) r).getResponse();
		}
		if (r instanceof ResponseFacade) {
			contentWritten = ((ResponseFacade) r).getContentWritten();
		}
		if (contentWritten > 0) {
			ranges = FULL;
		}

		if (isError || (((ranges == null) || (ranges.isEmpty())) && (request.getHeader("Range") == null)) || (ranges == FULL)) {

			// Set the appropriate output headers
			if (contentType != null) {
				if (log.isDebugEnabled())
					log.debug("DefaultServlet.serveFile:  contentType='" + contentType + "'");
				response.setContentType(contentType);
			}
			if ((cacheEntry.getResource() != null) && (contentLength >= 0) && (!serveContent || ostream != null)) {
				if (log.isDebugEnabled())
					log.debug("DefaultServlet.serveFile:  contentLength=" + contentLength);
				// Don't set a content length if something else has already
				// written to the response.
				if (contentWritten == 0) {
					if (contentLength < Integer.MAX_VALUE) {
						response.setContentLength((int) contentLength);
					} else {
						// Set the content-length as String to be able to use a
						// long
						response.setHeader("content-length", "" + contentLength);
					}
				}
			}

			final InputStream renderResult = null;

			// Copy the input stream to our output stream (if requested)
			if (serveContent) {
				try {
					response.setBufferSize(getOutputBufferSize());
				} catch (final IllegalStateException e) {
					// Silent catch
				}
				if (ostream != null) {
					copy(cacheEntry, renderResult, ostream);
				} else {
					copy(cacheEntry, renderResult, writer);
				}
			}

		} else {

			if ((ranges == null) || (ranges.isEmpty()))
				return;

			// Partial content response.

			response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

			if (ranges.size() == 1) {

				final Range range = ranges.get(0);
				response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
				final long length = range.end - range.start + 1;
				if (length < Integer.MAX_VALUE) {
					response.setContentLength((int) length);
				} else {
					// Set the content-length as String to be able to use a long
					response.setHeader("content-length", "" + length);
				}

				if (contentType != null) {
					if (log.isDebugEnabled())
						log.debug("DefaultServlet.serveFile:  contentType='" + contentType + "'");
					response.setContentType(contentType);
				}

				if (serveContent) {
					try {
						response.setBufferSize(getOutputBufferSize());
					} catch (final IllegalStateException e) {
						// Silent catch
					}
					if (ostream != null) {
						copy(cacheEntry, ostream, range);
					} else {
						// we should not get here
						throw new IllegalStateException();
					}
				}

			} else {

				response.setContentType("multipart/byteranges; boundary=" + mimeSeparation);

				if (serveContent) {
					try {
						response.setBufferSize(getOutputBufferSize());
					} catch (final IllegalStateException e) {
						// Silent catch
					}
					if (ostream != null) {
						copy(cacheEntry, ostream, ranges.iterator(), contentType);
					} else {
						// we should not get here
						throw new IllegalStateException();
					}
				}

			}

		}

	}

	/**
	 * Parse the range header.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @return Vector of ranges
	 */
	protected ArrayList<Range> parseRange(final HttpServletRequest request, final HttpServletResponse response, final Attributes resourceAttributes)
			throws IOException {

		// Checking If-Range
		final String headerValue = request.getHeader("If-Range");

		if (headerValue != null) {

			long headerValueTime = (-1L);
			try {
				headerValueTime = request.getDateHeader("If-Range");
			} catch (final IllegalArgumentException e) {
				// Ignore
			}

			final String eTag = resourceAttributes.getETag();
			final long lastModified = resourceAttributes.getLastModified();

			if (headerValueTime == (-1L)) {

				// If the ETag the client gave does not match the entity
				// etag, then the entire entity is returned.
				if (!eTag.equals(headerValue.trim()))
					return FULL;

			} else {

				// If the timestamp of the entity the client got is older than
				// the last modification date of the entity, the entire entity
				// is returned.
				if (lastModified > (headerValueTime + 1000))
					return FULL;

			}

		}

		final long fileLength = resourceAttributes.getContentLength();

		if (fileLength == 0)
			return null;

		// Retrieving the range header (if any is specified
		String rangeHeader = request.getHeader("Range");

		if (rangeHeader == null)
			return null;
		// bytes is the only range unit supported (and I don't see the point
		// of adding new ones).
		if (!rangeHeader.startsWith("bytes")) {
			response.addHeader("Content-Range", "bytes */" + fileLength);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return null;
		}

		rangeHeader = rangeHeader.substring(6);

		// Vector which will contain all the ranges which are successfully
		// parsed.
		final ArrayList<Range> result = new ArrayList<Range>();
		final StringTokenizer commaTokenizer = new StringTokenizer(rangeHeader, ",");

		// Parsing the range list
		while (commaTokenizer.hasMoreTokens()) {
			final String rangeDefinition = commaTokenizer.nextToken().trim();

			final Range currentRange = new Range();
			currentRange.length = fileLength;

			final int dashPos = rangeDefinition.indexOf('-');

			if (dashPos == -1) {
				response.addHeader("Content-Range", "bytes */" + fileLength);
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return null;
			}

			if (dashPos == 0) {

				try {
					final long offset = Long.parseLong(rangeDefinition);
					currentRange.start = fileLength + offset;
					currentRange.end = fileLength - 1;
				} catch (final NumberFormatException e) {
					response.addHeader("Content-Range", "bytes */" + fileLength);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					return null;
				}

			} else {

				try {
					currentRange.start = Long.parseLong(rangeDefinition.substring(0, dashPos));
					if (dashPos < rangeDefinition.length() - 1)
						currentRange.end = Long.parseLong(rangeDefinition.substring(dashPos + 1, rangeDefinition.length()));
					else
						currentRange.end = fileLength - 1;
				} catch (final NumberFormatException e) {
					response.addHeader("Content-Range", "bytes */" + fileLength);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					return null;
				}

			}

			if (!currentRange.validate()) {
				response.addHeader("Content-Range", "bytes */" + fileLength);
				response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
				return null;
			}

			result.add(currentRange);
		}

		return result;
	}

	/**
	 * Determines the appropriate path to prepend resources with when generating
	 * directory listings. Depending on the behaviour of
	 * {@link #getRelativePath(HttpServletRequest)} this will change.
	 *
	 * @param request
	 *            the request to determine the path for
	 * @return the prefix to apply to all resources in the listing.
	 */
	protected String getPathPrefix(final HttpServletRequest request) {
		return request.getContextPath();
	}

	/**
	 * Return the relative path associated with this servlet.
	 *
	 * @param request
	 *            The servlet request we are processing
	 */
	protected String getRelativePath(final HttpServletRequest request) {
		// IMPORTANT: DefaultServlet can be mapped to '/' or '/path/*' but
		// always
		// serves resources from the web app root with context rooted paths.
		// i.e. it can not be used to mount the web app root under a sub-path
		// This method must construct a complete context rooted path, although
		// subclasses can change this behaviour.

		// Are we being processed by a RequestDispatcher.include()?
		if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
			String result = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
			if (result == null) {
				result = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			} else {
				result = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH) + result;
			}
			if ((result == null) || (result.equals(""))) {
				result = "/";
			}
			return (result);
		}

		// No, extract the desired path directly from the request
		String result = request.getPathInfo();
		if (result == null) {
			result = request.getServletPath();
		} else {
			result = request.getServletPath() + result;
		}
		if ((result == null) || (result.equals(""))) {
			result = "/";
		}
		return (result);

	}

	/**
	 * Check if the conditions specified in the optional If headers are
	 * satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            The resource information
	 * @return boolean true if the resource meets all the specified conditions,
	 *         and false if any of the conditions is not satisfied, in which
	 *         case request processing is stopped
	 */
	protected boolean checkIfHeaders(final HttpServletRequest request, final HttpServletResponse response,
			final Attributes resourceAttributes) throws IOException {

		return checkIfMatch(request, response, resourceAttributes)
				&& checkIfModifiedSince(request, response, resourceAttributes)
				&& checkIfNoneMatch(request, response, resourceAttributes)
				&& checkIfUnmodifiedSince(request, response, resourceAttributes);
	}

	/**
	 * Check if the if-match condition is satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfMatch(final HttpServletRequest request, final HttpServletResponse response, final Attributes resourceAttributes)
			throws IOException {

		final String eTag = resourceAttributes.getETag();
		final String headerValue = request.getHeader("If-Match");
		if (headerValue != null) {
			if (headerValue.indexOf('*') == -1) {

				final StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
				boolean conditionSatisfied = false;

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					final String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag))
						conditionSatisfied = true;
				}

				// If none of the given ETags match, 412 Precodition failed is
				// sent back
				if (!conditionSatisfied) {
					response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
					return false;
				}

			}
		}
		return true;
	}

	/**
	 * Check if the if-modified-since condition is satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfModifiedSince(final HttpServletRequest request, final HttpServletResponse response, final Attributes resourceAttributes) {
		try {
			final long headerValue = request.getDateHeader("If-Modified-Since");
			final long lastModified = resourceAttributes.getLastModified();
			if (headerValue != -1) {

				// If an If-None-Match header has been specified, if modified
				// since
				// is ignored.
				if ((request.getHeader("If-None-Match") == null) && (lastModified < headerValue + 1000)) {
					// The entity has not been modified since the date
					// specified by the client. This is not an error case.
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					response.setHeader("ETag", resourceAttributes.getETag());

					return false;
				}
			}
		} catch (final IllegalArgumentException illegalArgument) {
			return true;
		}
		return true;
	}

	/**
	 * Check if the if-none-match condition is satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfNoneMatch(final HttpServletRequest request, final HttpServletResponse response, final Attributes resourceAttributes)
			throws IOException {

		final String eTag = resourceAttributes.getETag();
		final String headerValue = request.getHeader("If-None-Match");
		if (headerValue != null) {

			boolean conditionSatisfied = false;

			if (!headerValue.equals("*")) {

				final StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");

				while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
					final String currentToken = commaTokenizer.nextToken();
					if (currentToken.trim().equals(eTag))
						conditionSatisfied = true;
				}

			} else {
				conditionSatisfied = true;
			}

			if (conditionSatisfied) {

				// For GET and HEAD, we should respond with
				// 304 Not Modified.
				// For every other method, 412 Precondition Failed is sent
				// back.
				if (("GET".equals(request.getMethod())) || ("HEAD".equals(request.getMethod()))) {
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					response.setHeader("ETag", eTag);

					return false;
				}
				response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the if-unmodified-since condition is satisfied.
	 *
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are creating
	 * @param resourceAttributes
	 *            File object
	 * @return boolean true if the resource meets the specified condition, and
	 *         false if the condition is not satisfied, in which case request
	 *         processing is stopped
	 */
	protected boolean checkIfUnmodifiedSince(final HttpServletRequest request, final HttpServletResponse response, final Attributes resourceAttributes)
			throws IOException {
		try {
			final long lastModified = resourceAttributes.getLastModified();
			final long headerValue = request.getDateHeader("If-Unmodified-Since");
			if (headerValue != -1) {
				if (lastModified >= (headerValue + 1000)) {
					// The entity has not been modified since the date
					// specified by the client. This is not an error case.
					response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
					return false;
				}
			}
		} catch (final IllegalArgumentException illegalArgument) {
			return true;
		}
		return true;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param cacheEntry
	 *            The cache entry for the source resource
	 * @param is
	 *            The input stream to read the source resource from
	 * @param ostream
	 *            The output stream to write to
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(final CacheEntry cacheEntry, final InputStream is, final ServletOutputStream ostream) throws IOException {

		IOException exception = null;
		InputStream resourceInputStream = null;

		// Optimization: If the binary content has already been loaded, send
		// it directly
		final Resource resource = cacheEntry.getResource();
		if (resource != null) {
			final byte buffer[] = resource.getContent();
			if (buffer != null) {
				ostream.write(buffer, 0, buffer.length);
				return;
			}
			resourceInputStream = resource.streamContent();
		} else {
			resourceInputStream = is;
		}

		final InputStream istream = new BufferedInputStream(resourceInputStream, getInputBufferSize());

		// Copy the input stream to the output stream
		exception = copyRange(istream, ostream);

		// Clean up the input stream
		istream.close();

		// Rethrow any exception that has occurred
		if (exception != null)
			throw exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param istream
	 *            The input stream to read from
	 * @param ostream
	 *            The output stream to write to
	 * @return Exception which occurred during processing
	 */
	protected IOException copyRange(final InputStream istream, final ServletOutputStream ostream) {
		// Copy the input stream to the output stream
		IOException exception = null;
		final byte buffer[] = new byte[getInputBufferSize()];
		int len = buffer.length;
		while (true) {
			try {
				len = istream.read(buffer);
				if (len == -1)
					break;
				ostream.write(buffer, 0, len);
			} catch (final IOException e) {
				exception = e;
				len = -1;
				break;
			}
		}
		return exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param cacheEntry
	 *            The cache entry for the source resource
	 * @param is
	 *            The input stream to read the source resource from
	 * @param writer
	 *            The writer to write to
	 *
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(final CacheEntry cacheEntry, final InputStream is, final PrintWriter writer) throws IOException {
		IOException exception = null;

		InputStream resourceInputStream = null;
		final Resource resource = cacheEntry.getResource();
		if (resource != null) {
			resourceInputStream = resource.streamContent();
		} else {
			resourceInputStream = is;
		}

		Reader reader;
		final String fileEncoding = getFileEncoding();
		if (fileEncoding == null) {
			reader = new InputStreamReader(resourceInputStream);
		} else {
			reader = new InputStreamReader(resourceInputStream, fileEncoding);
		}

		// Copy the input stream to the output stream
		exception = copyRange(reader, writer);

		// Clean up the reader
		reader.close();

		// Rethrow any exception that has occurred
		if (exception != null)
			throw exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param reader
	 *            The reader to read from
	 * @param writer
	 *            The writer to write to
	 * @return Exception which occurred during processing
	 */
	protected IOException copyRange(final Reader reader, final PrintWriter writer) {
		// Copy the input stream to the output stream
		IOException exception = null;
		final char buffer[] = new char[getInputBufferSize()];
		int len = buffer.length;
		while (true) {
			try {
				len = reader.read(buffer);
				if (len == -1)
					break;
				writer.write(buffer, 0, len);
			} catch (final IOException e) {
				exception = e;
				len = -1;
				break;
			}
		}
		return exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param cacheEntry
	 *            The cache entry for the source resource
	 * @param ostream
	 *            The output stream to write to
	 * @param range
	 *            Range the client wanted to retrieve
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(final CacheEntry cacheEntry, final ServletOutputStream ostream, final Range range) throws IOException {
		IOException exception = null;

		final InputStream resourceInputStream = cacheEntry.getResource().streamContent();
		final InputStream istream = new BufferedInputStream(resourceInputStream, getInputBufferSize());
		exception = copyRange(istream, ostream, range.start, range.end);

		// Clean up the input stream
		istream.close();

		// Rethrow any exception that has occurred
		if (exception != null)
			throw exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param istream
	 *            The input stream to read from
	 * @param ostream
	 *            The output stream to write to
	 * @param start
	 *            Start of the range which will be copied
	 * @param end
	 *            End of the range which will be copied
	 * @return Exception which occurred during processing
	 */
	protected IOException copyRange(final InputStream istream, final ServletOutputStream ostream, final long start, final long end) {
		if (log.isDebugEnabled())
			log.debug("Serving bytes:" + start + "-" + end);

		long skipped = 0;
		try {
			skipped = istream.skip(start);
		} catch (final IOException e) {
			return e;
		}
		if (skipped < start) {
			return new IOException("Skip failed : "+Long.valueOf(skipped)+" / "+ Long.valueOf(start));
		}

		IOException exception = null;
		long bytesToRead = end - start + 1;

		final byte buffer[] = new byte[getInputBufferSize()];
		int len = buffer.length;
		while ((bytesToRead > 0) && (len >= buffer.length)) {
			try {
				len = istream.read(buffer);
				if (bytesToRead >= len) {
					ostream.write(buffer, 0, len);
					bytesToRead -= len;
				} else {
					ostream.write(buffer, 0, (int) bytesToRead);
					bytesToRead = 0;
				}
			} catch (final IOException e) {
				exception = e;
				len = -1;
			}
			if (len < buffer.length)
				break;
		}

		return exception;
	}

	/**
	 * Copy the contents of the specified input stream to the specified output
	 * stream, and ensure that both streams are closed before returning (even in
	 * the face of an exception).
	 *
	 * @param cacheEntry
	 *            The cache entry for the source resource
	 * @param ostream
	 *            The output stream to write to
	 * @param ranges
	 *            Enumeration of the ranges the client wanted to retrieve
	 * @param contentType
	 *            Content type of the resource
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	protected void copy(final CacheEntry cacheEntry, final ServletOutputStream ostream, final Iterator<Range> ranges, final String contentType) throws IOException {
		IOException exception = null;

		while ((exception == null) && (ranges.hasNext())) {

			final InputStream resourceInputStream = cacheEntry.getResource().streamContent();
			final InputStream istream = new BufferedInputStream(resourceInputStream, getInputBufferSize());

			final Range currentRange = ranges.next();

			// Writing MIME header.
			ostream.println();
			ostream.println("--" + mimeSeparation);
			if (contentType != null)
				ostream.println("Content-Type: " + contentType);
			ostream.println("Content-Range: bytes " + currentRange.start + "-" + currentRange.end + "/" + currentRange.length);
			ostream.println();

			// Printing content
			exception = copyRange(istream, ostream, currentRange.start, currentRange.end);

			istream.close();

		}

		ostream.println();
		ostream.print("--" + mimeSeparation + "--");

		// Rethrow any exception that has occurred
		if (exception != null)
			throw exception;
	}
}
