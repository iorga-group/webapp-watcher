package com.iorga.iraj.json;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextParams;
import com.iorga.iraj.annotation.ContextPath;

public class ContextParamsContextCaller implements ContextCaller {
	private final String contextMapKey;
	private final ContextCaller nextContextCaller;
	private final Type returnType;

	public ContextParamsContextCaller(final String elementName, final AnnotatedElement annotatedElement, final Class<?> declaringClass, final ContextParams contextParamsAnnotation) {
		final ContextPath contextPathAnnotation = annotatedElement.getAnnotation(ContextPath.class);
		// First we have to know what is the first context path part of the target annotated member
		final String nextPath;
		if (contextPathAnnotation != null) {
			// the context path is not null, let's take the first element as a key for the context map
			final String contextPath = contextPathAnnotation.value();
			contextMapKey = StringUtils.substringBefore(contextPath, ".");
			nextPath = StringUtils.substringAfter(contextPath, ".");
		} else {
			// No @ContextPath, let's take the elementName
			contextMapKey = elementName;
			nextPath = null;
		}

		// Now search the @ContextParam corresponding to the targetAnnotatedMember
		ContextCaller nextContextCaller = null;
		Type returnType = null;
		for (final ContextParam contextParam : contextParamsAnnotation.value()) {
			String contextName = contextParam.name();
			final Class<?> contextClass = contextParam.value();
			if (StringUtils.isBlank(contextName)) {
				contextName = StringUtils.uncapitalize(contextClass.getSimpleName());
			}
			if (contextMapKey.equals(contextName)) {
				// we have our @ContextParam now let's try to know if there is a "nextContextCaller"
				if (StringUtils.isEmpty(nextPath)) {
					// finished to handle the path, there is no other caller
					nextContextCaller = null;
					returnType = TemplateUtils.getGenericType(contextParam);
				} else {
					nextContextCaller = new ObjectContextCaller(contextClass, nextPath);
					returnType = nextContextCaller.getReturnType();
				}
				break;
			}
		}
		if (returnType == null) {
			throw new IllegalArgumentException("Couln't find the @ContextParam corresponding to "+contextMapKey+" in "+declaringClass);
		} else {
			this.nextContextCaller = nextContextCaller;
			this.returnType = returnType;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object callContext(final Object context) {
		final Object mappedObject = ((Map<String, Object>)context).get(contextMapKey);
		if (nextContextCaller != null && mappedObject != null) {
			return nextContextCaller.callContext(mappedObject);
		} else {
			return mappedObject;
		}
	}

	@Override
	public Type getReturnType() {
		return returnType;
	}
}
