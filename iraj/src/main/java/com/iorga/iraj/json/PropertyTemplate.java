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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;

import com.google.common.reflect.TypeToken;
import com.iorga.iraj.annotation.ContextPath;
import com.iorga.iraj.annotation.IgnoreProperty;
import com.iorga.iraj.annotation.JsonProperty;
import com.iorga.iraj.annotation.TargetType;

public abstract class PropertyTemplate<T extends AnnotatedElement & Member, C extends ContextCaller> implements Template {
	protected final byte[] jsonPropertyName;
	protected final Template propertyTemplate;
	protected final C contextCaller;

	public PropertyTemplate(final T targetAnnotatedMember, final JsonWriter jsonWriter) {
		final JsonProperty jsonProperty = targetAnnotatedMember.getAnnotation(JsonProperty.class);
		if (jsonProperty != null) {
			jsonPropertyName = jsonProperty.value().getBytes();
		} else {
			jsonPropertyName = getPropertyName(targetAnnotatedMember).getBytes();
		}
		contextCaller = createContextCaller(targetAnnotatedMember);

		// Now compute the target type
		final Type targetType;
		final TargetType targetTypeAnnotation = targetAnnotatedMember.getAnnotation(TargetType.class);
		if (targetTypeAnnotation != null) {
			targetType = TemplateUtils.getGenericType(targetTypeAnnotation);
		} else {
			targetType = getPropertyType(targetAnnotatedMember);
		}
		final Type sourceType = contextCaller.getReturnType();
		final TypeToken<?> targetTypeToken = TypeToken.of(targetType);
		final TypeToken<?> sourceTypeToken = TypeToken.of(sourceType);

		if (targetTypeToken.isAssignableFrom(sourceTypeToken)) {
			// Same & compatible types, let's use ObjectValueTemplate (which uses Jackson to do the work)
			propertyTemplate = new ObjectValueTemplate();
		} else {
			// First, see if we have an Iterable (List, Set...) in order to compare the type argument value to see if we must create a template with it
			if (Iterable.class.isAssignableFrom(targetTypeToken.getRawType())) {
				// We've got Iterables on both side, so the propertyTemplate is a class one, which will convert from the sourceItemType to the targetItemType
				final Class<?> sourceItemClass = targetTypeToken.resolveType(Iterable.class.getTypeParameters()[0]).getRawType();
				propertyTemplate = new IterableTemplate(ClassTemplate.getOrCreate(sourceItemClass, jsonWriter));
			} else {
				// We've got different types on both sides and it's not an iterable, we can use the ClassTemplate
				propertyTemplate = ClassTemplate.getOrCreate(targetTypeToken.getRawType(), jsonWriter);
			}
		}
	}

	public static boolean isPropertyTemplate(final AnnotatedElement annotatedElement) {
		return annotatedElement.isAnnotationPresent(ContextPath.class) || annotatedElement.isAnnotationPresent(JsonProperty.class) || !annotatedElement.isAnnotationPresent(IgnoreProperty.class);
	}


	protected abstract C createContextCaller(final T targetAnnotatedMember);

	protected abstract String getPropertyName(T targetAnnotatedMember);

	protected abstract Type getPropertyType(T targetAnnotatedMember);

	@Override
	public void writeJson(final OutputStream output, final Object context) throws IOException, WebApplicationException {
		output.write('"');
		output.write(jsonPropertyName);
		output.write('"');
		output.write(':');
		propertyTemplate.writeJson(output, contextCaller.callContext(context));
	}
}
