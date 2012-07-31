package com.iorga.webappwatcher.util;

public interface ParameterHandler<T, V> {
	Class<T> getOwnerClass();
	Class<V> getFieldClass();
	String getFieldName();
	boolean isFieldSetIfEmpty();
	boolean isFieldSetIfBlank();
	void setFieldValue(T owner, V value);
	void setFieldStringValue(T owner, String value);
	V getFieldValue(T owner);
	String getFieldStringValue(T owner);
}