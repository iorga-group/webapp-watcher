package com.iorga.iraj.json;

import java.lang.reflect.Type;

import org.jboss.weld.util.reflection.ParameterizedTypeImpl;

import com.iorga.iraj.annotation.ContextParam;
import com.iorga.iraj.annotation.ContextParams;
import com.iorga.iraj.annotation.TargetType;

public class TemplateUtils {
	public static boolean isTemplate(final Class<?> klass) {
		return klass.isAnnotationPresent(ContextParams.class) || klass.isAnnotationPresent(ContextParam.class);
	}

	public static Type getGenericType(final ContextParam contextParam) {
		return getGenericType(
			contextParam.value(),
			contextParam.parameterizedArguments()
		);
	}

	public static Type getGenericType(final TargetType targetType) {
		return getGenericType(
			targetType.value(),
			targetType.parameterizedArguments()
		);
	}

	public static Type getGenericType(final Class<?> rawClass, final Class<?>[] parameterizedArguments) {
		if (parameterizedArguments == null || parameterizedArguments.length == 0) {
			return rawClass;
		} else {
			return new ParameterizedTypeImpl(
				rawClass,
				parameterizedArguments,
				rawClass.getDeclaringClass());
		}
	}
}
