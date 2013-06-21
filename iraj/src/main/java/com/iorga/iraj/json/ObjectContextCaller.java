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
