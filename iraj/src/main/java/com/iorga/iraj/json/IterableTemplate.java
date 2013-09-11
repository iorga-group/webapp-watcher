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
package com.iorga.iraj.json;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;

public class IterableTemplate implements Template {
	private final Template itemTemplate;

	public IterableTemplate(final Template itemTemplate) {
		this.itemTemplate = itemTemplate;
	}

	@Override
	public void writeJson(final OutputStream output, final Object context) throws IOException, WebApplicationException {
		final Iterable<?> iterable = (Iterable<?>) context;
		boolean first = true;
		output.write('[');
		for (final Object item : iterable) {
			if (first) {
				first = false;
			} else {
				output.write(',');
			}
			itemTemplate.writeJson(output, item);
		}
		output.write(']');
	}

}
