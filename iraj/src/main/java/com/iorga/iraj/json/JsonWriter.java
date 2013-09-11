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
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import com.google.common.collect.Maps;

@ApplicationScoped
public class JsonWriter {
	private final Map<Class<?>, ClassTemplate> templateCache = Maps.newHashMap();

	public StreamingOutput writeWithTemplate(final Class<?> templateClass, final Object context) {
		final Template template = getFromCacheOrCreateTemplate(templateClass);
		return new StreamingOutput() {
			@Override
			public void write(final OutputStream output) throws IOException, WebApplicationException {
				template.writeJson(output, context);
			}
		};
	}

	private ClassTemplate getFromCacheOrCreateTemplate(final Class<?> templateClass) {
		ClassTemplate template = templateCache.get(templateClass);
		if (template == null) {
			template = ClassTemplate.getOrCreate(templateClass, this);
			templateCache.put(templateClass, template);
		}
		return template;
	}

	ClassTemplate getFromCache(final Class<?> templateClass) {
		return templateCache.get(templateClass);
	}

	ClassTemplate putInCache(final Class<?> templateClass, final ClassTemplate classTemplate) {
		return templateCache.put(templateClass, classTemplate);
	}

	public StreamingOutput writeIterableWithTemplate(final Class<?> itemTemplateClass, final Object iterableContext) {
		final Template itemTemplate = new IterableTemplate(getFromCacheOrCreateTemplate(itemTemplateClass));
		return new StreamingOutput() {
			@Override
			public void write(final OutputStream output) throws IOException, WebApplicationException {
				itemTemplate.writeJson(output, iterableContext);
			}
		};
	}
}
