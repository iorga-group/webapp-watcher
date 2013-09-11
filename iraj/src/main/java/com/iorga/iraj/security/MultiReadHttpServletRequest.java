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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;


// Based on http://stackoverflow.com/a/1048123/535203
public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;

    public MultiReadHttpServletRequest(final HttpServletRequest httpServletRequest) throws IOException {
        super(httpServletRequest);
        // Read the request body and save it as a byte array
        body = IOUtils.toByteArray(super.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new ServletInputStreamImpl(new ByteArrayInputStream(body));
    }

    public byte[] getBodyBytes() {
		return body;
	}

    @Override
    public BufferedReader getReader() throws IOException {
        String enc = getCharacterEncoding();
        if(enc == null) enc = "UTF-8";
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), enc));
    }

    private class ServletInputStreamImpl extends ServletInputStream {

        private final InputStream is;

        public ServletInputStreamImpl(final InputStream is) {
            this.is = is;
        }

        @Override
		public int read() throws IOException {
            return is.read();
        }

        @Override
		public boolean markSupported() {
            return false;
        }

        @Override
		public synchronized void mark(final int i) {
            throw new RuntimeException(new IOException("mark/reset not supported"));
        }

        @Override
		public synchronized void reset() throws IOException {
            throw new IOException("mark/reset not supported");
        }
    }
}
