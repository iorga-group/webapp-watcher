package com.iorga.webappwatcher.util;

public interface ParameterSetter<T, V> {
	Class<T> getOwnerClass();
	Class<V> getFieldClass();
	String getFieldName();
	boolean isFieldSetIfEmpty();
	boolean isFieldSetIfBlank();
	void setField(T owner, V value);
	void setFieldFromString(T owner, String value);
	V convertFromString(final String value);
}