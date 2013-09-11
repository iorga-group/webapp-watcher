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


public interface HttpRequestToSign {
	/**
	 *
	 * Returns the name of the HTTP method with which this request was made, for
	 * example, GET, POST, or PUT. Same as the value of the CGI variable
	 * REQUEST_METHOD.
	 *
	 * @return a <code>String</code> specifying the name of the method with
	 *         which this request was made
	 *
	 */

	public String getMethod();

	/**
	 * Retrieves the body of the request as binary data.
	 *
	 * @return a byte array containing the body of the request
	 *
	 * @exception IOException
	 *                if an input or output exception occurred
	 */
	public byte[] getBodyBytes() throws IOException;

	/**
	 * Returns the MIME type of the body of the request, or <code>null</code> if
	 * the type is not known. For HTTP servlets, same as the value of the CGI
	 * variable CONTENT_TYPE.
	 *
	 * @return a <code>String</code> containing the name of the MIME type of the
	 *         request, or null if the type is not known
	 *
	 */
	public String getContentType();

	/**
	 *
	 * Returns an iterable of all the header names this request contains.
	 *
	 * @return an iterable of all the header names sent with this request;
	 *
	 */
	public Iterable<String> getHeaderNames();

	/**
	 *
	 * Returns all the values of the specified request header as an
	 * <code>Iterable</code> of <code>String</code> objects.
	 *
	 * <p>
	 * Some headers, such as <code>Accept-Language</code> can be sent by clients
	 * as several headers each with a different value rather than sending the
	 * header as a comma separated list.
	 *
	 * The header name is case sensitive. You can use this method with any
	 * request header.
	 *
	 * @param name
	 *            a <code>String</code> specifying the header name
	 *
	 * @return an <code>Iterable</code> containing the values of the requested
	 *         header.
	 *
	 */
	public Iterable<String> getHeaders(String name);

	/**
	 *
	 * Returns the value of the specified request header as a
	 * <code>String</code>. If the request did not include a header of the
	 * specified name, this method returns <code>null</code>. If there are
	 * multiple headers with the same name, this method returns the first head
	 * in the request. The header name is case sensitive. You can use this
	 * method with any request header.
	 *
	 * @param name
	 *            a <code>String</code> specifying the header name
	 *
	 * @return a <code>String</code> containing the value of the requested
	 *         header, or <code>null</code> if the request does not have a
	 *         header of that name
	 *
	 */
	public String getHeader(String name);

	/**
     * Returns the resource wanted by this request.
     * It's the concatenation of any extra path information associated with
     * the URL the client sent when it made this request.
     * It follows the servlet path and also contains the query string and will start with
     * a "/" character.
     *
     * <p>Same as the value of the CGI variables PATH_INFO(?QUERY_STRING).
	 *
	 * @return the resource wanted by this request, or <code>null</code>
	 *
	 */
	public String getResource();
}
