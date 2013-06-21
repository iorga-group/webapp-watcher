package com.iorga.iraj.json;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class FieldTemplate extends PropertyTemplate<Field, ContextCaller> {

	public FieldTemplate(final Field targetField, final JsonWriter jsonWriter) {
		super(targetField, jsonWriter);
	}

	@Override
	protected String getPropertyName(final Field targetField) {
		return targetField.getName();
	}

	@Override
	protected Type getPropertyType(final Field targetField) {
		return targetField.getGenericType();
	}

	@Override
	protected ContextCaller createContextCaller(final Field targetField) {
		return ContextCallerUtils.createContextCaller(targetField.getName(), targetField, targetField.getDeclaringClass());
	}

}
