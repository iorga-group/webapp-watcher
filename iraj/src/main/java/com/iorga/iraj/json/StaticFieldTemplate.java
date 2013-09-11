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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class StaticFieldTemplate extends FieldTemplate {

	public StaticFieldTemplate(final Field targetField, final JsonWriter jsonWriter) {
		super(targetField, jsonWriter);
	}

	@Override
	protected ContextCaller createContextCaller(final Field targetField) {
		targetField.setAccessible(true);
		return new ContextCaller() {
			@Override
			public Type getReturnType() {
				return targetField.getGenericType();
			}

			@Override
			public Object callContext(final Object context) {
				try {
					return targetField.get(null);
				} catch (final Exception e) {
					throw new IllegalStateException("Problem while trying to get static value from "+targetField, e);
				}
			}
		};
	}
}
