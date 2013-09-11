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
