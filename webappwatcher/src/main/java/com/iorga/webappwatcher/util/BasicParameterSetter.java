package com.iorga.webappwatcher.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

public class BasicParameterSetter<T, V> implements ParameterSetter<T, V> {
	private final Class<T> ownerClass;
	private final Class<V> fieldClass;
	private final String fieldName;
	private final Method writeMethod;

	public BasicParameterSetter(final Class<T> ownerClass, final String fieldName, final Class<V> fieldClass) {
		this.ownerClass = ownerClass;
		this.fieldClass = fieldClass;
		this.fieldName = fieldName;
		this.writeMethod = findFieldPropertyDescriptor(fieldName).getWriteMethod();
	}

	@SuppressWarnings("unchecked")
	public BasicParameterSetter(final Class<T> ownerClass, final String fieldName) {
		this.ownerClass = ownerClass;
		this.fieldName = fieldName;
		final PropertyDescriptor fieldPropertyDescriptor = findFieldPropertyDescriptor(fieldName);
		this.writeMethod = fieldPropertyDescriptor.getWriteMethod();
		this.fieldClass = (Class<V>) fieldPropertyDescriptor.getPropertyType();

		if (this.writeMethod == null) {
			throw new IllegalStateException("Couldn't find setter for "+ownerClass+"."+fieldName);
		}
	}

	private PropertyDescriptor findFieldPropertyDescriptor(final String fieldName) {
		final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(ownerClass);
		for (final PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (fieldName.equals(propertyDescriptor.getName())) {
				return propertyDescriptor;
			}
		}
		throw new IllegalStateException("Couldn't find getter/setter for "+ownerClass+"."+fieldName);
	}

	@Override
	public void setField(final T owner, final V value) {
		try {
			writeMethod.invoke(owner, value);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Couldn't write "+value+" on "+owner, e);
		}
	}

	@Override
	public void setFieldFromString(final T owner, final String value) {
		if (!isFieldSetIfBlank()) {
			if (StringUtils.isBlank(value)) {
				return;
			}
		} else if (!isFieldSetIfEmpty()) {
			if (StringUtils.isEmpty(value)) {
				return;
			}
		}
		setField(owner, convertFromString(value));
	}

	@Override
	@SuppressWarnings("unchecked")
	public V convertFromString(final String value) {
		return (V) ConvertUtils.convert(value, getFieldClass());
	}

	@Override
	public Class<T> getOwnerClass() {
		return ownerClass;
	}

	@Override
	public Class<V> getFieldClass() {
		return fieldClass;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return false;
	}

	@Override
	public boolean isFieldSetIfEmpty() {
		return false;
	}
}