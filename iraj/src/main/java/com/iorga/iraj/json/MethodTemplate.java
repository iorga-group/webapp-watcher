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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.iraj.json.MethodTemplate.MethodContextCaller;

public class MethodTemplate extends PropertyTemplate<Method, MethodContextCaller> {
	private static final int PUBLIC_STATIC = Modifier.STATIC | Modifier.PUBLIC;
	protected final Method targetMethod;

	public MethodTemplate(final Method targetMethod, final JsonWriter jsonWriter) {
		super(targetMethod, jsonWriter);

		if ((targetMethod.getModifiers() & PUBLIC_STATIC) != PUBLIC_STATIC) {
			throw new IllegalArgumentException(targetMethod+" must be public static in order to be called without instantiating the template");
		}

		this.targetMethod = targetMethod;
	}

	public static class MethodContextCaller implements ContextCaller {
		protected final ContextCaller[] parameterCallers;
		protected final Method targetMethod;

		public MethodContextCaller(final Method targetMethod) {
			this.targetMethod = targetMethod;
			final Annotation[][] parameterAnnotations = targetMethod.getParameterAnnotations();
			parameterCallers = new ContextCaller[parameterAnnotations.length];

			for (int i = 0; i < parameterAnnotations.length; i++) {
				final Annotation[] parameterAnnotation = parameterAnnotations[i];

				// Create the annotatedParameter in order to work on it more simply
				final Map<Class<?>, Annotation> parameterAnnotationMap = Maps.newHashMap();
				for (final Annotation annotation : parameterAnnotation) {
					parameterAnnotationMap.put(annotation.getClass(), annotation);
				}

				final int index = i;
				final AnnotatedElement annotatedParameter = new AnnotatedElement() {
					@Override
					public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
						return parameterAnnotationMap.containsKey(annotationClass);
					}

					@Override
					public Annotation[] getDeclaredAnnotations() {
						return parameterAnnotation;
					}

					@Override
					public Annotation[] getAnnotations() {
						return parameterAnnotation;
					}

					@SuppressWarnings("unchecked")
					@Override
					public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
						return (T) parameterAnnotationMap.get(annotationClass);
					}

					@Override
					public String toString() {
						return "Argument "+index+" for "+targetMethod;
					}
				};

				// determine if we must create a complex context caller (@ContextPath has been specified) or a simple one which will just retrieve the context
				if (annotatedParameter.isAnnotationPresent(ContextPath.class)) {
					// this is a complex context caller
					parameterCallers[i] = ContextCallerUtils.createContextCaller(getPropertyNameFromMethod(targetMethod), annotatedParameter, targetMethod.getDeclaringClass());
				} else {
					// No @ContextPath, we just want the context to be passed to the target method
					parameterCallers[i] = ContextCallerUtils.createOnlyReturnContextCaller(targetMethod.getDeclaringClass());
				}
			}
		}

		@Override
		public Object callContext(final Object context) {
			// Will call all the arguments
			final Object[] parameterValues = new Object[parameterCallers.length];
			for (int i = 0; i < parameterCallers.length; i++) {
				parameterValues[i] = parameterCallers[i].callContext(context);
			}
			try {
				return targetMethod.invoke(null, parameterValues);
			} catch (final Exception e) {
				throw new IllegalStateException("Problem while calling "+targetMethod, e);
			}
		}

		@Override
		public Type getReturnType() {
			return targetMethod.getGenericReturnType();
		}
	}

	@Override
	protected MethodContextCaller createContextCaller(final Method targetMethod) {
		return new MethodContextCaller(targetMethod);
	}

	@Override
	protected String getPropertyName(final Method targetMethod) {
		return getPropertyNameFromMethod(targetMethod);
	}

	private static String getPropertyNameFromMethod(final Method targetMethod) {
		String name = targetMethod.getName();
		if (name.startsWith("get")) {
			name = name.substring(3);
		} else if (name.startsWith("is")) {
			name = name.substring(2);
		}
		return StringUtils.uncapitalize(name);
	}

	@Override
	protected Type getPropertyType(final Method targetMethod) {
		return targetMethod.getGenericReturnType();
	}

	public static boolean isMethodTemplate(final Method targetMethod) {
		return PropertyTemplate.isPropertyTemplate(targetMethod) && (targetMethod.getModifiers() & PUBLIC_STATIC) == PUBLIC_STATIC;
	}
}
