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
