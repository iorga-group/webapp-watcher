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
import java.lang.reflect.Type;
import java.util.Map;

import org.jboss.weld.util.reflection.ParameterizedTypeImpl;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextParams;

public class ContextCallerUtils {
	public static ContextCaller createContextCaller(final String elementName, final AnnotatedElement annotatedElement, final Class<?> declaringClass) {
		// First, determine the source type of the context caller
		final ContextParams contextParamsAnnotation = declaringClass.getAnnotation(ContextParams.class);
		if (contextParamsAnnotation != null) {
			// We call it from a context map, let's create such a ContextCaller
			return new ContextParamsContextCaller(elementName, annotatedElement, declaringClass, contextParamsAnnotation);
		} else {
			// We call it from the context directly, the type is the ContextParam type
			final ContextParam contextParamAnnotation = declaringClass.getAnnotation(ContextParam.class);
			if (contextParamAnnotation == null) {
				throw new IllegalArgumentException("Can't find @ContextParam or @ContextParams on type "+declaringClass.getName()+" for "+annotatedElement);
			}
			return new ObjectContextCaller(elementName, annotatedElement, contextParamAnnotation);
		}
	}

	public static Type getContextType(final Class<?> declaringClass) {
		final ContextParams contextParamsAnnotation = declaringClass.getAnnotation(ContextParams.class);
		if (contextParamsAnnotation != null) {
			return new ParameterizedTypeImpl(Map.class, new Class[] {String.class, Object.class}, declaringClass);
		} else {
			final ContextParam contextParamAnnotation = declaringClass.getAnnotation(ContextParam.class);
			if (contextParamAnnotation != null) {
				return TemplateUtils.getGenericType(contextParamAnnotation);
			} else {
				throw new IllegalArgumentException("Couldn't find @ContextParams nor @ContextParam on "+declaringClass);
			}
		}
	}

	public static ContextCaller createOnlyReturnContextCaller(final Class<?> declaringClass) {
		return new OnlyReturnContextCaller(getContextType(declaringClass));
	}
}
