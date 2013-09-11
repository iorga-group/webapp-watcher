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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;


public class InstanceSetParameterHandler<T, I> extends BasicParameterHandler<T, Set<I>> {

	private final static Logger log = LoggerFactory.getLogger(InstanceSetParameterHandler.class);

	private final Map<Class<?>, Object> parameterscontext;

	public InstanceSetParameterHandler(final Class<T> ownerClass, final String fieldName, final Map<Class<?>, Object> parameterscontext) {
		super(ownerClass, fieldName);
		this.parameterscontext = parameterscontext;
	}



	@SuppressWarnings("unchecked")
	@Override
	protected Set<I> convertFromString(final String value) {
		final Set<I> instances = Sets.newHashSet();
		final String[] includes = value.split(",");
		for (final String className : includes) {
			if (StringUtils.isNotBlank(className)) {
				try {
					instances.add((I) parameterscontext.get(Class.forName(className)));
				} catch (final ClassNotFoundException e) {
					log.warn("Can't get instance of "+className, e);
				}
			}
		}
		return instances;
	}

	@Override
	protected String convertToString(final Set<I> value) {
		if (value == null) {
			return "";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		boolean first = true;
		for (final I instance : value) {
			if (!first) {
				stringBuilder.append(',');
			} else {
				first = false;
			}
			stringBuilder.append(instance.getClass().getName());
		}
		return stringBuilder.toString();
	}

	@Override
	public boolean isFieldSetIfBlank() {
		return true;
	}
}
