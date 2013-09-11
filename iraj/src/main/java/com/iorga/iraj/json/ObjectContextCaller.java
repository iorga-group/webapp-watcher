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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextPath;

public class ObjectContextCaller implements ContextCaller {
	private Method getter;
	private final ContextCaller nextContextCaller;

	public ObjectContextCaller(final String elementName, final AnnotatedElement annotatedElement, final ContextParam contextParam) {
		this(contextParam.value(), getContextPath(elementName, annotatedElement));
	}

	private static String getContextPath(final String elementName, final AnnotatedElement annotatedElement) {
		final ContextPath contextPathAnnotation = annotatedElement.getAnnotation(ContextPath.class);
		if (contextPathAnnotation != null) {
			return contextPathAnnotation.value();
		} else {
			return elementName;
		}
	}

	public ObjectContextCaller(final Class<?> currentClass, final String currentContextPath) throws SecurityException {
		final String firstContextPathPart = StringUtils.substringBefore(currentContextPath, ".");

		try {
			getter = currentClass.getMethod("get"+StringUtils.capitalize(firstContextPathPart));
		} catch (final NoSuchMethodException e) {
			try {
				getter = currentClass.getMethod("is"+StringUtils.capitalize(firstContextPathPart));
			} catch (final NoSuchMethodException e1) {
				throw new IllegalArgumentException("No getter for "+firstContextPathPart+" on "+currentClass);
			}
		}

		if (!firstContextPathPart.equals(currentContextPath)) {
			// the path is not finished to handle
			nextContextCaller = new ObjectContextCaller(getter.getReturnType(), StringUtils.substringAfter(currentContextPath, "."));
		} else {
			nextContextCaller = null;
		}
	}

	@Override
	public Object callContext(final Object context) {
		try {
			if (context != null) {
				final Object nextContext = getter.invoke(context);
				if (nextContext != null && nextContextCaller != null) {
					return nextContextCaller.callContext(nextContext);
				} else {
					return nextContext;
				}
			} else {
				// If the current object is null, let's return null in order to avoid NPE
				return null;
			}
		} catch (final Exception e) {
			throw new IllegalStateException("Couldn't call "+getter, e);
		}
	}

	@Override
	public Type getReturnType() {
		return nextContextCaller != null ? nextContextCaller.getReturnType() : getter.getGenericReturnType();
	}

}
