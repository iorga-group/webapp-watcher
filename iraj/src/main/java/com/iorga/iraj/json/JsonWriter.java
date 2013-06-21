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
