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
