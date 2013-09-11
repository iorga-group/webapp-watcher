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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;

public class ClassTemplate implements Template {
	private static final byte[] NULL_BYTES = "null".getBytes();

	private final List<PropertyTemplate<?, ?>> templatesToCall = new LinkedList<PropertyTemplate<?, ?>>();

	public static ClassTemplate getOrCreate(final Class<?> targetClass, final JsonWriter jsonWriter) {
		ClassTemplate classTemplate = jsonWriter.getFromCache(targetClass);
		if (classTemplate == null) {
			// that class template is not in cache, let's create a new one
			classTemplate = new ClassTemplate(targetClass, jsonWriter);
		}
		return classTemplate;
	}

	private ClassTemplate(final Class<?> targetClass, final JsonWriter jsonWriter) {
		jsonWriter.putInCache(targetClass, this);
		processClass(targetClass, jsonWriter);
	}

	private void processClass(final Class<?> targetClass, final JsonWriter jsonWriter) {
		final Class<?> superclass = targetClass.getSuperclass();
		if (superclass != null && TemplateUtils.isTemplate(superclass)) {
			processClass(superclass, jsonWriter);
		}
		for(final Field targetField : targetClass.getDeclaredFields()) {
			if (PropertyTemplate.isPropertyTemplate(targetField)) {
				if ((targetField.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
					// this is a static field
					templatesToCall.add(new StaticFieldTemplate(targetField, jsonWriter));
				} else {
					templatesToCall.add(new FieldTemplate(targetField, jsonWriter));
				}
			}
		}
		for(final Method targetMethod : targetClass.getDeclaredMethods()) {
			if (MethodTemplate.isMethodTemplate(targetMethod)) {
				templatesToCall.add(new MethodTemplate(targetMethod, jsonWriter));
			}
		}
	}

	@Override
	public void writeJson(final OutputStream output, final Object context) throws IOException, WebApplicationException {
		if (context != null) {
			output.write('{');
			boolean first = true;
			for (final Template template : templatesToCall) {
				if (first) {
					first = false;
				} else {
					output.write(',');
				}
				template.writeJson(output, context);
			}
			output.write('}');
		} else {
			output.write(NULL_BYTES);
		}
	}

}
