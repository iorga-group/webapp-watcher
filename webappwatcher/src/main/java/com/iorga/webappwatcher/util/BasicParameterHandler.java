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
package com.iorga.webappwatcher.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

public class BasicParameterHandler<T, V> implements ParameterHandler<T, V> {
	private final Class<T> ownerClass;
	private final Class<V> fieldClass;
	private final String fieldName;
	private Method writeMethod;
	private Method readMethod;

	public BasicParameterHandler(final Class<T> ownerClass, final String fieldName, final Class<V> fieldClass) {
		this.ownerClass = ownerClass;
		this.fieldClass = fieldClass;
		this.fieldName = fieldName;
		initAccessorMethods(findFieldPropertyDescriptor(fieldName));
	}

	@SuppressWarnings("unchecked")
	public BasicParameterHandler(final Class<T> ownerClass, final String fieldName) {
		this.ownerClass = ownerClass;
		this.fieldName = fieldName;
		final PropertyDescriptor fieldPropertyDescriptor = findFieldPropertyDescriptor(fieldName);
		this.fieldClass = (Class<V>) fieldPropertyDescriptor.getPropertyType();

		initAccessorMethods(fieldPropertyDescriptor);
	}

	private void initAccessorMethods(final PropertyDescriptor fieldPropertyDescriptor) {
		this.writeMethod = fieldPropertyDescriptor.getWriteMethod();
		this.readMethod = fieldPropertyDescriptor.getReadMethod();
		if (this.writeMethod == null) {
			throw new IllegalStateException("Couldn't find setter for "+ownerClass+"."+fieldName);
		}
		if (this.readMethod == null) {
			throw new IllegalStateException("Couldn't find getter for "+ownerClass+"."+fieldName);
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
	public void setFieldValue(final T owner, final V value) {
		try {
			writeMethod.invoke(owner, value);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Couldn't write "+value+" on "+owner, e);
		}
	}

	@Override
	public void setFieldStringValue(final T owner, final String value) {
		if (!isFieldSetIfBlank()) {
			if (StringUtils.isBlank(value)) {
				return;
			}
		} else if (!isFieldSetIfEmpty()) {
			if (StringUtils.isEmpty(value)) {
				return;
			}
		}
		setFieldValue(owner, convertFromString(value));
	}

	@SuppressWarnings("unchecked")
	protected V convertFromString(final String value) {
		return (V) ConvertUtils.convert(value, getFieldClass());
	}

	protected String convertToString(final V value) {
		return ConvertUtils.convert(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getFieldValue(final T owner) {
		try {
			return (V) readMethod.invoke(owner);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Couldn't read "+fieldName+" from "+owner, e);
		}
	}

	@Override
	public String getFieldStringValue(final T owner) {
		return convertToString(getFieldValue(owner));
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